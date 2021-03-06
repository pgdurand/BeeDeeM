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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.Configuration;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.descriptor.DescriptorEntry;
import bzh.plealog.dbmirror.util.log.LoggerCentral;
import bzh.plealog.dbmirror.util.runner.DBMSExecNativeCommand;

/**
 * This class handles the configuration file of a FTP loader.
 * 
 * @author Patrick G. Durand
 */
public class PFTPLoaderDescriptor {
  private Properties         _properties       = new Properties();
  private String             _descriptor;
  private boolean            _makeAbsolutePath;
  
  public static final String DBLIST_KEY        = "db.list";
  public static final String FORCE_KEY         = "force.delete";
  
  // accepted values: download, info; default is info
  public static final String MAINTASK_KEY      = "db.main.task";
  public static final String MAINTASK_INFO     = "info";
  public static final String MAINTASK_DOWNLOAD = "download";
  public static final String TASK_DELAY_KEY    = "task.delay";
  public static final String FTP_DELAY_KEY     = "ftp.delay";
  public static final String FTP_RETRY_KEY     = "ftp.retry";

  public static final String MAILER_HOST       = "mail.smtp.host";
  public static final String MAILER_PORT       = "mail.smtp.port";
  public static final String MAILER_SENDER     = "mail.smtp.sender.mail";
  public static final String MAILER_PSWD       = "mail.smtp.sender.pswd";
  public static final String MAILER_RECP       = "mail.smtp.recipient.mail";
  public static final String MAILER_DEBUG      = "mail.debug";

  public static final String NO_RESUME_DATE    = "none";
  
  public static final String UNSET_VALUE    = "-";
  
  private String [] KEYS = {DBLIST_KEY,FORCE_KEY,MAINTASK_KEY,TASK_DELAY_KEY,FTP_DELAY_KEY,FTP_RETRY_KEY,MAILER_HOST,MAILER_PORT,
      MAILER_SENDER,MAILER_PSWD,MAILER_RECP};
  
  /**
   * Create a new PFTPLoaderDescriptor.
   * 
   * Set default values as follows db.list="", force.delete=false,
   * db.main.task=download, task.delay=1000, ftp.delay=5000, 
   * ftp.retry=3.*/
  public PFTPLoaderDescriptor(String descriptorName) {
    super();
    _descriptor = descriptorName;
    _makeAbsolutePath = true;
    initDefaults(_properties);
  }

  private static void initDefaults(Properties props) {
    props.setProperty(PFTPLoaderDescriptor.DBLIST_KEY, "");
    props.setProperty(PFTPLoaderDescriptor.FORCE_KEY, "false");
    props.setProperty(PFTPLoaderDescriptor.MAINTASK_KEY, MAINTASK_DOWNLOAD);
    props.setProperty(PFTPLoaderDescriptor.FTP_DELAY_KEY, "5000");
    props.setProperty(PFTPLoaderDescriptor.FTP_RETRY_KEY, "3");
    props.setProperty(PFTPLoaderDescriptor.TASK_DELAY_KEY, "1000");
  }
  
  public static PFTPLoaderDescriptor create(String force, String workerMode) {
    PFTPLoaderDescriptor loaderDesc = new PFTPLoaderDescriptor("mirror");
    loaderDesc.setProperty(PFTPLoaderDescriptor.FORCE_KEY, force);
    loaderDesc.setProperty(PFTPLoaderDescriptor.MAINTASK_KEY, workerMode);
    return loaderDesc;
  }

  public void addDbToInstall(DescriptorEntry entry) {
    String dbs = this.getProperty(PFTPLoaderDescriptor.DBLIST_KEY);
    if (dbs == null) {
      dbs = "";
    }
    dbs += entry.getFile() + ",";
    this.setProperty(PFTPLoaderDescriptor.DBLIST_KEY, dbs);
  }

