/* Copyright (C) 2007-2022 Patrick G. Durand
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
package bzh.plealog.dbmirror.util.log;

import java.io.File;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfigConstants;
import bzh.plealog.dbmirror.util.sequence.SequenceFileManager;

/**
 * This class is used to centralize all logs. In addition, this class can be
 * used to figure whether some warnings where emitted during application life
 * cycle.
 * 
 * @author Patrick G. Durand
 */
public class LoggerCentral {
  public static boolean               _errorEmitted;
  public static boolean               _warnEmitted;
  public static boolean               _processAborted;
  public static boolean               _isRunning;

  private static LoggerCentralGateway _logGateway;
  private static String               _logAppPath;
  private static String               _logAppFileName;
  private static String               _logAppFile;

  public static final String            APP_LOG_FILE_PROP_KEY        = DBMSAbstractConfigConstants.APP_KEY_PREFIX+"LOG_FILE";
  public static final String            APP_LOG_TYPE_PROP_KEY        = DBMSAbstractConfigConstants.APP_KEY_PREFIX+"LOG_TYPE";
  public static enum APP_LOG_TYPE {file, console, none}

  public static final String PATTERN_LAYOUT = "%d{dd-MM-yyyy HH:mm:ss} [%t] %-5p %c | %m%n";
  public static final String SIMPLE_PATTERN_LAYOUT = "%d{HH:mm:ss} | %-5p| %m%n";
  
  
  /**
   * Returns the path where the application stores log files.
   */
  public static String getLogAppPath() {
    String path;

    if (_logAppPath != null)
      return _logAppPath;
    path = DBMSAbstractConfigConstants.pruneQuotes(System.getenv(
        DBMSAbstractConfigConstants.APP_WORKING_DIR_PROP_KEY));
    if (path==null)
       path = DBMSAbstractConfigConstants.pruneQuotes(System.getProperty(
           DBMSAbstractConfigConstants.APP_WORKING_DIR_PROP_KEY));
    if (path != null)
      _logAppPath = Utils.terminatePath(path);
    else
      _logAppPath = Utils.terminatePath(DBMSAbstractConfigConstants.pruneQuotes(System
          .getProperty("java.io.tmpdir")));
    return _logAppPath;
  }

  /**
   * Returns the log file name.
   */
  public static String getLogAppFileName(){
    return _logAppFileName;
  }

  /**
   * Sets the log file name.
   */
  public static void setLogAppFileName(String logAppFileName){
    _logAppFileName = logAppFileName;
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
    //LOGGER.debug("log path: " + _logAppPath);
  }

