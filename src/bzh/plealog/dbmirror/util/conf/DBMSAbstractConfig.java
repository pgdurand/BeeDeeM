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
package bzh.plealog.dbmirror.util.conf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.configuration.event.ConfigurationEvent;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Category;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.PropertyConfigurator;

import bzh.plealog.dbmirror.fetcher.PProxyConfig;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptorUtils;
import bzh.plealog.dbmirror.util.event.DBMirrorEvent;
import bzh.plealog.dbmirror.util.event.DBMirrorListener;
import bzh.plealog.dbmirror.util.event.DBMirrorListenerSupport;
import bzh.plealog.dbmirror.util.log.LoggerCentral;
import bzh.plealog.dbmirror.util.runner.DBMSExecNativeCommand;

/**
 * This class contains the application configuration.
 * 
 * @author Patrick G. Durand
 */
public class DBMSAbstractConfig {
  private static String                  _localMirrorPath;
  private static String                  _localMirrorConfFile;
  private static String                  _installAppPath;
  private static String                  _logAppPath;
  private static String                  _logAppFileName;
  private static String                  _logAppFile;
  private static String                  _workingTmpPath;
  private static String                  _filterWorkingTmpPath;
  private static String                  _externalBinPath;
  private static String                  _confPath;
  private static String                  _startDate;
  private static PProxyConfig            _proxyConfig;
  private static boolean                 _standalone                  = false;
  private static boolean                 _enablePrefsInToolBar        = false;
  private static boolean                 _enableBioClassifOnly        = false;
  private static boolean                 _connectLogViewer            = false;
  private static boolean                 _enableExitApplication       = true;
  private static boolean                 _dbInstallAuthorized         = true;
  private static String                  _parentAppName               = "";
  private static String                  localMirrorPrepaPath         = null;
  // to avoid some dlg display during unit tests
  public static boolean                  testMode                     = false;

  // this boolean is used only if _dbInstallAuthorized = false
  private static boolean                 _overwriteInstallPermForDico = true;

