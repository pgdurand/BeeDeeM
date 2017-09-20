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
package bzh.plealog.dbmirror.util.descriptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import com.plealog.genericapp.api.file.EZFileFilter;

import bzh.plealog.dbmirror.fetcher.DBServerConfig;
import bzh.plealog.dbmirror.lucenedico.Dicos;
import bzh.plealog.dbmirror.task.PTask;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.conf.DBMirrorConfig;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptor.TYPE;
import bzh.plealog.dbmirror.util.runner.DBStampProperties;
import bzh.plealog.dbmirror.util.runner.FormatDBRunner;
import bzh.plealog.dbmirror.util.runner.UniqueIDGenerator;

/**
 * This class contains some utility methods to handle DBDescriptor objects.
 */
public class DBDescriptorUtils {
  /**
   * Sorts a list of DBDescriptor by ascending order of their names. This
   * comparator is not case sensitive. The ordered list is the returned value of
   * this method.
   */
  public static List<DBDescriptor> sort(List<DBDescriptor> dbListSrc) {
    ArrayList<DBDescriptor> dbList, tmpList;
    DBDescComparator comp;

    comp = new DBDescComparator();
    tmpList = new ArrayList<DBDescriptor>();
    dbList = new ArrayList<DBDescriptor>();
    // handle Proteic DB
    for (DBDescriptor db : dbListSrc) {
      if (db.getType() == DBDescriptor.TYPE.proteic)
        tmpList.add(db);
    }
    Collections.sort(tmpList, comp);
    for (DBDescriptor db : tmpList) {
      dbList.add(db);
    }
    // handle Nucleic DB
    tmpList.clear();
    for (DBDescriptor db : dbListSrc) {
      if (db.getType() == DBDescriptor.TYPE.nucleic)
        tmpList.add(db);
    }
    Collections.sort(tmpList, comp);
    for (DBDescriptor db : tmpList) {
      dbList.add(db);
    }
    // handle dico DB
    tmpList.clear();
    for (DBDescriptor db : dbListSrc) {
      if (db.getType() == DBDescriptor.TYPE.dico)
        tmpList.add(db);
    }
    Collections.sort(tmpList, comp);
    for (DBDescriptor db : tmpList) {
      dbList.add(db);
    }

    // handle blast proteic DB
    tmpList.clear();
    for (DBDescriptor db : dbListSrc) {
      if (db.getType() == DBDescriptor.TYPE.blastp)
        tmpList.add(db);
    }
    Collections.sort(tmpList, comp);
    for (DBDescriptor db : tmpList) {
      dbList.add(db);
    }
    // handle blast nucleic DB
    tmpList.clear();
    for (DBDescriptor db : dbListSrc) {
      if (db.getType() == DBDescriptor.TYPE.blastn)
        tmpList.add(db);
    }
    Collections.sort(tmpList, comp);
    for (DBDescriptor db : tmpList) {
      dbList.add(db);
    }
    return dbList;
  }

  public static List<IdxDescriptor> sort2(List<IdxDescriptor> dbListSrc) {
    ArrayList<IdxDescriptor> dbList, tmpList;
    DBDescComparator comp;

    comp = new DBDescComparator();
    tmpList = new ArrayList<IdxDescriptor>();
    dbList = new ArrayList<IdxDescriptor>();
    // handle Proteic DB
    for (IdxDescriptor db : dbListSrc) {
      if (db.getType() == DBDescriptor.TYPE.proteic)
        tmpList.add(db);
    }
    Collections.sort(tmpList, comp);
    for (IdxDescriptor db : tmpList) {
      dbList.add(db);
    }
    // handle Nucleic DB
    tmpList.clear();
    for (IdxDescriptor db : dbListSrc) {
      if (db.getType() == DBDescriptor.TYPE.nucleic)
        tmpList.add(db);
    }
    Collections.sort(tmpList, comp);
    for (IdxDescriptor db : tmpList) {
      dbList.add(db);
    }
    // handle dico DB
    tmpList.clear();
    for (IdxDescriptor db : dbListSrc) {
      if (db.getType() == DBDescriptor.TYPE.dico)
        tmpList.add(db);
    }
    Collections.sort(tmpList, comp);
    for (IdxDescriptor db : tmpList) {
      dbList.add(db);
    }

    // handle blast proteic DB
    tmpList.clear();
    for (IdxDescriptor db : dbListSrc) {
      if (db.getType() == DBDescriptor.TYPE.blastp)
        tmpList.add(db);
    }
    Collections.sort(tmpList, comp);
    for (IdxDescriptor db : tmpList) {
      dbList.add(db);
    }
    // handle blast nucleic DB
    tmpList.clear();
    for (IdxDescriptor db : dbListSrc) {
      if (db.getType() == DBDescriptor.TYPE.blastn)
        tmpList.add(db);
    }
    Collections.sort(tmpList, comp);
    for (IdxDescriptor db : tmpList) {
      dbList.add(db);
    }
    return dbList;
  }

