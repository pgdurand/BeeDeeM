/* Copyright (C) 2020 Patrick G. Durand
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
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.fetcher.UserProcessingMonitor;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.Configuration;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;
import bzh.plealog.dbmirror.util.runner.CommandArgument;
import bzh.plealog.dbmirror.util.runner.DBMSExecNativeCommand;
import bzh.plealog.dbmirror.util.runner.ExecMonitor;

/**
 * A task capable of executing an external script.
 * 
 * @author Patrick G. Durand
 */
public class PTaskExecScript extends PAbstractTask {
  private UserProcessingMonitor _userMonitor;
  private String                _scriptName;
  private String                _scriptCmd;
  private String                _dbInstallationPath;
  private String                _curFile;
  private String                _errMsg;
  private String                _bankName;
  private String                _bankType;
  protected Map<String, String> _args;
  
  //mandatory arguments user has to provide in 'script()' task
  private static final String SCRIPT_NAME = "name";
  private static final String SCRIPT_CMD_PATH = "path";
  //arguments passed in to the calling script
  public static final String WORK_DIR_ARG = "-w";  // path to BeeDeeM working dir
  public static final String INST_DIR_ARG = "-d";  // path to bank installation (unit and global tasks)
  public static final String INST_FILE_ARG = "-f"; // path to file (unit task only)
  public static final String BANK_NAME_ARG = "-n"; // bank name
  public static final String BANK_TYPE_ARG = "-t"; // bank type (p,n,d)
  
  public static final String UNIX_FILE_EXT = ".sh";
  public static final String WIN_FILE_EXT = ".bat";
  
  private static final Log LOGGER     = LogFactory
                                          .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                              + ".PTaskEngine");

  /**
   * Constructor.
   * 
   * @param dbPath path to bank installation (unit and global tasks)
   * @param currentFile path to file (unit task only)
   * @param bankName bank name
   * @param bankType bank type, one of p, n, d
   */
  public PTaskExecScript(String dbPath, String currentFile, String bankName, String bankType) {
    _dbInstallationPath = dbPath;
    _curFile = currentFile;
    _bankName = bankName;
    _bankType = bankType;
    
  }

  /**
   * Implementation of KLTask interface.
   */
  public void setParameters(String params) {
    if (params == null) {
      _args = new Hashtable<String, String>();
    }
    else {
      _args = Utils.getTaskArguments(params);
      _scriptName = _args.get(SCRIPT_NAME);
      //to ensure using software on all OS, script is passed in without
      //file extension. Its added here according to OS.
      _scriptCmd = _args.get(SCRIPT_CMD_PATH);
      if (DBMSExecNativeCommand.getOSType()==DBMSExecNativeCommand.WINDOWS_OS) {
        _scriptCmd += WIN_FILE_EXT;
      }
      else {
        _scriptCmd += UNIX_FILE_EXT;
      }
    }
    if (_dbInstallationPath!=null) {
      _args.put(INST_DIR_ARG, _dbInstallationPath);
      
    }
    if (_curFile!=null) {
      _args.put(INST_FILE_ARG, _curFile);
      
    }
  }

  /**
   * Set a user monitor. 
   * 
   * @param userMonitor user monitor
   * */
  public void setUserProcessingMonitor(UserProcessingMonitor userMonitor) {
    _userMonitor = userMonitor;
  }
  
  /**
   * Implementation of KLTask interface.
   */
  public String getName() {
    return "script";
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getUserFriendlyName() {
    return "executing external script";
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getErrorMsg() {
    return _errMsg;
  }

  /**
   * Implementation of KLTask interface.
   */
  public boolean execute() {
    LoggerCentral.info(LOGGER, getUserFriendlyName());
    
    //mandatory arguments user must provide when using 'script()' task
    if (_scriptName == null) {
      _errMsg = "command name is unknown";
      return false;
    }
    String resumeFile = Utils.terminatePath(_dbInstallationPath) + 
        "script-" + _scriptName;
    LoggerCentral.info(LOGGER, SCRIPT_NAME+": "+_scriptName);
    
    if (_scriptCmd == null) {
      _errMsg = "command to execute is unknown";
      return false;
    }
    
    //task already executed ? (resume)
    if (PAbstractTask.testTaskOkForFileExists(resumeFile)) {
      LoggerCentral.info(LOGGER, "skip script execution: already done");
      return true;
    }
    
    //figures out whether or not _scriptCmd is accessible
    File f = new File(_scriptCmd);
    //if user only provides script name, we suppose it is located in standard conf/scripts dir
    if (f.getParent()==null) {
      _scriptCmd = DBMSAbstractConfig.getConfPath(Configuration.SCRIPTS)+_scriptCmd;
    }

    LoggerCentral.info(LOGGER, SCRIPT_CMD_PATH+": "+_scriptCmd);

    //internal problem: not path to bank installation
    if (_dbInstallationPath == null) {
      _errMsg = "installation path is unknown";
      return false;
    }
    
    //start command
    DBMSExecNativeCommand executor = new DBMSExecNativeCommand(new MyMonitor());
    LinkedHashMap<String, CommandArgument> params;
    params = new LinkedHashMap<String, CommandArgument>();
    params.put(WORK_DIR_ARG, new CommandArgument(DBMSAbstractConfig.getWorkingPath(), true));
    params.put(INST_DIR_ARG, new CommandArgument(_dbInstallationPath, true));
    if (_curFile!=null) {
      params.put(INST_FILE_ARG, new CommandArgument(_curFile, true));
    }
    params.put(BANK_NAME_ARG, new CommandArgument(_bankName, false));
    params.put(BANK_TYPE_ARG, new CommandArgument(_bankType, false));
    
    Process proc = executor.executeAndReturn(_scriptCmd, params);
    
    int exitCode=0;
    boolean scriptRunning = true;
    
    //monitor process to detect interruption (UIinstaller)
    while (scriptRunning && (!(_userMonitor != null && _userMonitor.jobCancelled()))) {
      scriptRunning = false;
      try {
        exitCode = proc.exitValue();
      } catch (IllegalThreadStateException ex) {
        scriptRunning = true;
      }
      try {
        Thread.sleep(DBMSExecNativeCommand.DEFAULT_TIME_SLICE);
      } catch (InterruptedException e) {
      }
    }
    
    //properly close process (I/O streams)
    DBMSExecNativeCommand.terminateProcess(proc);
    
    if (exitCode==0) {
      PAbstractTask.setTaskOkForFile(resumeFile);
    }
    return exitCode==0;
  }

  

  private class MyMonitor implements ExecMonitor{
    public void warn(String msg) {
      if (_errMsg==null) {
        _errMsg = msg;
      }
      else {
        _errMsg += "\n";
        _errMsg += msg;
      }
    }

    public void info(String msg) {
      LoggerCentral.info(LOGGER, msg);
    }
  }
}
