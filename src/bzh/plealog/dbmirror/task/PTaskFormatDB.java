/* Copyright (C) 2007-2017 Patrick G. Durand
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
package bzh.plealog.dbmirror.task;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.lucenedico.DicoTermQuerySystem;
import bzh.plealog.dbmirror.reader.DBUtils;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.conf.DBMSConfigurator;
import bzh.plealog.dbmirror.util.conf.DBMirrorConfig;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptorUtils;
import bzh.plealog.dbmirror.util.log.LoggerCentral;
import bzh.plealog.dbmirror.util.runner.DBMSExecNativeCommand;
import bzh.plealog.dbmirror.util.runner.FormatDBMonitor;
import bzh.plealog.dbmirror.util.runner.FormatDBRunner;
import bzh.plealog.dbmirror.util.sequence.TaxonMatcherHelper;

/**
 * A task capable of transforming a FASTA sequence file into a BLAST database.
 * 
 * @author Patrick G. Durand
 */
public class PTaskFormatDB extends PAbstractTask {

  private List<String>       _files;
  private String             _dbPathName;
  private String             _errMsg;
  private String             _taxInclude;
  private String             _taxExclude;
  private boolean            _isNucleic;
  private boolean            _useNcbiIdFormat = true;
  private boolean            _checkFiles      = true;
  private boolean            _checkNR         = true;
  private int                _headerFormat    = DBUtils.NO_HEADER_FORMAT;
  private int                _volSize         = DBMSAbstractConfig
                                                  .getDefaultFastaVolumeSize();
  private int                _blastVer        = 5;
  
