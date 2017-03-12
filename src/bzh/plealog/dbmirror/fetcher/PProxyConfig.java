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

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

public class PProxyConfig {
  protected String                  _path;
  protected String                  _name;
  protected PropertiesConfiguration _pConfig;

  private static final Log          LOGGER             = LogFactory
                                                           .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                               + ".PProxyConfig");

  public static final String        NAME               = "PProxyConfig";
  public static final String        CONF_FILE          = "netsocks.config";

  // when adding a value here, do not forget to update method
  // prepareBasicConfiguration()
  public static final String        PROXY_HOST_KEY     = "proxy.host";
  public static final String        PROXY_PORT_KEY     = "proxy.port";
  public static final String        PROXY_USE_KEY      = "proxy.use";
  public static final String        PROXY_NEVER_KEY    = "proxy.never";
  public static final String        PROXY_LOGIN_KEY    = "proxy.login";
  public static final String        PROXY_PASSWORD_KEY = "proxy.password";

  public PProxyConfig() {
    try {
      _pConfig = new PropertiesConfiguration();
      _pConfig.setDelimiterParsingDisabled(true);
    } catch (Exception e) {
      LOGGER.warn(e);
    }
  }

  /**
   * Sets the name of this configuration.
   */
  public void setName(String name) {
    _name = name;
  }

  /**
   * Returns the name of this configuration.
   */
  public String getName() {
    return _name;
  }

  /**
   * Uploads a configuration file. This method delegates the load to the load
   * method of class ConfigurationProperties. So, a configuration file has to be
   * formatted according the ConfigurationProperties specifications.
   * 
   */
  public void load(InputStream inStream) throws IOException {
    try {
      _pConfig.load(inStream);
      cleanValues();
    } catch (Exception e) {
      // this has been done for backward compatibility when replacing
      // standard Properties by ConfigurationProperties
      throw new IOException(e.getMessage());
    }
  }

  /**
   * Uploads a configuration file. This method delegates the load to the load
   * method of class ConfigurationProperties. So, a configuration file has to be
   * formatted according the ConfigurationProperties specifications.
   * 
   * @param path
   *          the absolute path to the file
   * @param listenReload
   *          set to true to monitor and automatically reload the file when it
   *          is modified on disk.
   */
  public void load(String path, boolean listenReload) throws IOException {
    try {
      _pConfig = new PropertiesConfiguration(path);
      _path = path;
      cleanValues();
    } catch (Exception e) {
      // this has been done for backward compatibility when replacing
      // standard Properties by ConfigurationProperties
      throw new IOException(e.getMessage());
    }

    if (listenReload) {
      _pConfig.setReloadingStrategy(new FileChangedReloadingStrategy());
    }
  }

  /**
   * Returns the value corresponding to a particular key.
   * 
   * @see java.util.Properties#getProperty(java.lang.String)
   */
  public String getProperty(String key) {
    return _pConfig.getString(key);
  }

  /**
   * Sets a property.
   * 
   * @see java.util.Properties#setProperty(java.lang.String, java.lang.String)
   */
  public void setProperty(String key, String value) {
    _pConfig.setProperty(key, value);
  }

  /**
   * Dump the configuration using a logger. If log parameter is null, then a
   * default logger will be used.
   */
  public void dumpConfig() {
    Iterator<String> iter = _pConfig.getKeys();
    String key;

    while (iter.hasNext()) {
      key = iter.next().toString();
      LOGGER.debug(key + " = " + _pConfig.getString(key));
    }
  }

  /**
   * Save the configuration in its file.
   */
  public boolean save(String confPath, boolean listenReload) {
    boolean bRet = true;

    _path = confPath;
    _pConfig.setFileName(confPath);
    if (listenReload)
      _pConfig.setReloadingStrategy(new FileChangedReloadingStrategy());
    try {
      _pConfig.save();
    } catch (ConfigurationException e) {
      LOGGER.warn("unable to save configuration in " + _path + ": " + e);
      bRet = false;
    }
    return bRet;
  }

  protected void cleanValues() {
    Enumeration<Object> menum;
    Iterator<String> iter;
    Properties props;
    String key, value;

    iter = _pConfig.getKeys();
    props = new Properties();
    // note _pConfig cannot be modified while it is read.
    // Step 1 : store updated values in a Properties
    while (iter.hasNext()) {
      key = (String) iter.next();
      value = _pConfig.getString(key).trim();
      props.setProperty(key, value);
    }

    // Step 2: store updated values in _pConfig
    menum = props.keys();
    while (menum.hasMoreElements()) {
      key = (String) menum.nextElement();
      _pConfig.setProperty(key, props.getProperty(key));
    }
  }

  /**
   * Returns true if a network connection has to use a proxy.
   */
  public boolean useProxy() {
    String str = getProperty(PProxyConfig.PROXY_USE_KEY);
    return str != null && str.equals("true");
  }

  /**
   * Returns true if a network connection has to use a proxy and if host is not
   * contained in the list of exception.
   */
  public boolean useProxy(String targetServerAddress) {
    String str = getProperty(PProxyConfig.PROXY_USE_KEY);
    if (str != null && str.equals("true")) {
      str = getProperty(PProxyConfig.PROXY_NEVER_KEY);
      if (str != null && targetServerAddress != null) {
        return (str.indexOf(targetServerAddress) == -1 ? true : false);
      } else {
        return true;
      }
    } else {
      return false;
    }
  }

  /**
   * Configure a proxy connection if needed.
   * 
   * @param targetServerAddress
   *          the FTP server for which to configure the proxy.
   */
  public void configureProxyConnexion(String targetServerAddress) {
    String proxyHost = null, proxyPort = null, login, paswd;
    boolean setProxy, useProxy;

    useProxy = useProxy(targetServerAddress);
    if (useProxy) {
      proxyHost = getProperty(PProxyConfig.PROXY_HOST_KEY);
      proxyPort = getProperty(PProxyConfig.PROXY_PORT_KEY);
    }

    setProxy = (useProxy && proxyHost != null && !proxyHost.equals("")
        && proxyPort != null && !proxyHost.equals(""));

    if (setProxy) {
      LoggerCentral.info(LOGGER, "Use SOCKS proxy connection to: "
          + targetServerAddress + ". Proxy is: " + proxyHost + " on port "
          + proxyPort);
      System.getProperties().put("socksProxyPort", proxyPort);
      System.getProperties().put("socksProxyHost", proxyHost);
    } else {
      LoggerCentral.info(LOGGER, "Use direct connection to: "
          + targetServerAddress);
      System.getProperties().remove("socksProxyPort");
      System.getProperties().remove("socksProxyHost");
    }
    // http://stackoverflow.com/questions/1540424/java-proxy-client-class-that-supports-authentication
    login = getProperty(PProxyConfig.PROXY_LOGIN_KEY);
    paswd = getProperty(PProxyConfig.PROXY_PASSWORD_KEY);
    if (setProxy && login != null && paswd != null && login.length() > 1
        && paswd.length() > 1) {
      LoggerCentral.info(LOGGER, "Use authenticator: " + login);
      System.getProperties().put("java.net.socks.username", login);
      System.getProperties().put("java.net.socks.password", paswd);
    } else {
      System.getProperties().remove("java.net.socks.username");
      System.getProperties().remove("java.net.socks.password");
    }
  }

  /**
   * Sets use of proxy to false.
   */
  public void unsetProxy() {
    System.getProperties().remove("socksProxyPort");
    System.getProperties().remove("socksProxyHost");
    System.getProperties().remove("java.net.socks.username");
    System.getProperties().remove("java.net.socks.password");
  }

}
