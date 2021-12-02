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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.io.Util;

import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;
import bzh.plealog.dbmirror.util.runner.DBMSExecNativeCommand;

/**
 * This class allows to research files in directories defined in the file
 * descriptor (DBServerConfig).
 * 
 * 
 * @author Patrick G. Durand
 * 
 */
public class PLocalLoader {

  private static final Log      LOGGER    = LogFactory
                                              .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                  + ".PLocalLoader");
  public static final String    WORKER_ID = "LocalLoader";

  private DBServerConfig        _dbsc;
  private UserProcessingMonitor _userMonitor;

  public PLocalLoader(DBServerConfig dbsc) {
    _dbsc = dbsc;
  }

  public void setUserProcessingMonitor(UserProcessingMonitor userMonitor) {
    _userMonitor = userMonitor;
  }

  /**
   * Retrieves a files list from a local directory.
   * 
   * @param validNames
   *          will be filled with a list of File.
   * 
   * @return 1 if success, 0 if failure.
   */
  public boolean initFilesList(List<DBMSFile> validNames) {
    NameMatcher nMatcher;
    StringTokenizer remoteLocalFolders;
    String confFolders, rlFolder, excludeStr;
    List<String> rPaths = null;
    ArrayList<String> startList;
    NameMatcher exclude;
    boolean bDownload;
    int i = 1;
    String windowsRoot;
    File currentFile, aFile;
    File[] remoteFiles;
    File rFile;
    long size, totSize=0l;
    
    // get list of remote folders
    confFolders = _dbsc.getRemoteLocalFolders();
    // we just have a list of files ?
    if (confFolders == null || confFolders.length() == 0) {
      // when getRemoteLocalFolders() returns nothing, we consider that
      // getIncludeFileList()
      // returns a list of absolute file path ; introduce to enable local db
      // installation
      // from UiInstaller
      confFolders = _dbsc.getIncludeFileList();
      remoteLocalFolders = new StringTokenizer(confFolders, ",");
      LoggerCentral.info(LOGGER, "Preparing list of files:");
      while (remoteLocalFolders.hasMoreTokens()) {
        aFile = new File(remoteLocalFolders.nextToken().trim());
        size = aFile.length();
        totSize+=size;
        LoggerCentral.info(LOGGER, "  " + aFile.getAbsolutePath() + " (" + Utils.getBytes(size) + ")");
        validNames.add(new DBMSFile(aFile));
      }
    } else {
      confFolders = DBMSExecNativeCommand.formatNativePath(confFolders, false,
          false).replace(File.separatorChar, '|');
      remoteLocalFolders = new StringTokenizer(confFolders, ",");
      excludeStr = _dbsc.getRemoteLocalPatternsToExclude();

      // check if there are some directories where the system doesn't research
      // into.
      if (excludeStr != null && excludeStr.length() > 1) {
        exclude = new NameMatcher(Utils.tokenize(excludeStr), null);
      } else {
        exclude = null;
      }
      LoggerCentral.info(LOGGER, "Preparing list of files:");
      while (remoteLocalFolders.hasMoreTokens()) {
        // explore remote folders with criteria (each remote folder coming from
        // the
        // config file may contain regular expression enabling ftp server
        // navigation)
        rlFolder = remoteLocalFolders.nextToken();
        startList = new ArrayList<String>();

        if (DBMSExecNativeCommand.getOSType() == DBMSExecNativeCommand.WINDOWS_OS) {
          windowsRoot = rlFolder.substring(0, 2);
          startList.add(windowsRoot);
          rlFolder = rlFolder.substring(2);
        } else {
          startList.add("/");
        }
        // Return the list of the directory which match the regex.
        try {
          rPaths = exploreLocalDirectory(startList, new StringTokenizer(
              rlFolder, "|"), exclude);

          for (String rPath : rPaths) {
            // enter remote directory
            // List the files in the directory
            currentFile = new File(DBMSExecNativeCommand.formatNativePath(
                rPath, false, true));
            remoteFiles = currentFile.listFiles();
            // LoggerCentral.info(LOGGER, "Number of entries in : "+ rPath+": "+
            // remoteFiles.length );
            nMatcher = _dbsc.getFileMatcher();
            // get all the files matching criteria
            for (i = 0; i < remoteFiles.length; i++) {
              rFile = remoteFiles[i];
              if (rFile == null || rFile.isDirectory()) {
                continue;
              }
              bDownload = true;
              if (!bDownload)
                continue;
              if (nMatcher != null) {
                bDownload = nMatcher.match(rFile.getName());
              }
              if (bDownload) {
                validNames.add(new DBMSFile(rFile));
                size = rFile.length();
                totSize+=size;
                LoggerCentral.info(LOGGER, "  " + rFile.getAbsolutePath() + " (" + Utils.getBytes(size) + ")");
              }
            }
          }
        } catch (Exception e) {
          LoggerCentral.error(LOGGER, e.getMessage());
          return false;
        }
      }

    }

    if (validNames.size() == 0) {
      // just a warn because a databank can depend of another one which will
      // prepare files for it
      // (example : eggnog databanks will use the files prepared by the
      // EggNogIndex one)
      LoggerCentral.warn(LOGGER, "No files found");
    } else {
      LoggerCentral.info(LOGGER, "Nb. of files matching criteria: "
          + validNames.size());
      LoggerCentral.info(LOGGER, "Total bytes to copy: " + Utils.getBytes(totSize));
    }

    return true;
  }

  /**
   * Check if the regex match the directories found from the root of the system
   * 
   * @param currentList
   *          Directory which must be scanned
   * @param tokenizer
   *          the regEx which defined if the directory can be keep
   * @param exclude
   *          the regEx which defined the directory which must be excluded
   * 
   * @return the list of the directory kept
   * 
   * @throws Exception
   */
  private List<String> exploreLocalDirectory(List<String> currentList,
      StringTokenizer tokenizer, NameMatcher exclude) throws Exception {
    ArrayList<String> newList;
    String token, fName;
    NameMatcher nMatcher;
    File[] remoteFiles;
    File currentFile;
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
      if (!path.endsWith("/")) {
        path = path.trim() + "/";
      }
      currentFile = new File(path);
      if (currentFile.exists()) {
        // List the files in the directory
        remoteFiles = currentFile.listFiles();
  
        for (File rFile : remoteFiles) {
          if (rFile == null || !(rFile.isDirectory())) {
            continue;
          }
          fName = rFile.getName();
          if (nMatcher.match(fName)) {
            newList.add(path + fName);
          }
        }
      }
      else {
        LoggerCentral.warn(LOGGER, "Path not found: "+currentFile.getAbsolutePath());
      }
    }
    return exploreLocalDirectory(newList, tokenizer, exclude);
  }

  /**
   * Copy all files passed in param to the directory. Each file copied with
   * success is commited to the task monitor.
   * 
   * @param files
   *          list of the files which should be copied
   * @param destDir
   *          Path to the directory where the files should be copied
   * @param monitor
   *          the task monitor$
   * 
   * @return 1 if success, 0 if failure, 3 if aborted
   */
  public int copyFiles(List<DBMSFile> files, String destDir, LoaderMonitor monitor) {
    InputStream input = null;
    OutputStream output = null;
    String fName = null, msg;
    int result = 0;
    File destPath = new File(destDir);
    int fileNum, nFiles;
    long fSize;

    if (!destPath.exists()) {
      if (!destPath.mkdirs()) {
        LoggerCentral.error(LOGGER, "Cannot create destination directory: "
            + destDir);
        return result;
      }
    }

    fileNum = 0;
    nFiles = files.size();
    for (DBMSFile file : files) {
      try {
        fileNum++;
        fName = file.getName();

        if (monitor != null){
          monitor.beginLoading(fName);
        }
        
        msg = "Copying file " + fileNum + "/" + nFiles + ": " + fName;
        LoggerCentral.info(LOGGER, WORKER_ID + ": " + msg);
        
        if (_userMonitor != null) {
          _userMonitor.processingMessage(WORKER_ID, fName,
              UserProcessingMonitor.PROCESS_TYPE.FTP_LOADING,
              UserProcessingMonitor.MSG_TYPE.OK, msg);
        }
        
        // does not copy file if already done
        File dFile = new File(destDir + file.getName());
        if (dFile.exists() && dFile.length()==file.getSize()){
          msg = "Skip already copyied file: " + fName;
        }
        else{
          input = new FileInputStream(file.getFile());
          output = new FileOutputStream(destDir + file.getName());
          fSize = file.getSize();
          Util.copyStream(input, output, Util.DEFAULT_COPY_BUFFER_SIZE, fSize,
              new MyCopyStreamListener(WORKER_ID, _userMonitor, file.getName(),
                  file.getName(), fSize));
          msg = "Done coying file: " + fName;
        }
        
        LoggerCentral.info(LOGGER, msg);
        
        if (monitor != null) {
          monitor.doneLoading(fName, LoaderMonitor.STATUS_OK);
        }
        if (_userMonitor != null) {
          _userMonitor
              .processingMessage(WORKER_ID, fName,
                  UserProcessingMonitor.PROCESS_TYPE.FTP_LOADING,
                  UserProcessingMonitor.MSG_TYPE.OK, msg);
        }
        result = 1;
      } catch (MyCopyInteruptException ex) {
        result = 3;
      } catch (Exception e) {
        result = 0;
        LoggerCentral.error(LOGGER,
            e.getMessage() + ": " + file.getFile().getAbsolutePath());
        if (monitor != null)
          monitor.doneLoading(fName, LoaderMonitor.STATUS_FAILURE);
        if (_userMonitor != null) {
          _userMonitor.processingMessage(WORKER_ID, fName,
              UserProcessingMonitor.PROCESS_TYPE.FTP_LOADING,
              UserProcessingMonitor.MSG_TYPE.ERROR, "Cannot copy file: "
                  + fName);
        }
      } finally {
        IOUtils.closeQuietly(input);
        IOUtils.closeQuietly(output);
      }
    }

    return result;
  }

}