  private static void exploreConfig(DBMirrorConfig mirrorCfg, String dbKey,
      List<IdxDescriptor> dbs, DBDescriptor.TYPE type) {
    List<String> idxCodes;
    Iterator<String> iterS;
    IdxDescriptor db;
    String idxCode, reader;

    idxCodes = mirrorCfg.getMirrorCodes(dbKey);
    if (idxCodes != null) {
      iterS = idxCodes.iterator();
      while (iterS.hasNext()) {
        idxCode = iterS.next();
        db = new IdxDescriptor();
        db.setKbCode(idxCode);
        db.setName(mirrorCfg.getMirrorName(idxCode));
        db.setCode(mirrorCfg.getMirrorPath(idxCode));
        reader = mirrorCfg.getMirrorReader(idxCode);
        db.setReader(reader);
        db.setAuthorizedUsers(mirrorCfg.getMirrorAuthorizedUsers(idxCode));
        db.setDescription(mirrorCfg.getMirrorDescription(idxCode));
        db.setSequences(mirrorCfg.getMirrorSequences(idxCode));
        db.setBlastSize(mirrorCfg.getMirrorBlastSize(idxCode));
        db.setDiskSize(mirrorCfg.getMirrorDiskSize(idxCode));
        if (reader.equals(DBMirrorConfig.BLASTN_READER))
          db.setType(DBDescriptor.TYPE.blastn);
        else if (reader.equals(DBMirrorConfig.BLASTP_READER))
          db.setType(DBDescriptor.TYPE.blastp);
        else
          db.setType(type);
        addNewIndexDB(dbs, db);
      }
    }
  }

  public static List<IdxDescriptor> prepareIndexDBList(DBMirrorConfig mirrorCfg) {
    ArrayList<IdxDescriptor> dbs;

    dbs = new ArrayList<IdxDescriptor>();
    if (mirrorCfg == null)
      return dbs;
    exploreConfig(mirrorCfg, DBMirrorConfig.DICO_IDX, dbs,
        DBDescriptor.TYPE.dico);
    exploreConfig(mirrorCfg, DBMirrorConfig.PROTEIC_IDX, dbs,
        DBDescriptor.TYPE.proteic);
    exploreConfig(mirrorCfg, DBMirrorConfig.NUCLEOTIDEC_IDX, dbs,
        DBDescriptor.TYPE.nucleic);

    List<String> deletedCodes = mirrorCfg
        .getMirrorCodes(DBMirrorConfig.DELETED_IDX);
    if (deletedCodes != null) {
      for (String deletedCode : deletedCodes) {
        UniqueIDGenerator.registerKey(deletedCode);
      }
    }
    return dbs;
  }

