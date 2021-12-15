/* Copyright (C) 2007-2021 Patrick G. Durand
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
package bzh.plealog.dbmirror.util.runner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import bzh.plealog.dbmirror.lucenedico.DicoTermQuerySystem;
import bzh.plealog.dbmirror.task.PTaskEngineAbortException;
import bzh.plealog.dbmirror.task.PTaskFormatDB;
import bzh.plealog.dbmirror.task.PTaskMakeBlastAlias;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;
import bzh.plealog.dbmirror.util.sequence.SeqIOUtils;
import bzh.plealog.dbmirror.util.sequence.TaxonMatcherHelper;

/**
 * This class is capable of running a FormatDB job in a separate thread.
 * 
 * ftp://ftp.ncbi.nlm.nih.gov/blast/documents/formatdb.html
 */
public class FormatDBRunner extends Thread {
  private String                     _formatDBCmd;
  // 06/06/2014 new KLib => use blast databank instead of volume files
  // private String _makePlastDBCmd;
  private String                     _dbxrefsConfig;
  private String                     _dbPath;
  private String                     _dbName;
  private List<String>               _files;
  private FormatDBMonitor            _monitor;
  private TaxonMatcherHelper         _taxMatcher;
  private DicoTermQuerySystem        _dico;
  private boolean                    _useNcbiIdFormat;
  private boolean                    _isProteic;
  private boolean                    _checkInputFiles;
  private boolean                    _success                 = false;
  private int                        _headerFormat;
  private int                        _volumeSize;

  private static final int           REFORMAT_SEQ_FILE_ERROR  = 0;
  private static final int           REFORMAT_SEQ_FILE_OK     = 1;
  private static final int           REFORMAT_SEQ_FILE_NON_NR = 2;
  private static final String        NR_EXT                   = "_kb";

  public static final String         PROTEIN_ALIAS            = "pal";
  public static final String         NUCLEIC_ALIAS            = "nal";
  public static final String         PROTEIN_IDX              = "pin";
  public static final String         NUCLEIC_IDX              = "nin";

  public static final String         Plast_INFO_EXT           = ".info";

  public static final String         PROTEIN_ALIAS_EXT        = "."
                                                                  + PROTEIN_ALIAS;
  public static final String         NUCLEIC_ALIAS_EXT        = "."
                                                                  + NUCLEIC_ALIAS;
  public static final String         PROTEIN_IDX_EXT          = "."
                                                                  + PROTEIN_IDX;
  public static final String         NUCLEIC_IDX_EXT          = "."
                                                                  + NUCLEIC_IDX;

  // this tag is used to avoid this error message from Blast:
  // [NULL_Caption] WARNING: Recursive situation detected with xxx
  // where xxx is the dbName
  public static final String         BLAST_ALIAS_TAG          = "M";

  private static final Logger           LOGGER                   = LogManager.getLogger(
        DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY + ".FormatDBRunner");

  private static final MessageFormat FORMAT_MSG1_HEADER       = new MessageFormat(
                                                                  "Reading file ({0}/{1}) {2}...");
  private static final MessageFormat FORMAT_MSG2_HEADER       = new MessageFormat(
                                                                  "Formatting {0}...");
  private static final String        FORMAT_MSG3_HEADER       = "Creating Fasta volumes...";
  private static final String        FORMAT_MSG_OK            = "done.";
  private static final MessageFormat FORMAT_NON_NR_MSG        = new MessageFormat(
                                                                  "Found {0} redundant sequence IDs: only first occurrences have been retained.");
  private static final MessageFormat FORMAT_TOT_SEQ_MSG       = new MessageFormat(
                                                                  "File contains {0} sequences.");
  private static final MessageFormat FORMAT_GUESS_FORMAT_MSG  = new MessageFormat(
                                                                  "format is {0}...");

  public FormatDBRunner() {
  }

  public FormatDBRunner(FormatDBMonitor monitor, String formatDBCmd,
      String dbxrefsConfig, String dbPath, String dbName, List<String> files,
      TaxonMatcherHelper taxMatcher, DicoTermQuerySystem dico,
      boolean checkForNrID, boolean useNcbiIdFormat, boolean isProteic,
      boolean checkInputFiles, int headerFormat, int volumeSize) {
    _monitor = monitor;
    _monitor.setCheckNR(checkForNrID);
    _formatDBCmd = formatDBCmd;
    _dbxrefsConfig = dbxrefsConfig;
    _dbPath = dbPath;
    _dbName = dbName;
    _files = files;
    _taxMatcher = taxMatcher;
    _dico = dico;
    _useNcbiIdFormat = useNcbiIdFormat;
    _checkInputFiles = checkInputFiles;
    _isProteic = isProteic;
    _headerFormat = headerFormat;
    _volumeSize = volumeSize;
  }

