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
package bzh.plealog.dbmirror.util.runner;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import bzh.plealog.dbmirror.indexer.LuceneUtils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;

/**
 * This class represents a Lucene-based index aims at storing unique sequence
 * IDs.
 * 
 * @author Patrick G. Durand
 */
public class DBMSUniqueSeqIdIndex {

  private IndexWriter   _writer;
  private IndexSearcher _searcher;
  private String        _indexName;
  private Document      _document    = new Document();
  private Field         _field       = new Field(SEQID_FIELD, "",
                                         Field.Store.NO,
                                         Field.Index.NOT_ANALYZED);
  private Term          _term        = new Term(SEQID_FIELD);
  private int           nbDocInIndex = 0;

  public static enum MODE {
    READ_MODE, WRITE_MODE, WRITE_APPEND_MODE
  };

  private static final Log       LOGGER    = LogFactory
      .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY + "KDMSUniqueSeqIdIndex");
  private static final int    LUCENE_SEARCH_HITS_MAX_NUMBER = 1;

  private static final String SEQID_FIELD                   = "seqid";

  private static final String ERR_MSG3                      = "unable to add seqId: ";
  private static final String ERR_MSG4                      = "unable to commit data: ";

  public DBMSUniqueSeqIdIndex() {
    this._document.add(this._field);
  }

  public void commit() throws DBMSUniqueSeqIdIndexException {
    try {
      _writer.commit();
      this.openReader();
    } catch (Exception e) {
      throw new DBMSUniqueSeqIdIndexException(ERR_MSG4 + e);
    }
  }

  public void addSeqID(String seqId) throws DBMSUniqueSeqIdIndexException {

    try {
      this._field.setValue(seqId);
      _writer.addDocument(this._document);
    } catch (Exception e) {
      throw new DBMSUniqueSeqIdIndexException(ERR_MSG3 + seqId + ": "
          + e.getMessage());
    }
  }

  public boolean contains(String id) throws DBMSUniqueSeqIdIndexException {
    TopDocs hits = null;
    boolean bFound = false;

    if (!isReaderOpen()) {
      LOGGER.error("No reader open !");
      return false;
    }

    if (this.nbDocInIndex > 0) {
      try {

        hits = _searcher.search(new TermQuery(this._term.createTerm(id)),
            LUCENE_SEARCH_HITS_MAX_NUMBER);
        bFound = (hits.totalHits != 0);
      } catch (Exception e) {
        throw new DBMSUniqueSeqIdIndexException("Unable to query index: " + e);
      }
    }
    return bFound;
  }

  /**
   * Closes a Lucene reader.
   */
  private boolean closeReader() {
    boolean bRet = false;

    if (_searcher == null) {
      LOGGER.warn("index (read mode) not closed : no searcher open");
      LOGGER.warn("writer is : " + (_writer == null ? "null" : "not null"));
      return bRet;
    }

    try {
      _searcher.close();
      bRet = true;
    } catch (Exception e) {
      LOGGER.warn("Unable to close index (read mode): " + e);
    }
    return bRet;
  }

  /**
   * Closes a Lucene writer.
   */
  private boolean closeWriter(boolean optimize) {
    boolean bRet = false;

    if (_writer == null) {
      LOGGER.warn("index (read mode) not closed : no writer open");
      LOGGER.warn("searcher is : " + (_searcher == null ? "null" : "not null"));
      return bRet;
    }

    try {
      _writer.commit();
      if (optimize) {
        // this is a very time consuming step: does it only when required
        _writer.optimize();
      }
      _writer.close();
      _writer = null;
      bRet = true;
    } catch (Exception e) {
      LOGGER.warn("Unable to close index writer: " + e);
    }
    return bRet;
  }

  /**
   * Opens a Lucene writer.
   */
  private boolean openWriter() {

    if (_writer != null) {
      return true;
    }

    boolean bRet = true;

    try {

      File dirIndex = new File(_indexName);
      FSDirectory index = LuceneUtils.getDirectory(dirIndex);
      // if (!(dirIndex.exists())) {
      // _writer = new IndexWriter(index, new
      // StandardAnalyzer(Version.LUCENE_29), true,
      // IndexWriter.MaxFieldLength.UNLIMITED);
      // _writer.close();
      // }
      //
      // _writer = new IndexWriter(new RAMDirectory(index), new
      // StandardAnalyzer(Version.LUCENE_29), false,
      // IndexWriter.MaxFieldLength.UNLIMITED);
      _writer = new IndexWriter(index, new StandardAnalyzer(Version.LUCENE_29),
          !(dirIndex.exists()), IndexWriter.MaxFieldLength.UNLIMITED);
    } catch (IOException e) {
      LOGGER.warn("Unable to open index (write mode): " + e);
      bRet = false;
    }
    return bRet;
  }

  boolean isWriterOpen() {
    return _writer != null;
  }

  boolean isReaderOpen() {
    return _searcher != null;
  }

  /**
   * Opens a Lucene reader.
   */
  private boolean openReader() {
    boolean bRet = true;
    boolean readOnly = true;

    // WARNING - potential poor multi-threading performance - check
    // SimpleSimpleFSDirectory
    try {
      if (_searcher != null) {
        _searcher.close();
      }
      _searcher = new IndexSearcher(LuceneUtils.getDirectory(new File(_indexName)),
          readOnly);
      this.nbDocInIndex = _searcher.maxDoc();
    } catch (IOException e) {
      LOGGER.warn("Unable to open index (read mode): " + e);
      // e.printStackTrace();
      bRet = false;
    }
    return bRet;
  }

  public boolean close() {
    return close(false);
  }

  public boolean close(boolean optimize) {
    boolean closeWriter = closeWriter(optimize);
    return closeReader() && closeWriter;
  }

  public boolean open(String name, boolean readOnly) {

    _indexName = name;

    if (readOnly || openWriter()) {
      return openReader();
    }
    return false;

  }
}