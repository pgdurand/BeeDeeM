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
package bzh.plealog.dbmirror.util.runner;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * This class is responsible for executing an external application.
 * 
 * @author Patrick G. Durand
 */
public class DBMSExecNativeCommand {
  private int                  exitCode_               = -1;
  private ExecMonitor          monitor_;
  
  private static HashSet<Process>     runningProcesses_ = new HashSet<>();
  
  public static final int      WINDOWS_OS              = 0;
  public static final int      MAC_OS                  = 1;
  public static final int      LINUX_OS                = 2;
  public static final int      UNKNOWN_OS              = 3;

  public static final String   MIRRORDIR_VAR_NAME      = "${mirrordir}";
  public static final String   MIRRORPREPADIR_VAR_NAME = "${mirrorprepadir}";
  public static final String   APPDIR_VAR_NAME         = "${appdir}";
  public static final String   JTMPDIR_VAR_NAME        = "${javaTempDir}";
  public static final String   WORKDIR_VAR_NAME        = "${workdir}";
  public static final int      EXEC_INTERRUPTED        = -2;
  public static final long     DEFAULT_TIME_SLICE      = 2000; //milliseconds
  
  /** Access this string array using the xxx_OS constants defined here */
  public static final String[] OS_NAMES                = { "windows", "macos",
      "linux", "unknown"                              };

