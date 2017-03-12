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

import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPClient;

import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * This class is responsible for gently downloading a set of files from an FTP
 * server.
 * 
 * @author Patrick G. Durand
 */
public class PFTPLoaderEngine extends LoaderEngine {
  private List<DBMSFtpFile>     _files;
  private Iterator<DBMSFtpFile> _iterator;
  private UserProcessingMonitor _userMonitor;

  private long                  _scheduleTime = 5000;
  private int                   _retry        = 3;
  private int                   _counter      = 0;

  /**
   * Constructor.
   * 
   * @param fsc
   *          the FTP config.
   * @param monitor
   *          a monitor aims at following what is happening dufing FTP process.
   * @param files
   *          the list of files to retrieve.
   */
  public PFTPLoaderEngine(DBServerConfig fsc, LoaderMonitor monitor,
      List<DBMSFtpFile> files) {
    super(fsc, monitor);
    _files = files;
    _iterator = _files.iterator();
  }

  public void setScheduleTime(long time) {
    _scheduleTime = (time < 1000 ? 5000 : time);
  }

  public void setUserProcessingMonitor(UserProcessingMonitor userMonitor) {
    _userMonitor = userMonitor;
  }

  public void setRetry(int r) {
    if (r < 1 || r > 5) {
      _retry = 3;
    } else {
      _retry = r;
    }
  }

  public void run() {
    LoadWorker[] workers;
    int nbWorkers = DBMSAbstractConfig.getFileCopyWorkers();

    workers = new LoadWorker[nbWorkers];
    for (int i = 0; i < nbWorkers; i++) {
      workers[i] = new LoadWorker(i);
      workers[i].start();
    }
    for (int i = 0; i < nbWorkers; i++) {
      try {
        workers[i].join();
      } catch (Exception e) {// no bad
      }
    }
  }

  private DataShuttle nextFtpFile() {
    synchronized (this) {
      if (_iterator.hasNext()) {
        DataShuttle ds = new DataShuttle(_iterator.next(), _counter);
        _counter++;
        return ds;
      } else {
        return null;
      }
    }
  }

  private class LoadWorker extends Thread {
    private FTPClient  _ftp = null;
    private PFTPLoader _loader;
    private int        _id;

    private LoadWorker(int id) {
      _id = id;
    }

    private FTPClient getFtpClient() {
      if (_ftp != null && _ftp.isConnected()) {
        return _ftp;
      }
      _ftp = _loader.openConnection(get_dbsc());
      return _ftp;
    }

    public void run() {
      DataShuttle file;
      FTPClient ftp;
      String fName;
      int retry, nFiles, bRet;

      bRet = 0;
      _loader = new PFTPLoader(_id);
      _loader.setUserProcessingMonitor(_userMonitor);
      nFiles = _files.size();
      while ((file = nextFtpFile()) != null) {
        fName = file.getFile().getFtpFile().getName();
        if (_monitor != null)
          _monitor.beginLoading(fName);
        // try to get a ftp connection
        retry = 0;
        ftp = null;
        while (retry < _retry) {
          ftp = getFtpClient();
          if (ftp != null)
            break;
          retry++;
          LoggerCentral.info(
              LogFactory.getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                  + ".PFTPLoader"), _loader.getLoaderId() + ": "
                  + PFTPLoader.CONN_ERR_MSG);
          try {
            sleep(_scheduleTime);
          } catch (InterruptedException e) {
          }
        }
        if (ftp != null) {
          // start loading
          retry = 0;
          while (retry < _retry) {
            bRet = _loader.downloadFile(ftp, get_dbsc(), file.getFile(),
                file.getFileNum(), nFiles);
            if (bRet != 0)
              break;
            retry++;
            LoggerCentral.info(
                LogFactory.getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                    + ".PFTPLoader"), PFTPLoader.CONN_ERR_MSG);
            try {
              sleep(_scheduleTime);
            } catch (InterruptedException e) {
            }
          }
        }
        if (bRet == 0) {// failure? Report error now!
          LoggerCentral.error(
              LogFactory.getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                  + ".PFTPLoader"), _loader.getErrorMsg());
        } else if (bRet == 3) {
          LoggerCentral.info(
              LogFactory.getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                  + ".PFTPLoader"), _loader.getErrorMsg());
          LoggerCentral.abortProcess();
        }
        if (_monitor != null) {
          if (bRet == 0) {
            _monitor.doneLoading(fName, LoaderMonitor.STATUS_FAILURE);
          } else if (bRet == 3) {
            _monitor.doneLoading(fName, LoaderMonitor.STATUS_ABORTED);
          } else {
            _monitor.doneLoading(fName, LoaderMonitor.STATUS_OK);
          }
          // Optional todo: in case of failure: return (and stop the overall
          // download process)
        }
        if (bRet == 0 || bRet == 3)// failure or abort: stop processing of files
          break;
      }
    }
  }

  private class DataShuttle {
    private DBMSFtpFile file;
    private int         fileNum;

    public DataShuttle(DBMSFtpFile file, int fileNum) {
      super();
      this.file = file;
      this.fileNum = fileNum;
    }

    public DBMSFtpFile getFile() {
      return file;
    }

    public int getFileNum() {
      return fileNum;
    }

  }
}