  public static boolean isSilentMode() {
    String logType = DBMSAbstractConfigConstants.pruneQuotes(System.getenv(APP_LOG_TYPE_PROP_KEY));
    if (logType==null)
      logType = DBMSAbstractConfigConstants.pruneQuotes(System.getProperty(APP_LOG_TYPE_PROP_KEY));   
    
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
    
    String logType = DBMSAbstractConfigConstants.pruneQuotes(System.getenv(APP_LOG_TYPE_PROP_KEY));
    if (logType==null)
      logType = DBMSAbstractConfigConstants.pruneQuotes(System.getProperty(APP_LOG_TYPE_PROP_KEY)); 
    
    if (logType==null) {
      logType = APP_LOG_TYPE.file.toString();
    }

    String lvl = DBMSAbstractConfigConstants.pruneQuotes(System.getenv(
        DBMSAbstractConfigConstants.APP_DEBUG_MODE_PROP_KEY));
    Level aLevel;
    if (lvl==null)
      lvl = DBMSAbstractConfigConstants.pruneQuotes(System.getProperty(
          DBMSAbstractConfigConstants.APP_DEBUG_MODE_PROP_KEY));
    if ("true".equals(lvl)) {
      aLevel = Level.DEBUG;
    } else {
      aLevel = Level.INFO;
    }
    
    if (logType.equalsIgnoreCase(APP_LOG_TYPE.console.toString())) {
      configureConsoleLogger(aLevel);
    }
    else if (logType.equalsIgnoreCase(APP_LOG_TYPE.file.toString())) {
      configureRollingFileLogger(aLevel, logName);
    }
    else {
      ConfigurationBuilder<BuiltConfiguration> builder = 
          ConfigurationBuilderFactory.newConfigurationBuilder();
      builder.setStatusLevel(Level.OFF);
      builder.add(builder.newRootLogger(Level.OFF));
      Configurator.initialize(builder.build());
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

  /******************************************************************************/
  
  public static LoggerCentralGateway getLogGateway() {
    return _logGateway;
  }

  public static void setLogGateway(LoggerCentralGateway logGateway) {
    _logGateway = logGateway;
  }

  public static synchronized void info(Log logger, String msg) {
    logger.info(msg);
    if (_logGateway != null) {
      _logGateway.info(msg);
    }
  }

  public static synchronized void warn(Log logger, String msg) {
    _warnEmitted = true;
    logger.warn(msg);
    if (_logGateway != null) {
      _logGateway.warn(msg);
    }
  }

  public static synchronized void error(Log logger, String msg) {
    _errorEmitted = true;
    logger.error(msg);
    if (_logGateway != null) {
      _logGateway.error(msg);
    }
  }

  public static synchronized boolean warnMsgEmitted() {
    return _warnEmitted;
  }

  public static synchronized boolean errorMsgEmitted() {
    return _errorEmitted;
  }

  public static synchronized void abortProcess() {
    _processAborted = true;
    if (LoggerCentral.sfm != null) {
      LoggerCentral.sfm.abortProcess();
    }
  }

  public static synchronized boolean processAborted() {
    return _processAborted;
  }

  public static synchronized void reset() {
    _errorEmitted = false;
    _processAborted = false;
    _isRunning = false;
  }

  public static synchronized void setRunning(boolean running) {
    _isRunning = running;
  }

  public static synchronized boolean isRunning() {
    return _isRunning;
  }

  private static SequenceFileManager sfm = null;

  public static synchronized void stopThisSfmIfAbort(SequenceFileManager sfm) {
    LoggerCentral.sfm = sfm;
  }

  public static synchronized void removeSfmToAbort() {
    LoggerCentral.sfm = null;
  }
  
  private static ConfigurationBuilder<BuiltConfiguration> prepareBuilder(Level aLevel){
    // See https://logging.apache.org/log4j/2.x/manual/customconfig.html
    ConfigurationBuilder<BuiltConfiguration> builder = 
        ConfigurationBuilderFactory.newConfigurationBuilder();

    // Control global debug
    builder.setStatusLevel(aLevel);
    builder.setConfigurationName("BeeDeeM-Log");
    builder.add(builder.newFilter("ThresholdFilter", 
        Filter.Result.ACCEPT, 
        Filter.Result.NEUTRAL).addAttribute("level", aLevel));
    return builder;
  }
  
  private static void initLoggers(ConfigurationBuilder<BuiltConfiguration> builder,
      Level aLevel, String refAppender) {
    builder.add(builder.newLogger("bzh.plealog.dbmirror", aLevel).add(
        builder.newAppenderRef(refAppender)).addAttribute("additivity", false));
    builder.add(builder.newLogger(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY, aLevel).add(
        builder.newAppenderRef(refAppender)).addAttribute("additivity", false));
    builder.add(builder.newLogger("org.apache.logging.log4j", Level.ERROR).add(
        builder.newAppenderRef(refAppender)).addAttribute("additivity", false));
    builder.add(builder.newLogger("org.exolab.castor", Level.WARN).add(
        builder.newAppenderRef(refAppender)).addAttribute("additivity", false));
    
    builder.add(builder.newRootLogger(aLevel).add(builder.newAppenderRef(refAppender)));
    Configurator.reconfigure(builder.build());
  }
  
  /**
   * Configure underlying core implementation of a logging system.
   * To be called only by main method if needed.
   */
  private static void configureConsoleLogger(Level aLevel) {
    ConfigurationBuilder<BuiltConfiguration> builder = prepareBuilder(aLevel);
    
    AppenderComponentBuilder appenderBuilder = builder.newAppender(
        "Stdout", "CONSOLE").addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT);
    appenderBuilder.add(builder.newLayout("PatternLayout").addAttribute(
        "pattern", PATTERN_LAYOUT));
    appenderBuilder.add(
        builder.newFilter("MarkerFilter", 
            Filter.Result.DENY, 
            Filter.Result.NEUTRAL).addAttribute("marker", "FLOW"));
    builder.add(appenderBuilder);
    
    initLoggers(builder, aLevel, "Stdout");
  }  
  
  private static void configureRollingFileLogger(Level aLevel, String logName) {
    
    // prepare file name and pattern
    String userPath, szLogFileName, szLogFilePattern, sysLogName;

    sysLogName = DBMSAbstractConfigConstants.pruneQuotes(System.getenv(APP_LOG_FILE_PROP_KEY));
    if (sysLogName==null)
      sysLogName = DBMSAbstractConfigConstants.pruneQuotes(System.getProperty(APP_LOG_FILE_PROP_KEY));

    if (sysLogName != null)
      _logAppFile = sysLogName;
    else
      _logAppFile = logName + ".log";

    setLogAppFileName(_logAppFile);
    
    userPath = getLogAppPath();
    szLogFileName = userPath + _logAppFile;
    szLogFilePattern = userPath + "%d{MM-dd-yy}-" + _logAppFile;
    try {
      cleanSystemLogs(userPath, _logAppFile);
    } catch (Exception e) {
      // not bad
    }

    //configure Loggers 
    ConfigurationBuilder<BuiltConfiguration> builder = prepareBuilder(aLevel);
   
    LayoutComponentBuilder layoutBuilder = builder.newLayout("PatternLayout")
        .addAttribute("pattern", PATTERN_LAYOUT);
    ComponentBuilder<?> triggeringPolicy = builder.newComponent("Policies")
        .addComponent(builder.newComponent("SizeBasedTriggeringPolicy").addAttribute("size", "100M"));
    AppenderComponentBuilder appenderBuilder = builder.newAppender("rolling", "RollingFile")
        .addAttribute("fileName", szLogFileName)
        .addAttribute("filePattern", szLogFilePattern)
        .add(layoutBuilder)
        .addComponent(triggeringPolicy);
    appenderBuilder.add(
        builder.newFilter("MarkerFilter", 
            Filter.Result.DENY, 
            Filter.Result.NEUTRAL).addAttribute("marker", "FLOW"));
    builder.add(appenderBuilder);
    
    initLoggers(builder, aLevel, "rolling");    
  }

  /**
   * Configure a standard Console logger using INFO log level. 
   */
  public static void configure() {
    configureConsoleLogger(Level.INFO);
  }
  

  /**
   * Add a new appender to logger system.
   * Mostly used by BeeDeeM UI.
   */
  public static void addAppender(Appender appender) {
    // adapted from https://logging.apache.org/log4j/2.x/manual/customconfig.html (bottom of page)
    LoggerContext context = LoggerContext.getContext(false);
    Configuration config = context.getConfiguration();
    appender.start();
    config.addAppender(appender);
    Level level = null;
    Filter filter = null;
    for (final LoggerConfig loggerConfig : config.getLoggers().values()) {
        loggerConfig.addAppender(appender, level, filter);
    }
    config.getRootLogger().addAppender(appender, level, filter);
  }
}
