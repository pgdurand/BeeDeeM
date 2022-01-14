/* Copyright (C) 2007-2020 Patrick G. Durand
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
  private String                _fileOfFiles;
  private FTPClient             _ftp = null;
  
  protected static final String CANCEL_MSG   = "job cancelled";
  protected static final String CONN_ERR_MSG = "Server does not answer. Retry...";

  public static final String    FTP_WORKER    = "FTPLoader";

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
    _loaderId = getWorkerBaseName() + "-" + id;
  }

  public String getLoaderId() {
    return _loaderId;
  }

  public String getWorkerBaseName() {
	  return FTP_WORKER;
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
  /**
   * Set a path to a file. When using Install Tool with info task-name,
   * list of files to download will be written in such a file.
   */
  public void setFileOfFiles(String fof) {
    _fileOfFiles = fof;
  }
  public void closeLoader() {
    // Logout from the FTP Server and disconnect
    // for unknown reasons, sometimes got a Connection Reset SocketException.
    // do nothing in that case
    try {
      _ftp.logout();
    } catch (Exception e) {
    }
  }

  public boolean readyToDownload() {
    return (_ftp != null && _ftp.isConnected());
  }
  
  public boolean prepareLoader(DBServerConfig fsc) {
    FTPClient ftp = null;

    ftp = new FTPClient();
    if(!configureFtpClient(ftp, fsc)) {
    	return false;
    }
    
    _ftp = ftp;
    return true;
  }

  private boolean configureFtpClient(FTPClient ftp, DBServerConfig fsc) {
	  int reply;  
	  boolean bRet = true;
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
            getLoaderId() + ": connected to " + fsc.getAddress() + ": " + reply);
        if (!FTPReply.isPositiveCompletion(reply)) {
          throw new Exception(getLoaderId() + ": FTP server refused connection.");
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
        bRet = false;
        
      }
	  return bRet;
  }
  /**
   * Download a file from remote server.
   * 
   * @param fsc bank descriptor
   * @param rFile remote file descriptor
   * @param file local file path
   * 
   * @return 1 if success, 0 if failure, 2 if skip (file already loaded ; when
   *         resuming from a previous work) and 3 if aborted.
   * */
	protected int downloadFile(DBServerConfig fsc, DBMSFile rFile, File file, long lclFSize) {
		FileOutputStream fos = null;
		InputStream ftpIS = null;
		String remoteFName;
		long remoteFSize;
		Date remoteFDate;
		int bRet = 1;
		
		remoteFName = rFile.getName();
		remoteFDate = rFile.getDateStamp();
		remoteFSize = rFile.getSize();
		try {
			// enter remote directory
			if (_ftp.changeWorkingDirectory(rFile.getRemoteDir())) {
				// download file
				LoggerCentral.info(LOGGER, "  " + getLoaderId() + ": download: " + rFile.getRemoteDir() + remoteFName);
				
				if (lclFSize!=0) {
				  fos = new FileOutputStream(file, true);
				  _ftp.setRestartOffset(lclFSize);
				}
				else {
          fos = new FileOutputStream(file);  
          _ftp.setRestartOffset(0l);
				}
				ftpIS = _ftp.retrieveFileStream(remoteFName);
				if (ftpIS == null) {
					throw new Exception(getLoaderId() + ": unable to open remote input stream: " + _ftp.getReplyString());
				}
				Util.copyStream(ftpIS, fos, Util.DEFAULT_COPY_BUFFER_SIZE, remoteFSize,
						new MyCopyStreamListener(getLoaderId(), _userMonitor, fsc.getName(), remoteFName, remoteFSize, lclFSize));
				IOUtils.closeQuietly(ftpIS);
				fos.flush();
				IOUtils.closeQuietly(fos);
				if (_ftp.completePendingCommand()) {
					file.setLastModified(remoteFDate.getTime());
				} else {
					throw new Exception(getLoaderId() + ": unable to download full file.");
				}
			} else {
				throw new Exception(getLoaderId() + ": unable to enter remote dir: " + rFile.getRemoteDir());
			}
		} catch (MyCopyInteruptException ex) {
			_errMsg = ex.getMessage();
			bRet = 3;
		} catch (Exception e) {
			_errMsg = "Error during FTP download: " + e.getMessage();
			bRet = 0;
		} finally {
			IOUtils.closeQuietly(ftpIS);
			IOUtils.closeQuietly(fos);
		}
		return bRet;
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
  public int downloadFile(DBServerConfig fsc, DBMSFile rFile,
      int fileNum, int totFiles) {
	  File file, filegz, tmpDir;
    String remoteFName, name, msg;
    long remoteFSize, lclFSize;
    int iRet;
    
    _errMsg = null;
    // check whether remote file already exists locally
    remoteFName = rFile.getName();
    //remoteFDate = rFile.getFtpFile().getTimestamp().getTime();
    remoteFSize = rFile.getSize();
    file = new File(fsc.getLocalTmpFolder() + remoteFName);
    lclFSize = file.length();
    if (file.exists() && lclFSize == remoteFSize) {
      msg = "Skipping already loaded file "
          + (fileNum + 1) + "/" + totFiles + ": ";
      LoggerCentral.info(LOGGER,
          getLoaderId() + ": " + msg + file.getAbsolutePath());
      if (_userMonitor != null) {
        _userMonitor.processingMessage(getLoaderId(), fsc.getName(),
            UserProcessingMonitor.PROCESS_TYPE.FTP_LOADING,
            UserProcessingMonitor.MSG_TYPE.OK, msg + remoteFName);
      }
      return 2;
    } else {
      // check gzip file: remove extension and check if file exist. If yes,
      // skip.
      name = file.getAbsolutePath();
      if (name.endsWith(".gz")) {
        filegz = new File(name.substring(0, name.length() - 3));
        if (filegz.exists()) {
          msg = "Skipping already loaded file " + (fileNum + 1) + "/"
              + totFiles + ": ";
          LoggerCentral.info(LOGGER, getLoaderId() + ": " + msg
              + file.getAbsolutePath() + ": gunzipped version already here.");
          if (_userMonitor != null) {
            _userMonitor.processingMessage(getLoaderId(), fsc.getName(),
                UserProcessingMonitor.PROCESS_TYPE.FTP_LOADING,
                UserProcessingMonitor.MSG_TYPE.OK,
                msg + remoteFName);
          }
          return 2;
        }
      }
    }
    
    // need to manage directory creation
    tmpDir = new File(fsc.getLocalTmpFolder());
    if (!tmpDir.exists()) {
      LoggerCentral.info(LOGGER, "  " + getLoaderId() + ": create local dir: " + fsc.getLocalTmpFolder());
      tmpDir.mkdirs();
    }

    // if not: start download
    msg = "loading file " + (fileNum + 1) + "/" + totFiles + ": " + remoteFName
        + " (" + Utils.getBytes(remoteFSize) + ")";
    LoggerCentral.info(LOGGER, getLoaderId() + ": " + msg);
    if (_userMonitor != null) {
      _userMonitor.processingMessage(getLoaderId(), fsc.getName(),
          UserProcessingMonitor.PROCESS_TYPE.FTP_LOADING,
          UserProcessingMonitor.MSG_TYPE.OK,
          msg);
    }
    iRet = downloadFile(fsc, rFile, file, lclFSize<remoteFSize?lclFSize:0);
    if (_userMonitor != null) {
      _userMonitor.processingMessage(getLoaderId(), fsc.getName(), 
          UserProcessingMonitor.PROCESS_TYPE.FTP_LOADING,
          iRet == 1 ? UserProcessingMonitor.MSG_TYPE.OK : UserProcessingMonitor.MSG_TYPE.ERROR,
          "done loading " + remoteFName);
    }
    return iRet;
  }

  private void dumpFileListInLog(DBServerConfig fsc, List<DBMSFile> fNames) {
    long val, totBytes = 0;
    int nFiles;
    String curPath = "";
    nFiles = fNames.size();

    LoggerCentral.info(LOGGER, "Files matching constraints : " + nFiles);

    for (DBMSFile rFile : fNames) {
      if (curPath.equals(rFile.getRemoteDir()) == false) {
        LoggerCentral.info(LOGGER, "  Files in: " + rFile.getRemoteDir());
        curPath = rFile.getRemoteDir();
      }
      val = rFile.getSize();
      LoggerCentral.info(LOGGER, "    " + rFile.getName() + ": "
          + Utils.getBytes(val));
      totBytes += val;
    }
    LoggerCentral.info(LOGGER, "Total bytes to download: " + Utils.getBytes(totBytes));
    if (_userMonitor != null) {
      _userMonitor.fileTransferInfo(getLoaderId(), fsc, nFiles, totBytes);
    }
  }

  private void dumpFileListInFof(DBServerConfig fsc, List<DBMSFile> fNames) {
    if (_fileOfFiles==null) {
      return;
    }
    
    Path   path    = Paths.get(_fileOfFiles);
    String header  = "ftp://", 
           newLine = System.getProperty("line.separator");
    
    header += fsc.getAddress();

    try (BufferedWriter writer = Files.newBufferedWriter(path)) {
      for (DBMSFile rFile : fNames) {
        writer.write(header);
        writer.write("\t");
        rFile.write(writer);
        writer.write(newLine);
      }
      writer.flush();
    } catch (IOException e) {
      LoggerCentral.error(LOGGER,"Unable to write list of files in: "+_fileOfFiles);
      LoggerCentral.error(LOGGER,e.toString());
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
  public int initFilesList(DBServerConfig fsc, List<DBMSFile> validNames) {
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
                validNames.add(new DBMSFile(rPath, rFile));
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
      dumpFileListInFof(fsc, validNames);
      
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
