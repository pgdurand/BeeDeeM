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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.fetcher.DBServerConfig;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptor;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptorUtils;
import bzh.plealog.dbmirror.util.runner.DBStampProperties;

/**
 * This class contains the configuration file listing the available Lucene
 * indexes that can be used to query sequence databases indexed with KDMS.
 * 
 * @author Patrick G. Durand
 */
public class DBMirrorConfig {
  private Properties         _dbMirrorConfig;

  // !!!!!!! adding keys requires to update the save() method

  public static final String USERS           = ".users";
  public static final String READER          = ".reader";
  public static final String MIRROR          = ".mirror";
  public static final String MIRRORS         = ".mirrors";
  public static final String NAME            = ".name";
  public static final String DESC            = ".desc";
  public static final String SEQS            = ".seqs";
  public static final String BLAST_SIZE      = ".bsize";
  public static final String DISK_SIZE       = ".dsize";

  // special keys. When adding something here, please update the copy() method.
  public static final String IDKEY           = "id.key";
  public static final String SEQLIMIT        = "size.max";
  public static final String KEEPTMPFILE     = "keep.native";

  public static final String DEF_DB_NAME     = "data index";

  // available keys to be used in the configuration file defining
  // the access to the mirror (see conf/dbmirror.properties)
  public static final String GB_READER       = "gb";                      // Genbank
  public static final String GP_READER       = "gp";                      // Genpept
  public static final String EM_READER       = "embl";                    // embl
  public static final String UP_READER       = "up";                      // Uniprot
  public static final String FASTN_READER    = "fastn";                   // Fasta
                                                                           // nucleic
                                                                           // with
                                                                           // taxon
  public static final String FASTP_READER    = "fastp";                   // Fasta
                                                                           // proteic
                                                                           // with
                                                                           // taxon

  public static final String BLASTN_READER   = "blastn";                  // BLAST
                                                                           // nucleic
                                                                           // fomatdb
                                                                           // type
                                                                           // (special
                                                                           // case)
  public static final String BLASTP_READER   = "blastp";                  // BLAST
                                                                           // proteic
                                                                           // fomatdb
                                                                           // type
                                                                           // (special
                                                                           // case)

  // 3 types of index
  public static final String PROTEIC_IDX     = "protein";
  public static final String NUCLEOTIDEC_IDX = "nucleotide";
  public static final String DICO_IDX        = "dico";
  public static final String DELETED_IDX     = "deleted";

  public static final String P_CODE          = "|"
                                                 + DBServerConfig.PROTEIN_TYPE
                                                 + "|";
  public static final String N_CODE          = "|"
                                                 + DBServerConfig.NUCLEIC_TYPE
                                                 + "|";
  public static final String D_CODE          = "|" + DBServerConfig.DICO_TYPE
                                                 + "|";