  /**
   * Adds a new index in the List. Overwrite existing idx by new definition.
   */
  public static void addNewIndexDB(List<IdxDescriptor> dbs, IdxDescriptor idx) {
    UniqueIDGenerator.registerKey(idx.getKbCode());
    String key;
    String keyEnzyme = null;
    IdxDescriptor desc, descToUpdate = null;
    int i, size;

    // compatibilty for enzyme after dico refactoring
    if (idx.getReader().equals(Dicos.ENZYME.readerId)) {
      keyEnzyme = idx.getName() + "ecd"; // previous enzyme reader code
    }

    key = idx.getName() + idx.getReader();

    size = dbs.size();
    // locate idx if it already exists in the list
    for (i = 0; i < size; i++) {
      desc = dbs.get(i);
      if ((desc.getName() + desc.getReader()).equals(key)) {
        descToUpdate = desc;
        break;
      }
      if (StringUtils.isNotBlank(keyEnzyme)) {
        if ((desc.getName() + desc.getReader()).equals(keyEnzyme)) {
          descToUpdate = desc;
          descToUpdate.setReader(Dicos.ENZYME.readerId);
          break;
        }
      }
    }
    // replace old by new one if any
    if (descToUpdate != null) {
      descToUpdate.setCode(idx.getCode());
    }
    // otherwise, add new descriptor at the end of the list
    else {
      dbs.add(idx);
    }
  }

  /**
   * Convert the List of mirror indexes into a freshly created DBMirrorConfig.
   * 
   * @param dbs
   *          list of descriptors
   * @param oldConfig
   *          if this parameter is not null, make a shallow copy of oldCOnfig
   *          within the new config. See DBMirrorConfig.copy() for more
   *          information.
   */
  public static DBMirrorConfig getMirrorConfig(Collection<IdxDescriptor> dbs,
      DBMirrorConfig oldConfig) {
    DBMirrorConfig newConf;
    String indexType;
    DBDescriptor.TYPE type;

    newConf = new DBMirrorConfig();
    if (oldConfig != null)
      newConf.copy(oldConfig);
    for (IdxDescriptor idx : dbs) {
      type = idx.getType();
      if (type == DBDescriptor.TYPE.nucleic)
        indexType = DBMirrorConfig.NUCLEOTIDEC_IDX;
      else if (type == DBDescriptor.TYPE.proteic)
        indexType = DBMirrorConfig.PROTEIC_IDX;
      else
        indexType = DBMirrorConfig.DICO_IDX;
      newConf.addMirrorCode(indexType, idx.getKbCode(), idx.getCode(),
          idx.getName(), idx.getDescription(), idx.getReader(),
          idx.getAuthorizedUsers(), idx.getSequences(), idx.getDiskSize(),
          idx.getBlastSize());
    }
    return newConf;
  }

  /**
   * Insert a new index within the Map of available mirror index. Overwrite
   * existing idx by new definition.
   * 
   * @param reader
   *          one of DBMirrorConfig.XXX_READER values.
   * @param dbDownloadPath
   */
  public static void addNewIndex(List<IdxDescriptor> dbs, String dbName,
      String description, String fullPath, String reader, String dbDownloadPath) {
    DBDescriptor.TYPE type;
    IdxDescriptor idx;
    String uniqueID;
    long sequences, diskSize, blastSize;

    uniqueID = UniqueIDGenerator.getUniqueKey("ldx");
    if (reader.equals(DBMirrorConfig.GB_READER)
        || reader.equals(DBMirrorConfig.EM_READER)
        || reader.equals(DBMirrorConfig.FASTN_READER))
      type = DBDescriptor.TYPE.nucleic;
    else if (reader.equals(DBMirrorConfig.UP_READER)
        || reader.equals(DBMirrorConfig.GP_READER)
        || reader.equals(DBMirrorConfig.FASTP_READER))
      type = DBDescriptor.TYPE.proteic;
    else if (reader.equals(DBMirrorConfig.BLASTN_READER))
      type = DBDescriptor.TYPE.blastn;
    else if (reader.equals(DBMirrorConfig.BLASTP_READER))
      type = DBDescriptor.TYPE.blastp;
    else
      type = DBDescriptor.TYPE.dico;

    File file = new File(dbDownloadPath);
    if (file != null && file.exists()) {
      if (file.isDirectory()) {
        dbDownloadPath = file.getAbsolutePath();
      } else {
        dbDownloadPath = file.getParent();
      }
    }

    try {
      Properties props = DBMSAbstractConfig.readDBStamp(dbDownloadPath);
      sequences = Long.valueOf(props
          .getProperty(DBStampProperties.NB_SEQUENCES));
    } catch (Exception e) {
      sequences = 0;
    }

    try {
      diskSize = FileUtils.sizeOfDirectory(new File(dbDownloadPath));
    } catch (Exception e) {
      diskSize = 0;
    }

    try {
      blastSize = Utils.getBlastVolumesSize(dbDownloadPath);
    } catch (Exception e) {
      blastSize = 0;
    }

    idx = new IdxDescriptor(dbName, description, fullPath, uniqueID, reader,
        type, sequences, diskSize, blastSize);
    addNewIndexDB(dbs, idx);
  }

