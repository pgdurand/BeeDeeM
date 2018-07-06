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
package bzh.plealog.dbmirror.indexer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.FSLockFactory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.store.NativeFSLockFactory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.store.SimpleFSLockFactory;

import bzh.plealog.dbmirror.fetcher.UserProcessingMonitor;
import bzh.plealog.dbmirror.lucenedico.DicoStorageSystem;
import bzh.plealog.dbmirror.lucenedico.DicoStorageSystemImplem;
import bzh.plealog.dbmirror.lucenedico.DicoTerm;
import bzh.plealog.dbmirror.task.PTaskEngine;
import bzh.plealog.dbmirror.task.PTaskEngineAbortException;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.conf.DBMSConfigurator;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * This class contains some utility methods to handle Lucene-based index files.
 * 
 * @author Patrick G. Durand
 */
public class LuceneUtils {

  private static final Log                              LOGGER                  = LogFactory
                                                                                    .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                                                        + ".LuceneUtils");

  public static final String                            DIR_TMP_FEXT            = ".ld.tmp";
  public static final String                            DIR_OK_FEXT             = ".ld";

  public static final String                            IDX_TMP_FEXT            = ".ldx.tmp";
  public static final String                            IDX_OK_FEXT             = ".ldx";

  private static Hashtable<String, LuceneStorageSystem> _openedSequenceStorages = new Hashtable<String, LuceneStorageSystem>();
  private static Hashtable<String, DicoStorageSystem>   _openedDicoStorages     = new Hashtable<String, DicoStorageSystem>();

  // from: http://www.ncbi.nlm.nih.gov/staff/tao/URLAPI/formatdb_fastacmd.html
  // gi,gb,emb,dbj,pir,prf,sp,pdb,pat,bbs,ref,gnl,lcl
  // if gnl, skip next token (database)
  // if pdb, get next token and wait (do not get chain)
  public static HashSet<String>                        DB_TOKENS;
  static {
    DB_TOKENS = new HashSet<String>();
    DB_TOKENS.add("GI");
    DB_TOKENS.add("GB");
    DB_TOKENS.add("EMB");
    DB_TOKENS.add("DBJ");
    DB_TOKENS.add("PIR");
    DB_TOKENS.add("PRF");
    DB_TOKENS.add("SP");
    DB_TOKENS.add("TR");
    DB_TOKENS.add("PDB");
    DB_TOKENS.add("PAT");
    DB_TOKENS.add("BBS");
    DB_TOKENS.add("REF");
    DB_TOKENS.add("GNL");
    DB_TOKENS.add("LCL");
  }

  /*
   * private static long getDirSize(String path){ long totSize = 0; File dir;
   * File[] files;
   * 
   * dir = new File(path); files = dir.listFiles(); if (files!=null &&
   * files.length!=0){ for(int i=0;i<files.length;i++){ totSize +=
   * files[i].length(); } } return totSize; }
   */
  private static synchronized LuceneStorageSystem getSequenceStorage(
      String idxName) {
    LuceneStorageSystem lss;

    lss = _openedSequenceStorages.get(idxName);
    if (lss != null)
      return lss;
    LOGGER.debug("open sequence storage : " + idxName);
    lss = new LuceneStorageSystem();
    if (!lss.open(idxName, StorageSystem.READ_MODE))
      return null;
    _openedSequenceStorages.put(idxName, lss);
    return lss;
  }

  private static synchronized DicoStorageSystem getDicoStorage(String idxName) {
    DicoStorageSystem lss;

    lss = _openedDicoStorages.get(idxName);
    if (lss != null)
      return lss;
    LOGGER.debug("open dictionary storage : " + idxName);
    lss = new DicoStorageSystemImplem();
    if (!lss.open(idxName, StorageSystem.READ_MODE))
      return null;
    _openedDicoStorages.put(idxName, lss);
    return lss;
  }

  public static synchronized void closeStorages() {
    Enumeration<LuceneStorageSystem> enums;
    Enumeration<DicoStorageSystem> dnums;

    LOGGER.debug("close storages");
    enums = _openedSequenceStorages.elements();
    while (enums.hasMoreElements()) {
      enums.nextElement().close();
    }
    dnums = _openedDicoStorages.elements();
    while (dnums.hasMoreElements()) {
      dnums.nextElement().close();
    }
    _openedSequenceStorages.clear();
    _openedDicoStorages.clear();
  }

  /**
   * Creates a new index named indexNamed and put all the content of the indexes
   * listed by indexPaths.
   * 
   * @param indexName
   *          the new index
   * @param indexPaths
   *          the list of full paths to individual index
   * @return true if success.
   */
  public static boolean mergeIndex(String indexName, List<String> indexPaths,
      UserProcessingMonitor monitor) {

    String path;
    int i, size;
    File f;
    ArrayList<String> fullIdxPath;
    try {
      size = indexPaths.size();
      fullIdxPath = new ArrayList<String>();
      for (i = 0; i < size; i++) {
        path = indexPaths.get(i);
        // check here before calling SimpleFSDirectory API: require otherwise
        // that API may create path if it does not exist.
        /*
         * f = new File(path); if (f.exists()==false){ throw new
         * Exception("index "+path+" not found"); }
         */
        // added because of KLDicoIndex tasks (see line
        // KLTaskDicoIndexer dicoTask = new KLTaskDicoIndexer(aName);
        // in DefaultLoaderMonitor
        if (!path.endsWith(LuceneUtils.DIR_OK_FEXT)) {
          LuceneUtils.collectIndex(path, fullIdxPath, LuceneUtils.DIR_OK_FEXT);
        } else {
          f = new File(path);
          if (f.exists() == false) {
            LoggerCentral.info(LOGGER, "skip " + path
                + " for merging: not found");
          } else {
            fullIdxPath.add(path);
          }
        }
      }
      LuceneStorageSystem lss = new LuceneStorageSystem();
      lss.open(indexName, StorageSystem.WRITE_MODE, false);
      size = fullIdxPath.size();

      IndexReader reader = null;
      int indexSize = 0;
      FSDirectory indexDir = null;
      for (i = 0; i < size; i++) {
        path = fullIdxPath.get(i);
        if (new File(path).exists()) {
          if (monitor != null) {
            monitor.processingMessage(PTaskEngine.WORKER_ID, null,
                UserProcessingMonitor.PROCESS_TYPE.TASK_EXECUTION,
                UserProcessingMonitor.MSG_TYPE.OK, "merging file '"
                    + new File(path).getName() + "'  " + (i + 1) + "/" + size);
          }
          LoggerCentral.info(LOGGER, "merging (" + (i + 1) + "/" + size + ") "
              + path + " with main index");
          indexDir = LuceneUtils.getDirectory(new File(path));
          reader = IndexReader.open(indexDir, true);
          indexSize = reader.maxDoc();
          if (indexSize > 1000000) {
            lss._writer.addIndexesNoOptimize(new Directory[] { indexDir });
          } else {
            for (int j = 0; j < reader.maxDoc(); j++) {
              lss.addDocument(reader.document(j));
            }
          }
          reader.close();
          if (LoggerCentral.processAborted()) {
            throw new PTaskEngineAbortException();
          }
        } else {
          LoggerCentral
              .info(LOGGER, "skip " + path + " for merging: not found");
        }
      }
      lss.close();
    } catch (Exception e) {
      LoggerCentral.error(LOGGER, "Unable to merge indexes: " + e);
      return false;
    }
    return true;
  }

  /**
   * Utility method aims at collecting all Lucene index files located under a
   * specific directory. This method also explores rootPath sub-directories if
   * any.
   */
  private static void collectIndex(String rootPath, ArrayList<String> lst,
      String ext) {
    File dir;
    File[] files;
    String name;

    dir = new File(rootPath);
    if (!dir.isDirectory())
      return;
    // does not collect index from DB under installation or discarded
    name = dir.getName();
    if (name.startsWith(DBMSAbstractConfig.CURRENTON_DIR)
        || Utils.isValidDate(name)) {
      return;
    }
    files = dir.listFiles();
    if (files == null || files.length == 0)
      return;
    for (File f : files) {
      if (f.isDirectory()) {
        if (f.getName().endsWith(ext)) {
          lst.add(f.getAbsolutePath());
        } else {
          collectIndex(f.getAbsolutePath(), lst, ext);
        }
      }
    }

  }

  /**
   * Starting from rootPath, explore the file system structure to collect all
   * directories terminating with extension defined by IDX_OK_FEXT. This method
   * never returns null.
   */
  public static List<String> collectIndex(String rootPath) {
    ArrayList<String> lst;

    lst = new ArrayList<String>();
    collectIndex(rootPath, lst, IDX_OK_FEXT);
    if (lst.isEmpty())
      lst.add(rootPath);
    return lst;
  }

  /**
   * Returns the number of entries contains in an index.
   */
  public static int getSize(String indexName) {
    LuceneStorageSystem lss = LuceneUtils.getSequenceStorage(indexName);
    if (lss != null)
      return lss.size();
    else
      return 0;
  }

  /**
   * Return an enumeration over all entries contained in an index. It is worth noting
   * that this method does not handle deleted documents, so use it only with clean
   * index.
   * 
   * @return an enumeration over all entries contained in the index or null.
   */
  public static Enumeration<DBEntry> entries(String indexName){
    LuceneStorageSystem lss = LuceneUtils.getSequenceStorage(indexName);
    if (lss != null)
      return lss.entries();
    else
      return null;
  }
  
  @SuppressWarnings("deprecation")
  protected static Query getQuery(String id) throws ParseException {
    StringTokenizer tokenizer;
    String token;
    BooleanQuery q;
    boolean isPdb = false;
    Analyzer an;
    QueryParser parser;

    tokenizer = new StringTokenizer(id, "|");
    q = new BooleanQuery();
    an = new StandardAnalyzer();
    parser = new QueryParser(LuceneStorageSystem.IDXABLE_FIELD, an);
    while (tokenizer.hasMoreTokens()) {
      token = tokenizer.nextToken();
      if (DB_TOKENS.contains(token)) {
        if ("GNL".equals(token)) {
          // before skipping, check if it is possible to do so
          if (tokenizer.hasMoreTokens())
            tokenizer.nextToken();
        } else if ("PDB".equals(token)) {
          // when PDB is detected, read next token : the entry ID
          // after reading that, we quit, since last token is the chain ID
          isPdb = true;
        }
        continue;
      }
      q.add(parser.parse(token), BooleanClause.Occur.SHOULD);
      if (isPdb)
        break;
    }
    return q;
  }

  /**
   * Return a database entry given a sequence ID and a list of Lucene index
   * files.
   * 
   * @return null if entry id cannot be found or if any errors occurred during
   *         the index searching.
   */
  public static DBEntry getEntry(List<String> idxNames, String id) {
    LuceneStorageSystem lss = null;
    DBEntry[] entries;
    DBEntry entry = null;
    Query q;

    if (idxNames == null || idxNames.size() == 0 || id == null)
      return null;
    try {
      q = getQuery(id);
      for (String idxName : idxNames) {
        lss = LuceneUtils.getSequenceStorage(idxName);
        if (lss != null)
          entries = lss.getEntry(id, q);
        else
          entries = null;
        if (entries != null) {
          if (entries.length > 1) {
            // this may happen with Uniprot entries. As for now, the
            // software returns the first entry.
            LOGGER.warn("ID " + id
                + " refers to multiple entries. First entry is returned.");
          }
          entry = entries[0];
        }
        if (entry != null)
          break;
      }
    } catch (Exception ex) {
      LOGGER.warn("Unable to get DBEntry: " + ex);
    }
    if (entry == null) {
      LOGGER.debug("Unable to locate ID " + id);
    }
    return entry;
  }

  /**
   * Return a database entry given a sequence ID and the rootPath containing
   * Lucene index files.
   * 
   * @return null if entry id cannot be found or if any errors occurred during
   *         the index searching.
   */
  public static DBEntry getEntry(String rootPath, String id) {
    DBEntry entry;
    if (rootPath == null || id == null)
      return null;
    entry = getEntry(LuceneUtils.collectIndex(rootPath), id);
    if (entry == null)
      LOGGER.debug("Unable to locate ID " + id);
    return entry;
  }

  /**
   * Return a database entry given a dictionary term ID and a list of Lucene
   * index files.
   * 
   * @return null if entry id cannot be found or if any errors occurred during
   *         the index searching.
   */
  public static DicoTerm[] getTerms(List<String> idxNames, String[] ids) {
    DicoStorageSystem lss = null;
    DicoTerm[] entries = null;

    if (idxNames == null || idxNames.size() == 0 || ids == null
        || ids.length == 0)
      return null;
    try {
      for (String idxName : idxNames) {
        lss = LuceneUtils.getDicoStorage(idxName);
        if (lss != null)
          entries = lss.getTerms(ids);
        else
          entries = null;
        lss = null;
        if (entries != null)
          break;
      }
    } catch (Exception ex) {
      LOGGER.warn("Unable to get DicoTerm: " + ex);
    }
    if (entries == null) {
      LOGGER.debug("Unable to locate ID " + ids[0] + "...");
    }
    return entries;
  }

  /**
   * Return a database entry given a dictionary term ID and a list of Lucene
   * index files.
   * 
   * @return null if entry id cannot be found or if any errors occurred during
   *         the index searching.
   */
  public static DicoTerm[] getTerms(String rootPath, String[] ids) {
    DicoTerm[] entries = null;
    if (rootPath == null || ids == null || ids.length == 0)
      return null;
    entries = getTerms(LuceneUtils.collectIndex(rootPath), ids);
    if (entries == null)
      LOGGER.debug("Unable to locate ID " + ids[0] + "...");
    return entries;
  }

  /**
   * Factory method used to create a Lucene based file system for a given
   * directory path.
   * 
   * @param directory use an absolute path
   * 
   * @return a Lucene based file system
   */
  public static FSDirectory getDirectory(File directory) throws IOException{
	  DBMSConfigurator.LUCENE_LK_VALUES lkType = DBMSAbstractConfig.getLuceneLockType();
	  DBMSConfigurator.LUCENE_FS_VALUES fsType = DBMSAbstractConfig.getLuceneFSType();

	  // backward compatibility prior to release 4.1.1: this system was not available
	  if (lkType.equals(DBMSConfigurator.LUCENE_LK_VALUES.LK_DEFAULT) &&
			  fsType.equals(DBMSConfigurator.LUCENE_FS_VALUES.FS_DEFAULT)){
		  return FSDirectory.open(directory);
	  }

	  FSDirectory fsDir;
	  FSLockFactory fsLock;

	  switch(lkType){
	  case LK_SIMPLE:
		  fsLock = new SimpleFSLockFactory();
		  break;
	  case LK_NATIVE:
		  fsLock = new NativeFSLockFactory();
		  break;
	  default:
		  fsLock = null;
	  }

	  // Notice: using xxxFSDirectory constructor with Lock argument does NOT
	  // behave the same as using xxxFSDirectory basic constructor, then calling
	  // setLockFactory(). So, we retain the following ugly code.
	  switch(fsType){
	  case FS_NIO:
		  fsDir = fsLock!=null ? NIOFSDirectory.open(directory, fsLock) : 
			  NIOFSDirectory.open(directory);
		  break;
	  case FS_SIMPLE:
		  fsDir = fsLock!=null ? SimpleFSDirectory.open(directory, fsLock) :
			  SimpleFSDirectory.open(directory);
		  break;
	  default:
		  fsDir = fsLock!=null ? FSDirectory.open(directory, fsLock) :
			  FSDirectory.open(directory);
	  }

	  return fsDir;
  }
}
