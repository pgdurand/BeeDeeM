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
package bzh.plealog.dbmirror.lucenedico.eggnog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import bzh.plealog.dbmirror.indexer.ParserMonitor;
import bzh.plealog.dbmirror.indexer.eggnog.EggNogEntry;
import bzh.plealog.dbmirror.indexer.eggnog.EggNogPreparator;
import bzh.plealog.dbmirror.indexer.eggnog.EggNogPropertyFile;
import bzh.plealog.dbmirror.lucenedico.DicoParsable;
import bzh.plealog.dbmirror.lucenedico.DicoParserException;
import bzh.plealog.dbmirror.lucenedico.DicoStorageSystem;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * This class is used to create the EggNog dico The file parsed is a lucene
 * index zipped created by EggNogIndexer
 * 
 * @see bzh.plealog.dbmirror.indexer.eggnog.EggNogPreparator
 * 
 * @author Ludovic Antin
 * 
 */
public class EggNogDicoParser implements DicoParsable {

  private static final Logger       LOGGER                  = LogManager.getLogger(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                                    + ".EggNogDicoParser");

  private static final String       FILE_PREFIX_FUNCCAT     = ".funccat.txt";
  private static final String       FILE_PREFIX_DESCRIPTION = ".description.txt";

  // the format for an entry in the dictionary
  public static final MessageFormat entryFormat             = new MessageFormat(
                                                                "[{0}] {1} ({2} prot. in {3} sp.)");

  private int                       nbEntries               = 0;
  @SuppressWarnings("unused")
  private boolean                   verbose                 = false;
  private ParserMonitor             monitor                 = null;

  @Override
  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  @Override
  public void setParserMonitor(ParserMonitor pm) {
    this.monitor = pm;
  }

  @Override
  public int getTerms() {
    return this.nbEntries;
  }

  @Override
  /**
   * We don't care about the file parameter because all data were prepared
   * by the EggNogIndexer class (called using a depends databank in the dsc file)
   */
  public void parse(String file, DicoStorageSystem ss)
      throws DicoParserException {

    if ((EggNogPreparator.membersDirectory == null)
        || (!EggNogPreparator.membersDirectory.exists())) {
      throw new DicoParserException("Unable to read members files");
    }

    BufferedReader reader = null;
    String line = null;
    EggNogEntry currentEntry = null;
    EggNogEntry previousEntry = null;
    int nbProteins = 0;
    int nbSpecies = 0;

    File descriptionFile = null;
    EggNogPropertyFile descriptionProperties = null;
    File funcCatFile = null;
    EggNogPropertyFile funcCatProperties = null;

    // the dictionary data
    String entryId = null;
    Object[] entryData = new Object[4];

    // run through all members files
    File[] allMembers = EggNogPreparator.membersDirectory.listFiles();

    if (monitor != null) {
      monitor.startProcessingFile(EggNogPreparator.membersDirectory.getName(),
          allMembers.length);
    }

    for (File memberFile : allMembers) {
      LoggerCentral
          .info(LOGGER, "Parsing file " + memberFile.getAbsolutePath());

      // load the description file for this member file
      descriptionFile = new File(EggNogPreparator.descriptionsDirectory,
          FilenameUtils.getBaseName(FilenameUtils.getBaseName(memberFile
              .getName())) + FILE_PREFIX_DESCRIPTION);
      if (!descriptionFile.exists()) {
        throw new DicoParserException(descriptionFile.getAbsolutePath()
            + " does not exists");
      }
      descriptionProperties = new EggNogPropertyFile(descriptionFile);
      descriptionProperties.load();

      // load the funccat file for this member file
      funcCatFile = new File(EggNogPreparator.funcCatsDirectory,
          FilenameUtils.getBaseName(FilenameUtils.getBaseName(memberFile
              .getName())) + FILE_PREFIX_FUNCCAT);
      if (!funcCatFile.exists()) {
        throw new DicoParserException(funcCatFile.getAbsolutePath()
            + " does not exists");
      }
      funcCatProperties = new EggNogPropertyFile(funcCatFile);
      funcCatProperties.load();

      try {
        // run through the member file
        reader = new BufferedReader(new InputStreamReader(new FileInputStream(
            memberFile), "UTF-8"));
        previousEntry = null;
        nbProteins = 0;
        nbSpecies = 0;

        while (true) {
          // create a new entry
          try {
            line = reader.readLine();
            if ((line != null) && (line.startsWith("#"))) { // this is the
                                                            // header
              continue;
            }
            currentEntry = new EggNogEntry(line);

            if ((line == null) // the last entry
                || ((previousEntry != null) && (!previousEntry.getNogName()
                    .equals(currentEntry.getNogName())))) { // next nog

              // entry id example : bactNOG00029
              entryId = previousEntry.getNogName();
              // entry data example : [R] L-Ascorbate 6-phosphate lactonase (122
              // prot. in 122 sp.)
              entryData[0] = funcCatProperties.get(previousEntry.getNogName());
              entryData[1] = descriptionProperties.get(previousEntry
                  .getNogName());
              entryData[2] = nbProteins;
              entryData[3] = nbSpecies;

              // add the entry in the dictionary
              ss.addEntry(entryId, entryFormat.format(entryData));
              if (monitor != null) {
                monitor.seqFound(entryId, entryFormat.format(entryData),
                    memberFile.getName(), 1, 1, false);
              }
              this.nbEntries++;

              // reset data
              nbProteins = 1; // 1 for the current entry
              nbSpecies = 1;// 1 for the current entry
              entryData = new Object[4];

            } else { // always in the same nog : check for nb proteins and
                     // species

              if (previousEntry == null) { // the first one
                nbProteins++;
                nbSpecies++;
              } else {
                if (!previousEntry.getProteinId().equals(
                    currentEntry.getProteinId())) { // a new protein
                  nbProteins++;
                }
                if (!previousEntry.getTaxonId().equals(
                    currentEntry.getTaxonId())) { // a new species
                  nbSpecies++;
                }
              }
            }
            // enf of file ?
            if (line == null) {
              break;
            }
            // process aborted ?
            if (LoggerCentral.processAborted()) {
              break;
            }
            previousEntry = currentEntry;

          } catch (Exception ex) {
            if (!line.startsWith("#")) { // do not log the first line
              LoggerCentral.warn(LOGGER, "Unable to create entry for '" + line
                  + "' : " + ex.getMessage());
            }
          }

        }
      } catch (Exception ex) {
        throw new DicoParserException(ex.getMessage());
      } finally {
        IOUtils.closeQuietly(reader);

      }
    }
    if (monitor != null) {
      monitor.stopProcessingFile(file, this.nbEntries);
    }
  }
}
