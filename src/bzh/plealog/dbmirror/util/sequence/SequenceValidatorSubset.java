/* Copyright (C) 2007-2021 Ludovic Antin
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  You may obtain a copy of the License at
 *
 *     https://www.gnu.org/licenses/agpl-3.0.txt
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 */
package bzh.plealog.dbmirror.util.sequence;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;

/**
 * A sequence validator which makes x subsets of the input files
 * 
 * @author Ludovic Antin
 * 
 */
public class SequenceValidatorSubset extends SequenceValidatorAbstract {

  private static final Logger       LOGGER                     = LogManager.getLogger(
      DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY + ".SequenceValidatorSubset");

  private int                    nbSubsets                  = 1;
  private boolean                isActive                   = true;
  private long                   maxSubsetSize              = 0;
  private int                    currentSubsetNumber        = 1;
  private File                   currentSubsetFile          = null;
  private String                 inputAbsolutePath;
  private long                   currentSubsetSize          = 0;
  private BufferedOutputStream   output                     = null;
  private HashMap<File, Integer> createdSubsets             = new HashMap<File, Integer>();
  private byte[]                 newLine                    = new String("\n")
                                                                .getBytes();
  private long                   totalSizeWritten           = 0;
  private long                   inputLength                = 0;
  private int                    nbSequencesInCurrentSubset = 0;

  public SequenceValidatorSubset(int nbSubsets) {
    this.nbSubsets = nbSubsets;
  }

  public List<File> getCreatedSubsets() {
    return new ArrayList<File>(createdSubsets.keySet());
  }

  public Integer getNbSequences(File subset) {
    return createdSubsets.get(subset);
  }

  @Override
  public void initialise(File input) {
    if ((input == null) || (this.nbSubsets <= 1)) {
      this.isActive = false;
    } else {
      this.inputLength = input.length();
      this.maxSubsetSize = this.inputLength / this.nbSubsets;
      if (this.maxSubsetSize < 100) // too small
      {
        isActive = false;
      } else {
        this.currentSubsetNumber = 0;
        this.inputAbsolutePath = input.getAbsolutePath();
        this.getNextSubsetFile();
      }
    }
  }

  private boolean closeCurrentSubset() {
    try {
      if ((this.output != null) && (this.currentSubsetSize > 0)) {
        this.output.flush();
        this.createdSubsets.put(this.currentSubsetFile,
            this.nbSequencesInCurrentSubset);
      }
    } catch (IOException e) {
      LOGGER.warn("Unable to close subset of '"
          + this.currentSubsetFile.getAbsolutePath() + "' : " + e.getMessage());
      return false;
    }
    IOUtils.closeQuietly(output);
    return true;
  }

  private void getNextSubsetFile() {
    try {
      if (this.totalSizeWritten < this.inputLength) {
        do {
          this.nbSequencesInCurrentSubset = 0;
          this.currentSubsetNumber++;
          this.currentSubsetFile = new File(inputAbsolutePath + "_"
              + this.currentSubsetNumber);
        } while (this.currentSubsetFile.exists());
        this.currentSubsetSize = 0;
        output = new BufferedOutputStream(Files.newOutputStream(
            this.currentSubsetFile.toPath(), StandardOpenOption.CREATE));
      }
    } catch (IOException e) {
      LOGGER.warn("Unable to create subsets of '" + this.inputAbsolutePath
          + "' : " + e.getMessage());
      isActive = false;
    }
  }

  @Override
  public boolean startEntry() {
    this.nbSequencesInCurrentSubset++;
    return true;
  }

  @Override
  public boolean stopEntry() {
    if ((this.currentSubsetSize >= this.maxSubsetSize)) {
      if (this.closeCurrentSubset()) {
        this.getNextSubsetFile();
      } else {
        isActive = false;
      }
    }
    return true;
  }

  @Override
  public void finish() {
    this.closeCurrentSubset();
  }

  @Override
  public boolean startSequence() {
    return true;
  }

  @Override
  public boolean analyseLine(StringBuilder line) {
    if (this.isActive) {
      byte[] lineBytes = line.toString().getBytes();
      this.currentSubsetSize += lineBytes.length;
      try {
        this.output.write(lineBytes);
        this.output.write(newLine);
        totalSizeWritten += lineBytes.length + newLine.length;
      } catch (IOException e) {
        LOGGER.warn("Unable to write data in '"
            + this.currentSubsetFile.getAbsolutePath() + "' : "
            + e.getMessage());
        this.isActive = false;
      }
    }
    return true;
  }

  @Override
  public boolean isActive() {
    return isActive;
  }

  @Override
  public String toParametersForUnitTask() {
    // nothing to do because this validator is not used by KDMS
    return null;
  }

}
