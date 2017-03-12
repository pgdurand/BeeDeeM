/* Copyright (C) 2007-2017 Ludovic Antin
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
import java.util.LinkedHashMap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;

/**
 * A sequence validator which paginate with max X sequences per batch
 * 
 * @author Ludovic Antin
 * 
 */
public class SequenceValidatorPaginate extends SequenceValidatorAbstract {

  private static final Log             LOGGER                     = LogFactory
                                                                      .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                                          + ".SequenceValidatorSubset");

  private int                          paginateSize               = 1;
  private boolean                      isActive                   = true;
  private int                          currentSubsetNumber        = 1;
  private File                         currentSubsetFile          = null;
  private String                       inputAbsolutePath;
  private BufferedOutputStream         output                     = null;
  private LinkedHashMap<File, Integer> createdBatches             = new LinkedHashMap<File, Integer>();
  private byte[]                       newLine                    = new String(
                                                                      "\n")
                                                                      .getBytes();
  private long                         totalSizeWritten           = 0;
  private long                         inputLength                = 0;
  private int                          nbSequencesInCurrentSubset = 0;

  public SequenceValidatorPaginate(int paginateSize) {
    this.paginateSize = paginateSize;
  }

  public LinkedHashMap<File, Integer> getCreatedBatches() {
    return this.createdBatches;
  }

  public Integer getNbSequences(File batch) {
    return createdBatches.get(batch);
  }

  @Override
  public void initialise(File input) {
    this.inputLength = input.length();
    if ((input == null) || (this.paginateSize <= 1)) {
      this.isActive = false;
    } else {
      this.currentSubsetNumber = 0;
      this.inputAbsolutePath = input.getAbsolutePath();
      this.getNextBatchFile();
    }
  }

  private boolean closeCurrentBatch() {
    try {
      if ((this.output != null) && (this.nbSequencesInCurrentSubset > 0)) {
        this.output.flush();
        this.createdBatches.put(this.currentSubsetFile,
            this.nbSequencesInCurrentSubset);
      }
    } catch (IOException e) {
      LOGGER.warn("Unable to close batch of '"
          + this.currentSubsetFile.getAbsolutePath() + "' : " + e.getMessage());
      return false;
    }
    IOUtils.closeQuietly(output);
    return true;
  }

  private void getNextBatchFile() {
    try {
      if (this.totalSizeWritten < this.inputLength) {
        do {
          this.nbSequencesInCurrentSubset = 0;
          this.currentSubsetNumber++;
          this.currentSubsetFile = new File(inputAbsolutePath + "_"
              + this.currentSubsetNumber);
        } while (this.currentSubsetFile.exists());
        output = new BufferedOutputStream(Files.newOutputStream(
            this.currentSubsetFile.toPath(), StandardOpenOption.CREATE));
      }
    } catch (IOException e) {
      LOGGER.warn("Unable to create batch of '" + this.inputAbsolutePath
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
    if ((this.nbSequencesInCurrentSubset == this.paginateSize)) {
      if (this.closeCurrentBatch()) {
        this.getNextBatchFile();
      } else {
        isActive = false;
      }
    }
    return true;
  }

  @Override
  public void finish() {
    this.closeCurrentBatch();
  }

  @Override
  public boolean startSequence() {
    return true;
  }

  @Override
  public boolean analyseLine(StringBuilder line) {
    if (this.isActive) {
      byte[] lineBytes = line.toString().getBytes();
      try {
        this.output.write(lineBytes);
        this.output.write(newLine);
        this.totalSizeWritten += lineBytes.length + newLine.length;
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
