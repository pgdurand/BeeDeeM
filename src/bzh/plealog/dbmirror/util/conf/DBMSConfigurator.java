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
package bzh.plealog.dbmirror.util.conf;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.event.ConfigurationListener;
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * This class handles the data contains in the standard KDMS Main Configuration
 * file. This system has been introduced for the GUI.
 * 
 * @author Patrick G. Durand
 */
public class DBMSConfigurator {
  protected String                  _pathToFile;
  protected PropertiesConfiguration _pConfig;
  protected boolean                 _listenReload;

  private static final String       SYS_NAME             = "kdmsMainConfig";

  public static final String        MIRROR_PATH          = "mirror.path";
  public static final String        MIRROR_PREPA_PATH    = "mirrorprepa.path";
  public static final String        MIRROR_FILE          = "mirror.file";
  public static final String        LONG_FILE_NAME       = "long.file.name";
  public static final String        FDB_PRG_NAME         = "formater";
  public static final String        FDB_PATH_NAME        = "location";
  public static final String        UI_SHOW_PATH         = "ui.showpath";
  public static final String        COPY_WORKERS         = "copy.workers";
  //unit is number of Gb
  public static final String        FASTA_VOLSIZE        = "fasta.volsize";
  //use to handle Lucene FileSystem and Lock without recompiling the soft
  // introduced to handle particular FS, such as Lustre, etc.
  // Introduced in BeeDeeM 4.1.1; for backward compatibility, these keys are
  // not mandatory in dbms.config files. If so, default Lucene library values
  // are use (see Lucene API doc).
  public static final String        LUCENE_FS            = "lucene.fs";
  public static final String        LUCENE_LOCK          = "lucene.lock";

  public static enum LUCENE_FS_VALUES {FS_DEFAULT, FS_NIO, FS_SIMPLE};
  public static enum LUCENE_LK_VALUES {LK_DEFAULT, LK_NATIVE, LK_SIMPLE};
  
  private static final Log          LOGGER               = LogFactory
                                                             .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                                 + ".DBMSConfigurator");

  private static final String[] KEYS ={MIRROR_PATH, MIRROR_PREPA_PATH, MIRROR_FILE,
		  LONG_FILE_NAME, FDB_PRG_NAME, FDB_PATH_NAME, UI_SHOW_PATH, COPY_WORKERS,
		  FASTA_VOLSIZE, LUCENE_FS, LUCENE_LOCK};
  
  public static String              TMP_FILTER_DIRECTORY = Utils
                                                             .terminatePath(DBMSAbstractConfig
                                                                 .getLocalMirrorPath())
                                                             + "tmp";

  public DBMSConfigurator() {
    try {
      _pConfig = new PropertiesConfiguration();
      _pConfig.setDelimiterParsingDisabled(true);
    } catch (Exception e) {
      LOGGER.warn(e);
    }

  }

  public void addConfigurationListener(ConfigurationListener l) {
    _pConfig.addConfigurationListener(l);
  }

  public void removeConfigurationListener(ConfigurationListener l) {
    _pConfig.removeConfigurationListener(l);
  }

  /**
   * Returns the name of this configuration.
   */
  public String getName() {
    return SYS_NAME;
  }

  /**
   * Uploads a configuration file. This method delegates the load to the load
   * method of class ConfigurationProperties. So, a configuration file has to be
   * formatted according the ConfigurationProperties specifications.
   * 
   */
  /*
   * public void load(InputStream inStream) throws IOException{ try {
   * _pConfig.load(inStream); cleanValues(); } catch (Exception e) { //this has
   * been done for backward compatibility when replacing //standard Properties
   * by ConfigurationProperties throw new IOException(e.getMessage()); } }
   */

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
      _pathToFile = path;
      cleanValues();
    } catch (Exception e) {
      // this has been done for backward compatibility when replacing
      // standard Properties by ConfigurationProperties
      throw new IOException(e.getMessage());
    }
    _listenReload = listenReload;
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
   * Returns the an enumeration over the property names.
   * 
   * @see java.util.Properties#propertyNames()
   */
  public Enumeration<?> propertyNames() {
    return Utils.enumerator(_pConfig.getKeys());
  }

  /**
   * Dump the configuration using a logger. If log parameter is null, then a
   * default logger will be used.
   */
  /*
   * public void dumpConfig(Log log){ Iterator iter = _pConfig.getKeys(); String
   * key;
   * 
   * while(iter.hasNext()){ key = iter.next().toString(); if (log!=null)
   * log.info(key+" = "+_pConfig.getString(key)); else
   * LOGGER.info(key+" = "+_pConfig.getString(key)); } }
   */
  /**
   * Save the configuration in its file.
   */
  public boolean save() {
    boolean bRet = true;

    try {
      _pConfig.save();
    } catch (ConfigurationException e) {
      LOGGER.warn("unable to save configuration in " + _pathToFile + ": " + e);
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

  public void dumpContent(Log logger) {
	  for (String key : KEYS) {
		  if (_pConfig.containsKey(key) == false)
			  continue;
		  LoggerCentral.info(logger, key + "=" + _pConfig.getProperty(key));
	  }
  }

}