  private static final Log               LOGGER                       = LogFactory
                                                                          .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                                              + ".DBMSAbstractConfig");
  private static DBMSConfigurator        _configurator;
  private static DBMirrorListenerSupport _listenerSupport             = new DBMirrorListenerSupport();  ;

  public static final String             CONF_PATH_NAME               = "conf";
  public static final String             EXT_PATH_NAME                = "external";
  public static final String             BIN_PATH_NAME                = "bin";
  public static final String             APP_KEY_PREFIX               = "KL_";
  private static final String            APP_HOME_PROP_KEY            = APP_KEY_PREFIX+"HOME";
  private static final String            APP_WORKING_DIR_PROP_KEY     = APP_KEY_PREFIX+"WORKING_DIR";
  private static final String            APP_CONF_DIR_PROP_KEY        = APP_KEY_PREFIX+"CONF_DIR";
  public  static final String            APP_DEBUG_MODE_PROP_KEY      = APP_KEY_PREFIX+"DEBUG";
  private static final String            APP_LOG_FILE_PROP_KEY        = APP_KEY_PREFIX+"LOG_FILE";
  private static final String            APP_LOG_TYPE_PROP_KEY        = APP_KEY_PREFIX+"LOG_TYPE";
  private static final String            USER_DIR_PROP_KEY            = "user.dir";

  private static final String            DEF_DB_PATH                  = "beedeem_banks_repository";

  // file containing the DB_XREFs definition
  public static final String             DB_XREF_CONF_FILE            = "dbxrefsForFasta.config";

  // KDMS master configuration file
  public static final String             MASTER_CONF_FILE             = "dbms.config";

  // path elemnt used to specify current DB in prod...
  public static final String             CURRENT_DIR                  = "current";
  // downloading...
  public static final String             DOWNLOADING_DIR              = "download";
  // ...and old DB
  public static final String             CURRENTON_DIR                = "currentOn";
  // File extension for global descriptor
  public static final String             FEXT_GD                      = ".gd";
  // file extension for bank descriptor
  public static final String             FEXT_DD                      = ".dsc";
  // file prefix for bank descriptor automatically created by the system
  public static final String             FPREF_DD                     = "dbmgr";
  // file extension for bank nb. of entries
  public static final String             FEXT_NUM                     = ".num";
  // file extension for bank nb. of sequences (formatdb only)
  public static final String             FDBEXT_NUM                   = "_fdb.num";

  public static final String             KDMS_ROOTLOG_CATEGORY        = "dbms";
  
  private static MyConfigurationListener CONF_LISTENER                = new MyConfigurationListener();

  private static enum APP_LOG_TYPE {file, console, none};
  
  /**
   * Returns the path where the application is installed.
   */
  public static String getInstallAppPath() {

    if (_installAppPath != null)
      return _installAppPath;
    String path = pruneQuotes(System.getProperty(APP_HOME_PROP_KEY));
    if (path != null) {
      _installAppPath = path;
    } else {
      _installAppPath = pruneQuotes(System.getProperty(USER_DIR_PROP_KEY));
    }
    _installAppPath = Utils.terminatePath(_installAppPath);
    LOGGER.debug("install app path: " + _installAppPath);
    return (_installAppPath);
  }

  public static void setInstallAppPath(String path) {
    _installAppPath = Utils.terminatePath(path);
    LOGGER.debug("install app path: " + _installAppPath);
  }

  public static void setLocalMirrorConfFile(String localMirrorConfFile) {
    _localMirrorConfFile = localMirrorConfFile;
  }

  public static String getLocalMirrorConfFile() {
    return _localMirrorConfFile;
  }

  public static void setLocalMirrorPath(String path) {
    _localMirrorPath = Utils.terminatePath(path);
    LOGGER.debug("local mirror path: " + _localMirrorPath);
  }

  /**
   * Returns the path where the application deploys databank mirrors.
   */
  public static String getLocalMirrorPath() {

    if (_localMirrorPath != null)
      return _localMirrorPath;
    String path = pruneQuotes(System.getProperty(APP_KEY_PREFIX+DBMSConfigurator.MIRROR_PATH));
    if (path != null) {
      _localMirrorPath = path;
    } else {
      // this test has been introduced because Tmp dir can be quite exotic,
      // especially
      // with MacOS X based OS. So, for Unix systems, we target the home dir of
      // the
      // user (we are also quite sure he/she has access in rwx modes)
      if (DBMSExecNativeCommand.getOSType() == DBMSExecNativeCommand.WINDOWS_OS) {
        _localMirrorPath = "c:\\" + DEF_DB_PATH;
      } else {
        _localMirrorPath = pruneQuotes(DBMSExecNativeCommand
            .getUserHomeDirectory());
        _localMirrorPath = Utils.terminatePath(_localMirrorPath) + DEF_DB_PATH;
      }
    }
    _localMirrorPath = Utils.terminatePath(_localMirrorPath);
    LOGGER.debug("local mirror path: " + _localMirrorPath);
    return (_localMirrorPath);
  }

  /**
   * The local mirror prepa path is the directory where the databanks are
   * downloaded, indexed and formatted before the InstallInProduction task <br>
   * This path is setted in the conf/kdms.config file in the mirrorprepa.path
   * property <br>
   * If this property is not setted, the prepa directory = the prod directory
   * (local mirror path)
   * 
   * @return the directory path where databanks are prepared before installing
   *         them in prod directory (local mirror path)
   */
  public static String getLocalMirrorPrepaPath() {
    if (StringUtils.isBlank(localMirrorPrepaPath)) {
      localMirrorPrepaPath = getLocalMirrorPath();
    }
    return localMirrorPrepaPath;
  }

  /**
   * Returns the path where the application stores log files.
   */
  public static String getLogAppPath() {
    String path;

    if (_logAppPath != null)
      return _logAppPath;
    path = pruneQuotes(System.getenv(APP_WORKING_DIR_PROP_KEY));
    if (path==null)
       path = pruneQuotes(System.getProperty(APP_WORKING_DIR_PROP_KEY));
    if (path != null)
      _logAppPath = Utils.terminatePath(path);
    else
      _logAppPath = Utils.terminatePath(pruneQuotes(System
          .getProperty("java.io.tmpdir")));
    return _logAppPath;
  }

  public static String getLogAppFileName(){
    return _logAppFileName;
  }

  public static void setLogAppFileName(String logAppFileName){
    _logAppFileName = logAppFileName;
  }
  
  public static String pruneQuotes(String str) {
    if (str == null)
      return str;
    str = str.trim();
    if (str.charAt(0) == '"' || str.charAt(0) == '\'') {
      str = str.substring(1);
    }
    int lastPos = str.length() - 1;
    if (str.charAt(lastPos) == '"' || str.charAt(lastPos) == '\'') {
      str = str.substring(0, lastPos);
    }
    return str;
  }

  /**
   * Returns the log file name. Return a non null value only if configureLog4J()
   * has been call.
   */
  public static String getLogAppFile() {
    return _logAppFile;
  }

  public static void setLogAppPath(String path) {
    _logAppPath = Utils.terminatePath(path);
    LOGGER.debug("log path: " + _logAppPath);
  }

  /**
   * Returns the path where the application stores tmp files.
   */
  public static String getWorkingPath() {
    String path;

    if (_workingTmpPath != null)
      return _workingTmpPath;
    path = pruneQuotes(System.getenv(APP_WORKING_DIR_PROP_KEY));
    if (path==null)
       path = pruneQuotes(System.getProperty(APP_WORKING_DIR_PROP_KEY));
    if (path != null)
      _workingTmpPath = Utils.terminatePath(path);
    else
      _workingTmpPath = Utils.terminatePath(pruneQuotes(System
          .getProperty("java.io.tmpdir")));
    LOGGER.debug("working path: " + _workingTmpPath);
    return _workingTmpPath;
  }

  public static String getWorkingFilterPath() {
    if (_filterWorkingTmpPath != null)
      return _filterWorkingTmpPath;
    _filterWorkingTmpPath = Utils.terminatePath(getWorkingPath()+"filter");
    File f = new File(_filterWorkingTmpPath);
    if (!f.exists()) {
      f.mkdirs();
    } else {
      try {
        FileUtils.cleanDirectory(f);
      } catch (IOException e) {
        // do not throw exception for this
        LoggerCentral.warn(
            LOGGER,
            "Unable to manage the tmp filter directory in '"
                + _filterWorkingTmpPath + "' : "
                + e.getMessage());
      }
    }
    return _filterWorkingTmpPath;
  }
  /**
   * Returns the path pointing to external binaries. The path specifically
   * points to the OS localized dir (linux, windows or macos).
   */
  public static String getInstallExternalPath() {
    if (_externalBinPath != null)
      return _externalBinPath;
    _externalBinPath = getInstallAppPath() + EXT_PATH_NAME + File.separator
        + BIN_PATH_NAME + File.separator + DBMSExecNativeCommand.getOSName()
        + File.separator;
    LOGGER.debug("install external path: " + _externalBinPath);
    return _externalBinPath;
  }

  public static void setInstallExternalPath(String path) {
    _externalBinPath = Utils.terminatePath(path);
    LOGGER.debug("install external path: " + _externalBinPath);
  }

  /**
   * Returns the application starting date.
   */
  public static String getStarterDate() {
    return _startDate;
  }

  /**
   * Sets the application starting date. Can be used to resume an interrupted
   * job.
   */
  public static void setStarterDate(String dt) {
    _startDate = dt;
  }

  public static void setConfPath(String path) {
    _confPath = Utils.terminatePath(path);
  }

  public static String getConfPath(Configuration confType) {
    if (_confPath != null)
      return Utils.terminatePath(_confPath+confType.getDirectoryName());
    
    String path = pruneQuotes(System.getenv(APP_CONF_DIR_PROP_KEY));
    if (path==null)
       pruneQuotes(System.getProperty(APP_CONF_DIR_PROP_KEY));
    if (path != null) {
      _confPath = Utils.terminatePath(path);
    } else {
      _confPath = Utils.terminatePath(DBMSAbstractConfig.getInstallAppPath()+CONF_PATH_NAME);
    }
    
    return (Utils.terminatePath(_confPath+confType.getDirectoryName()));
  }

  public static void setupLoggers(String logName) {
    setupLoggers(logName, true);
  }

  public static void setupLoggers(String logName, boolean updateLogLevel) {
    DailyRollingFileAppender drfa;
    String userPath, szLogFileName, lvl, sysLogName;

    Category cat = Logger.getInstance(KDMS_ROOTLOG_CATEGORY);
    cat.setAdditivity(false);

    if (updateLogLevel) {
      lvl = pruneQuotes(System.getenv(APP_DEBUG_MODE_PROP_KEY));
      if (lvl==null)
        lvl = pruneQuotes(System.getProperty(APP_DEBUG_MODE_PROP_KEY));
      if ("true".equals(lvl)) {
        cat.setLevel(Level.DEBUG);
        Logger.getRootLogger().setLevel(Level.DEBUG);
      } else {
        cat.setLevel(Level.INFO);
        Logger.getRootLogger().setLevel(Level.INFO);
      }
    }

    sysLogName = pruneQuotes(System.getenv(APP_LOG_FILE_PROP_KEY));
    if (sysLogName==null)
      sysLogName = pruneQuotes(System.getProperty(APP_LOG_FILE_PROP_KEY));
    if (sysLogName != null)
      _logAppFile = sysLogName;
    else
      _logAppFile = logName + ".log";

    DBMSAbstractConfig.setLogAppFileName(_logAppFile);
    
    userPath = DBMSAbstractConfig.getLogAppPath();
    szLogFileName = userPath + _logAppFile;

    try {
      cleanSystemLogs(userPath, _logAppFile);
    } catch (Exception e) {
      // not bad
    }

    drfa = new DailyRollingFileAppender();
    drfa.setFile(szLogFileName);
    drfa.setLayout(new PatternLayout(
        "%d{dd-MM-yyyy HH:mm:ss} [%t] %-5p %c %x | %m%n"));
    drfa.setDatePattern("yyyy-MM-dd");
    drfa.activateOptions();

    cat.addAppender(drfa);
  }

  public static boolean isSilentMode() {
    String logType = pruneQuotes(System.getenv(APP_LOG_TYPE_PROP_KEY));
    if (logType==null)
      logType = pruneQuotes(System.getProperty(APP_LOG_TYPE_PROP_KEY));   
    
    if (logType==null) {
      logType = APP_LOG_TYPE.file.toString();
    }
    
    return logType.equalsIgnoreCase(APP_LOG_TYPE.none.toString());
  }
  /**
   * Configure the logging system. To configure path where to locate the log
   * file, you may call setLogAppPath(). Do not call if the Log4J logger system
   * is already configured, use setupLoggers() instead.
   */
  public static void configureLog4J(String logName) {
    BasicConfigurator.configure();
    
    String logType = pruneQuotes(System.getenv(APP_LOG_TYPE_PROP_KEY));
    if (logType==null)
      logType = pruneQuotes(System.getProperty(APP_LOG_TYPE_PROP_KEY)); 
    
    if (logType==null) {
      logType = APP_LOG_TYPE.file.toString();
    }

    if (logType.equalsIgnoreCase(APP_LOG_TYPE.console.toString())) {
      Properties props = new Properties();
      String lvl = pruneQuotes(System.getenv(APP_DEBUG_MODE_PROP_KEY));
      if(lvl==null)
        lvl = pruneQuotes(System.getProperty(APP_DEBUG_MODE_PROP_KEY));
      if ("true".equals(lvl)) {
        lvl="debug";
      } else {
        lvl="info";
      }
      props.put("log4j.rootCategory",lvl+",console");
      props.put("log4j.logger.bzh.plealog",lvl+",console");
      props.put("log4j.additivity.com.demo.package","false");
      props.put("log4j.appender.console","org.apache.log4j.ConsoleAppender");
      props.put("log4j.appender.console.target","System.out");
      props.put("log4j.appender.console.immediateFlush","true");
      props.put("log4j.appender.console.encoding","UTF-8");
      props.put("log4j.appender.console.threshold",lvl);
            
      props.put("log4j.appender.console.layout","org.apache.log4j.PatternLayout");
      props.put("log4j.appender.console.layout.conversionPattern","%d{dd-MM-yyyy HH:mm:ss} [%t] %-5p %c %x | %m%n");
      PropertyConfigurator.configure(props); 
    }
    else if (logType.equalsIgnoreCase(APP_LOG_TYPE.file.toString())) {
      setupLoggers(logName);
    }
    else {
      Logger.getRootLogger().setLevel(Level.OFF);
    }
  }

  /**
   * This method delete system log files older than one month.
   */
  private static void cleanSystemLogs(String logPath, String logName) {
    Calendar cal;
    Date limitDate;
    File path;

    path = new File(logPath);
    cal = Calendar.getInstance();
    cal.add(Calendar.DAY_OF_MONTH, -30);
    limitDate = cal.getTime();
    for (File f : path.listFiles()) {
      if (f.isDirectory())
        continue;
      if (f.getName().startsWith(logName)) {
        if (new Date(f.lastModified()).before(limitDate)) {
          f.delete();
        }
      }
    }
  }

  /**
   * Sets if KDMS UI part has to connect directly the LoggerCentral system to
   * the KDMSLogViewer. This has been added for CLC Bio to bypass their use of
   * SLF4J logger API to prevent the use of the full log4J API used by KDMS.
   * Among others, using SLF4J does not allow to add an Appender to the Root
   * logger.
   */
  public static void setUseDirectConnectionFromLogToLogViewer(
      boolean connectLogViewer) {
    _connectLogViewer = connectLogViewer;
  }

  /**
   * Figures out if KDMS UI part has to connect directly the LoggerCentral
   * system to the KDMSLogViewer.
   */
  public static boolean isUsingDirectConnectionFromLogToLogViewer() {
    return _connectLogViewer;
  }

  /**
   * Adds an appender to listen to the KDMS internal logging system.
   */
  public static void addLogAppender(Appender app) {
    Category cat = Logger.getInstance(KDMS_ROOTLOG_CATEGORY);
    cat.addAppender(app);
  }

  /**
   * Initializes the Configurator object given a standard KDMS Configuration
   * file. This system has been introduced for the GUI. Calling this method
   * once is enough to initialize the db configurator.
   */
  public static void initializeConfigurator(String kdmsConfFile) {
    if (_configurator == null) {
      _configurator = new DBMSConfigurator();
      try {
        _configurator.load(kdmsConfFile, true);
      } catch (IOException e) {
        throw new RuntimeException("Unable to load KDMS configuration file: "
            + e.getMessage());
      }
      updateConfiguration();
      _configurator.addConfigurationListener(CONF_LISTENER);
      LoggerCentral.info(LOGGER, "*** START APPLICATION *** " + new Date());
      LoggerCentral.info(LOGGER, "Configuration:");
      LoggerCentral.info(LOGGER, "file: "+kdmsConfFile);
      _configurator.dumpContent(LOGGER);
    }
  }

  /**
   * This method forces the initialization of the Configurator. This method has
   * been introduced for servlet-based system to enable the reload of a new db
   * mirror config at runtime within a Tomcat system. Each call of this method
   * initializes a new db config.
   */
  public static void forceInitializeConfigurator(String kdmsConfFile) {
    if (_configurator != null) {
      _configurator.removeConfigurationListener(CONF_LISTENER);
    }
    _configurator = new DBMSConfigurator();
    try {
      _configurator.load(kdmsConfFile, true);
    } catch (IOException e) {
      throw new RuntimeException("Unable to load KDMS configuration file: "
          + e.getMessage());
    }
    updateConfiguration();
    _configurator.addConfigurationListener(CONF_LISTENER);
    LoggerCentral.info(LOGGER, "Configuration:");
    _configurator.dumpContent(LOGGER);
  }

  /**
   * Check whether or not long file names are authorized. These are file names
   * with space characters.
   */
  public static boolean authorizeLongFileName() {
    if (_configurator == null) {
      return false;
    }
    return "true".equals(_configurator
        .getProperty(DBMSConfigurator.LONG_FILE_NAME));
  }

  /**
   * Returns the default size of Fasta volumes. Unit is a number of Gigabytes.
   * Default value is 8, i.e. 8 Gb.
   */
  public static int getDefaultFastaVolumeSize() {
    int def = 8;
    if (_configurator == null) {
      return def;
    }
    String value = _configurator.getProperty(DBMSConfigurator.FASTA_VOLSIZE);
    int volsize = def;
    try {
      volsize = Integer.valueOf(value.trim());
      if (volsize < 1) {
        volsize = def;
      }
    } catch (Exception e) {
    }
    return def;
  }

  /**
   * Returns the number of workers to use for FTP and Local File copy
   * processing.
   * 
   **/
  public static int getFileCopyWorkers() {
    if (_configurator == null) {
      return 3;
    }
    String value = _configurator.getProperty(DBMSConfigurator.COPY_WORKERS);
    int workers = 3;
    try {
      workers = Integer.valueOf(value.trim());
      if (workers < 1 || workers > 5) {
        workers = 3;
      }
    } catch (Exception e) {
    }
    return workers;
  }

  /**
   * Returns the current configuration definition of DB xRefs.
   */
  public static String getDbXrefTagConfiguration() {
    File file;
    BufferedReader reader = null;
    String line;
    StringBuffer buf;
    String conf = null;

    try {
      file = new File(DBMSAbstractConfig.getConfPath(Configuration.SYSTEM)
          + DB_XREF_CONF_FILE);
      reader = new BufferedReader(new InputStreamReader(new FileInputStream(
          file), "UTF-8"));
      buf = new StringBuffer();
      while ((line = reader.readLine()) != null) {
        buf.append(line);
        buf.append("\n");
      }
      conf = buf.toString();
    } catch (Exception e) {
      conf = null;
    }
    if (reader != null) {
      try {
        reader.close();
      } catch (IOException e) {
      }
    }

    return conf;
  }

  /**
   * Sets the proxy configuration to use to manage remote FTP server
   * connections.
   */
  public static void setProxyConfig(PProxyConfig pConfig) {
    _proxyConfig = pConfig;
  }

  /**
   * Returns the current proxy configuration.
   */
  public static PProxyConfig getProxyConfig() {
    return _proxyConfig;
  }

  /**
   * Returns the Configuration object. This system has been introduced for the
   * GUI.
   */
  public static DBMSConfigurator getConfigurator() {
    return _configurator;
  }

  public static DBMSConfigurator.LUCENE_FS_VALUES getLuceneFSType(){
    if (_configurator == null) {
      return DBMSConfigurator.LUCENE_FS_VALUES.FS_DEFAULT;
    }
    String value = _configurator.getProperty(DBMSConfigurator.LUCENE_FS);
    if ("nio".equalsIgnoreCase(value)){
    	return DBMSConfigurator.LUCENE_FS_VALUES.FS_NIO;
    }
    else if ("simple".equalsIgnoreCase(value)){
    	return DBMSConfigurator.LUCENE_FS_VALUES.FS_SIMPLE;
    }
    else {
    	return DBMSConfigurator.LUCENE_FS_VALUES.FS_DEFAULT;
    }
  }

  public static DBMSConfigurator.LUCENE_LK_VALUES getLuceneLockType(){
    if (_configurator == null) {
      return DBMSConfigurator.LUCENE_LK_VALUES.LK_DEFAULT;
    }
    String value = _configurator.getProperty(DBMSConfigurator.LUCENE_LOCK);
    if ("simple".equalsIgnoreCase(value)){
    	return DBMSConfigurator.LUCENE_LK_VALUES.LK_SIMPLE;
    }
    else if ("native".equalsIgnoreCase(value)){
    	return DBMSConfigurator.LUCENE_LK_VALUES.LK_NATIVE;
    }
    else {
    	return DBMSConfigurator.LUCENE_LK_VALUES.LK_DEFAULT;
    }
  }

  public static void addDBMirrorListener(DBMirrorListener listener) {
    _listenerSupport.addDBMirrorListener(listener);
  }

  public static void removeDBMirrorListener(DBMirrorListener listener) {
    _listenerSupport.removeDBMirrorListener(listener);
  }

  public static void fireMirrorEvent(DBMirrorEvent event) {
    _listenerSupport.fireHitChange(event);
  }

  private static void updateConfiguration() {
    String path, file;

    path = _configurator.getProperty(DBMSConfigurator.MIRROR_PATH);
    if (path != null && path.length() > 0) {
      path = DBMSExecNativeCommand.formatNativePath(path, false, false);
      path = Utils.terminatePath(path);
      DBMSAbstractConfig.setLocalMirrorPath(path);
      file = _configurator.getProperty(DBMSConfigurator.MIRROR_FILE);
      DBMSAbstractConfig.setLocalMirrorConfFile(path + file);
      DBMirrorConfig mirrorConfig = DBDescriptorUtils
          .getDBMirrorConfig(DBMSAbstractConfig.getLocalMirrorConfFile());
      DBMSAbstractConfig.fireMirrorEvent(new DBMirrorEvent(mirrorConfig,
          DBMirrorEvent.TYPE.dbAdded));
    }

    // mirror prepa directory path
    path = _configurator.getProperty(DBMSConfigurator.MIRROR_PREPA_PATH);
    if (StringUtils.isNotBlank(path)) {
      path = DBMSExecNativeCommand.formatNativePath(path, false, false);
      localMirrorPrepaPath = Utils.terminatePath(path);
    }

  }

  public static void setStandalone(boolean standalone) {
    _standalone = standalone;
  }

  public static boolean isStandalone() {
    return _standalone;
  }

  public static void setEnablePrefsInToolBar(boolean enable) {
    _enablePrefsInToolBar = enable;
  }

  public static boolean enablePrefsInToolBar() {
    return _enablePrefsInToolBar;
  }

  public static void setEnableBioClassifOnly(boolean enable) {
    _enableBioClassifOnly = enable;
  }

  public static boolean enableBioClassifOnly() {
    return _enableBioClassifOnly;
  }

  public static void setEnableExitApplication(boolean enableExit) {
    _enableExitApplication = enableExit;
  }

  public static boolean isEnableExitApplication() {
    return _enableExitApplication;
  }

  /**
   * @param dbInstallAuthorized
   *          if set to false : kdms does not allow install and filter
   * @param butDicoIsAlwaysAuthorized
   *          : if dbInstallAuthorized==false, only dico installation from
   *          public panel is authorized
   */
  public static void setDbInstallAuthorized(boolean dbInstallAuthorized,
      boolean butDicoIsAlwaysAuthorized) {
    _dbInstallAuthorized = dbInstallAuthorized;
    _overwriteInstallPermForDico = butDicoIsAlwaysAuthorized;
  }

  public static boolean isDbInstallAuthorized() {
    return _dbInstallAuthorized;
  }

  public static boolean getOerwriteInstallPermForDico() {
    return _overwriteInstallPermForDico;
  }

  public static void setParentAppName(String parentAppName) {
    _parentAppName = parentAppName;
  }

  public static String getParentAppName() {
    return _parentAppName;
  }

  private static class MyConfigurationListener implements ConfigurationListener {

    public void configurationChanged(ConfigurationEvent arg0) {
      updateConfiguration();
    }

  }
}
