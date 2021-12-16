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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.lucenedico.Dicos;
import bzh.plealog.dbmirror.reader.DBFormatEntry;
import bzh.plealog.dbmirror.task.PTask;
import bzh.plealog.dbmirror.task.PTaskFormatDB;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.Configuration;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.conf.DBMirrorConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;
import bzh.plealog.dbmirror.util.runner.DBMSExecNativeCommand;

/**
 * This class contains the description of a specific database that has to be
 * downloaded locally.
 * 
 * @author Patrick G. Durand
 */
public class DBServerConfig {

  private static final Log      LOGGER               = LogFactory
                                                         .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                             + ".DBServerConfig");

  private Properties            _properties          = new Properties();

  private static final String   DBNAME_KEY           = "db.name";
  private static final String   DBDESC_KEY           = "db.desc";
  private static final String   DBPROVIDER_KEY       = "db.provider";
  // p(rotein), n(nucleic) or d(ictionary)
  private static final String   DBTYPE_KEY           = "db.type";
  private static final String   DBLDIR_KEY           = "db.ldir";
  private static final String   DBINCFILES_KEY       = "db.files.include";
  private static final String   DBEXCFILES_KEY       = "db.files.exclude";
  // for future use
  private static final String   DBDTFROM_KEY         = "db.date.from";
  private static final String   DBDTTO_KEY           = "db.date.to";

  private static final String   TASKS_U_POST         = "tasks.unit.post";
  private static final String   TASKS_G_POST         = "tasks.global.post";

  private static final String   TASKS_G_PRE          = "tasks.global.pre";

  private static final String   FTPSERVER_KEY        = "ftp.server";
  private static final String   FTPPORT_KEY          = "ftp.port";
  private static final String   FTPUNAME_KEY         = "ftp.uname";
  private static final String   FTPPSWD_KEY          = "ftp.pswd";
  private static final String   FTPRDIR_KEY          = "ftp.rdir";
  private static final String   FTPRDIR_EXCL_KEY     = "ftp.rdir.exclude";
  private static final String   FTPALT_PROTOCOL_KEY  = "ftp.alt.protocol";

  
  private static final String   ASPERAUSE_KEY        = "aspera.use";
  private static final String   ASPERASERVER_KEY     = "aspera.server";
  private static final String   ASPERAARGS_KEY       = "aspera.args";

  private static final String   LOCALRDIR_KEY        = "local.rdir";
  private static final String   LOCALRDIR_EXCL_KEY   = "local.rdir.exclude";

  private static final String   KEEP_HISTORY_KEY     = "history";

  private static final String   INSTALL_IN_PROD_DATA = "iip.data";

  // databanks to install before this one : this property must contains dsc file
  // name separated by comma
  private static final String   DB_DEPENDS           = "depends";

  public static final String    PROTEIN_TYPE         = "p";                       // do
                                                                                   // not
                                                                                   // modify
  public static final String    NUCLEIC_TYPE         = "n";                       // do
                                                                                   // not
                                                                                   // modify
  public static final String    DICO_TYPE            = "d";                       // do
                                                                                   // not
                                                                                   // modify

  public static final String[]  TYPE_ORDER           = { PROTEIN_TYPE,
      NUCLEIC_TYPE, DICO_TYPE                       };

  public static final String    CENTRAl_CONF         = "central";

  private static final String[] KEYS                 = { DBNAME_KEY,
      DBDESC_KEY, DBPROVIDER_KEY, DBTYPE_KEY, DBLDIR_KEY, DBINCFILES_KEY, DBEXCFILES_KEY,
      DBDTFROM_KEY, DBDTTO_KEY, TASKS_U_POST, TASKS_G_POST, TASKS_G_PRE,
      FTPSERVER_KEY,
      FTPPORT_KEY, FTPUNAME_KEY, FTPPSWD_KEY, FTPRDIR_KEY, FTPRDIR_EXCL_KEY,
      FTPALT_PROTOCOL_KEY,
      LOCALRDIR_KEY, LOCALRDIR_EXCL_KEY, ASPERAUSE_KEY, ASPERASERVER_KEY,
      ASPERAARGS_KEY, KEEP_HISTORY_KEY, DB_DEPENDS };

  public DBServerConfig() {
    super();
  }

  public DBServerConfig(String name, String description, DBFormatEntry format,
      String taxIncludes, String taxExcludes, String filters,
      List<File> databankFiles) {
    super();
    StringBuilder prepaFolder = new StringBuilder();
    StringBuilder unitTasks = new StringBuilder();
    StringBuilder globalTasks = new StringBuilder();
    StringBuilder formatDbParams = new StringBuilder();
    StringBuilder filepathList = new StringBuilder();
    StringBuilder taxonFilter = new StringBuilder();

    this.setName(name);
    this.setDescription(description);

    prepaFolder.append(DBMSExecNativeCommand.MIRRORPREPADIR_VAR_NAME);
    prepaFolder.append("|");

    unitTasks.append(PTask.TASK_U_GUNZIP);
    unitTasks.append(",");

    globalTasks.append(PTask.TASK_G_DELETEGZ);
    globalTasks.append(",");
    globalTasks.append(PTask.TASK_G_DELETETMPIDX);
    globalTasks.append(",");

    formatDbParams.append(PTask.TASK_G_FORMATDB);
    formatDbParams.append("(");
    formatDbParams.append(PTaskFormatDB.USE_LCL_ID + "=" + Boolean.FALSE);
    formatDbParams.append(";");
    formatDbParams.append(PTaskFormatDB.CHECK_FILE + "=" + Boolean.TRUE);
    formatDbParams.append(";");
    formatDbParams.append(PTaskFormatDB.CHECK_NR + "=" + Boolean.TRUE);
    if (StringUtils.isNotBlank(taxIncludes)) {
      taxonFilter.append(";");
      taxonFilter.append(PTask.TAX_INCLUDE + "=" + taxIncludes);
    }
    if (StringUtils.isNotBlank(taxExcludes)) {
      taxonFilter.append(";");
      taxonFilter.append(PTask.TAX_EXCLUDE + "=" + taxExcludes);
    }
    formatDbParams.append(taxonFilter);
    if (format.isSilva()) {
      formatDbParams.append(";");
      formatDbParams.append(PTaskFormatDB.IS_SILVA + "=" + Boolean.TRUE);
    }
    if (format.isTaxonomy()) {
      formatDbParams.append(";");
      formatDbParams.append(PTaskFormatDB.IS_TAXO + "=" + Boolean.TRUE);
    }
    formatDbParams.append(")");
    globalTasks.append(formatDbParams);

    // check value
    String dbType = format.getDBType();
    Dicos dico = Dicos.getAssociatedDico(dbType);

    if (dico != null) {
      // When using Biological classification, we redefined tasks to conform to
      // Network
      // based descriptor. Indeed, when installing such classifications from
      // local files
      // we force user to provide files as they were retrieved by KDMS
      this.setDictionaryType();
      this.setName(dico.name);
      this.setDescription(dico.description);
      prepaFolder.append(DBServerConfig.DICO_TYPE);
      if (dico == Dicos.NCBI_TAXONOMY) {
        if (databankFiles.size() == 1) {// user provides taxdump.tar.gz
          unitTasks = new StringBuilder("gunzip,untar,idxdico(type="
              + Dicos.NCBI_TAXONOMY.readerId + ";file=names.dmp:merged.dmp),idxdico(type="
              + Dicos.NCBI_TAXONOMY.additionalReaderId + ";file=nodes.dmp)");
          globalTasks = new StringBuilder("deltmpidx,deltar,delgz");
        } else {// user provides names.dmp and nodes.dmp
          unitTasks = new StringBuilder("");
          globalTasks = new StringBuilder("idxdico(type="
              + Dicos.NCBI_TAXONOMY.readerId + ";file=names.dmp),idxdico(type="
              + Dicos.NCBI_TAXONOMY.additionalReaderId
              + ";file=nodes.dmp),deltmpidx");
        }
      } else if (dico == Dicos.ENZYME) {
        unitTasks = new StringBuilder("");
        globalTasks = new StringBuilder("idxdico(type="
            + Dicos.ENZYME.additionalReaderId
            + ";file=enzclass.txt),idxdico(type=" + Dicos.ENZYME.readerId
            + ";file=enzyme.dat),deltmpidx");
      } else {
        unitTasks = new StringBuilder("gunzip,untar,idxdico(type="
            + dico.readerId + ")");
        globalTasks = new StringBuilder("deltmpidx");
      }

    } else if (dbType.equals(DBMirrorConfig.GB_READER)) {
      this.setNucleicType();
      prepaFolder.append(DBServerConfig.NUCLEIC_TYPE);
      if (format.isBOLD()) {
        unitTasks.append(PTask.TASK_U_BOLD2GB);
        unitTasks.append(",");
      }
      unitTasks.append(PTask.TASK_U_GB_IDX);
    } else if (dbType.equals(DBMirrorConfig.GP_READER)) {
      this.setProteicType();
      prepaFolder.append(DBServerConfig.PROTEIN_TYPE);
      unitTasks.append(PTask.TASK_U_GP_IDX);
    } else if (dbType.equals(DBMirrorConfig.EM_READER)) {
      this.setNucleicType();
      prepaFolder.append(DBServerConfig.NUCLEIC_TYPE);
      unitTasks.append(PTask.TASK_U_EM_IDX);
    } else if (dbType.equals(DBMirrorConfig.BLASTP_READER)) {
      this.setProteicType();
      prepaFolder.append(DBServerConfig.PROTEIN_TYPE);
      unitTasks.append(PTask.TASK_U_FAS_IDX);
    } else if (dbType.equals(DBMirrorConfig.BLASTN_READER)) {
      this.setNucleicType();
      prepaFolder.append(DBServerConfig.NUCLEIC_TYPE);
      unitTasks.append(PTask.TASK_U_FAS_IDX);
    } else {
      // default is UniProt
      this.setProteicType();
      prepaFolder.append(DBServerConfig.PROTEIN_TYPE);
      unitTasks.append(PTask.TASK_U_SW_IDX);
    }

    String allFilters = PTaskFormatDB.CHECK_NR + "=" + Boolean.TRUE;
    allFilters += ";" + taxonFilter.toString();
    if (StringUtils.isNotEmpty(filters)) {
      allFilters += ";" + filters;
    }

    unitTasks.append("(" + allFilters + ")");

    this.setUnitPostTasks(unitTasks.toString());
    this.setGlobalPostTasks(globalTasks.toString());

    prepaFolder.append("|");
    prepaFolder.append(this.getName());

    this.setLocalFolder(prepaFolder.toString());

    this.setHistory(0);

    for (File file : databankFiles) {
      filepathList.append(",");
      filepathList.append(file.getAbsolutePath());
    }
    this.setIncludeFileList(filepathList.toString().substring(1)); // remove the
                                                                   // first
                                                                   // comma

  }

  /**
   * Upload a file using the load(InputSream) method
   * 
   * @param filePath
   * 
   * @throws IOException
   * @throws FileNotFoundException
   */
  public void load(String filePath) throws FileNotFoundException, IOException {
    try (FileInputStream fis = new FileInputStream(filePath)) {
      _properties.load(fis);
    }
  }

  /**
   * Create a temp file and store this descriptor inside. The created file will
   * be deleted on current JRE process exit
   * 
   * @param parentDirectory
   *          : the directory where the temp file will be stored. If null,
   *          File.createTempFile(...) will be called
   * 
   * @return the created temp file
   * 
   * @throws IOException
   */
  public File createTemporaryDscFile(File parentDirectory) throws IOException {
    FileOutputStream fos = null;
    File descFile = null;
    try {
      if (parentDirectory == null) {
        descFile = File.createTempFile(DBMSAbstractConfig.FPREF_DD,
            DBMSAbstractConfig.FEXT_DD);
      } else {
        descFile = new File(parentDirectory, DBMSAbstractConfig.FPREF_DD
            + UUID.randomUUID().toString() + DBMSAbstractConfig.FEXT_DD);
      }
      fos = new FileOutputStream(descFile);
      this.store(fos, this.getName());
    } finally {
      IOUtils.closeQuietly(fos);

      // to simplify handling of temporary descriptor created to install local
      // files, we ask the JRE to delete
      // the temp file
      try {
        if (fos != null && descFile != null) {
          descFile.deleteOnExit();
        }
      } catch (Exception ex2) {
      }
    }
    return descFile;
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
  protected Properties getProperties() {
    return _properties;
  }

  /**
   * Returns the system name.
   */
  public String getName() {
    return _properties.getProperty(DBNAME_KEY);
  }

  public void setName(String name) {
    if (name == null || name.length() == 0)
      return;
    _properties.setProperty(DBNAME_KEY, name);
  }

  /**
   * Returns a description.
   */
  public String getDescription() {
    return _properties.getProperty(DBDESC_KEY);
  }

  public void setDescription(String desc) {
    if (desc == null || desc.length() == 0)
      return;
    _properties.setProperty(DBDESC_KEY, desc);
  }

  /**
   * Returns a provider.
   */
  public String getProvider() {
    //added on June 2021, ensure backward compatibility
    String prov = _properties.getProperty(DBPROVIDER_KEY);
    return prov!=null?prov:"n/a";
  }

  public void setProvider(String prov) {
    if (prov == null || prov.length() == 0)
      return;
    _properties.setProperty(DBPROVIDER_KEY, prov);
  }

  
  /**
   * Return the databank to install before this one. If there is no depends db,
   * an empty List is returned (never null).
   * 
   * Depends dbs are stored in the descriptor file like this :
   * NCBI_Taxonomy,Enzyme,GeneOntology_terms.dsc...
   * 
   * @return the databank descriptor files to install before this one
   */
  public List<File> getDependsDatabanks() {
    List<File> result = new ArrayList<File>();
    String depends = _properties.getProperty(DB_DEPENDS);
    if (StringUtils.isNotBlank(depends)) {
      StringTokenizer tokens = new StringTokenizer(depends, ",");
      String token = null;
      File dependDbFile = null;
      while (tokens.hasMoreTokens()) {
        token = tokens.nextToken().trim();
        dependDbFile = new File(DBMSAbstractConfig.getConfPath(Configuration.DESCRIPTOR), token
            + DBMSAbstractConfig.FEXT_DD);
        if (dependDbFile.exists()) {
          result.add(dependDbFile);
        } else {
          LoggerCentral.warn(LOGGER, "Unable to find dependent databank: "
              + token + "(" + dependDbFile.getAbsolutePath() + ")");
        }
      }
    }
    return result;
  }

  /**
   * @return the data needed by the install in prod task if this descriptor is
   *         done to ONLY install in production
   */
  public String getInstallInProdData() {
    return _properties.getProperty(INSTALL_IN_PROD_DATA);
  }

  /**
   * Figures out whether the db is a nucleotide one. Returning false means that
   * the db is a protein one.
   */
  public boolean isNucleic() {
    String val = _properties.getProperty(DBTYPE_KEY);
    return NUCLEIC_TYPE.equals(val);
  }

  public boolean isProteic() {
    String val = _properties.getProperty(DBTYPE_KEY);
    return PROTEIN_TYPE.equals(val);
  }

  public boolean isDictionary() {
    String val = _properties.getProperty(DBTYPE_KEY);
    return DICO_TYPE.equals(val);
  }

  public String getTypeCode() {
    return _properties.getProperty(DBTYPE_KEY);
  }

  public void setProteicType() {
    _properties.setProperty(DBTYPE_KEY, PROTEIN_TYPE);
  }

  public void setNucleicType() {
    _properties.setProperty(DBTYPE_KEY, NUCLEIC_TYPE);
  }

  public void setDictionaryType() {
    _properties.setProperty(DBTYPE_KEY, DICO_TYPE);
  }

  public void setHistory(int val) {
    if (val < 0 || val > 5)
      return;
    _properties.setProperty(KEEP_HISTORY_KEY, String.valueOf(val));
  }

  /**
   * Returns the folder where the db will be installed on the local system.
   */
  public String getLocalFolder() {
    return Utils.terminatePath(DBMSExecNativeCommand.formatNativePath(
        _properties.getProperty(DBLDIR_KEY), false, false));
  }

  public void setLocalFolder(String folderPath) {
    if (folderPath == null || folderPath.length() == 0)
      return;
    _properties.setProperty(DBLDIR_KEY, folderPath);
  }

  /**
   * Returns the folder where the db will be installed on the local system
   * during FTP retrieval process.
   */
  public String getLocalTmpFolder() {
    return getLocalFolder() + DBMSAbstractConfig.DOWNLOADING_DIR
        + File.separator
        + DBMSExecNativeCommand.formatNativePath(getName(), false, false);
  }

  /**
   * Returns the folder where the db will be installed on the local system ready
   * for production usage.
   */
  public String getLocalProdFolder() {
    return Paths.get(DBMSAbstractConfig.getLocalMirrorPath(),
        this.getTypeCode(), this.getName(), DBMSAbstractConfig.CURRENT_DIR,
        DBMSExecNativeCommand.formatNativePath(getName(), false, false))
        .toString();
  }

  /**
   * Returns the file descriptor used to select the files to retrieve from the
   * remote system.
   */
  public NameMatcher getFileMatcher() {
    String[] incPatterns = Utils.tokenize(_properties
        .getProperty(DBINCFILES_KEY));
    String[] excPatterns = Utils.tokenize(_properties
        .getProperty(DBEXCFILES_KEY));
    return new NameMatcher(incPatterns, excPatterns);
  }

  public void setIncludeFileList(String files) {
    _properties.setProperty(DBINCFILES_KEY, files);
  }

  public String getIncludeFileList() {
    String val = _properties.getProperty(DBINCFILES_KEY);
    if (val!=null) {
      val = val.replaceAll("\\|", File.separator);
    }
    return val;
  }

  public String getDBLocalInstallDir() {
    return _properties.getProperty(DBLDIR_KEY);
  }

  public void setDBLocalInstallDir(String dbLDir) {
    _properties.setProperty(DBLDIR_KEY, dbLDir);
  }

  /**
   * Returns the Calendar descriptor used to select the files to retrieve from
   * the remote system. For tuture usage, always return null for now.
   */
  public CalendarMatcher getCalendarMatcher() {
    /* not yet implemented: will use DBDTxxx_KEY defined above */
    return null;
  }

  
  /**
   * Returns the Aspera server address. Format must be user@server.
   */
  public String getAsperaAddress() {
    return _properties.getProperty(ASPERASERVER_KEY);
  }
  /**
   * Do we have to bypass FTP server and use Aspera instead?
   */
  public boolean useAspera() {
    return Boolean.TRUE.toString().equalsIgnoreCase(_properties.getProperty(ASPERAUSE_KEY));
  }
  /**
   * Returns arguments to use with the Aspera CLI tool ascp.
   */
  public String getAsperaArguments() {
    return _properties.getProperty(ASPERAARGS_KEY);
  }
  
  /**
   * Returns the remote system Internet address.
   */
  public String getAddress() {
    return _properties.getProperty(FTPSERVER_KEY);
  }

  /**
   * Returns the connection port to use to access the remote system.
   */
  public int getPort() {
    return Integer.valueOf(_properties.getProperty(FTPPORT_KEY)).intValue();
  }

  /**
   * Returns the user name used to open the connection.
   */
  public String getUserName() {
    return _properties.getProperty(FTPUNAME_KEY);
  }

  /**
   * Returns the password used to open the connection.
   */
  public String getPassWord() {
    return _properties.getProperty(FTPPSWD_KEY);
  }

  /**
   * Returns the remote directory from where to download files.
   */
  public String getRemoteFolders() {
    return _properties.getProperty(FTPRDIR_KEY);
  }

  /**
   * Returns the list of patterns that cannot be found in remote directories.
   */
  public String getRemotePatternsToExclude() {
    return _properties.getProperty(FTPRDIR_EXCL_KEY);
  }
  
  /**
   * Returns protocol to use as an alternative to FTP.
   */
  public String getFTPAternativeProtocol() {
    return _properties.getProperty(FTPALT_PROTOCOL_KEY);
  }
  
  private String getNotNullProperty(String key) {
    String value = _properties.getProperty(key);
    if (value==null) {
      value="";
    }
    return value;
  }

  public String getUnitPostTasks() {
    return getNotNullProperty(TASKS_U_POST);
  }

  public void setUnitPostTasks(String tasks) {
    _properties.setProperty(TASKS_U_POST, tasks);
  }

  public String getGlobalPostTasks() {
    return getNotNullProperty(TASKS_G_POST);
  }

  public void setGlobalPostTasks(String tasks) {
    _properties.setProperty(TASKS_G_POST, tasks);
  }

  public String getGlobalPreTasks() {
    return getNotNullProperty(TASKS_G_PRE);
  }

  public void setGlobalPreTasks(String tasks) {
    _properties.setProperty(TASKS_G_PRE, tasks);
  }

  public String getHistoryToKeep() {
    return _properties.getProperty(KEEP_HISTORY_KEY);
  }

  public String getRemoteLocalFolders() {
    return _properties.getProperty(LOCALRDIR_KEY);
  }

  /**
   * Returns the list of patterns that cannot be found in remote directories.
   */
  public String getRemoteLocalPatternsToExclude() {
    return _properties.getProperty(LOCALRDIR_EXCL_KEY);
  }

  public void dumpContent(Log logger) {
    for (String key : KEYS) {
      if (_properties.containsKey(key) == false)
        continue;
      LoggerCentral.info(logger, key + "=" + _properties.get(key));
    }
  }

  /**
   * 
   * @return true if the databank must be install in production at the end of
   *         the install process
   */
  public boolean mustBeInstallInProduction() {
    if (StringUtils.isNotBlank(this.getGlobalPostTasks())) {
      StringTokenizer tokenizer = new StringTokenizer(
          this.getGlobalPostTasks(), ",");
      String task = null;
      while (tokenizer.hasMoreTokens()) {
        task = tokenizer.nextToken();
        if (task.contains(PTask.TASK_G_NOTINSTALLINPROD)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Create a descriptor that contains the minimum data to install in prod this
   * databank and store it in a new file in the conf directory
   * 
   * @return the new descriptor file
   * 
   * @throws Exception
   */
  public File createJustInstallInProd() throws Exception {
    DBServerConfig result = new DBServerConfig();
    result.setName(this.getName());
    result.setDescription(this.getDescription());
    result.setProperty(DBServerConfig.DBTYPE_KEY, this.getTypeCode());
    result.setLocalFolder(this.getLocalFolder());
    result.setProperty(DBServerConfig.DBEXCFILES_KEY, "");
    result.setProperty(DBServerConfig.DBINCFILES_KEY, "");
    result.setUnitPostTasks("");
    result.setGlobalPostTasks("");
    result.setGlobalPreTasks("");
    // keep all previous tasks for install in prod
    result.setProperty(DBServerConfig.INSTALL_IN_PROD_DATA,
        this.getUnitPostTasks() + "," + this.getGlobalPostTasks());

    File descriptorFile = new File(DBMSAbstractConfig.getConfPath(Configuration.DESCRIPTOR), "iip_"
        + result.getName() + DBMSAbstractConfig.FEXT_DD);
    int i = 1;
    while (descriptorFile.exists()) {
      if (!descriptorFile.delete()) {
        descriptorFile = new File(DBMSAbstractConfig.getConfPath(Configuration.DESCRIPTOR), "iip" + i
            + "_" + result.getName() + DBMSAbstractConfig.FEXT_DD);
        i++;
      }
    }
    try (FileOutputStream fos = new FileOutputStream(descriptorFile)) {
      result.store(fos, null);
    }
    return descriptorFile;
  }
}
