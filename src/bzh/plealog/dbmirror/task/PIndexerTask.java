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
package bzh.plealog.dbmirror.task;

import java.io.File;
import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.indexer.DBParsable;
import bzh.plealog.dbmirror.indexer.LuceneStorageSystem;
import bzh.plealog.dbmirror.indexer.LuceneUtils;
import bzh.plealog.dbmirror.indexer.ParserMonitor;
import bzh.plealog.dbmirror.indexer.StorageSystem;
import bzh.plealog.dbmirror.task.PTaskEngine.MyParserMonitor;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.ant.PAntTasks;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.descriptor.DatabankFormat;
import bzh.plealog.dbmirror.util.log.LoggerCentral;
import bzh.plealog.dbmirror.util.sequence.SequenceFileManager;
import bzh.plealog.dbmirror.util.sequence.SequenceValidatorCutFile;
import bzh.plealog.dbmirror.util.sequence.SequenceValidatorDescription;
import bzh.plealog.dbmirror.util.sequence.SequenceValidatorRenamer;
import bzh.plealog.dbmirror.util.sequence.SequenceValidatorSize;

/**
 * Abstract class for indexer tasks
 * 
 * @author Ludovic Antin
 * 
 */
public abstract class PIndexerTask extends PAbstractTask implements
    PParserTask {

  private String                _src;
  private String                _errMsg;
  private ParserMonitor         _monitor;
  // parameters section
  protected Map<String, String> _args;
  protected boolean             _checkNR                = false;
  protected String              _filterSequenceSize;
  protected String              _filterDescription;
  protected boolean             _filterExactDescription = true;
  protected String              _cutFile;
  protected String              _rename;

  private static final Log      LOGGER                  = LogFactory
                                                            .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                                + ".PTaskEngine");

  public static long            firstNbForRename        = 0;

  public PIndexerTask(String src) {
    this._src = src;
  }

  protected void setSrc(String src) {
    this._src = src;
  }

  public String getSrc() {
    return this._src;
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getErrorMsg() {
    return _errMsg;
  }

  public void setParserMonitor(ParserMonitor pm) {
    _monitor = pm;
  }

  public ParserMonitor getParserMonitor() {
    return this._monitor;
  }

  public MyParserMonitor getMyParserMonitor() {
    if (this._monitor instanceof MyParserMonitor) {
      return (MyParserMonitor) this._monitor;
    }
    return null;
  }

  public void setParameters(String params) {
    String value;
    _args = new Hashtable<String, String>();

    if (params == null)
      return;

    _args = Utils.getTaskArguments(params);
    value = _args.get(CHECK_NR);
    if (value != null)
      _checkNR = Boolean.TRUE.toString().equalsIgnoreCase(value);

    value = _args.get(SequenceValidatorSize.PARAMETER_TERM);
    if (value != null)
      _filterSequenceSize = value;

    value = _args.get(SequenceValidatorDescription.PARAMETER_TERM);
    if (value != null)
      _filterDescription = value;

    value = _args.get(SequenceValidatorDescription.PARAMETER_EXACT_TERM);
    if (value != null)
      _filterExactDescription = Boolean.TRUE.toString().equalsIgnoreCase(value);

    value = _args.get(SequenceValidatorCutFile.PARAMETER_TERM);
    if (value != null)
      _cutFile = value;

    value = _args.get(SequenceValidatorRenamer.PARAMETER_TERM);
    if (value != null)
      _rename = value;
  }

  /**
   * Implementation of KLTask interface.
   */
  public boolean execute() {
    LuceneStorageSystem lss = null;
    String idxName;
    File srcFile = new File(_src);
    SequenceValidatorRenamer renamer = null;

    if (_src == null) {
      _errMsg = "source file is unknown";
      return false;
    }

    LoggerCentral.info(LOGGER, getName() + " started on " + _src);

    // check whether index already exists
    idxName = _src + LuceneUtils.DIR_OK_FEXT;
    
    if (new File(idxName).exists() || PAbstractTask.testTaskOkForFileExists(idxName)) {
      LoggerCentral.info(LOGGER, getName() + ": " + idxName
          + ": index skipped: arleady exists");
      return true;
    }
    try {
      // checker whether tmp index exists
      idxName = _src + LuceneUtils.DIR_TMP_FEXT;
      if (new File(idxName).exists()) {
        if (!PAntTasks.deleteDirectory(idxName)) {
          throw new Exception("unable to delete old index: " + idxName);
        }
      }

      // are there some filters to process on this sequence file ?
      SequenceFileManager sfm = new SequenceFileManager(_src,
          this.getDatabankFormat(), LOGGER, null);
      if (StringUtils.isNotBlank(this._cutFile)) {
        try {
          sfm.addValidator(new SequenceValidatorCutFile(this._cutFile));
        } catch (NumberFormatException ex) {
          LoggerCentral.warn(LOGGER,
              "Unable to cut sequence file with this parameter :"
                  + this._cutFile);
        }
      }
      if (StringUtils.isNotBlank(this._filterSequenceSize)) {
        try {
          sfm.addValidator(new SequenceValidatorSize(this._filterSequenceSize));
        } catch (NumberFormatException ex) {
          LoggerCentral.warn(LOGGER,
              "Unable to filter by sequence size with this parameter :"
                  + this._filterSequenceSize);
        }
      }
      if (StringUtils.isNotBlank(this._filterDescription)) {
        SequenceValidatorDescription validatorDescription = new SequenceValidatorDescription(
            this._filterDescription);
        validatorDescription.setExactSearch(this._filterExactDescription);
        sfm.addValidator(validatorDescription);
      }
      if (StringUtils.isNotBlank(this._rename)) {
        try {
          renamer = new SequenceValidatorRenamer(this._rename);

          // if a rename has be done for a previous file
          // set the expected next id
          if (PIndexerTask.firstNbForRename != 0) {
            renamer.setFirstId(PIndexerTask.firstNbForRename);
          }

          sfm.addValidator(renamer);
        } catch (Exception ex) {
          LoggerCentral.warn(LOGGER, "Unable to rename : " + ex.getMessage());
        }
      }
      if (sfm.somethingToDo()) {
        File filteredFile = null;
        try {
          if (this.getMyParserMonitor() != null) {
            this.getMyParserMonitor().processingMessage(
                "Filtering file '" + srcFile.getName() + "'...");
          }
          LoggerCentral.stopThisSfmIfAbort(sfm);
          filteredFile = sfm.execute().get(0);
        } catch (Exception ex) {
          LoggerCentral.warn(LOGGER, "Unable to filter the sequence file '"
              + _src + "' : " + ex.getMessage());
        } finally {
          LoggerCentral.removeSfmToAbort();
        }
        if (filteredFile != null) {
          if (this.getMyParserMonitor() != null) {
            this.getMyParserMonitor().processingMessage(
                "Filtering done. Copying filtered file...");
          }
          try {
            // rename the _src file
            srcFile.renameTo(new File(_src + ".beforeFilter"));
            // move the filtered file to the _src location
            srcFile = new File(_src);
            FileUtils.moveFile(filteredFile, srcFile);
          } catch (Exception ex) {
            LoggerCentral.warn(LOGGER,
                "Unable to copy the filtered file in the correct place :'"
                    + _src + "' : " + ex.getMessage());
          }
        }

        // set the next id for rename
        if (renamer != null) {
          PIndexerTask.firstNbForRename = renamer.getNextId();
        }
      }

      if (this.getMyParserMonitor() != null) {
        this.getMyParserMonitor().processingMessage(
            "Indexing file '" + srcFile.getName() + "'...");
      }
      lss = new LuceneStorageSystem();
      this.getParser().setParserMonitor(_monitor);
      this.getParser().setCheckSeqIdRedundancy(_checkNR);
      lss.open(idxName, StorageSystem.WRITE_MODE);
      this.getParser().parse(_src, lss);
      lss.close();
      lss = null;
      // if index is empty: ok, but remove it. This has been introduced to
      // enable
      // a clean resume of file processing, especially when playing with
      // constraints
      // based sequence subsets (taxonomy, etc).
      if (this.getParser().getEntries() == 0) {
        LoggerCentral.info(LOGGER, getName() + ": " + idxName
            + ": index removed: empty");
        PAntTasks.deleteDirectory(idxName);
      } else {
        // index successfully created
        if (!PAntTasks.movefile(idxName, _src + LuceneUtils.DIR_OK_FEXT)) {
          throw new Exception("unable to rename index: " + idxName);
        }
        PAbstractTask.setTaskOkForFile(_src + LuceneUtils.DIR_OK_FEXT);
      }
      // something to do in inherited classes ?
      this.parsingDone();
    } catch (Exception ex) {
      _errMsg = " indexing of " + _src + " failed: " + ex;
      return false;
    } finally {
      try {
        if (lss != null){lss.close();}
      } catch (Exception e) {}
    }
    return true;
  }

  public abstract DBParsable getParser();

  /**
   * Method called after parsing
   */
  public abstract void parsingDone();

  /**
   * Which format is supported by this indexer ?
   * 
   * @return
   */
  public abstract DatabankFormat getDatabankFormat();
}
