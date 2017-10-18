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
package bzh.plealog.dbmirror.main;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.fetcher.PProxyConfig;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.Configuration;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * This class contains common code to start standalone programs.
 * 
 * @author Patrick G. Durand
 */
public class StarterUtils {
  private static final Log    LOGGER   = LogFactory
                                           .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                               + ".StarterUtils");
  private static final String MSG1     = "Loaded network config from: ";
  private static final String ERR1     = "Unable to load network config: ";
  private static final String USER_DIR = "user.dir";

  /**
   * Return the content of the version resource.
   */
  protected static Properties getVersionProperties() {
    Properties props = new Properties();
    try (InputStream in = StarterUtils.class
        .getResourceAsStream("version.properties");) {
      props.load(in);
      in.close();
    } catch (Exception ex) {// should not happen
      System.err.println("Unable to read props: " + ex.toString());
    }
    return props;
  }
  
  protected static String getConfPath() {
    String path;

    /*
     * path = System.getProperty( "KDMS_CONF_DIR");
     */
    path = DBMSAbstractConfig.getInstallAppPath();
    if (path != null)
      path = Utils.terminatePath(path);
    else
      path = Utils.terminatePath(System.getProperty(USER_DIR));
    path += (DBMSAbstractConfig.CONF_PATH_NAME + File.separator);
    return path;
  }

  private static void initNetConfig() {
    PProxyConfig proxy;
    String netConf;
    File f;

    netConf = DBMSAbstractConfig.getInstallAppConfPath(Configuration.SYSTEM)
        + PProxyConfig.CONF_FILE;
    f = new File(netConf);
    if (f.exists() == false) {
      return;
    }

    proxy = new PProxyConfig();
    try {
      proxy.load(netConf, true);
      LOGGER.debug(MSG1 + netConf);
      proxy.dumpConfig();
      DBMSAbstractConfig.setProxyConfig(proxy);
    } catch (IOException e) {
      LOGGER.warn(ERR1 + e);
    }
  }

  private static final void endWithError(Log logger, boolean standalone) {
    String msg = "Unable to start Databank Manager";
    
    if (logger != null){
      logger.warn(msg);
    }
    else{
      System.err.println(msg);
    }
    if (standalone) {
      System.exit(1);
    } else {
      throw new RuntimeException();
    }
  }

  public static void configureApplication(String appHome, String nameLogger,
      boolean standalone, boolean useUI, boolean configureLogger) {

    if (standalone) {
      checkJVM();
    }

    if (appHome != null)
      DBMSAbstractConfig.setInstallAppPath(Utils.terminatePath(appHome));
    if (configureLogger)
      DBMSAbstractConfig.configureLog4J(nameLogger);
    String confPath = getConfPath();
    DBMSAbstractConfig.setInstallAppConfPath(confPath);
    DBMSAbstractConfig.setOSDepConfPath(DBMSAbstractConfig.getConfPath(Configuration.ROOT));
    initNetConfig();
    DBMSAbstractConfig.initializeConfigurator(confPath
        + DBMSAbstractConfig.MASTER_CONF_FILE);
    LoggerCentral.reset();
  }

  /**
   * This method checks that the GNU libgcj or OpenJDK JVM are not used when
   * starting the application under Linux. If yes, terminate the application
   * since libgcj/openJDK is not compatible.
   */
  public static void checkJVM() {
    String jvmName;
    String prop = "java.vm.name";
    String lib1 = "libgcj";
    String lib2 = "openjdk";
    String msg1 = "GNU libgcj is not compatible with the application.";
    String msg2 = "OpenJDK is not compatible with the application.";
    String msg3 = "Please use a Sun/Oracle Java Virtual Machine 1.7+.";

    jvmName = System.getProperty(prop).toLowerCase();

    // Soft does not work with the GNU libgcj JVM
    if (jvmName.indexOf(lib1) != -1) {
      //do not use logger here; Log4J not yet inited
      System.err.println(msg1 + "\n" + msg3);
      endWithError(null, true);
    }
    // Soft does not work with the OpenJDK JVM
    if (jvmName.indexOf(lib2) != -1) {
      System.err.println(msg2 + "\n" + msg3);
      endWithError(null, true);
    }

  }

}
