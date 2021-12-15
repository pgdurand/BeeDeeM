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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import bzh.plealog.dbmirror.indexer.eggnog.EggNogSequenceWriter;
import bzh.plealog.dbmirror.indexer.eggnog.EggNogSuperKingdoms;
import bzh.plealog.dbmirror.lucenedico.DicoTermQuerySystem;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptorUtils;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

public class SequenceValidatorEggNog extends SequenceValidatorAbstract {

  private static final Logger           LOGGER          = LogManager.getLogger(
      DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY + ".SequenceValidatorEggNog");

  // writers list
  private List<EggNogSequenceWriter> writers         = new ArrayList<EggNogSequenceWriter>();

  private boolean                    isHeader        = false;
  private String                     header;
  private boolean                    isSequence      = false;
  private StringBuilder              sequenceLetters = new StringBuilder();
  private String                     previousTaxonId = "";
  private String                     currentTaxPath  = "";
  private DicoTermQuerySystem        dicoSearcher    = DicoTermQuerySystem
                                                         .getDicoTermQuerySystem(DBDescriptorUtils
                                                             .getLocalDBMirrorConfig());

  /**
   * 
   * @param resultDirPath
   *          : where to store the uniprot formatted sequences file
   * 
   * @param superKingdoms
   *          : which super kingdoms are accepted and will result in a uniprot
   *          formatted file
   */
  public SequenceValidatorEggNog(String resultDirPath,
      EggNogSuperKingdoms... superKingdoms) {
    for (EggNogSuperKingdoms superKingdom : superKingdoms) {
      writers.add(new EggNogSequenceWriter(new File(resultDirPath, superKingdom
          .name() + ".dat"), superKingdom.taxonId));
    }
  }

  @Override
  public void finish() {
    for (EggNogSequenceWriter writer : writers) {
      writer.close();
    }
  }

  @Override
  public boolean startEntry() {
    isHeader = true;

    return true;
  }

  @Override
  public boolean stopEntry() {

    if (StringUtils.isNotBlank(this.currentTaxPath)) {
      for (EggNogSequenceWriter writer : writers) {
        if (writer.accept(this.currentTaxPath)) {
          try {
            writer.write(this.header, this.sequenceLetters.toString());
          } catch (Exception ex) {
            LoggerCentral.warn(LOGGER, "Unable to write'" + this.header
                + "' : " + ex.getMessage());
          }
        }
      }
    }
    return true;
  }

  @Override
  public boolean startSequence() {
    isSequence = true;
    sequenceLetters = new StringBuilder();
    return true;
  }

  @Override
  public boolean analyseLine(StringBuilder line) {
    if (this.isHeader) {
      // start at 1 to remove the '>'
      // and replace . by _ to avoid pb with '.' in headears @see
      // WWWSeqUtils.getIdAndDb
      this.header = line.toString().substring(1).replace('.', '_');
      try {
        this.extractTaxPath(this.header);
      } catch (Exception ex) {
        LoggerCentral.warn(LOGGER, "Unable to get tax path from '"
            + this.header + "' : " + ex.getMessage());
        return false;
      }

      this.isHeader = false;
    } else if (this.isSequence) {
      this.sequenceLetters.append(line);
    }

    return true;
  }

  @Override
  public boolean isActive() {
    // always active
    return true;
  }

  @Override
  public String toParametersForUnitTask() {
    // nothing to do because this validator is not used by command line
    return null;
  }

  /**
   * Get taxon path from header header format is <taxonId>.<sequenceId> example
   * : 515620.EUBELI_01191
   * 
   * @param header
   * 
   * @throws Exception
   */
  private void extractTaxPath(String header) throws Exception {

    String taxonId = header.substring(0, header.indexOf('_'));
    // is the same taxon than the previous sequence ?
    if (!taxonId.equals(this.previousTaxonId)) {
      this.previousTaxonId = taxonId;
      this.currentTaxPath = this.dicoSearcher.getTaxPathIds(taxonId, false);

      if (StringUtils.isBlank(this.currentTaxPath)) {
        LoggerCentral.warn(LOGGER, "Unable to get tax path for taxon id '"
            + this.previousTaxonId + "'");
      }
    }

  }

}