  protected static final Log LOGGER          = LogFactory
                                                 .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                     + ".DBMirrorConfig");

  /**
   * Constructor.
   */
  public DBMirrorConfig() {
    _dbMirrorConfig = new Properties();
  }

  /**
   * Makes a shallow copy of this DBMirrorConfig. All keys but idx ones are
   * copied.
   */
  public void copy(DBMirrorConfig src) {
    this.setIdKeyString(src.getIdKeyString());
    this.setKeepTmpFile(src.keepTmpFile());
    this.setMaxSequenceSize(src.getMaxSequenceSize());

    List<String> deletedCodes = src.getMirrorCodes(DBMirrorConfig.DELETED_IDX);
    if (deletedCodes != null) {
      this.removeMirrorCode(deletedCodes);
    }
  }

  private void dumpDbs(String indexKey, BufferedWriter writer)
      throws IOException {
    List<String> ids;
    Iterator<String> iter;
    String dbKey, value, dbPath;

    ids = getMirrorCodes(indexKey);
    if (ids == null)
      return;
    // write header: list of db ids
    writer.write(indexKey);
    writer.write(MIRRORS);
    writer.write("=");
    iter = ids.iterator();
    while (iter.hasNext()) {
      writer.write(iter.next());
      if (iter.hasNext()) {
        writer.write(",");
      }
    }
    writer.write("\n");
    // write global reader type
    dbKey = indexKey + READER;
    value = _dbMirrorConfig.getProperty(dbKey);
    if (value != null) {
      writer.write(dbKey);
      writer.write("=");
      writer.write(value);
      writer.write("\n");
    }
    writer.write("\n");
    if (!indexKey.equalsIgnoreCase(DELETED_IDX)) {
      // write each db
      iter = ids.iterator();
      while (iter.hasNext()) {
        dbKey = iter.next();
        dbPath = new File(getMirrorPath(dbKey)).getParent();
        // name
        writer.write(dbKey);
        writer.write(NAME);
        writer.write("=");
        writer.write(_dbMirrorConfig.getProperty(dbKey + NAME));
        writer.write("\n");
        // desc
        writer.write(dbKey);
        writer.write(DESC);
        writer.write("=");
        writer.write(_dbMirrorConfig.getProperty(dbKey + DESC));
        writer.write("\n");
        // path
        writer.write(dbKey);
        writer.write(MIRROR);
        writer.write("=");
        writer.write(_dbMirrorConfig.getProperty(dbKey + MIRROR));
        writer.write("\n");
        // reader
        writer.write(dbKey);
        writer.write(READER);
        writer.write("=");
        writer.write(_dbMirrorConfig.getProperty(dbKey + READER));
        writer.write("\n");
        // users
        // compatibility issue: USERS was introduced for KDMS 3.1
        value = _dbMirrorConfig.getProperty(dbKey + USERS);
        if (value == null) {
          value = DBDescriptor.ALL_USERS;
        }
        writer.write(dbKey);
        writer.write(USERS);
        writer.write("=");
        writer.write(value);
        writer.write("\n");
        // nb sequences
        // compatibility issue: SEQS was introduced for KDMS 4.4.1
        value = _dbMirrorConfig.getProperty(dbKey + SEQS);
        if (value == null) {
          try {
            Properties props = DBMSAbstractConfig.readDBStamp(dbPath);
            value = props.getProperty(DBStampProperties.NB_SEQUENCES);
          } catch (Exception e) {
            value = "0";
          }
        }
        writer.write(dbKey);
        writer.write(SEQS);
        writer.write("=");
        writer.write(value);
        writer.write("\n");
        // disk size
        // compatibility issue: DISK_SIZE was introduced for KDMS 4.4.1
        value = _dbMirrorConfig.getProperty(dbKey + DISK_SIZE);
        if (value == null) {
          try {
            value = String.valueOf(FileUtils.sizeOfDirectory(new File(dbPath)));
          } catch (Exception e) {
            value = "0";
          }
        }
        writer.write(dbKey);
        writer.write(DISK_SIZE);
        writer.write("=");
        writer.write(value);
        writer.write("\n");
        // blast size
        // compatibility issue: BLAST_SIZE was introduced for KDMS 4.4.1
        value = _dbMirrorConfig.getProperty(dbKey + BLAST_SIZE);
        if (value == null) {
          try {
            value = String.valueOf(Utils.getBlastVolumesSize(dbPath));
          } catch (Exception e) {
            value = "0";
          }
        }
        writer.write(dbKey);
        writer.write(BLAST_SIZE);
        writer.write("=");
        writer.write(value);
        writer.write("\n");
        // end
        writer.write("\n");
      }
    }
  }

  /**
   * Save the current configuration. This method flushes the stream, but does
   * not close the output stream.
   */
  public boolean save(OutputStream os) {
    BufferedWriter writer;
    try {
      writer = new BufferedWriter(new OutputStreamWriter(os));
      // header
      writer.write("#DB Mirror configuration\n");
      writer.write("#Last update:");
      writer.write(Calendar.getInstance().getTime().toString());
      writer.write("\n\n");
      // global properties ; use method calls to handle first init
      writer.write(IDKEY);
      writer.write("=");
      writer.write(getIdKeyString());
      writer.write("\n");
      writer.write(SEQLIMIT);
      writer.write("=");
      writer.write(new Integer(getMaxSequenceSize()).toString());
      writer.write("\n");
      writer.write(KEEPTMPFILE);
      writer.write("=");
      writer.write(keepTmpFile() ? Boolean.TRUE.toString() : Boolean.FALSE
          .toString());
      writer.write("\n");
      writer.write("\n");
      // list of mirrors
      dumpDbs(PROTEIC_IDX, writer);
      dumpDbs(NUCLEOTIDEC_IDX, writer);
      dumpDbs(DICO_IDX, writer);
      dumpDbs(DELETED_IDX, writer);
      writer.flush();
    } catch (IOException e) {
      LOGGER.warn("unable to save db mirror configuration" + e);
      return false;
    }
    return true;
  }

  /**
   * Uploads the configuration file. This method delegates the load to the load
   * method of class Properties. So, a configuration file has to be formatted
   * according the Properties specifications.
   */
  public boolean load(InputStream inStream) {
    try {
      _dbMirrorConfig.load(inStream);
    } catch (Exception e1) {
      LOGGER.warn("unable to load db mirror configuration: " + e1);
      return false;
    }

    Enumeration<Object> e = _dbMirrorConfig.keys();
    String key, value;

    while (e.hasMoreElements()) {
      key = (String) e.nextElement();
      value = _dbMirrorConfig.getProperty(key).trim();
      _dbMirrorConfig.setProperty(key, value);
    }
    return true;
  }

  public boolean isEmpty() {
    List<String> lst;

    lst = getMirrorCodes(DBMirrorConfig.DICO_IDX);
    if (lst != null && lst.isEmpty() == false)
      return false;
    lst = getMirrorCodes(DBMirrorConfig.PROTEIC_IDX);
    if (lst != null && lst.isEmpty() == false)
      return false;
    lst = getMirrorCodes(DBMirrorConfig.NUCLEOTIDEC_IDX);
    if (lst != null && lst.isEmpty() == false)
      return false;
    return true;
  }

  private List<String> getList(String str) {
    ArrayList<String> set;
    StringTokenizer tokenizer;

    tokenizer = new StringTokenizer(str.trim(), ",\n");
    String token;

    set = new ArrayList<String>();
    while (tokenizer.hasMoreTokens()) {
      token = tokenizer.nextToken().trim();
      set.add(token);
    }
    return set;
  }

  /**
   * Returns the list of db codes associated to a particular db key. Return null
   * if no codes exist for the provided db key.
   */
  public List<String> getMirrorCodes(String dbKey) {
    String val;

    val = _dbMirrorConfig.getProperty(dbKey + MIRRORS);
    if (val == null)
      return null;
    return getList(val);
  }

  /**
   * Return the Lucene index path given a db key.
   * 
   * @param dbKey
   *          can be a db key or a code key.
   */
  public String getMirrorPath(String dbKey) {
    String val, dbPath;
    int idx;

    val = _dbMirrorConfig.getProperty(dbKey + MIRROR);
    if (val != null) {
      // this code has been added to work with relative paths (ngPlast 4.2+).
      //
      // dbmirror.config stores absolute paths. We continue to do that, however,
      // when
      // the system reloads the file, it will update paths to take into account
      // where
      // the dbmirror.config file is actually located. This new behavior enables
      // the
      // move of an entire KDMS repository very easily.
      //
      // To ensure backward compatibility, we get the db path (which can be an
      // absolute path)
      // and we replace the part located before "|p|", "|n|" or "|d|" by the
      // dbMirrorConfig
      // path.
      dbPath = DBMSAbstractConfig.getLocalMirrorPath();
      dbPath = Utils.transformCode(dbPath, false);
      idx = val.indexOf(P_CODE);
      if (idx != -1) {
        // +1 : dbPath already contains a terminal "|"
        val = dbPath + val.substring(idx + 1);
      } else {
        idx = val.indexOf(N_CODE);
        if (idx != -1) {
          val = dbPath + val.substring(idx + 1);
        } else {
          idx = val.indexOf(D_CODE);
          if (idx != -1) {
            val = dbPath + val.substring(idx + 1);
          }
        }
      }
      val = Utils.transformCode(val, true);
    }
    return val;
  }

  /**
   * Return the db key given a db name
   * 
   * @param dbName
   * @return dbKey
   */
  public String getDbKey(String dbName) {

    Enumeration<Object> e = _dbMirrorConfig.keys();
    while (e.hasMoreElements()) {
      String key = (String) e.nextElement();
      if (key.contains(NAME) && _dbMirrorConfig.getProperty(key).equals(dbName))
        return key.replace(NAME, "").trim();
    }

    return null;
  }

  /**
   * Return the Lucene index name given a db key.
   * 
   * @param dbKey
   *          can be a db key or a code key.
   */
  public String getMirrorName(String dbKey) {
    return _dbMirrorConfig.getProperty(dbKey + NAME);
  }

  /**
   * Return the Lucene index description given a db key.
   * 
   * @param dbKey
   *          can be a db key or a code key.
   */
  public String getMirrorDescription(String dbKey) {
    return _dbMirrorConfig.getProperty(dbKey + DESC);
  }

  /**
   * Return the sequence format reader given a db key.
   * 
   * @param dbKey
   *          can be a db key or a code key.
   * @return one of the XX_READER constants defined in this class.
   */
  public String getMirrorReader(String dbKey) {
    return _dbMirrorConfig.getProperty(dbKey + READER);
  }

  /**
   * Return the list of user names authorized to use a db key.
   * 
   * @param dbKey
   *          can be a db key or a code key.
   * @return either a * or a list of user names. Star character means that
   *         everyone can use db key. Otherwise only the list of users are
   *         authorized to use the db key.
   */
  public String getMirrorAuthorizedUsers(String dbKey) {
    String value;

    value = _dbMirrorConfig.getProperty(dbKey + USERS);
    // compatibility issue: USERS was introduced for KDMS 3.1
    if (value == null)
      value = DBDescriptor.ALL_USERS;
    return value;
  }

  /**
   * Return the the number of sequences given a db key.
   * 
   * @param dbKey
   *          can be a db key or a code key.
   */
  public long getMirrorSequences(String dbKey) {
    String sValue;
    long lValue = 0;

    try {
      sValue = _dbMirrorConfig.getProperty(dbKey + SEQS);
      // compatibility issue: SEQS was introduced for KDMS 4.4.1
      if (StringUtils.isBlank(sValue)) {
        String dbPath = new File(getMirrorPath(dbKey)).getParent();
        Properties props = DBMSAbstractConfig.readDBStamp(dbPath);
        lValue = Long
            .valueOf(props.getProperty(DBStampProperties.NB_SEQUENCES));
      } else {
        lValue = Long.valueOf(sValue);
      }
    } catch (Exception e) {
    }

    return lValue;
  }

  /**
   * Return the size on disk given a db key.
   * 
   * @param dbKey
   *          can be a db key or a code key.
   */
  public long getMirrorDiskSize(String dbKey) {
    String sValue;
    long lValue = 0;

    try {
      sValue = _dbMirrorConfig.getProperty(dbKey + DISK_SIZE);
      // compatibility issue: DISK_SIZE was introduced for KDMS 4.4.1
      if (StringUtils.isBlank(sValue)) {
        String dbPath = new File(getMirrorPath(dbKey)).getParent();
        lValue = FileUtils.sizeOfDirectory(new File(dbPath));
      } else {
        lValue = Long.valueOf(sValue);
      }
    } catch (Exception e) {
    }

    return lValue;
  }

  /**
   * Return the size of blast files given a db key.
   * 
   * @param dbKey
   *          can be a db key or a code key.
   */
  public long getMirrorBlastSize(String dbKey) {
    String sValue;
    long lValue = 0;

    try {
      sValue = _dbMirrorConfig.getProperty(dbKey + BLAST_SIZE);
      // compatibility issue: DISK_SIZE was introduced for KDMS 4.4.1
      if (StringUtils.isBlank(sValue)) {
        String dbPath = new File(getMirrorPath(dbKey)).getParent();
        lValue = Utils.getBlastVolumesSize(dbPath);
      } else {
        lValue = Long.valueOf(sValue);
      }
    } catch (Exception e) {
    }

    return lValue;
  }

  /**
   * Checks is a db path exists.
   * 
   * @param dbKey
   *          can be a db key or a code key.
   */
  public boolean mirrorPathExists(String dbKey) {
    return _dbMirrorConfig.getProperty(dbKey + MIRROR) != null;
  }

  /**
   * Checks is a db code exists.
   */
  public boolean mirrorCodeExists(String dbKey, String code) {
    List<String> set;
    String val;

    val = _dbMirrorConfig.getProperty(dbKey + MIRRORS);
    if (val == null)
      return false;
    set = getList(val);
    return set.contains(code);
  }

  /**
   * Adds a db mirror to this configuration. dbKey, path and reader are
   * mandatory. If a db mirror with the same dbKey already exists in this
   * configuration, it'll be replaced.
   * 
   * @return true if the db mirror has been added to this configuration.
   */
  public boolean addMirrorPath(String dbKey, String path, String dbName,
      String dbDesc, String reader, String users, long sequences,
      long diskSize, long blastSize) {
    if (dbKey == null || reader == null || path == null) {
      return false;
    }
    if (dbName == null)
      dbName = dbKey + " " + DEF_DB_NAME;
    if (dbDesc == null)
      dbDesc = dbName;
    _dbMirrorConfig.setProperty(dbKey + MIRROR,
        Utils.transformCode(path, false));
    _dbMirrorConfig.setProperty(dbKey + NAME, dbName);
    _dbMirrorConfig.setProperty(dbKey + DESC, dbDesc);
    _dbMirrorConfig.setProperty(dbKey + READER, reader);
    _dbMirrorConfig.setProperty(dbKey + USERS,
        (users == null ? DBDescriptor.ALL_USERS : users));
    _dbMirrorConfig.setProperty(dbKey + SEQS, String.valueOf(sequences));
    _dbMirrorConfig.setProperty(dbKey + DISK_SIZE, String.valueOf(diskSize));
    _dbMirrorConfig.setProperty(dbKey + BLAST_SIZE, String.valueOf(blastSize));
    return true;
  }

  private String getFormattedCodeList(String dbKey, String code) {
    List<String> codes;
    Iterator<String> iter;
    StringBuffer buf;
    String val;
    boolean alreadyExist = false;

    codes = getMirrorCodes(dbKey);
    if (codes == null)
      return code;
    buf = new StringBuffer();
    iter = codes.iterator();
    while (iter.hasNext()) {
      val = iter.next();
      if (val.equals(code)) {
        alreadyExist = true;
      }
      buf.append(val);
      if (iter.hasNext())
        buf.append(",");
    }
    if (!alreadyExist) {
      buf.append(",");
      buf.append(code);
    }
    return buf.toString();
  }

  private String getFormattedCodeList(List<String> codes) {
    Iterator<String> iter;
    StringBuffer buf;

    buf = new StringBuffer();
    iter = codes.iterator();
    while (iter.hasNext()) {
      buf.append(iter.next());
      if (iter.hasNext())
        buf.append(",");
    }
    return buf.toString();
  }

  /**
   * Adds a db mirror code and its definition to this configuration. dbKey,
   * code, path and reader are mandatory. If a db mirror with the same dbKey
   * already exists in this configuration, it'll be replaced.
   * 
   * @return true if the db mirror has been added to this configuration.
   */
  public boolean addMirrorCode(String dbKey, String code, String path,
      String dbName, String dbDesc, String reader, String users,
      long sequences, long diskSize, long blastSize) {
    if (dbKey == null || code == null || reader == null || path == null) {
      return false;
    }
    _dbMirrorConfig.setProperty(dbKey + MIRRORS,
        getFormattedCodeList(dbKey, code));
    _dbMirrorConfig.setProperty(dbKey + READER, reader);
    return addMirrorPath(code, path, dbName, dbDesc, reader, users, sequences,
        diskSize, blastSize);
  }

  /**
   * Store the kbCodes in the "deleted.mirrors" property to not reuse it
   * 
   * @param kbCodesToDelete
   */
  public void removeMirrorCode(List<String> kbCodesToDelete) {
    StringBuilder allDeleted = new StringBuilder();
    Set<String> all = new HashSet<String>();

    // memory current deleted codes
    List<String> stillDeleteds = getMirrorCodes(DELETED_IDX);
    if (stillDeleteds != null) {
      all.addAll(stillDeleteds);
    }

    // disk deleted codes
    List<String> deletedsInCurrentConfig = DBDescriptorUtils.getDBMirrorConfig(
        DBMSAbstractConfig.getLocalMirrorConfFile())
        .getMirrorCodes(DELETED_IDX);
    if (deletedsInCurrentConfig != null) {
      all.addAll(deletedsInCurrentConfig);
    }

    // new ones
    if (kbCodesToDelete != null) {
      all.addAll(kbCodesToDelete);
    }

    for (String toDelete : all) {
      allDeleted.append(toDelete);
      allDeleted.append(",");
    }

    _dbMirrorConfig.setProperty(DELETED_IDX + MIRRORS, allDeleted.toString()
        .substring(0, allDeleted.length() - 1));
  }

  /**
   * Reorder an existing list of db codes. This method checks for the existence
   * of dbKey, but not for individual code.
   * 
   * @return true if success.
   */
  public boolean setMirrorCodesOrder(String dbKey, List<String> codes) {
    if (dbKey == null || codes == null || codes.isEmpty())
      return false;
    if (_dbMirrorConfig.getProperty(dbKey + MIRRORS) == null)
      return false;
    _dbMirrorConfig.setProperty(dbKey + MIRRORS, getFormattedCodeList(codes));
    return true;
  }

  /**
   * Reorder an existing list of db codes. This method checks for the existence
   * of dbKey, but not for individual code.
   * 
   * @return true if success.
   */
  public boolean setMirrorCodesOrder(String dbKey, String codes) {
    if (dbKey == null || codes == null)
      return false;
    if (_dbMirrorConfig.getProperty(dbKey + MIRRORS) == null)
      return false;
    _dbMirrorConfig.setProperty(dbKey + MIRRORS, codes);
    return true;
  }

  /**
   * Returns a comma separated list of string codes each of them being some kind
   * of db identifier. Such an id can be used to analyse sequence entry id
   * definition line to get back a correct sequence id.
   */
  public String getIdKeyString() {
    String val = _dbMirrorConfig.getProperty(IDKEY);

    if (val == null)
      return "gi,sp,lcl";
    else
      return val;
  }

  public void setIdKeyString(String ids) {
    _dbMirrorConfig.setProperty(IDKEY, ids);
  }

  /**
   * Returns the maximum size allowed to download a sequence.
   */
  public int getMaxSequenceSize() {
    String val = _dbMirrorConfig.getProperty(SEQLIMIT);
    int max;
    if (val == null) {
      return 32000;
    } else {
      max = Integer.valueOf(val);
    }
    return max;
  }

  public void setMaxSequenceSize(int max) {
    _dbMirrorConfig.setProperty(SEQLIMIT, String.valueOf(max));
  }

  /**
   * Figures if the system should keep the temporary file created.
   */
  public boolean keepTmpFile() {
    String val = _dbMirrorConfig.getProperty(KEEPTMPFILE);

    if (val == null)
      return false;
    else
      return "true".equalsIgnoreCase(val);
  }

  public void setKeepTmpFile(boolean val) {
    _dbMirrorConfig.setProperty(KEEPTMPFILE, val ? "true" : "false");
  }

  public String getValue(String key) {
    return _dbMirrorConfig.getProperty(key);
  }

  /**
   * @param dbName
   * 
   * @return true if the parameter is one of the registered db name
   */
  public boolean containsDbName(String dbName) {

    Enumeration<Object> keys = _dbMirrorConfig.keys();
    while (keys.hasMoreElements()) {
      String key = (String) keys.nextElement();
      if (key.contains(NAME)
          && _dbMirrorConfig.getProperty(key).equalsIgnoreCase(dbName)) {
        return true;
      }
    }

    return false;
  }
}