  public static DBMirrorConfig getDBMirrorConfig(String confFile) {
    DBMirrorConfig config = null;
    File f;
    FileInputStream fis = null;

    config = new DBMirrorConfig();
    if (confFile == null)
      return config;
    f = new File(confFile);
    if (f.exists()) {
      try {
        fis = new FileInputStream(f);
        config.load(fis);
      } catch (Exception e) {
        throw new RuntimeException("unable to read mirror configuration: "
            + confFile + ": " + e);
      } finally {
        IOUtils.closeQuietly(fis);
      }
    }
    return config;
  }

  public static DBMirrorConfig getLocalDBMirrorConfig() {
    return DBDescriptorUtils.getDBMirrorConfig(DBMSAbstractConfig
        .getLocalMirrorConfFile());
  }

  public static void saveDBMirrorConfig(String confFile, DBMirrorConfig conf) {
    FileOutputStream fos = null;
    File f;

    if (confFile == null)
      return;
    f = new File(confFile);
    try {
      fos = new FileOutputStream(f);
      conf.save(fos);
    } catch (Exception e) {
      throw new RuntimeException("unable to save mirror configuration: "
          + confFile + ": " + e);
    } finally {
      IOUtils.closeQuietly(fos);
    }
  }

  public static boolean hasTaxonomyConstrainArgs(DBServerConfig config) {
    String args;

    args = config.getGlobalPostTasks() + "," + config.getUnitPostTasks();
    if (args.indexOf(PTask.TAX_EXCLUDE) != -1
        || args.indexOf(PTask.TAX_INCLUDE) != -1) {
      return true;
    } else {
      return false;
    }
  }

  public static List<IdxDescriptor> getBlastDbList(DBMirrorConfig config,
      boolean orderByName) {
    Iterator<IdxDescriptor> iter;
    IdxDescriptor db;
    ArrayList<IdxDescriptor> dbs;
    List<IdxDescriptor> dbsSource;
    HashSet<TYPE> types;

    dbs = new ArrayList<IdxDescriptor>();
    types = new HashSet<TYPE>();
    types.add(DBDescriptor.TYPE.blastn);
    types.add(DBDescriptor.TYPE.blastp);
    dbsSource = DBDescriptorUtils.prepareIndexDBList(config);
    if (orderByName)
      dbsSource = DBDescriptorUtils.sort2(dbsSource);
    iter = dbsSource.iterator();
    while (iter.hasNext()) {
      db = iter.next();
      if (types != null && types.contains(db.getType())) {
        dbs.add(db);
      }
    }
    return dbs;
  }

  public static List<IdxDescriptor> getDBList(DBMirrorConfig config,
      DBDescriptor.TYPE type, boolean orderByName) {
    Iterator<IdxDescriptor> iter;
    IdxDescriptor db;
    ArrayList<IdxDescriptor> dbs;
    List<IdxDescriptor> dbsSource;

    dbs = new ArrayList<IdxDescriptor>();
    dbsSource = DBDescriptorUtils.prepareIndexDBList(config);
    if (orderByName)
      dbsSource = DBDescriptorUtils.sort2(dbsSource);
    iter = dbsSource.iterator();
    while (iter.hasNext()) {
      db = iter.next();
      if (db.getType().equals(type)) {
        dbs.add(db);
      }
    }
    return dbs;
  }