  /**
   * Uploads a configuration file. This method delegates the load to the load
   * method of class Properties. So, a configuration file has to be formatted
   * according the Properties specifications.
   * 
   * @see java.util.Properties#load(java.io.InputStream)
   */
  public void load(InputStream inStream, boolean makeAbsolutePath)
      throws IOException {
    _properties.load(inStream);

    Enumeration<?> e;
    String key, value;

    _makeAbsolutePath = makeAbsolutePath;
    e = _properties.keys();
    while (e.hasMoreElements()) {
      key = (String) e.nextElement();
      value = _properties.getProperty(key).trim();
      _properties.setProperty(key, value);
    }
    String dbs = prepareDBList(this.getProperty(PFTPLoaderDescriptor.DBLIST_KEY));
    if (dbs!=null){
      _properties.setProperty(PFTPLoaderDescriptor.DBLIST_KEY, dbs);
    }
    //added to ensure backward compatibility; replacement of resume.date by force.delete
    // and new mode being "resume" by default, we do not force bank deletion
    if (_properties.containsKey(FORCE_KEY)==false){
      _properties.put(FORCE_KEY, "false");
    }
  }

  private String prepareDBList(String desc){
    StringBuffer buf;
    String fName, dbName;
    String[] dbNames;
    // when loading a conf file, db descriptor could be relative : make absolute
    // file name
    dbNames = Utils.tokenize(desc);
    if (dbNames.length == 0)
      return null;
    buf = new StringBuffer();
    for (int i = 0; i < dbNames.length; i++) {
      dbName = dbNames[i];
      if(new File(dbName).exists()) {
        buf.append(dbName);
      }
      else {
        if (_makeAbsolutePath)
          fName = DBMSAbstractConfig.getConfPath(Configuration.DESCRIPTOR) + dbName;
        else
          fName = DBMSExecNativeCommand.formatNativePath(dbName, false, true);
        fName += DBMSAbstractConfig.FEXT_DD;
        buf.append(fName);
      }
      if ((i + 1 < dbNames.length)) {
        buf.append(",");
      }
    }
    return buf.toString();
  }
  
  /**
   * Stores a configuration file. This method delegates the store to the store
   * method of class Properties.
   * 
   * @see java.util.Properties#store(java.io.OutputStream, java.lang.String)
   */
  public void store(OutputStream out, String header) throws IOException {
    _properties.store(out, header);
  }

  /**
   * Returns the value corresponding to a particular key.
   * 
   * @see java.util.Properties#getProperty(java.lang.String)
   */
  public String getProperty(String key) {
    return _properties.getProperty(key);
  }

  /**
   * Sets a property.
   * 
   * @see java.util.Properties#setProperty(java.lang.String, java.lang.String)
   */
  public void setProperty(String key, String value) {
    _properties.setProperty(key, value);
  }

  /**
   * Returns the an enumeration over the property names.
   * 
   * @see java.util.Properties#propertyNames()
   */
  public Enumeration<?> propertyNames() {
    return _properties.propertyNames();
  }

  /**
   * Returns the Properties object wrapped in this configuration.
   */
  public Properties getProperties() {
    return _properties;
  }

  /**
   * Return the descriptor name.
   */
  public String getDescriptorName() {
    return _descriptor;
  }
  
  public void setDescriptorName(String name) {
    _descriptor = name;
  }
  
  /**
   * Update the content of this descriptor with values from parameter.
   */
  public void update(PFTPLoaderDescriptor desc){
    String value;
    for(String key : KEYS){
      value = desc.getProperty(key);
      // key is not defined in "desc"
      if (value==null || value.isEmpty()){
        continue;
      }
      // we want to reset a value
      if (value.equals(UNSET_VALUE)){
        value="";
      }
      this.setProperty(key, value);
    }
    //only update DB list if desc provides new one(s)
    value = desc.getProperty(PFTPLoaderDescriptor.DBLIST_KEY);
    if (value==null || value.isEmpty()){
      return;
    }
    String dbs = prepareDBList(this.getProperty(PFTPLoaderDescriptor.DBLIST_KEY));
    if (dbs!=null){
      setProperty(PFTPLoaderDescriptor.DBLIST_KEY, dbs);
    }
  }
  
  public void dumpContent() {
    Log LOGGER = LogFactory
        .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY+".LoaderDescriptor");
	  LoggerCentral.info(LOGGER, String.format("Content of global descriptor: %s",_descriptor));
	  for(String key : KEYS){
		  LoggerCentral.info(LOGGER, String.format("%s=%s",key, _properties.getProperty(key)));
	  }
  }
}