  private void removeOldFiles(String path, boolean isProteic) {
    File[] files;
    File f;
    String fName, fDbName1, fDbName2;
    int i;

    // construct the name of the file that were (or will) be created during
    // formatdb step
    // depending if the user chosen to check or not the provided files
    // dbFileName may contain the '_kb' or not. So we have to locate
    // both types of files
    fDbName1 = new File(path).getName() + ".";
    fDbName2 = new File(path).getName() + "_";
    // get an array of all the files contained in the directory where to
    // install
    // the blast database
    files = new File(path).getParentFile().listFiles();
    // remove all files prefixed with fDbName
    for (i = 0; i < files.length; i++) {
      f = files[i];
      if (!f.isFile())
        continue;
      fName = f.getName();
      if ((fName.startsWith(fDbName1) || fName.startsWith(fDbName2))
          && !(fName.endsWith(DBMSAbstractConfig.FEXT_NUM)
              || fName.endsWith("gz") || fName.endsWith("tgz") || fName
                .endsWith("zip"))) {
        f.delete();
      }
    }
  }

  private void removeOldAlias(String path, boolean isProteic) {
    String fExt;
    File f;
    String fName;

    // remove old alias
    fExt = (!isProteic ? NUCLEIC_ALIAS_EXT : PROTEIN_ALIAS_EXT);
    fName = path + BLAST_ALIAS_TAG + fExt;
    f = new File(fName);
    if (f.exists())
      f.delete();
  }

  /**
   * This method overrides the standard alias file created by formatdb since it
   * seems it does strange stuff with several Fasta files.
   */
  private boolean prepareAliasFile(String path, String dbName,
      List<String> dbFileNames, boolean isProteic) {
    String fExt1, fExt2;
    File[] files;
    File f;
    PrintWriter writer = null;
    String fName, parentDir;
    List<String> lines;
    boolean bRet = false;
    int i, pos;

    fExt1 = (!isProteic ? NUCLEIC_IDX_EXT : PROTEIN_IDX_EXT);
    fExt2 = (!isProteic ? NUCLEIC_ALIAS_EXT : PROTEIN_ALIAS_EXT);

    try {
      parentDir = new File(path).getParent();
      fName = path + BLAST_ALIAS_TAG + fExt2;
      //delete old alias before creating it
      f = new File(fName);
      if (f.exists()) {
        f.delete();
      }
      //get content of NCBI-based BLAST alias file if any found
      //(this may happen when installing native NCBI BLAST bank)
      //this has to be done BEFORE creating new alias file!!!
      lines = PTaskMakeBlastAlias.getDataFromNativeAliasFile(parentDir, fExt2);

      //create new alias file
      writer = new PrintWriter(fName);
      writer.print("TITLE ");
      writer.println(dbName);
      writer.print("DBLIST ");
      files = new File(parentDir).listFiles();
      for (i = 0; i < files.length; i++) {
        f = files[i];
        if (!f.isFile())
          continue;
        fName = f.getName();
        pos = fName.indexOf(fExt1);
        if (pos >= 0 /* && isFileNameOk(fName, dbFileNames, fExt1) */) {
          writer.print(fName.substring(0, pos) + " ");
        }
      }
      writer.println();
      //write additional content of native BLAST alias file if any
      if (lines!=null) {
        for (String str : lines) {
          writer.println(str);
        }
      }
      writer.flush();
      writer.close();
      bRet = true;
    } catch (Exception e) {
      LoggerCentral.error(LOGGER, "unable to create alias file: " + e);
    } finally {
      IOUtils.closeQuietly(writer);
    }
    return bRet;
  }