  // DBDescriptor.TYPE.blastn or DBDescriptor.TYPE.blastp
  public static List<String> getBlastDbKeysList(DBMirrorConfig config,
      DBDescriptor.TYPE type) {
    Iterator<IdxDescriptor> iter;
    IdxDescriptor db;
    ArrayList<String> dbs;
    List<IdxDescriptor> dbsSource;
    HashSet<TYPE> types;

    dbs = new ArrayList<String>();
    types = new HashSet<TYPE>();
    types.add(type);
    dbsSource = DBDescriptorUtils.prepareIndexDBList(config);
    iter = dbsSource.iterator();
    while (iter.hasNext()) {
      db = iter.next();
      if (types != null && types.contains(db.getType())) {
        dbs.add(db.getKbCode());
      }
    }
    return dbs;
  }

  /**
   * Returns the list of Fasta volumes given a Blast alias file.
   * 
   * @param dbAliasFile
   *          the absolute path to the Blast alias file (.nal or .pal)
   * 
   * @return a list of absolute paths to Fasta volumes. List may be empty if
   *         alias does not refer to any Fasta volumes.
   */
  public static List<String> getFastaVolumes(String dbAliasFile) {
    ArrayList<String> dbPaths;
    File dbDir;
    String dbName, fName;
    File[] files;

    dbPaths = new ArrayList<String>();
    dbDir = new File(new File(dbAliasFile).getParent());
    dbName = dbDir.getName();

    files = dbDir.listFiles();
    if (files == null)
      return dbPaths;
    for (File f : files) {
      if (f.isDirectory())
        continue;
      fName = f.getName();
      if (fName.startsWith(dbName)
          && Character.isDigit(fName.charAt(fName.length() - 1))
          && Character.isDigit(fName.charAt(fName.length() - 2))) {
        dbPaths.add(f.getAbsolutePath());
      }
    }
    return dbPaths;
  }

  /**
   * Returns the list of Fasta volumes given a db descriptor
   * 
   * @param db
   * 
   * @return a list of absolute paths to Fasta volumes. List may be empty if
   *         alias does not refer to any Fasta volumes.
   */
  public static List<String> getFastaVolumes(DBDescriptor db) {
    DBMirrorConfig config = DBDescriptorUtils.getLocalDBMirrorConfig();
    String dbKey = config.getDbKey(db.getName());
    return Utils.getFileVolumes(
        new File(config.getMirrorPath(dbKey)).getParent(), db.getName());
  }

  /**
   * 
   * Gets the cumulative size of BLAST volumes.
   * 
   * @param db
   * 
   * @return the cumulative size of BLAST volumes.
   */
  public static long getBlastVolumesSize(DBDescriptor db) {
    DBMirrorConfig config = DBDescriptorUtils.getLocalDBMirrorConfig();
    String dbKey = config.getDbKey(db.getName());
    return Utils.getBlastVolumesSize(new File(config.getMirrorPath(dbKey))
        .getParent());
  }

  /**
   * Returns the list of original files used to create the db
   * 
   * @param db
   * 
   * @return a list of original files. The list can be empty if no original
   *         files were found
   */
  public static List<File> getOriginalFile(DBDescriptor db) {
    DBMirrorConfig config = DBDescriptorUtils.getLocalDBMirrorConfig();
    String dbKey = config.getDbKey(db.getName());
    return Utils.getOriginalFiles(new File(config.getMirrorPath(dbKey))
        .getParentFile());
  }

  /**
   * Returns the absolute path of the Plast info file.
   * 
   * @param dbAliasFile
   *          the absolute path to the Blast alias file (.nal or .pal)
   * 
   * @return the absolute path of the Plast info file or null if not found. Such
   *         a info file may exist within the DB repository, next to fasta
   *         volumes and Blast alias, when the makeplastindex has been used.
   */
  public static String getInfoVolume(String dbAliasFile) {
    String dbInfoFile = null;
    int idx;

    // locate the nal or pal file extention
    idx = dbAliasFile.lastIndexOf(".");
    if (idx == -1)
      return null;

    // locate the info file, which must have the same file name as the dbAlias
    // file
    dbInfoFile = dbAliasFile.substring(0, idx) + FormatDBRunner.Plast_INFO_EXT;
    if (new File(dbInfoFile).exists() == false)
      return null;

    return dbInfoFile;
  }

