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
package bzh.plealog.dbmirror.fetcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.task.PTaskEngine;
import bzh.plealog.dbmirror.task.PTaskHandleHistory;
import bzh.plealog.dbmirror.task.PTaskInstallInProduction;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.ant.PAntTasks;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * This class is the central system responsible for downloading sets of files
 * from various FTP servers or local directories.
 * 
 * @author Patrick G. Durand
 */
public class PFTPLoaderSystem {
  private PTaskEngine           _taskEngine;
  private PFTPLoaderDescriptor[] _fDescriptors;
  private UserProcessingMonitor  _userMonitor;

  private static final Log       LOGGER    = LogFactory
                                               .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY+".PFTPLoaderSystem");

  public static final String     WORKER_ID = "MainLoaderSystem";

  public PFTPLoaderSystem(PFTPLoaderDescriptor[] fDescriptors) {
    super();
    _fDescriptors = fDescriptors;
  }

  public void setUserProcessingMonitor(UserProcessingMonitor userMonitor) {
    _userMonitor = userMonitor;
  }

  /**
   * Transforms a string into an integer. Returns either the integer
   * representation of the string or 0 (zero) in case of failure.
   */
  protected int getNumber(String val) {
    int num = 0;
    try {
      num = Integer.valueOf(val).intValue();
    } catch (Exception e) {
    }
    return num;
  }

  /**
   * Starts the process.
   */
  public synchronized void runProcessing() {
    String[] dbNames;
    PFTPLoader ftpLoader;
    DBServerConfig dbConf;
    String fName, maintask, str;
    ArrayList<DBMSFtpFile> validNames = null;
    ArrayList<File> validNamesFile = null;
    List<DBServerConfig> processedDB;
    boolean proceed;
    LoaderEngine loaderEngine;
    FileListLoader fll;
    DefaultLoaderMonitor monitor;
    PProxyConfig pConfig;
    boolean resume;
    int i, ftpRetry;
    long taskDelay, ftpDelay;
    boolean isFTP;

    LoggerCentral.info(LOGGER, "*** START PROCESSING *** " + new Date());

    if (LoggerCentral.isRunning()) {
      if (_userMonitor != null)
        _userMonitor.processingDone(UserProcessingMonitor.MSG_TYPE.ERROR);
      LoggerCentral.error(LOGGER, "unable to start : already running");
      LoggerCentral.info(LOGGER, "*** DONE *** " + new Date());
      return;
    }

    LoggerCentral.setRunning(true);
    if (_userMonitor != null)
      _userMonitor.processingStarted();

    if (_fDescriptors == null) {
      if (_userMonitor != null)
        _userMonitor.processingDone(UserProcessingMonitor.MSG_TYPE.ERROR);
      LoggerCentral.error(LOGGER, "no descriptor to handle");
      LoggerCentral.info(LOGGER, "*** DONE *** " + new Date());
      return;
    }

    LoggerCentral.info(LOGGER, "DB list to retrieve:");
    for (PFTPLoaderDescriptor descriptor : _fDescriptors) {
      LoggerCentral.info(LOGGER,
          " - " + descriptor.getProperty(PFTPLoaderDescriptor.DBLIST_KEY));
    }

    // set proxy if any
    pConfig = DBMSAbstractConfig.getProxyConfig();

    // for all descriptors
    for (PFTPLoaderDescriptor descriptor : _fDescriptors) {
      // gets the names of the databases to download
      dbNames = Utils.tokenize(descriptor
          .getProperty(PFTPLoaderDescriptor.DBLIST_KEY));
      if (dbNames.length == 0) {
        if (_userMonitor != null) {
          _userMonitor.processingMessage(WORKER_ID,
              DBServerConfig.CENTRAl_CONF,
              UserProcessingMonitor.PROCESS_TYPE.FTP_LOADING,
              UserProcessingMonitor.MSG_TYPE.OK, "nothing to load");
          _userMonitor.processingDone(UserProcessingMonitor.MSG_TYPE.OK);
        }
        return;
      }
      // figures out if one has to resume a previous aborted download process
      resume = !(PFTPLoaderDescriptor.NO_RESUME_DATE.equals(descriptor
          .getProperty(PFTPLoaderDescriptor.RESUMEDT_KEY)));
      if (resume) {
        LoggerCentral.info(LOGGER, "Resume previous download job");
      }
      DBMSAbstractConfig.setStarterDate(Utils.encodeDate(new Date()));
      // gets some FTP parameters
      taskDelay = this.getNumber(descriptor
          .getProperty(PFTPLoaderDescriptor.TASK_DELAY_KEY));
      ftpDelay = this.getNumber(descriptor
          .getProperty(PFTPLoaderDescriptor.FTP_DELAY_KEY));
      ftpRetry = this.getNumber(descriptor
          .getProperty(PFTPLoaderDescriptor.FTP_RETRY_KEY));

      ftpLoader = new PFTPLoader(0);
      ftpLoader.setUserProcessingMonitor(_userMonitor);
      _taskEngine = new PTaskEngine(taskDelay);
      _taskEngine.setUserProcessingMonitor(_userMonitor);
      maintask = descriptor.getProperty(PFTPLoaderDescriptor.MAINTASK_KEY);
      if (maintask == null) {
        maintask = PFTPLoaderDescriptor.MAINTASK_INFO;
      }

      processedDB = new ArrayList<DBServerConfig>();
      // For each database descriptor
      for (i = 0; i < dbNames.length; i++) {
        if (LoggerCentral.errorMsgEmitted() || LoggerCentral.processAborted())
          break;
        fName = dbNames[i];
        LoggerCentral.info(LOGGER, "Start processing db: " + fName);
        // load a database descriptor
        dbConf = new DBServerConfig();

        LoggerCentral.info(LOGGER, "Loading db descriptor: " + fName);
        proceed = false;
        try {
          dbConf.load(fName);
          proceed = true;
        } catch (FileNotFoundException e) {
          LoggerCentral.error(LOGGER, "File not found: " + fName);
        } catch (IOException e) {
          LoggerCentral.error(LOGGER, "Error while reading: " + fName + ": "
              + e);
        }
        if (!proceed)
          continue;
        if (_userMonitor != null) {
          _userMonitor.startProcessing(dbConf.getName());
        }

        // if no unit tasks and no global tasks : just install in prod
        if (StringUtils.isBlank(dbConf.getUnitPostTasks())
            && StringUtils.isBlank(dbConf.getGlobalPostTasks())) {
          processedDB.add(dbConf);
        } else {

          if (dbConf.getAddress() != null && !dbConf.getAddress().equals("")) {
            LoggerCentral.info(LOGGER, "FTP descriptor file: " + fName);
            isFTP = true;
            validNames = new ArrayList<DBMSFtpFile>();
          } else {
            LoggerCentral.info(LOGGER, "Local descriptor file: " + fName);
            isFTP = false;
            validNamesFile = new ArrayList<File>();
          }
          // the following has been added to help support (we usually do not
          // have user's
          // descriptors, so we dump the content here)
          LoggerCentral.info(LOGGER, "Descriptor content:");
          dbConf.dumpContent(LOGGER);
          LoggerCentral.info(LOGGER, "--");

          if (isFTP) {
            if (pConfig != null) {
              pConfig.configureProxyConnexion(dbConf.getAddress());
            }

            LoggerCentral.info(LOGGER, "Loading db files list (FTP).");
            // get files list to download using a task capable of retrying
            // download
            // process several times in case of failure
            fll = new FTPFileListLoader(dbConf, ftpLoader, validNames,
                ftpDelay, ftpRetry);

            String msg;
            if (PFTPLoaderDescriptor.MAINTASK_INFO.equals(maintask)) {
              msg = "connecting to " + dbConf.getAddress()
                  + " to retrieve databank information";
            } else {
              msg = "connecting to " + dbConf.getAddress()
                  + " to download files";
            }
            LoggerCentral.info(LOGGER, msg);
            if (_userMonitor != null) {
              _userMonitor.processingMessage(WORKER_ID, dbConf.getName(),
                  UserProcessingMonitor.PROCESS_TYPE.FTP_LOADING,
                  UserProcessingMonitor.MSG_TYPE.OK, msg);
            }

          } else {
            LoggerCentral.info(LOGGER, "Loading db files list (local). ");
            fll = new PLocalFileListLoader(dbConf, validNamesFile);
            ((PLocalFileListLoader) fll).setUserProcessingMonitor(_userMonitor);
          }

          fll.start();
          try {
            fll.join();
          } catch (InterruptedException e) {
          }
          // something to do ?
          if (fll.listingOk()) {
            if (validNames != null && validNames.isEmpty()) {
              if (_userMonitor != null) {
                _userMonitor.processingMessage(WORKER_ID, dbConf.getName(),
                    UserProcessingMonitor.PROCESS_TYPE.FTP_LOADING,
                    UserProcessingMonitor.MSG_TYPE.OK, "nothing to load");
              }
              LoggerCentral.info(LOGGER, "Nothing to load.");
            } else if (validNamesFile != null && validNamesFile.isEmpty()) {
              LoggerCentral.info(LOGGER, "Nothing to load.");
            } else {
              if (PFTPLoaderDescriptor.MAINTASK_INFO.equals(maintask)) {
                // just dump the list of files to retrieve
                // already done by FTPLoader task, so does nothing here
              } else {
                // download and process the DB
                LoggerCentral.info(LOGGER, "Loading db files.");
                if (_userMonitor != null) {
                  _userMonitor.startProcessing(dbConf.getName());
                }

                processedDB.add(dbConf);

                if (!resume) {
                  str = dbConf.getLocalFolder()
                      + DBMSAbstractConfig.DOWNLOADING_DIR;
                  if (new File(str).exists()) {
                    if (!PAntTasks.deleteDirectory(str)) {
                      LoggerCentral.error(LOGGER,
                          "unable to delete old index: " + str);
                    }
                  }
                }

                if (isFTP) {
                  monitor = new DefaultLoaderMonitor(_taskEngine, dbConf,
                      validNames.size());
                  loaderEngine = new PFTPLoaderEngine(dbConf, monitor,
                      validNames);
                  ((PFTPLoaderEngine) loaderEngine)
                      .setUserProcessingMonitor(_userMonitor);
                  ((PFTPLoaderEngine) loaderEngine).setScheduleTime(ftpDelay);
                  ((PFTPLoaderEngine) loaderEngine).setRetry(ftpRetry);
                } else {
                  String destPath = dbConf.getLocalTmpFolder();
                  monitor = new DefaultLoaderMonitor(_taskEngine, dbConf,
                      validNamesFile.size());
                  loaderEngine = new PLocalLoaderEngine(dbConf, validNamesFile,
                      destPath, monitor);
                  ((PLocalLoaderEngine) loaderEngine)
                      .setUserProcessingMonitor(_userMonitor);
                }

                loaderEngine.start();

                try {
                  loaderEngine.join();
                } catch (InterruptedException e) {
                  LoggerCentral.error(LOGGER,
                      "Unexpected thread interruption while processing db: "
                          + dbNames[i]);
                }

                if (isFTP) {
                  if (_userMonitor != null) {
                    _userMonitor.processingMessage(WORKER_ID, dbConf.getName(),
                        UserProcessingMonitor.PROCESS_TYPE.FTP_LOADING,
                        UserProcessingMonitor.MSG_TYPE.OK,
                        "file transfer processing done");
                  }
                } else {
                  // copie ok
                }
              }
            }
          } else {
            // we come here in two cases : error or job aborted
            if (_userMonitor != null) {
              if (LoggerCentral.processAborted() == false) {
                _userMonitor.processingMessage(WORKER_ID, dbConf.getName(),
                    UserProcessingMonitor.PROCESS_TYPE.FTP_LOADING,
                    UserProcessingMonitor.MSG_TYPE.ERROR,
                    "unable to retrieve the list of files");
              } else {
                _userMonitor.processingMessage(WORKER_ID, dbConf.getName(),
                    UserProcessingMonitor.PROCESS_TYPE.FTP_LOADING,
                    UserProcessingMonitor.MSG_TYPE.ABORTED,
                    "FTP transaction stopped");
              }
            }
            if (LoggerCentral.processAborted() == false) {
              LoggerCentral.error(LOGGER,
                  "Failed to retrieve files list for db: " + dbNames[i]);
            }
          }
        }
      }
      // final step: install downloaded/processed DBs into production
      if (processedDB.isEmpty() == false) {

        ArrayList<DBServerConfig> installInProd = new ArrayList<DBServerConfig>();

        for (DBServerConfig dbc : processedDB) {
          str = dbc.getHistoryToKeep();
          if (str != null) {
            _taskEngine.addTask(new PTaskHandleHistory(dbc.getLocalFolder(),
                Integer.valueOf(str)), dbc.getName());
          }

          // Install in prod only if required
          if (dbc.mustBeInstallInProduction()) {
            installInProd.add(dbc);
          }
        }

        _taskEngine.addTask(new PTaskInstallInProduction(installInProd),
            DBServerConfig.CENTRAl_CONF);
      }
      // wait for all jobs to terminate
      SystemTerminator st = new SystemTerminator();
      st.start();
      try {
        st.join();
      } catch (InterruptedException e) {
      }
      if (LoggerCentral.processAborted()) {
        break;
      }
    } // end for all descriptors

    if (_userMonitor != null) {
      if (LoggerCentral.errorMsgEmitted()) {
        _userMonitor.processingDone(UserProcessingMonitor.MSG_TYPE.ERROR);
      } else if (LoggerCentral.processAborted()) {
        _userMonitor.processingDone(UserProcessingMonitor.MSG_TYPE.ABORTED);
      } else {
        _userMonitor.processingDone(UserProcessingMonitor.MSG_TYPE.OK);
      }
    }

    if (pConfig != null) {
      pConfig.unsetProxy();
    }
    LoggerCentral.info(LOGGER, "*** DONE *** " + new Date());
  }

  /**
   * This thread aims at retrieving a file list from a remote FTP server using a
   * retry process capable of handling possible FTP session errors.
   */
  private class FTPFileListLoader extends FileListLoader {
    private PFTPLoader             _ftpLoader;
    private ArrayList<DBMSFtpFile> _files;
    private long                   _delay;
    private int                    _retry;

    public FTPFileListLoader(DBServerConfig fsc, PFTPLoader ftpLoader,
        ArrayList<DBMSFtpFile> files, long delay, int ftpRetry) {

      super(fsc);
      _ftpLoader = ftpLoader;
      _files = files;
      _delay = delay;
      _retry = ftpRetry;
    }

    /**
     * Call this method after the thread run to figure out if the file list has
     * been retrieved.
     */
    public void run() {
      int bRet = 0;
      int retry = 0;

      while (retry < _retry) {
        bRet = _ftpLoader.initFilesList(get_dbsc(), _files);
        if (bRet != 0)
          break;
        retry++;
        _files.clear();
        LoggerCentral.info(
            LogFactory.getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                + ".PFTPLoader"), PFTPLoader.CONN_ERR_MSG);
        try {
          sleep(_delay);
        } catch (InterruptedException e) {

        }
      }
      if (bRet == 0) {// failure? Report error now!
        LoggerCentral.error(
            LogFactory.getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                + ".PFTPLoader"), _ftpLoader.getErrorMsg());
      } else if (bRet == 3) {
        LoggerCentral.info(
            LogFactory.getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                + ".PFTPLoader"), _ftpLoader.getErrorMsg());
        LoggerCentral.abortProcess();
      }
      set_ok((bRet == 1));
      if (_userMonitor != null) {
        _userMonitor.endProcessing(get_dbsc().getName());
      }
    }
  }

  /**
   * This thread aims at waiting until all tasks have been executed by the task
   * engine.
   */
  private class SystemTerminator extends Thread {
    public void run() {
      while (true) {
        if (_taskEngine.terminate())
          break;
        try {
          sleep(PTaskEngine.ENGINE_SCHEDULING_TIMER);
        } catch (InterruptedException e) {
        }
      }
      if (LoggerCentral.errorMsgEmitted()) {
        LoggerCentral.error(
            LogFactory.getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                + ".PMirror"), "Processing failed. Check ERROR messages.");
      } else if (LoggerCentral.processAborted()) {
        LoggerCentral.error(
            LogFactory.getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                + ".PMirror"), "Processing aborted.");
      } else {
        LoggerCentral.info(
            LogFactory.getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                + ".PMirror"), "Processing ok.");
      }
    }
  }
}