  /**
   * Prepares the formatdb command-line parameters.
   */
  private Map<String, CommandArgument> prepareParamsFormatDB(String dbPath,
      boolean isProt, boolean useNcbiIdFormat, String dbName,
      List<String> dbFileNames) {
    Hashtable<String, CommandArgument> params;
    StringBuffer buf;
    String p;
    int i, size;

    params = new Hashtable<String, CommandArgument>();
    // isProteic ?
    params.put("-p", new CommandArgument((isProt ? "T" : "F"), false));
    // db File to format
    buf = new StringBuffer();
    size = dbFileNames.size();
    i = 0;
    for (String dataFile : dbFileNames) {
      buf.append(dataFile);
      if ((i + 1) < size) {
        buf.append(",");
      }
      i++;
    }
    p = buf.toString();
    params.put("-i", new CommandArgument(p, true, true, true));
    // parse gi (required to use NCBI recommendations)
    params.put("-o", new CommandArgument((useNcbiIdFormat ? "T" : "F"), false));
    // log file
    params.put("-l", new CommandArgument("formatdb.log", false));
    // db name; only when we have multiple input files
    params.put("-n", new CommandArgument(dbName, false));
    return params;
  }

  /**
   * Prepares the formatdb command-line parameters.
   */
  private Map<String, CommandArgument> prepareParamsMakeBlastDB(String dbPath,
      boolean isProt, boolean useNcbiIdFormat, String dbName,
      List<String> dbFileNames) {
    Hashtable<String, CommandArgument> params;
    StringBuffer buf;
    String p;
    int i, size;

    params = new Hashtable<String, CommandArgument>();
    // isProteic ?
    params.put("-dbtype", new CommandArgument((isProt ? "prot" : "nucl"), false));
    // db File to format
    buf = new StringBuffer();
    size = dbFileNames.size();
    i = 0;
    for (String dataFile : dbFileNames) {
      buf.append(dataFile);
      if ((i + 1) < size) {
        buf.append(",");
      }
      i++;
    }
    p = buf.toString();
    params.put("-in", new CommandArgument(p, true, true, true));
    // parse gi (required to use NCBI recommendations)
    if (useNcbiIdFormat)
      params.put("-parse_seqids", new CommandArgument("", false));
    // log file
    params.put("-logfile", new CommandArgument("makeblastdb.log", false));
    // db name; only when we have multiple input files
    params.put("-title", new CommandArgument(dbName, false));
    params.put("-out", new CommandArgument(dbName, false));
    return params;
  }

  /**
   * 
   * 
   * @param si
   * @param checkInputFiles
   * @param isProt
   * @return
   */
  private int prepareTmpFastaFile(SeqInfo si, boolean checkInputFiles,
      boolean isProt) {
    String msg;
    File f1;
    int[] rets;
    int ret = REFORMAT_SEQ_FILE_ERROR, format, totSeq = 0;

    try {
      format = SeqIOUtils.guessFileFormat(si.dbFileName);
      si.msg += FORMAT_GUESS_FORMAT_MSG
          .format(new Object[] { SeqIOUtils.FILE_TYPES[format] });
      _monitor.setTxtMessage(si.msg);
      si.fileType = format;
      if (format == SeqIOUtils.UNKNOWN) {
        throw new Exception("Unknown file format.");
      }

      if (!checkInputFiles) // user does not want to check input file
      {
        // but in fasta format, it is mandatory to do just a little things
        if (format == SeqIOUtils.FASTADNA || format == SeqIOUtils.FASTAPROT
            || format == SeqIOUtils.FASTARNA) {
          // remove old files and set the fasta file created = the fasta file
          // downloaded
          f1 = new File(si.dbFileName);
          removeOldFiles(si.dbFileName, _isProteic);
          si.preparedDbName = si.dbFileName;

          // it is required to count sequences to get the grand total
          // number of sequences available
          countSequence(f1, _monitor);
          si.converted = false;

          // it is ok !
          return REFORMAT_SEQ_FILE_OK;
        }
      }
      removeOldFiles(si.dbFileName, _isProteic);
      si.converted = true;
      si.msg += "converting to Fasta...";
      _monitor.setTxtMessage(si.msg);
      rets = SeqIOUtils.convertToFasta(si.dbFileName, si.preparedDbName,
          format, _monitor, _taxMatcher, _dico, _headerFormat);
      if (rets != null) {
        totSeq = rets[0];
      }
      ret = REFORMAT_SEQ_FILE_OK;
    } catch (PTaskEngineAbortException ex) {
      si.msg = ex.getMessage();
      ret = REFORMAT_SEQ_FILE_ERROR;
    } catch (Exception e) {
      msg = "Unable to read sequence file.\n" + si.dbFileName + ": "
          + e.getMessage() + ".\nCheck your file for sequence ID: "
          + _monitor.getLastID();
      si.msg = msg;
      ret = REFORMAT_SEQ_FILE_ERROR;
    }
    si.discardSeq = 0;
    si.totSeq = totSeq;
    return ret;
  }