  // 06/06/2014 new KLib => use blast databank instead of volume files
  // /**
  // * Returns the absolute path of the Plast info file.
  // *
  // * @param dbAliasFile the absolute path to the Blast alias file (.nal or
  // .pal)
  // * @param maxdbsize the max database size
  // *
  // * @return the absolute path of the Plast info file or null if not found.
  // Such a info file may
  // * exist within the DB repository, next to fasta volumes and Blast alias,
  // when the makeplastindex
  // * has been used.
  // */
  // public static String getInfoVolume(String dbAliasFile, int maxdbsize){
  // String dbInfoFile=null;
  // int idx;
  //
  // // check if maxdbsize is an authorized value.
  // if (!FormatDBRunner.MAX_DB_SIZE_LIST.containsKey(maxdbsize)) {
  // return null;
  // }
  //
  // //locate the nal or pal file extention
  // idx = dbAliasFile.lastIndexOf(".");
  // if (idx==-1)
  // return null;
  //
  // //locate the info file, which must have the same file name as the dbAlias
  // file
  // dbInfoFile = dbAliasFile.substring(0, idx) +
  // FormatDBRunner.MAX_DB_SIZE_LIST.get(maxdbsize);
  // if (new File(dbInfoFile).exists() == false)
  // return null;
  //
  // return dbInfoFile;
  // }

  /**
   * Format a list of absolute paths to Fasta volumes. Any volume is separate by
   * a decimal point with no space.
   * 
   * @param dbPaths
   * @return dbpaths format for Plast execution parameter
   */
  public static String formatDbAbsolutePath(List<String> dbPaths) {
    StringBuffer sbBuffer = new StringBuffer();

    Iterator<String> iter = dbPaths.iterator();
    while (iter.hasNext()) {
      String dbPath = (String) iter.next();
      sbBuffer.append(dbPath);
      if (iter.hasNext())
        sbBuffer.append(",");
    }

    return sbBuffer.toString();
  }

  public static boolean hasSequenceDatabankAvailable(DBMirrorConfig conf) {
    List<String> prots, nucs;

    if (conf == null)
      return false;
    nucs = conf.getMirrorCodes(DBMirrorConfig.NUCLEOTIDEC_IDX);
    prots = conf.getMirrorCodes(DBMirrorConfig.PROTEIC_IDX);
    if (nucs != null || prots != null) {
      return true;
    } else {
      return false;
    }
  }

  public static boolean hasBlastDatabankAvailable(DBMirrorConfig conf) {
    List<String> dicos;
    String reader;

    if (conf == null)
      return false;
    dicos = conf.getMirrorCodes(DBMirrorConfig.DICO_IDX);
    if (dicos == null)
      return false;
    for (String dico : dicos) {
      reader = conf.getMirrorReader(dico);
      if (reader.equals(DBMirrorConfig.BLASTN_READER)
          || reader.equals(DBMirrorConfig.BLASTP_READER)) {
        return true;
      }
    }
    return false;
  }

  public static boolean hasDicoDatabankAvailable(DBMirrorConfig conf) {
    List<String> dicos;
    String reader;

    if (conf == null)
      return false;
    dicos = conf.getMirrorCodes(DBMirrorConfig.DICO_IDX);
    if (dicos == null)
      return false;
    for (String dico : dicos) {
      reader = conf.getMirrorReader(dico);
      if (!reader.equals(DBMirrorConfig.BLASTN_READER)
          && !reader.equals(DBMirrorConfig.BLASTP_READER)) {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Locate a Blast bank given a databank install path.
   * 
   * @param path path targeting databank installation location
   * 
   * @return the Blast bank alias name without its extension
   * */
  public static String getBlastBankAliasName(String dbPath){
    String aliasName = null;
    
    Iterator<File> files = FileUtils.iterateFiles(new File(dbPath), 
        new String[]{FormatDBRunner.PROTEIN_ALIAS, FormatDBRunner.NUCLEIC_ALIAS},  false);
    
    if (files.hasNext() == false)
      return null;
    aliasName =  EZFileFilter.getWithoutExtension(files.next());
    return aliasName;
  }
}