  private static final Log   LOGGER           = LogFactory
                                                  .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                      + ".PTaskEngine");

  public static final String BLAST_VER        = "blastver";//will be 5 by default
  public static final String USE_LCL_ID       = "lclid";
  public static final String CHECK_FILE       = "check";
  public static final String IS_SILVA         = "silva";
  public static final String IS_TAXO          = "taxonomy";
  // the CDD sequences databank is the CDD dico + the associated Conserved
  // Domains sequences
  public static final String IS_CDD           = "cdd";
  // fasta volume size; nb. of Go
  public static final String VOL_SIZE         = "volsize";

  // file indicates that the fasta volumes are ok
  public static final String VOL_FILES_OK     = "volumes"+PTask.TASK_OK_FEXT;

  /**
   * Store the nbSequences in a temporary file for the db given in parameter
   * 
   * @param dbPath
   * @param nbSequences
   */
  public static void addNbSequences(String dbPath, int nbSequences) {
    int oldNbSequences = getNbSequences(dbPath);
    try {
      Utils.writeInFile(getTmpEntriesCountFilepath(dbPath), new Integer(
          oldNbSequences + nbSequences).toString());
    } catch (Exception e) {
      LOGGER.warn("Unable to reset the number of sequences for file "
          + getTmpEntriesCountFilepath(dbPath));
    }

  }

  /**
   * Get the nbSequences in the temporary file corresponding in the db given in
   * parameter
   * 
   * @param dbPath
   */
  public static int getNbSequences(String dbPath) {
    String result = "";
    File tmpFile = new File(getTmpEntriesCountFilepath(dbPath));
    try {

      if (tmpFile.exists()) {
        result = Utils.readFirstLine(tmpFile);
        return new Integer(result).intValue();
      }
    } catch (Exception e) {
      LOGGER.info("Unable to read the number of sequences for file "
          + tmpFile.getName() + " : " + e.getMessage());
    }
    if (!result.isEmpty()) {
      try {
        return Integer.getInteger(result).intValue();
      } catch (Exception e) {
        LOGGER.info("Unable to read the number of sequences for file "
            + tmpFile.getName() + " : " + e.getMessage());
      }
    }
    return 0;
  }

  /**
   * Reset the nbSequences in the temporary file corresponding in the db given
   * in parameter by 1. Try to delete the temporary file 2. If not deleted : set
   * 0
   * 
   * @param dbPath
   */
  public static void resetNbSequences(String dbPath) {
    boolean writeZero = false;
    String filepath = getTmpEntriesCountFilepath(dbPath);

    try {
      if (!new File(filepath).delete()) {
        writeZero = true;
      }
    } catch (Exception e) {
      LOGGER.warn("Unable to delete the file " + filepath
          + ". Try to reset the number");
      writeZero = true;
    }

    if (writeZero) {
      try {
        Utils.writeInFile(filepath, new String("0"));
        LOGGER.info("Reset ok for file " + filepath);
      } catch (Exception e1) {
        LOGGER.warn("Unable to reset the number of sequences for file "
            + filepath);
      }
    }
  }

  public static String getTmpEntriesCountFilepath(String dbPath) {
    return Utils.terminatePath(dbPath) + "tmp" + DBMSAbstractConfig.FDBEXT_NUM;
  }

  public PTaskFormatDB(List<String> files, String dbPathName, boolean isNucleic) {
    _files = files;
    _isNucleic = isNucleic;
    _dbPathName = dbPathName;
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getName() {
    return "NCBIFormatDB";
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getUserFriendlyName() {
    return "preparing Blast databank (Fasta checking + Blast-DB conversion)";
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getErrorMsg() {
    return _errMsg;
  }

  private void writeSeqNums(String dbFileName, int entries) {
    PrintWriter writer = null;
    File f;

    // usually this part is done using the MyParserMonitor from KLTaskEngine
    // However, FormatDB task is not really a parsing task even if it's call
    // convertToFasta(): formatdb task convert several files at a time, and
    // ParserMonitor only handles one file at a time.
    f = new File(dbFileName + DBMSAbstractConfig.FDBEXT_NUM);

    try {
      writer = new PrintWriter(new FileWriter(f));
      writer.write(String.valueOf(entries));
      writer.flush();
    } catch (IOException e) {
      LOGGER.info("Unable to store the number of sequences (" + entries
          + ") in '" + f.getName() + "' : " + e.getMessage());
    } finally {
      IOUtils.closeQuietly(writer);
    }
  }

  private String getFormatDbPath() {
    DBMSConfigurator conf;
    String formatdb, path;

    conf = DBMSAbstractConfig.getConfigurator();
    if (conf == null) {
      return null;
    }

    formatdb = conf.getProperty(DBMSConfigurator.FDB_PRG_NAME);
    path = conf.getProperty(DBMSConfigurator.FDB_PATH_NAME);
    if (formatdb == null || path == null) {
      return null;
    }

    path = DBMSExecNativeCommand.formatNativePath(path, (path
        .indexOf(DBMSExecNativeCommand.APPDIR_VAR_NAME) >= 0) ? true : false,
        false);
    path = Utils.terminatePath(path) + formatdb;
    if (DBMSExecNativeCommand.getOSType() == DBMSExecNativeCommand.WINDOWS_OS) {
      path += ".exe";
    }
    if (new File(path).exists() == false) {
      return null;
    }
    return path;
  }

  public boolean execute() {
    TaxonMatcherHelper taxMatcher;
    DicoTermQuerySystem dico = null;
    DBMirrorConfig mirrorConfig;
    FormatDBRunner runner;
    MyMonitor jobMonitor;
    String formatDBCmd;

    File dbPathName;

    if (_files == null) {
      _errMsg = "file list unknown";
      return false;
    }
    jobMonitor = new MyMonitor();
    formatDBCmd = getFormatDbPath();
    if (formatDBCmd == null) {
      _errMsg = "'formatdb' or 'makeblastdb' command not available";
      return false;
    }

    if (_taxInclude != null || _taxExclude != null) {
      taxMatcher = new TaxonMatcherHelper();
      taxMatcher.setTaxonomyFilter(_taxInclude, _taxExclude);
      taxMatcher.initTaxonMatcher();
    } else {
      taxMatcher = null;
    }
    dbPathName = new File(_dbPathName);

    if (_headerFormat != DBUtils.NO_HEADER_FORMAT) {
      mirrorConfig = DBDescriptorUtils.getDBMirrorConfig(DBMSAbstractConfig
          .getLocalMirrorConfFile());
      dico = DicoTermQuerySystem.getDicoTermQuerySystem(mirrorConfig);
    }

    runner = new FormatDBRunner(jobMonitor, formatDBCmd,
        DBMSAbstractConfig.getDbXrefTagConfiguration(), dbPathName.getParent(),
        dbPathName.getName(), _files, taxMatcher, dico, _checkNR,
        _useNcbiIdFormat, !_isNucleic, _checkFiles, _headerFormat, _volSize, _blastVer);
    // end new KLib
    runner.start();
    try {
      runner.join();
    } catch (InterruptedException e) {
    }

    if (jobMonitor.success == false) {
      _errMsg = jobMonitor.getErrMsg();
      return false;
    } else {
      // check if total sequences is still written (maybe after a previous stop
      // process)
      int totalSequences = 0;
      try {
        String result = Utils.readFirstLine(new File(_dbPathName
            + DBMSAbstractConfig.FDBEXT_NUM));
        totalSequences = Integer.valueOf(result);
      } catch (Exception ex) {

      }

      if (totalSequences == 0) {
        // lan 21/03/2014 during the indexing tasks, a total sequences counting
        // was done
        // in a file. If the file exists for the current db : just read it
        // instead of looking
        // in the job monitor
        String tmpPathFile = new File(_dbPathName).getParent();
        totalSequences = PTaskFormatDB.getNbSequences(tmpPathFile);
        if (totalSequences == 0) {
          totalSequences = jobMonitor.getTotalSequences();
        } else {
          // reset the file which contains the number of sequences
          PTaskFormatDB.resetNbSequences(tmpPathFile);
        }
        writeSeqNums(_dbPathName, totalSequences);
      }
      return true;
    }
  }

  // Note: there are 5 possible formatdb parameters:
  // 1. set local Ids (default is false)
  // 2. check file consistency (default is true)
  // 3. check file redundancy (default is true)
  // 4. include taxon IDs
  // 5. exclude taxon IDs
  public void setParameters(String params) {
    Map<String, String> args;
    String value;

    if (params == null)
      return;

    args = Utils.getTaskArguments(params);

    // formatdb task expects an argument saying "use local ID?". However,
    // the internal code of KDMS uses another argument saying
    // "use NCBI Id formatted string?". Since the
    // first one is the opposite of the second one, one can understand the
    // following line of code.
    value = args.get(USE_LCL_ID);
    if (value != null)
      _useNcbiIdFormat = Boolean.FALSE.toString().equals(value);

    value = args.get(CHECK_FILE);
    if (value != null)
      _checkFiles = Boolean.TRUE.toString().equals(value);

    if (isTrueValue(args, IS_SILVA)) {
      _headerFormat = DBUtils.SILVA_HEADER_FORMAT;
    } else if (isTrueValue(args, IS_TAXO)) {
      _headerFormat = DBUtils.TAXONOMY_HEADER_FORMAT;
    } else if (isTrueValue(args, IS_CDD)) {
      _headerFormat = DBUtils.CDD_HEADER_FORMAT;
    } else {
      _headerFormat = DBUtils.NO_HEADER_FORMAT;
    }

    value = args.get(CHECK_NR);
    if (value != null)
      _checkNR = Boolean.TRUE.toString().equals(value);

    value = args.get(PTask.TAX_INCLUDE);
    if (value != null)
      _taxInclude = value;

    value = args.get(PTask.TAX_EXCLUDE);
    if (value != null)
      _taxExclude = value;

    value = args.get(VOL_SIZE);
    if (value != null) {
      int data;
      try {
        data = Integer.valueOf(value);
      } catch (NumberFormatException e) {
        data = DBMSAbstractConfig.getDefaultFastaVolumeSize();
      }
      if (data < 2 || data > 20) {
        data = DBMSAbstractConfig.getDefaultFastaVolumeSize();
      }
      _volSize = data;
    }
    
    value = args.get(BLAST_VER);
    if (value != null) {
      int data;
      try {
        data = Integer.valueOf(value);
      } catch (NumberFormatException e) {
        data = 5;
      }
      _blastVer = data;
    }
  }

  private boolean isTrueValue(Map<String, String> data, String key) {
    String value = data.get(key);
    if (StringUtils.isNotBlank(value)) {
      return Boolean.TRUE.toString().equals(value);
    }
    return false;
  }

  private class MyMonitor extends FormatDBMonitor {
    private boolean success;

    public void setTxtMessage(String msg) {
      LoggerCentral.info(LOGGER, msg);
    }

    public void jobDone(boolean success) {
      this.success = success;
    }

  }

}