  /**
   * Runs a formatdb job.
   */
  private void doJob() {
    DBMSExecNativeCommand executor;
    Process proc = null;
    int exitCode = -1;
    boolean formatdbrunning, stopped;
    Map<String, CommandArgument> params;

    String fName, dbLocation, dbPath, msg;
    List<String> dbList;
    ArrayList<String> formattedDbList;
    Object[] values = new Object[3]; // needed to format the messages
    File volumeOkFile;
    List<String> volumes;

    SeqInfo si;
    int ret, totSeq, curFile=0, totFiles;
    long tim;
    boolean runOk = true;

    dbLocation = Utils.terminatePath(_dbPath);
    dbPath = dbLocation + _dbName;
    executor = new DBMSExecNativeCommand();
    dbList = _files;
    formattedDbList = new ArrayList<String>();
    totSeq = 0;
    try {

      // get or create the volumes ?
      // it depends of the volumes.ok file
      volumeOkFile = new File(dbLocation, PTaskFormatDB.VOL_FILES_OK);
      if (volumeOkFile.exists()) {
        volumes = Utils.getFileVolumes(dbLocation, _dbName);
      } else {

        si = new SeqInfo();

        // required
        removeOldAlias(dbPath, _isProteic);
        // required
        totFiles=dbList.size();
        for (String dbFileName : dbList) {
          curFile++;
          if (!new File(dbFileName).exists()) {
            msg = "File not found: " + dbFileName;
            _monitor.setErrMsg(msg);
            throw new Exception(msg);
          }

          si.dbFileName = dbFileName;
          fName = new File(dbFileName).getName();
          // fasta converted filename
          si.preparedDbName = dbLocation + fName + NR_EXT;

          // first pass: check the source file for its format and for redundant
          // sequences
          
          values[0] = curFile;
          values[1] = totFiles;
          values[2] = fName;
          msg = FORMAT_MSG1_HEADER.format(values);
          _monitor.setTxtMessage(msg);
          tim = System.currentTimeMillis();
          LoggerCentral.info(LOGGER, "Checking: " + dbFileName);
          si.msg = msg;
          ret = prepareTmpFastaFile(si, _checkInputFiles, _isProteic);
          LoggerCentral.info(LOGGER,
              "checking time: " + ((System.currentTimeMillis() - tim) / 1000)
                  + " s");
          if (ret == REFORMAT_SEQ_FILE_ERROR) {
            _monitor.setErrMsg(si.msg);
            runOk = false;
            break;
          }
          msg = si.msg;
          // compatible format ?
          /*
           * if (isProt != SeqIOUtils.isProt(si.fileType)){
           * _monitor.setErrMsg(BAD_FILE_FORMAT_MSG.format( new
           * Object[]{getTypeString(isProt),dbFileName,
           * getTypeString(SeqIOUtils.isProt(si.fileType))})); runOk = false;
           * break; }
           */
          msg += FORMAT_MSG_OK;
          if (si.converted) {
            msg += " ";
            msg += FORMAT_TOT_SEQ_MSG.format(new Object[] { si.totSeq });
            if (ret == REFORMAT_SEQ_FILE_NON_NR) {
              msg += " ";
              msg += FORMAT_NON_NR_MSG.format(new Object[] { si.discardSeq });
            }
            totSeq += si.totSeq;
          }
          formattedDbList.add(si.preparedDbName);
          _monitor.setTxtMessage(msg);
          System.gc();
          try {
            sleep(1000);
          } catch (InterruptedException e) {
          }
        }

        if (_taxMatcher != null) {
          _taxMatcher.dumpTaxonNotFound(LOGGER);
        }

        // second pass: create data volumes
        if (!runOk) {
          throw new Exception(si.msg);
        }
        if (_checkInputFiles && totSeq == 0) {
          throw new Exception("Fasta files do not contain any sequences.");
        }
        msg = FORMAT_MSG3_HEADER;
        _monitor.setTxtMessage(msg);

        volumes = Utils.createFileVolumes(formattedDbList, dbLocation, _dbName,
            (long) _volumeSize * Utils.GIGA, true);
        if (volumes == null) {
          throw new Exception("Unable to create data volumes.");
        }

        // create the volumes.ok file
        try {
          volumeOkFile.createNewFile();
        } catch (Exception ex) {
          // Do not stop the process if the volumes.ok file is not created
          LOGGER.warn("Unable to create the " + PTaskFormatDB.VOL_FILES_OK
              + " file for " + volumeOkFile.getAbsolutePath(), ex);
        }

      } // end test the volumes.ok file

      // third pass: start formatdb process
      values[0] = volumes.toString();
      values[1] = null;
      values[2] = null;
      msg = FORMAT_MSG2_HEADER.format(values);
      _monitor.setTxtMessage(msg);
      if (_formatDBCmd.indexOf("formatdb") > 1) {
        params = prepareParamsFormatDB(dbPath, _isProteic, _useNcbiIdFormat, _dbName, volumes);
      } else {
        params = prepareParamsMakeBlastDB(dbPath, _isProteic, _useNcbiIdFormat, _dbName, volumes);
      }
      tim = System.currentTimeMillis();

      proc = executor.executeAndReturn(_formatDBCmd, params);

      formatdbrunning = true;
      stopped = false;
      while (formatdbrunning && (!stopped)) {
        formatdbrunning = false;
        try {
          exitCode = proc.exitValue();
        } catch (IllegalThreadStateException ex) {
          formatdbrunning = true;
        }
        stopped = (LoggerCentral.processAborted());
        Thread.sleep(1000);
      }
      proc.destroy();
      if (stopped) {
        exitCode = DBMSExecNativeCommand.EXEC_INTERRUPTED;
      }

      // figures out if something wrong occurs
      if (exitCode == 0) {
        msg += FORMAT_MSG_OK;
        _monitor.setTxtMessage(msg);
        // LoggerCentral.info(LOGGER,
        // "formatting time: "+((System.currentTimeMillis()-tim)/1000)+" s");
      } else if (exitCode == DBMSExecNativeCommand.EXEC_INTERRUPTED) {
        _monitor.setErrMsg("Job cancelled.");
        runOk = false;
      } else {
        _monitor.setErrMsg("Unable to format sequence file (FormatDB error).");
        runOk = false;
      }
    } catch (Exception e) {
      LoggerCentral.error(LOGGER, "Unable to run formatdb: " + e);
      _monitor.setErrMsg(e.getMessage());
      runOk = false;
    } finally {
      if (proc != null) {
        try {
          proc.getErrorStream().close();
        } catch (Exception e) {
        }// not bad
        try {
          proc.getInputStream().close();
        } catch (Exception e) {
        }// not bad
        try {
          proc.getOutputStream().close();
        } catch (Exception e) {
        }// not bad
      }
    }
    System.gc();
    if (runOk) {
      if (prepareAliasFile(dbPath, _dbName, formattedDbList, _isProteic)) {
        runOk = true;
      } else {
        runOk = false;
      }
    }
    if (_taxMatcher != null)
      _taxMatcher.closeTaxonMatcher();
    _success = runOk;
    _monitor.setTxtMessage("");
  }

