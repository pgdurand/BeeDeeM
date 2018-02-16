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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Hit;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * This class represents a Lucene-based index aims at storing required data to
 * retrieve specific entries from sequence database files.
 * 
 * @author Patrick G. Durand
 */
@SuppressWarnings("deprecation")
public class LuceneStorageSystem implements StorageSystem {
  protected IndexWriter             _writer;
  protected IndexSearcher           _searcher;
  protected RAMDirectory            _ramDir;
  protected IndexWriter             _ramWriter;
  protected int                     _mode;
  protected boolean                 _writerOk           = false;
  protected boolean                 _readerOk           = false;
  protected String                  _indexName;
  protected Document[]              _docs;
  protected int                     _maxDocs            = 10000;
  protected int                     _curDoc;
  private int                       _docsAddedCount     = 0;
  private int                       _queryCounter       = 1;
  private Hashtable<String, String> _keyNames           = new Hashtable<String, String>();

  public static final String        ID_FIELD            = "id";
  protected static final String     NAME_FIELD          = "name";
  protected static final String     FNAME_FIELD         = "fName";
  protected static final String     START_FIELD         = "start";
  protected static final String     STOP_FIELD          = "stop";
  public static final String        IDXABLE_FIELD       = "idxable";

  private static final Log          LOGGER              = LogFactory
                                                            .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                                + ".LuceneStorageSystem");

  private static final boolean      USE_RAM_DRIVE       = true;
  private static final String       ERR_MSG1            = "While adding entry in index: ";
  private static final int          DOC_ADDED_PAGE_SIZE = 200000;

  // this was added to ensure compatibility with prevsious release of kdms
  // where idxFileName did contain only the file name without full path.
  // Full path were added in 'dxxx' files to handle the case where source
  // files are not located in the same directory.
  public static final String        FP_KEY              = "FP:";
  public static final String        FP_KEY_FILE_PREFIX  = "d";
  private static final Object       LOCKER              = new Object();

  // doc to reuse for insert in lucene index
  private Document                  doc;
  private Field                     fieldId;
  private Field                     fieldName;
  private Field                     fieldFName;
  private Field                     fieldStart;
  private Field                     fieldStop;
  private Field                     fieldIdxAble;

  public LuceneStorageSystem() {
    this.doc = new Document();
    this.fieldId = new Field(ID_FIELD, "", Field.Store.YES, Field.Index.NO);
    this.fieldName = new Field(NAME_FIELD, "", Field.Store.YES, Field.Index.NO);
    this.fieldFName = new Field(FNAME_FIELD, "", Field.Store.YES,
        Field.Index.NO);
    this.fieldStart = new Field(START_FIELD, "", Field.Store.YES,
        Field.Index.NO);
    this.fieldStop = new Field(STOP_FIELD, "", Field.Store.YES, Field.Index.NO);
    this.fieldIdxAble = new Field(IDXABLE_FIELD, "", Field.Store.YES,
        Field.Index.ANALYZED);
    this.doc.add(this.fieldId);
    this.doc.add(this.fieldName);
    this.doc.add(this.fieldFName);
    this.doc.add(this.fieldStart);
    this.doc.add(this.fieldStop);
    this.doc.add(this.fieldIdxAble);
  }

  private void checkRamUsage() throws IOException {
    long totMem = Runtime.getRuntime().totalMemory(); // current heap allocated
                                                      // to the VM process
    long freeMem = Runtime.getRuntime().freeMemory(); // out of the current
                                                      // heap, how much is free
    long maxMem = Runtime.getRuntime().maxMemory(); // Max heap VM can use e.g.
                                                    // Xmx setting
    long usedMemory = totMem - freeMem; // how much of the current heap the VM
                                        // is using

    if (_docsAddedCount % DOC_ADDED_PAGE_SIZE == 0) {
      LoggerCentral.info(LOGGER, "Sequences added: " + _docsAddedCount + " ["
          + Utils.getBytes(usedMemory) + "/" + Utils.getBytes(totMem) + "//"
          + Utils.getBytes(maxMem) + "]");
    }

    // when used memory is > 70 of max memory, report a warning
    if ((usedMemory * 100 / maxMem) > 70) {
      LoggerCentral.info(LOGGER, "flush RAM: " + Utils.getBytes(usedMemory)
          + "/" + Utils.getBytes(maxMem));
      _ramWriter.flush();
      _ramWriter.close();
      _writer.addIndexes(new Directory[] { _ramDir });
      System.gc();
      _ramWriter = new IndexWriter(_ramDir, new StandardAnalyzer(), true);

    }
  }

  /**
   * Implementation of StorageSystem interface.
   */
  public void addEntry(String id, String name, String fName, long start,
      long stop) throws StorageSystemException {
    String keyName;

    if (_writerOk == false)
      throw new StorageSystemException("LuceneStorageSystem not initialised.");
    try {
      synchronized (LOCKER) {
        keyName = getIdxKeyName(_indexName, fName);
      }
      this.fieldId.setValue(id);
      this.fieldName.setValue(name);
      this.fieldFName.setValue(keyName);
      this.fieldStart.setValue(String.valueOf(start));
      this.fieldStop.setValue(String.valueOf(stop));

      if (name.isEmpty()) {
        this.fieldIdxAble.setValue(id);
      } else {
        this.fieldIdxAble.setValue(id + " " + name);
      }
      this.addDocument(this.doc);

    } catch (Exception e) {
      throw new StorageSystemException(ERR_MSG1 + e);
    }

  }

  public void addDocument(Document doc) throws IOException {
    _docsAddedCount++;
    if (USE_RAM_DRIVE) {
      checkRamUsage();
      _ramWriter.addDocument(doc);
    } else {
      if (_docsAddedCount % DOC_ADDED_PAGE_SIZE == 0) {
        LoggerCentral.info(LOGGER, "Sequences added: " + _docsAddedCount);
      }
      _writer.addDocument(doc);
    }

  }

  protected String convertPath(String fName) {
    String str;
    int idx1, idx2;

    // this method is used to replace the time stamp (starter date) used
    // in path name of "dx" file. This way, the term "current" is appropriately
    // set in the "dx" files during auto-mirror indexing.
    str = DBMSAbstractConfig.DOWNLOADING_DIR;
    if (str == null)
      return fName;
    str = File.separator + str + File.separator;

    idx1 = fName.indexOf(str);
    if (idx1 == -1)
      return fName;
    idx2 = idx1 + str.length();
    return fName.substring(0, idx1 + 1) + DBMSAbstractConfig.CURRENT_DIR
        + fName.substring(idx2 - 1);
  }

  private String getIdxKeyName(String idxName, String fName) throws IOException {
    PrintWriter writer;
    String path, keyName;
    File file;

    keyName = _keyNames.get(fName);
    if (keyName != null) {
      return keyName;
    }
    file = new File(idxName);
    String parent = file.getParent();
    if (parent!=null)
      path = Utils.terminatePath(parent);
    else
      path="";
    while (true) {
      keyName = FP_KEY_FILE_PREFIX + _queryCounter;
      file = new File(path + keyName);
      if (file.exists() == false) {
        writer = new PrintWriter(new FileWriter(file));
        // backward compatibilty ; we won't use anymore absolute path to
        // retrieve
        // path to data storage (see DBMirrorConfig.getMirrorPath()). So, here
        // we build
        // the full path from the index location of only the file name part
        // stored in
        // the "dX" file name.
        // backward compatibility when data file was written as "FP:XXX" where
        // XXX was an absolute file path
        writer.write(FP_KEY + convertPath(fName));
        writer.close();
        _keyNames.put(fName, keyName);
        break;
      }
      _queryCounter++;
    }
    return keyName;
  }

  private String getRealFName(String key) {
    BufferedReader reader = null;
    String fName = null;
    String path, srcFile;
    File file;

    try {
      file = new File(_indexName);
      path = Utils.terminatePath(file.getParent());
      reader = new BufferedReader(new FileReader(path + key));
      srcFile = reader.readLine();
      if (srcFile != null) {
        if (srcFile.startsWith(FP_KEY)) {
          // backward compatibilty ; we won't use anymore absolute path to
          // retrieve
          // path to data storage (see DBMirrorConfig.getMirrorPath()). So, here
          // we build
          // the full path from the index location of only the file name part
          // stored in
          // the "dX" file name.
          // backward compatibility when data file was written as "FP:XXX" where
          // XXX was an absolute file path
          file = new File(srcFile.substring(FP_KEY.length()));
          fName = path + file.getName();
        } else {
          fName = path + srcFile;
        }
      }
      reader.close();
    } catch (Exception e) {
      fName = null;
    } finally {
      IOUtils.closeQuietly(reader);
    }
    return fName;
  }

  /**
   * Implementation of StorageSystem interface. Please note that this method
   * only works for Index opened in READ mode. Returns zero otherwise.
   */
  public int size() {
    if (_searcher != null)// read mode
      return _searcher.getIndexReader().numDocs();
    else
      return 0;
  }

  /**
   * Implementation of StorageSystem interface.
   */
  public DBEntry[] getEntry(String id) throws StorageSystemException {
    DBEntry[] entries = null;
    try {
      entries = getEntry(id, LuceneUtils.getQuery(id));
    } catch (ParseException e) {
      throw new StorageSystemException(e.getMessage());
    }

    return entries;
  }

  /**
   * Implementation of StorageSystem interface.
   */
  public DBEntry[] getEntry(String id, Query q) throws StorageSystemException {
    DBEntry[] entries = null;
    Hits hits = null;
    Hit hit;
    Iterator<?> iter;
    Document doc;
    String key, fName;
    int i = 0;

    if (_readerOk == false)
      throw new StorageSystemException("LuceneStorageSystem not initialised.");

    try {
      hits = _searcher.search(q);
      if (hits.length() != 0) {
        iter = hits.iterator();
        entries = new DBEntry[hits.length()];
        while (iter.hasNext()) {
          hit = (Hit) iter.next();
          doc = hit.getDocument();
          key = doc.get(FNAME_FIELD);
          fName = getRealFName(key);
          if (fName == null) {
            throw new Exception("Unable to get data fName for key: " + key);
          }
          entries[i] = new DBEntry(doc.get(ID_FIELD), doc.get(NAME_FIELD),
              fName, doc.get(START_FIELD), doc.get(STOP_FIELD));
          entries[i].setIndexPath(_indexName);
          i++;
        }
      }
    } catch (Exception e) {
      throw new StorageSystemException("Unable to query index: " + e);
    }
    return entries;
  }

  /**
   * Closes a Lucene writer.
   */
  protected boolean closeWriter() {
    boolean bRet = true;
    if (_writerOk == false)
      return true;
    try {
      if (USE_RAM_DRIVE) {
        _ramWriter.flush();
        _ramWriter.close();
        _writer.addIndexes(new Directory[] { _ramDir });
      }
      _writer.flush();
      _writer.optimize();
      _writer.close();
    } catch (IOException e) {
      LoggerCentral.error(LOGGER, "Unable to close index (write mode): " + e);
      bRet = false;
    }
    return bRet;
  }

  /**
   * Opens a Lucene writer.
   */
  protected boolean openWriter() {
    boolean bRet = true;

    try {
      _writer = new IndexWriter(_indexName, new StandardAnalyzer(
          Version.LUCENE_29), true);
      if (USE_RAM_DRIVE) {
        _ramDir = new RAMDirectory();
        _ramWriter = new IndexWriter(_ramDir, new StandardAnalyzer(
            Version.LUCENE_29), true);
      }
      _writerOk = true;

    } catch (IOException e) {
      LoggerCentral.error(LOGGER, "Unable to open index (write mode): " + e);
      bRet = false;
    }
    return bRet;
  }

  /**
   * Closes a Lucene reader.
   */
  protected boolean closeReader() {
    boolean bRet = true;
    if (_readerOk == false)
      return true;
    try {
      _searcher.close();
    } catch (IOException e) {
      LoggerCentral.error(LOGGER, "Unable to close index (read mode): " + e);
      bRet = false;
    }
    return bRet;
  }

  /**
   * Opens a Lucene reader.
   */
  protected boolean openReader(boolean loadInRAM) {
    boolean bRet = true;

    try {
      if (loadInRAM)
        _searcher = new IndexSearcher(new RAMDirectory(_indexName));
      else
        _searcher = new IndexSearcher(_indexName);
      _readerOk = true;
    } catch (IOException e) {
      LoggerCentral.error(LOGGER, "Unable to open index (read mode): " + e);
      bRet = false;
    }
    return bRet;
  }

  /**
   * Implementation of StorageSystem interface.
   */
  public boolean close() {
    boolean bRet = true;

    switch (_mode) {
      case StorageSystem.WRITE_MODE:
        bRet = closeWriter();
        break;
      case StorageSystem.READ_MODE:
        bRet = closeReader();
        break;
    }
    _readerOk = false;
    return bRet;
  }

  /**
   * Implementation of StorageSystem interface.
   */
  public boolean open(String name, int mode, boolean loadInRAM) {
    boolean bRet = true;

    _mode = mode;
    _indexName = name;

    switch (mode) {
      case StorageSystem.WRITE_MODE:
        bRet = openWriter();
        break;
      case StorageSystem.READ_MODE:
        bRet = openReader(loadInRAM);
        break;
    }
    return bRet;
  }

  /**
   * Implementation of StorageSystem interface.
   */
  public boolean open(String name, int mode) {
    return open(name, mode, false);
  }
}