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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.io.Util;

import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * This class is a basic FTP file loader.
 * 
 * @author Patrick G. Durand
 */
public class PFTPLoader {
  private static final Log      LOGGER       = LogFactory
                                                 .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                     + ".PFTPLoader");

  UserProcessingMonitor         _userMonitor;
  private String                _errMsg;
  private String                _loaderId;
  private int                   _timeout     = 50000;

  protected static final String CANCEL_MSG   = "job cancelled";
  protected static final String CONN_ERR_MSG = "Server does not answer. Retry...";

  public static final String    WORKER_ID    = "FTPLoader";

  //
  // http://www.informit.com/guides/content.aspx?g=java&seqNum=40&rl=1
  // Ftp et Proxy:
  // http://wiki.apache.org/jakarta-commons/Net/FrequentlyAskedQuestions
  // Ftp et Proxy:
  // http://www.koders.com/java/fid83474576AF8E037E96D72957E5726B750C386DE0.aspx?s=ftp+proxy
  // sinon rechercher 'ftp proxy', ou bien cf classe SetProxy.java recuperee
  // cette classe provient de Ant!!!! Verifier donc si on peut faire du
  // ftp/proxy.
  // et bien oui: http://ant.apache.org/manual/proxy.html
  // cf aussi site web apache/commons-net

  @SuppressWarnings("unused")
  private PFTPLoader() {
  }

  public PFTPLoader(int id) {
    _loaderId = WORKER_ID + "-" + id;
  }

  public String getLoaderId() {
    return _loaderId;
  }

  /**
   * Returns the latest error message. Returns null if no error msg was emitted.
   * This method should be used in cooperation with
   * this.download(DBServerConfig, FTPFile).
   * 
   */
  public String getErrorMsg() {
    return _errMsg;
  }

  public void setUserProcessingMonitor(UserProcessingMonitor userMonitor) {
    _userMonitor = userMonitor;
  }

  public void closeConnection(FTPClient ftp) {
    // Logout from the FTP Server and disconnect
    // for unknown reasons, sometimes got a Connection Reset SocketException.
    // do nothing in that case
    try {
      ftp.logout();
    } catch (Exception e) {
    }
  }

  public FTPClient openConnection(DBServerConfig fsc) {
    FTPClient ftp;
    int reply;

    ftp = new FTPClient();
    try {
      ftp.setDataTimeout(_timeout);
      ftp.setDefaultTimeout(_timeout);

      // no longer available in commons-net 3
      // ftp.setReaderThread(false);

      // according to
      // http://www.coderanch.com/t/207085/sockets/java/FTP-connection-Proxy
      // the following line should be used when using proxy... need checking
      // cf aussi: http://wiki.apache.org/commons/Net/FrequentlyAskedQuestions
      // ftp.setRemoteVerificationEnabled(false);

      // Connect and logon to FTP Server
      ftp.connect(fsc.getAddress());
      ftp.login(fsc.getUserName(), fsc.getPassWord());

      // Check the reply
      reply = ftp.getReplyCode();
      LoggerCentral.info(LOGGER,
          _loaderId + ": connected to " + fsc.getAddress() + ": " + reply);
      if (!FTPReply.isPositiveCompletion(reply)) {
        throw new Exception(_loaderId + ": FTP server refused connection.");
      }

      // configure session
      ftp.setFileType(FTP.BINARY_FILE_TYPE);
      ftp.enterLocalPassiveMode();
    } catch (Exception ex) {
      if (ftp.isConnected()) {// if exception comes after connection is
                              // established, close it
        try {
          ftp.disconnect();
        } catch (IOException ioe) {
          // do nothing
        }
      }
      ftp = null;
    }
    return ftp;
  }

  /**
   * Download a single file from an FTP server. This method has been designed to
   * be called successively several times in case of failure. When the method
   * fails, it returns false and an error message can be retrieved (immediately
   * after a call to this method) using the getErrorMsg() method. In that case,
   * one can call again download to retry downloading the file. This method is
   * intended to be call several times. As a consequence, it has to be called as
   * follows: (1) call openConnection() on this KFTPLoader, (2) cal as many as
   * downloadFile() as needed, (3) call closeConnection() to finish the work.
   * 
   * @param fsc
   *          the FTP server config object.
   * @param rFile
   *          the file to download.
   * @param fileNum
   *          the file order number. Zero-based.
   * @param totFiles
   *          total number of files to download
   * @return 1 if success, 0 if failure, 2 if skip (file already loaded ; when
   *         resuming from a previous work) and 3 if aborted.
   */
  public int downloadFile(FTPClient ftp, DBServerConfig fsc, DBMSFtpFile rFile,
      int fileNum, int totFiles) {
    FileOutputStream fos = null;
    InputStream ftpIS = null;
    File file, filegz;
    String remoteFName, name;
    Date remoteFDate;
    long remoteFSize, lclFSize;
    int bRet;

    _errMsg = null;
    bRet = 1;
    // check whether remote file already exists locally
    remoteFName = rFile.getFtpFile().getName();
    remoteFDate = rFile.getFtpFile().getTimestamp().getTime();
    remoteFSize = rFile.getFtpFile().getSize();
    file = new File(fsc.getLocalTmpFolder() + remoteFName);
    lclFSize = file.length();
    if (file.exists() && lclFSize == remoteFSize) {
      LoggerCentral.info(LOGGER,
          _loaderId + ": Skip existing file: " + file.getAbsolutePath());
      if (_userMonitor != null) {
        _userMonitor.processingMessage(_loaderId, fsc.getName(),
            UserProcessingMonitor.PROCESS_TYPE.FTP_LOADING,
            UserProcessingMonitor.MSG_TYPE.OK, "Skipping already loaded file "
                + (fileNum + 1) + "/" + totFiles + ": " + remoteFName);
      }
      return 2;
    } else {
      // check gzip file: remove extension and check if file exist. If yes,
      // skip.
      name = file.getAbsolutePath();
      if (name.endsWith(".gz")) {
        filegz = new File(name.substring(0, name.length() - 3));
        if (filegz.exists()) {
          LoggerCentral.info(LOGGER, _loaderId + ": Skip existing file: "
              + file.getAbsolutePath() + ": gunzipped version already here.");
          if (_userMonitor != null) {
            _userMonitor.processingMessage(_loaderId, fsc.getName(),
                UserProcessingMonitor.PROCESS_TYPE.FTP_LOADING,
                UserProcessingMonitor.MSG_TYPE.OK,
                "Skipping already loaded file " + (fileNum + 1) + "/"
                    + totFiles + ": " + remoteFName);
          }
          return 2;
        }
      }
    }
    // if not: start download
    if (_userMonitor != null) {
      _userMonitor.processingMessage(_loaderId, fsc.getName(),
          UserProcessingMonitor.PROCESS_TYPE.FTP_LOADING,
          UserProcessingMonitor.MSG_TYPE.OK,
          "loading file " + (fileNum + 1) + "/" + totFiles + ": " + remoteFName
              + " (" + Utils.getBytes(remoteFSize) + ")");
    }
    try {
      // enter remote directory
      if (ftp.changeWorkingDirectory(rFile.getRemoteDir())) {
        file = new File(fsc.getLocalTmpFolder());
        if (!file.exists()) {
          LoggerCentral.info(LOGGER, "  " + _loaderId + ": create local dir: "
              + fsc.getLocalTmpFolder());
          file.mkdirs();
        }
        // download file
        LoggerCentral.info(LOGGER,
            "  " + _loaderId + ": download: " + rFile.getRemoteDir()
                + remoteFName);
        file = new File(fsc.getLocalTmpFolder() + remoteFName);
        fos = new FileOutputStream(file);
        ftpIS = ftp.retrieveFileStream(remoteFName);
        if (ftpIS == null) {
          throw new Exception(_loaderId
              + ": unable to open remote input stream: " + ftp.getReplyString());
        }
        Util.copyStream(ftpIS, fos, Util.DEFAULT_COPY_BUFFER_SIZE, remoteFSize,
            new MyCopyStreamListener(_loaderId, _userMonitor, fsc.getName(),
                remoteFName, remoteFSize));
        IOUtils.closeQuietly(ftpIS);
        fos.flush();
        IOUtils.closeQuietly(fos);
        if (ftp.completePendingCommand()) {
          file.setLastModified(remoteFDate.getTime());
        } else {
          throw new Exception(_loaderId + ": unable to download full file.");
        }
      } else {
        throw new Exception(_loaderId + ": unable to enter remote dir: "
            + rFile.getRemoteDir());
      }
    } catch (MyCopyInteruptException ex) {
      _errMsg = ex.getMessage();
      bRet = 3;
    } catch (Exception e) {
      _errMsg = "Error during FTP download: " + e.getMessage();
      bRet = 0;
    } finally {
      if (_userMonitor != null) {
        _userMonitor.processingMessage(_loaderId, fsc.getName(),
            UserProcessingMonitor.PROCESS_TYPE.FTP_LOADING,
            bRet == 1 ? UserProcessingMonitor.MSG_TYPE.OK
                : UserProcessingMonitor.MSG_TYPE.ERROR, "done loading "
                + remoteFName);
      }
      IOUtils.closeQuietly(ftpIS);
      IOUtils.closeQuietly(fos);
    }
    return bRet;
  }

  private void dumpFileListInLog(DBServerConfig fsc, List<DBMSFtpFile> fNames) {
    long val, totBytes = 0;
    int nFiles;
    String curPath = "";
    nFiles = fNames.size();

    LoggerCentral.info(LOGGER, "Files matching constraints : " + nFiles);

    for (DBMSFtpFile rFile : fNames) {
      if (curPath.equals(rFile.getRemoteDir()) == false) {
        LoggerCentral.info(LOGGER, "  Files in: " + rFile.getRemoteDir());
        curPath = rFile.getRemoteDir();
      }
      val = rFile.getFtpFile().getSize();
      LoggerCentral.info(LOGGER, "    " + rFile.getFtpFile().getName() + ": "
          + val + " bytes");
      totBytes += val;
    }
    LoggerCentral.info(LOGGER, "Total bytes to download: " + totBytes);
    if (_userMonitor != null) {
      _userMonitor.fileTransferInfo(_loaderId, fsc, nFiles, totBytes);
    }
  }

  /**
   * Retrieves a files list from an FTP server. This method has been designed to
   * be called successively several times in case of failure. When the method
   * fails, it returns false and an error message can be retrieved (immediately
   * after a call to this method) using the getErrorMsg() method. In that case,
   * one can call again initFilesList to retry downloading the files list. This
   * method works in an atomic model: it opens FTP remote connection, do its
   * jobs, then close the FTP connection.
   * 
   * @param fsc
   *          the FTP server config object.
   * @param validNames
   *          will be filled with a list of FTPFile. Always passes in here an
   *          allocated list.
   * 
   * @return 1 if success, 0 if failure and 3 if aborted.
   */
  public int initFilesList(DBServerConfig fsc, List<DBMSFtpFile> validNames) {
    CalendarMatcher cMatcher;
    NameMatcher nMatcher;
    FTPClient ftp;
    FTPFile[] remoteFiles;
    FTPFile rFile;
    StringTokenizer remoteFolders;
    String confFolders, rFolder, excludeStr;
    List<String> rPaths;
    ArrayList<String> startList;
    NameMatcher exclude;
    boolean bDownload;
    int i, reply, bRet = 1;

    _errMsg = null;
    // Connect and logon to FTP Server
    LoggerCentral.info(LOGGER, "Opening connection to " + fsc.getAddress());
    ftp = new FTPClient();
    try {
      ftp.setDataTimeout(_timeout);
      ftp.setDefaultTimeout(_timeout);

      // no longer available in commons-net 3
      // ftp.setReaderThread(false);

      // according to
      // http://www.coderanch.com/t/207085/sockets/java/FTP-connection-Proxy
      // the following line should be used when using proxy... need checking
      // ftp.setRemoteVerificationEnabled(false);
      // cf aussi: http://wiki.apache.org/commons/Net/FrequentlyAskedQuestions

      ftp.connect(fsc.getAddress());
      ftp.login(fsc.getUserName(), fsc.getPassWord());

      // Check the reply
      reply = ftp.getReplyCode();
      LoggerCentral.info(LOGGER, "Connected to " + fsc.getAddress() + ": "
          + reply);
      if (!FTPReply.isPositiveCompletion(reply)) {
        throw new Exception("FTP server refused connection.");
      }
      // configure session
      ftp.setFileType(FTP.BINARY_FILE_TYPE);
      ftp.enterLocalPassiveMode();
      // get list of remote folders
      confFolders = fsc.getRemoteFolders();
      remoteFolders = new StringTokenizer(confFolders, ",");
      excludeStr = fsc.getRemotePatternsToExclude();
      if (excludeStr != null && excludeStr.length() > 1) {
        exclude = new NameMatcher(Utils.tokenize(excludeStr), null);
      } else {
        exclude = null;
      }
      while (remoteFolders.hasMoreTokens()) {
        // explore remote folders with criteria (each remote folder coming from
        // the
        // config file may contain regular expression enabling ftp server
        // navigation)
        rFolder = remoteFolders.nextToken();
        startList = new ArrayList<String>();
        startList.add("/");
        rPaths = exploreFTPServer(startList, new StringTokenizer(rFolder, "/"),
            ftp, exclude);
        for (String rPath : rPaths) {
          // enter remote directory
          if (ftp.changeWorkingDirectory(rPath)) {
            // List the files in the directory
            remoteFiles = ftp.listFiles();
            LoggerCentral.info(LOGGER, "Number of entries in : " + rPath + ": "
                + remoteFiles.length);
            cMatcher = fsc.getCalendarMatcher();
            nMatcher = fsc.getFileMatcher();
            // get all the files matching criteria
            for (i = 0; i < remoteFiles.length; i++) {
              rFile = remoteFiles[i];
              // isSymbolicLink removed: starting on 2013, InterPro provides
              // data files through symlinks...
              if (rFile == null || rFile.isDirectory() /*
                                                        * ||
                                                        * rFile.isSymbolicLink()
                                                        */) {
                continue;
              }
              bDownload = true;
              if (cMatcher != null) {
                bDownload = cMatcher.match(rFile.getTimestamp().getTime());
              }
              if (!bDownload)
                continue;
              if (nMatcher != null) {
                bDownload = nMatcher.match(rFile.getName());
              }
              if (bDownload) {
                validNames.add(new DBMSFtpFile(rPath, rFile));
              }
              if (_userMonitor != null && _userMonitor.jobCancelled()) {
                throw new MyCopyInteruptException();
              }
            }
          } else {
            throw new Exception("unable to enter remote dir: " + rPath);
          }
        }
      }

      dumpFileListInLog(fsc, validNames);
      // Logout from the FTP Server and disconnect
      // for unknown reasons, sometimes got a Connection Reset SocketException.
      // do nothing in that case
      try {
        ftp.logout();
      } catch (Exception e) {
      }
    } catch (MyCopyInteruptException ex) {
      _errMsg = ex.getMessage();
      bRet = 3;
    } catch (Exception e) {
      _errMsg = "Error during FTP listing: " + e.toString();
      bRet = 0;
    } finally {
      if (ftp.isConnected()) {
        try {
          ftp.disconnect();
        } catch (IOException ioe) {
          // do nothing
        }
      }
      LoggerCentral.info(LOGGER, "Closing connection to " + fsc.getAddress()
          + ". Ending code: " + bRet);
    }
    return bRet;
  }

  private List<String> exploreFTPServer(List<String> currentList,
      StringTokenizer tokenizer, FTPClient ftp, NameMatcher exclude)
      throws Exception {
    ArrayList<String> newList;
    String token, fName;
    NameMatcher nMatcher;
    FTPFile[] remoteFiles;

    if (tokenizer.hasMoreTokens() == false) {
      // we have explore all possible paths on the server. Now it's time to
      // check if we have to discard some
      if (exclude == null)
        return currentList;
      newList = new ArrayList<String>();
      for (String path : currentList) {
        if (!exclude.match(path)) {
          newList.add(path);
        }
      }
      return newList;
    }
    newList = new ArrayList<String>();
    token = tokenizer.nextToken();
    nMatcher = new NameMatcher(new String[] { token }, null);
    for (String path : currentList) {
      if (ftp.changeWorkingDirectory(path)) {
        // List the files in the directory
        remoteFiles = ftp.listFiles();
        for (FTPFile rFile : remoteFiles) {
          if (rFile == null || !(rFile.isDirectory() || rFile.isSymbolicLink())) {
            continue;
          }
          fName = rFile.getName();
          if (nMatcher.match(fName)) {
            newList.add(path + fName + "/");
          }
        }
      } else {
        throw new Exception("unable to enter remote dir: " + path);
      }
    }

    return exploreFTPServer(newList, tokenizer, ftp, exclude);
  }
}