  private void setDbXrefTagConfiguration() {
    if (_dbxrefsConfig != null) {
      SeqIOUtils.setDbXrefTagManager(_dbxrefsConfig);
    }
  }

  public void run() {
    LoggerCentral.info(LOGGER, "Start making BLAST db processing");
    if (_monitor.setJobRunnig(true)) {
      setDbXrefTagConfiguration();
      doJob();
      DicoTermQuerySystem.closeDicoTermQuerySystem();
    }
    _monitor.setJobRunnig(false);
    _monitor.jobDone(_success);
    LoggerCentral.info(LOGGER, "Done making BLAST db processing");
  }

  private void countSequence(File f, FormatDBMonitor monitor) throws Exception {
    FileInputStream fis = null;
    BufferedReader reader = null;
    String line, id;
    int i, size, idx;

    try {
      fis = new FileInputStream(f);
      reader = new BufferedReader(new InputStreamReader(fis));
      while ((line = reader.readLine()) != null) {
        if (line.charAt(0) == '>') {
          i = 1;
          size = line.length();
          // some Fasta may contains space between > and the seqId
          for (i = 1; i < size; i++) {
            if (line.charAt(i) != ' ') {
              break;
            }
          }
          idx = line.indexOf(' ', i);
          if (idx == -1)
            idx = line.length();
          id = line.substring(i, idx);
          monitor.seqFound(id);
        }
      }
    } finally {
      if (fis != null) {
        try {
          fis.close();
        } catch (Exception ex) {
        }
      }
    }
  }

  private class SeqInfo {
    int     totSeq;
    int     discardSeq;
    @SuppressWarnings("unused")
    int     fileType;
    boolean converted;
    String  msg;
    String  dbFileName;
    String  preparedDbName;
  }

}