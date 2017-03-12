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
package bzh.plealog.dbmirror.indexer.eggnog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import javax.swing.JProgressBar;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.fetcher.DBServerConfig;
import bzh.plealog.dbmirror.fetcher.UserProcessingMonitor;
import bzh.plealog.dbmirror.task.PTaskEngine;

import bzh.plealog.dbmirror.util.ant.PAntTasks;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.descriptor.DatabankFormat;
import bzh.plealog.dbmirror.util.log.LoggerCentral;
import bzh.plealog.dbmirror.util.sequence.SequenceFileManager;
import bzh.plealog.dbmirror.util.sequence.SequenceValidatorEggNog;

/**
 * Class used to prepare an eggnog databank : - untar all files - index which
 * stores the relations between an eggnog protein (sequence id) and the
 * associated nog with the positions. - translated sequences files from fasta to
 * uniprot
 * 
 * @author Ludovic Antin
 * 
 */
public class EggNogPreparator {

  private static final Log      LOGGER                = LogFactory
                                                          .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                              + ".EggNogPreparator");

  // where are decompressed all members files
  public static File            membersDirectory      = null;
  // where are decompressed all descriptions files
  public static File            descriptionsDirectory = null;
  // where are decompressed all functional categories files
  public static File            funcCatsDirectory     = null;
  // the lucene index which associates taxon ids and nogs+sequenceId
  public static EggNogIndex     reverseIndex          = null;
  // the fasta sequences file
  public static File            sequencesFile         = null;

  // a file indicates the the job has been done
  public static File            doneFile              = null;

  // the tar files which contains all members, descriptions and funccats txt
  // files
  private File                  membersTarFile        = null;
  private File                  descriptionsTarFile   = null;
  private File                  funcCatsTarFile       = null;
  // the reverse index directory
  private File                  indexDirectory        = null;

  private DBServerConfig        config                = null;

  private UserProcessingMonitor monitor;

  public EggNogPreparator(File membersTarFile, File descriptionsTarFile,
      File funcCatsTarFile, DBServerConfig config, UserProcessingMonitor monitor) {
    this.membersTarFile = membersTarFile;
    this.descriptionsTarFile = descriptionsTarFile;
    this.funcCatsTarFile = funcCatsTarFile;
    this.config = config;
    this.monitor = monitor;
  }

  /**
   * Untar all eggnog files
   */
  private void untarFiles() throws Exception {
    this.untarFile(EggNogPreparator.membersDirectory, this.membersTarFile);
    this.untarFile(EggNogPreparator.descriptionsDirectory,
        this.descriptionsTarFile);
    this.untarFile(EggNogPreparator.funcCatsDirectory, this.funcCatsTarFile);
  }

  /**
   * Untar one eggnog file
   * 
   * @param directory
   * @param tarFile
   * @throws Exception
   */
  private void untarFile(File directory, File tarFile) throws Exception {
    if (tarFile == null) {
      throw new Exception("No tar file provided");
    }

    if (directory == null) {
      throw new Exception("No directory provided to untar the file '"
          + tarFile.getAbsolutePath() + "'.");
    }

    if (directory.exists()) {
      FileUtils.deleteQuietly(directory);
    }
    LoggerCentral.info(LOGGER, "Untar " + tarFile.getAbsolutePath() + " to "
        + directory.getAbsolutePath());
    if (!PAntTasks.untar(tarFile.getAbsolutePath(),
        directory.getAbsolutePath())) {
      throw new Exception("Unable to untar '" + tarFile.getAbsolutePath()
          + "' in '" + directory.getAbsolutePath() + "'");
    }
  }

  /**
   * Create the lucene index used to convert sequences into UniProt format
   */
  private void createLuceneIndex() throws Exception {
    BufferedReader reader = null;
    String line = null;

    // remove the old one
    if (indexDirectory.exists()) {
      FileUtils.deleteQuietly(indexDirectory);
    }

    try {
      reverseIndex.open();

      File[] allMembers = EggNogPreparator.membersDirectory.listFiles();

      // run through each file to create an index entry and to add it in the
      // lucene index
      for (int i = 0; i < allMembers.length; i++) {
        File memberFile = allMembers[i];

        if (this.monitor != null) {
          this.monitor.processingFile(PTaskEngine.WORKER_ID,
              this.config.getName(),
              UserProcessingMonitor.PROCESS_TYPE.TASK_EXECUTION,
              memberFile.getName(), i + 1, allMembers.length);
        }
        LoggerCentral.info(LOGGER,
            "Parsing file " + memberFile.getAbsolutePath());
        try {
          reader = new BufferedReader(new InputStreamReader(
              new FileInputStream(memberFile), "UTF-8"));
          while ((line = reader.readLine()) != null) {
            // create a new entry
            try {
              reverseIndex.add(new EggNogEntry(line).getProteinId(), line);

              // process aborted ?
              if (LoggerCentral.processAborted()) {
                break;
              }
            } catch (Exception ex) {
              if (!line.startsWith("#")) { // do not log for the first line
                LoggerCentral.warn(LOGGER, "Unable to create entry for '"
                    + line + "' : " + ex.getMessage());
              }
            }

          }
        } finally {
          IOUtils.closeQuietly(reader);
        }
      }
    } finally {
      reverseIndex.close(this.monitor);
    }
  }

  /**
   * Convert the sequences file into multiple uniprot formatted files
   */
  private void convertSequences() throws Exception {

    JProgressBar progress = null;
    /*
     * if (this.monitor != null && (this.monitor instanceof
     * MyUserProcessingMonitor)) { progress = ((MyUserProcessingMonitor)
     * this.monitor).getTaskProgress(); }
     */

    SequenceFileManager sfm = new SequenceFileManager(
        sequencesFile.getAbsolutePath(), DatabankFormat.fasta, LOGGER, progress);
    sfm.setMustCreateAFilteredFile(false);
    // will create all uniprot files
    sfm.addValidator(new SequenceValidatorEggNog(this.config
        .getLocalTmpFolder(), EggNogSuperKingdoms.values()));
    sfm.execute();
  }

  /**
   * Prepare the egg nog data in order to install the egg nog dictionary and the
   * egg nog proteins databanks
   * 
   * @throws Exception
   */
  public void execute() throws Exception {
    // init static properties in order to be used by other classes
    EggNogPreparator.doneFile = new File(this.config.getLocalTmpFolder(),
        "done");
    EggNogPreparator.membersDirectory = new File(
        this.config.getLocalTmpFolder(), "members");
    EggNogPreparator.descriptionsDirectory = new File(
        this.config.getLocalTmpFolder(), "descriptions");
    EggNogPreparator.funcCatsDirectory = new File(
        this.config.getLocalTmpFolder(), "funccats");
    // the lucene index
    indexDirectory = new File(this.config.getLocalTmpFolder(), "members.ldx");
    reverseIndex = new EggNogIndex(indexDirectory);

    if (!EggNogPreparator.doneFile.exists()) {

      if (this.monitor != null) {
        this.monitor.processingMessage(PTaskEngine.WORKER_ID,
            this.config.getName(),
            UserProcessingMonitor.PROCESS_TYPE.TASK_EXECUTION,
            UserProcessingMonitor.MSG_TYPE.OK, "Untar EggNog files");
      }
      // first of all : untar all files
      this.untarFiles();

      if (this.monitor != null) {
        this.monitor.processingMessage(PTaskEngine.WORKER_ID,
            this.config.getName(),
            UserProcessingMonitor.PROCESS_TYPE.TASK_EXECUTION,
            UserProcessingMonitor.MSG_TYPE.OK,
            "Index sequences and associated nogs");
      }
      // create the lucene index
      this.createLuceneIndex();

      if (this.monitor != null) {
        this.monitor.processingMessage(PTaskEngine.WORKER_ID,
            this.config.getName(),
            UserProcessingMonitor.PROCESS_TYPE.TASK_EXECUTION,
            UserProcessingMonitor.MSG_TYPE.OK,
            "Convert sequences from Fasta to Uniprot");
      }
      // create sequences files in uniprot format
      this.convertSequences();

      // Indicates that the job has been done
      EggNogPreparator.doneFile.createNewFile();
    }

  }

}