  private static final Log     LOGGER                  = LogFactory
                                                           .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                               + ".PDMSExecNativeCommand");

  public DBMSExecNativeCommand() {
    monitor_ = new MyMonitor();
  }

  public DBMSExecNativeCommand(ExecMonitor em) {
    setMonitor(em);
  }

  public void setMonitor(ExecMonitor em) {
    monitor_ = em;
  }

  private void warn(String msg) {
    if (monitor_ != null)
      monitor_.warn(msg);
  }

  private void info(String msg) {
    if (monitor_ != null)
      monitor_.info(msg);
  }

  public static String getUserHomeDirectory() {
    String userHome = "";

    userHome = System.getProperty("user.home");

    // workaround bug #4787931 Windows specific
    // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4787931
    if (DBMSExecNativeCommand.getOSType() == DBMSExecNativeCommand.WINDOWS_OS) {
      if (userHome.indexOf("Windows") != -1) {
        userHome = System.getenv("USERPROFILE");
      }
    }

    return userHome;
  }

  /**
   * Returns the Operating System identifier.
   * 
   * @return one of the XXX_OS constants defined in this class
   */
  public static int getOSType() {
    Properties props;
    String osName;

    props = System.getProperties();
    osName = props.getProperty("os.name");
    osName = osName.toLowerCase();
    if (osName.indexOf("windows") >= 0) {
      return WINDOWS_OS;
    } else if (osName.indexOf("linux") >= 0) {
      return LINUX_OS;
    } else if (osName.equals("mac os x")) {
      return MAC_OS;
    } else {
      return UNKNOWN_OS;
    }
  }

  /**
   * Returns the OS name.
   */
  public static String getOSName() {
    return OS_NAMES[getOSType()];
  }

  /**
   * This method can be used to format abstract path to native one. Basically,
   * there are two formatting rules. First, an abstract path contains a pipe
   * character | as the path separator: that | is replaced by the OS dependant
   * path separator. Second, if the path begins with the reserved key ${appdir},
   * that key is replaced by the path where KLblast is installed. In addition,
   * if param appendOSname is true, the OS name is appended to the returned
   * value.<br>
   * <br>
   * Example: if param path is ${appdir}|external|bin, the returned value will
   * be c:\Program Files\KLBlaster\external\bin\ on a Windows platform (if
   * KLBlaster has been installed in c:\Program Files), and
   * /usr/bin/KLblaster/external/bin/ on a Unix system (if KLBlaster has been
   * installed in /usr/bin/KLblaster).<br>
   * <br>
   * By default, this method always adds an ending path separator: set param
   * removeEndingSeparator to true to remove it.
   */
  public static String formatNativePath(String path, boolean appendOSname,
      boolean removeEndingSeparator) {
    StringBuffer szBuf;
    StringTokenizer tokenizer;
    String token;

    szBuf = new StringBuffer();
    if (path.charAt(0) == '|' || path.charAt(0) == '/'
        || path.charAt(0) == '\\')
      szBuf.append(File.separator);
    tokenizer = new StringTokenizer(path, "|/\\");
    while (tokenizer.hasMoreTokens()) {
      token = tokenizer.nextToken();
      if (token.equalsIgnoreCase("${userDir}")) {
        szBuf.append(Utils.terminatePath(DBMSExecNativeCommand
            .getUserHomeDirectory()));
      } else if (token.equalsIgnoreCase(JTMPDIR_VAR_NAME)){
        String tempDir = System.getProperty("java.io.tmpdir");
        szBuf.append(Utils.terminatePath(tempDir));
      } else if (token.equalsIgnoreCase(APPDIR_VAR_NAME)) {
        szBuf.append(DBMSAbstractConfig.getInstallAppPath());
      } else if (token.equalsIgnoreCase(MIRRORDIR_VAR_NAME)) {
        szBuf.append(DBMSAbstractConfig.getLocalMirrorPath());
      } else if (token.equalsIgnoreCase(MIRRORPREPADIR_VAR_NAME)) {
        szBuf.append(DBMSAbstractConfig.getLocalMirrorPrepaPath());
      } else if (token.equalsIgnoreCase(WORKDIR_VAR_NAME)) {
        szBuf.append(DBMSAbstractConfig.getWorkingPath());
      } else if (token.equalsIgnoreCase("${os}")) {
        szBuf.append(getOSName());
        szBuf.append(File.separator);
      } else {
        szBuf.append(token);
        szBuf.append(File.separator);
      }
    }
    if (appendOSname) {
      szBuf.append(DBMSExecNativeCommand.OS_NAMES[DBMSExecNativeCommand
          .getOSType()]);
      szBuf.append(File.separator);
    }
    token = szBuf.toString();
    if (removeEndingSeparator) {
      if (token.charAt(token.length() - 1) == File.separatorChar)
        token = token.substring(0, token.length() - 1);
    }
    return token;
  }

  /**
   * Returns the exit code of the application. This method can be called after a
   * call to execute() method.
   */
  public int getExitCode() {
    return exitCode_;
  }

  /**
   * Executes an application.
   * 
   * @param cmd
   *          a fully formatted command-line to execute
   * 
   * @throws DBMSExecNativeCommandException
   *           if something wrong occured during execution
   */
  public void execute(String cmd) throws DBMSExecNativeCommandException {
    Runtime rtime;
    Process proc = null;
    InputStream processOut = null;
    InputStream processErr = null;
    Thread thread;

    if (cmd == null) {
      throw new DBMSExecNativeCommandException(
          "Command to execute is not defined.");
    }
    try {
      rtime = Runtime.getRuntime();
      info("Start execution of: " + cmd);
      proc = rtime.exec(cmd);

      runningProcesses_.add(proc);
      
      processOut = proc.getInputStream();
      processErr = proc.getErrorStream();

      thread = new MonitorInputStreamThread(processOut, true, true);
      thread.start();

      thread = new MonitorInputStreamThread(processErr, true, true);
      thread.start();

      proc.waitFor();
      exitCode_ = proc.exitValue();
      info("Exit code for " + cmd + ": " + exitCode_);
    } catch (Exception ex) {
      throw new DBMSExecNativeCommandException(ex.toString());
    } finally {
      IOUtils.closeQuietly(processErr);
      IOUtils.closeQuietly(processOut);
      if (proc != null) {

        IOUtils.closeQuietly(proc.getInputStream());

        try {
          proc.destroy();
        } catch (Exception e) {
          LOGGER.debug("Unable to destroy process : " + e.getMessage());
        }
        
        runningProcesses_.remove(proc);
      }
    }
  }

  /**
   * Executes an application.
   * 
   * @param cmd
   *          the abs path to the application to execute
   * @param param
   *          the command-line parameters for the application. In this map, keys
   *          identified command-line options (-x) and values are the
   *          corresponding values.
   * 
   * @throws DBMSExecNativeCommandException
   *           if something wrong occurred during execution
   */
  public void execute(String cmd, Map<String, String> param)
      throws DBMSExecNativeCommandException {
    StringBuffer szBuf;
    Iterator<String> iter;
    String key, cmdLine;
    int osType;

    if (cmd == null || !(new File(cmd)).exists()) {
      throw new DBMSExecNativeCommandException("Program does not exit: " + cmd);
    }
    osType = DBMSExecNativeCommand.getOSType();
    if (osType == WINDOWS_OS) {
      cmd = "\"" + cmd + "\"";
    }
    if (param != null && !param.isEmpty()) {
      szBuf = new StringBuffer(" ");
      iter = param.keySet().iterator();
      while (iter.hasNext()) {
        key = iter.next().toString();
        szBuf.append(key);
        szBuf.append(" ");
        szBuf.append(param.get(key));
        szBuf.append(" ");
      }
      cmdLine = cmd + szBuf.toString();
    } else {
      cmdLine = cmd;
    }
    execute(cmdLine);
  }

  private String getCmdLineAsString(String[] cmd) {
    StringBuffer buf = new StringBuffer();

    for (String value : cmd) {
      buf.append(value);
      buf.append(" ");
    }
    return buf.toString();
  }

  /**
   * Executes an application.
   * 
   * @param cmd
   *          a fully formatted command-line to execute
   * 
   * @throws ExecNativeCommandException
   *           if something wrong occured during execution
   */
  private void executeAntWait(String[] cmd, String workDir, boolean logInfo,
      boolean logError) throws DBMSExecNativeCommandException {
    Process proc = null;
    InputStream processOut = null;
    InputStream processErr = null;
    Thread thread;

    if (cmd == null) {
      throw new DBMSExecNativeCommandException(
          "Command to execute is not defined.");
    }
    LOGGER.debug("Can execute: "+cmd[0]+": "+new File(cmd[0]).canExecute());
    try {
      if (logInfo)
        info("Start execution of: " + getCmdLineAsString(cmd));
      proc = Runtime.getRuntime().exec(cmd, null,
          workDir != null ? new File(workDir) : null);
      
      runningProcesses_.add(proc);
      
      processOut = proc.getInputStream();
      processErr = proc.getErrorStream();
      thread = new MonitorInputStreamThread(processOut, logInfo, logError);
      thread.start();

      thread = new MonitorInputStreamThread(processErr, logInfo, logError);
      thread.start();
      proc.waitFor();
      exitCode_ = proc.exitValue();
      LOGGER.debug("Exit code: "+exitCode_);
      if (logInfo)
        info("Exit code: " + exitCode_);
    } catch (InterruptedException ex) {
      exitCode_ = EXEC_INTERRUPTED;
    } catch (Exception ex) {
      throw new DBMSExecNativeCommandException(ex.toString());
    } finally {
      IOUtils.closeQuietly(processOut);
      IOUtils.closeQuietly(processErr);
      if (proc != null) {
        IOUtils.closeQuietly(proc.getInputStream());
        try {
          proc.destroy();
        } catch (Exception e) {
          LOGGER.debug("Unable to destroy process : " + e.getMessage());
        }
        runningProcesses_.remove(proc);
      }
    }
  }

  /**
   * Executes an application and returns the running job.
   * 
   * @param cmd
   *          a fully formatted command-line to execute
   * 
   * @throws ExecNativeCommandException
   *           if something wrong occured during execution
   */
  private Process executeAndReturn(String[] cmd, String workDir,
      boolean logInfo, boolean logError) throws DBMSExecNativeCommandException {
    Process proc = null;
    Thread thread;

    if (cmd == null) {
      throw new DBMSExecNativeCommandException(
          "Command to execute is not defined.");
    }
    try {
      if (logInfo)
        info("Start execution of: " + getCmdLineAsString(cmd));
      proc = Runtime.getRuntime().exec(cmd, null,
          workDir != null ? new File(workDir) : null);

      runningProcesses_.add(proc);
      
      thread = new MonitorInputStreamThread(proc.getInputStream(), logInfo,
          logError);
      thread.start();

      thread = new MonitorInputStreamThread(proc.getErrorStream(), logInfo,
          logError);
      thread.start();
    } catch (Exception ex) {
      throw new DBMSExecNativeCommandException(ex.toString());
    }
    return proc;
  }

  @SuppressWarnings("unused")
  private String getEscapedFilePath(int osType, String path) {
    String nativePath = null;

    if (path == null)
      return null;
    try {
      switch (osType) {
        case WINDOWS_OS:
          nativePath = "\"" + path + "\"";
          break;
        default:// Unix-like
          nativePath = Utils.replaceAll(path, " ", "\\ ");
          break;
      }
    } catch (Exception e) {
      warn(e.toString());
      nativePath = null;
    }
    if (nativePath == null)
      throw new DBMSExecNativeCommandException(
          "unable to get native path for: " + path);

    return nativePath;
  }

  /**
   * Prepares a command-line.
   * 
   * @param cmd
   *          the absolute path to the application to execute
   * @param param
   *          the command-line parameters for the application. In this map, keys
   *          identified command-line options (-x) and values are the
   *          corresponding values.
   * 
   * @return an array of strings forming the command line. Please note that the
   *         last string is always the working directory, and it may be null if
   *         none is available.
   * 
   * @throws ExecNativeCommandException
   *           if something wrong occurred during execution
   */
  private String[] prepareCmdLine(String cmd, Map<String, CommandArgument> param)
      throws DBMSExecNativeCommandException {
    ArrayList<String> cmdLineParts;
    CommandArgument arg;
    StringBuffer argsBuf;
    Iterator<String> iter;
    String key, chdirPath = null;
    String[] args, cmdLine;
    File f;
    int i;

    if (cmd == null || !(new File(cmd)).exists()) {
      throw new DBMSExecNativeCommandException("Program does not exit: " + cmd);
    }

    // cmd = getEscapedFilePath(osType, cmd);

    if (param != null && !param.isEmpty()) {
      cmdLineParts = new ArrayList<String>();
      iter = param.keySet().iterator();
      cmdLineParts.add(cmd);
      while (iter.hasNext()) {
        key = iter.next().toString();
        cmdLineParts.add(key);
        arg = param.get(key);
        if (arg.isPath()) {
          // args can be a list (comma separated list of strings)
          args = Utils.tokenize(arg.getArgument());
          argsBuf = new StringBuffer();
          i = 0;
          for (String str : args) {
            if (arg.isKeepFileNameOnly()) {
              f = new File(str);
              argsBuf.append(f.getName());
            } else {
              argsBuf.append(str);
            }
            if ((i + 1) < args.length) {
              argsBuf.append(" ");
            }
            // all args in a list are supposed to be in a same path
            if (chdirPath == null && arg.isWorkingPath()) {
              f = new File(str);
              chdirPath = f.getParent();
            }
            i++;
          }
          // cmdLineParts.add(getEscapedFilePath(osType, argsBuf.toString()));
          cmdLineParts.add(argsBuf.toString());
        } else {
          // For argument having no value: do not add empty
          // value otherwise Java Runtime.exec(cmd[]) will fail.
          // Problem detected with "-parse_seqids" argument
          // of NCBI BLAST/makeblastdb program !
          if (arg.getArgument().length()>0){
            cmdLineParts.add(arg.getArgument());
          }
        }
      }
      cmdLineParts.add(chdirPath);
      cmdLine = cmdLineParts.toArray(new String[0]);
    } else {
      cmdLine = new String[2];
      cmdLine[0] = cmd;
      cmdLine[1] = null;
    }

    return cmdLine;
  }

  /**
   * Executes an application and wait until the process terminates.
   * 
   * @param cmd
   *          the absolute path to the application to execute
   * @param param
   *          the command-line parameters for the application. In this map, keys
   *          identified command-line options (-x) and values are the
   *          corresponding values.
   * 
   * @throws ExecNativeCommandException
   *           if something wrong occurred during execution
   */
  public void executeAndWait(String cmd, Map<String, CommandArgument> param)
      throws DBMSExecNativeCommandException {
    boolean logInfo = true;
    boolean logError = true;

    String[] params = prepareCmdLine(cmd, param);
    executeAntWait(Utils.copyOf(params, params.length - 1),
        params[params.length - 1], logInfo, logError);
  }

  /**
   * Executes an application and returns the running job..
   * 
   * @param cmd
   *          the absolute path to the application to execute
   * @param param
   *          the command-line parameters for the application. In this map, keys
   *          identified command-line options (-x) and values are the
   *          corresponding values.
   * 
   * @throws ExecNativeCommandException
   *           if something wrong occurred during execution
   */
  public Process executeAndReturn(String cmd, Map<String, CommandArgument> param)
      throws DBMSExecNativeCommandException {
    boolean logInfo = true;
    boolean logError = true;
    String[] params = prepareCmdLine(cmd, param);
    return executeAndReturn(Utils.copyOf(params, params.length - 1),
        params[params.length - 1], logInfo, logError);
  }

  /**
   * Close all streams and destroy the process
   * @param process
   */
  public static void terminateProcess(Process process) {
    if (process != null) {
      IOUtils.closeQuietly(process.getErrorStream());
      IOUtils.closeQuietly(process.getInputStream());
      IOUtils.closeQuietly(process.getOutputStream());
      process.destroy();
      runningProcesses_.remove(process);
    }
  }

  /**
   * For internal use.
   */
  public static void terminateAllProcesses() {
    List<Process> processes = new ArrayList<Process>(runningProcesses_);
    for (Process process : processes) {
      LoggerCentral.info(LOGGER, 
          String.format("Killing Process PID: %d", getProcessID(process)));
      if (process.isAlive()) {
        terminateProcess(process);
      }
    }
  }
  
  /**
   * A class aims at reading standard output and error streams during the
   * application execution. These streams are sent to a logger.
   */
  private class MonitorInputStreamThread extends Thread {
    private BufferedReader reader;
    private boolean        logInfo;
    private boolean        logError;

    public MonitorInputStreamThread(InputStream in, boolean logInfo,
        boolean logError) {
      reader = new BufferedReader(new InputStreamReader(in));
      setDaemon(true);
      this.logInfo = logInfo;
      this.logError = logError;
    }

    public void run() {
      try {
        String s;
        int os = DBMSExecNativeCommand.getOSType();
        while ((s = reader.readLine()) != null) {
          if (os == DBMSExecNativeCommand.WINDOWS_OS) {
            // discard 'cmd.exe' header
            if (s.length() < 2 || s.indexOf("Microsoft Windows") != -11
                || s.indexOf("Microsoft Corp") != -11
                || s.indexOf(">exit") != -11) {
              continue;
            }
          }
          if (logInfo)
            info(s);
        }
      } catch (IOException ioe) {
        if (logError)
          warn(ioe.getMessage());
      } finally {
        IOUtils.closeQuietly(reader);
      }
    }
  }

  /**
   * Return a PID of a Process.
   * 
   * @param p a process
   * 
   * @return a PID on Unix, a Handle on Windows, -1 if neither can be obtained
   * */
  public static long getProcessID(Process p){
      long result = -1;
      try{
          //for windows
          if (p.getClass().getName().equals("java.lang.Win32Process") ||
                 p.getClass().getName().equals("java.lang.ProcessImpl")) {
              Field f = p.getClass().getDeclaredField("handle");
              f.setAccessible(true); 
              result = f.getLong(p);
              /* requires JNA...
              long handl = f.getLong(p);
              Kernel32 kernel = Kernel32.INSTANCE;
              WinNT.HANDLE hand = new WinNT.HANDLE();
              hand.setPointer(Pointer.createConstant(handl));
              result = kernel.GetProcessId(hand);*/
              f.setAccessible(false);
          }
          //for unix based operating systems
          else if (p.getClass().getName().equals("java.lang.UNIXProcess")){
              Field f = p.getClass().getDeclaredField("pid");
              f.setAccessible(true);
              result = f.getLong(p);
              f.setAccessible(false);
          }
      }
      catch(Exception ex){
          result = -1;
      }
      return result;
  }
  
  private class MyMonitor implements ExecMonitor {
    public void warn(String msg) {
      LoggerCentral.error(LOGGER, msg);
    }

    public void info(String msg) {
      LoggerCentral.info(LOGGER, msg);
    }
  }
}
