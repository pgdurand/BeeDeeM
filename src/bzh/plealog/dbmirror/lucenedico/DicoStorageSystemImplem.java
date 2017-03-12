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
package bzh.plealog.dbmirror.lucenedico;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import bzh.plealog.dbmirror.lucenedico.go.Serialization_GeneOntologyTerm;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * This class represents a Lucene-based index aims at storing required data to
 * retrieve specific terms from dictionary files.
 * 
 * @author Patrick G. Durand
 */
public class DicoStorageSystemImplem implements DicoStorageSystem,
    Comparable<Object> {
  protected IndexWriter         _writer;
  protected IndexSearcher       _searcher;
  protected FSDirectory         _readerFsDir;
  protected int                 _mode;
  protected boolean             _writerOk                 = false;
  protected boolean             _readerOk                 = false;
  protected String              _indexName;

  protected static final String ID_FIELD                  = "id";
  protected static final String DATA_FIELD                = "name";
  protected static final String DATA_FIELD_NOT_ANALYZED   = "name_na";
  protected static final String OBJECT_FIELD              = "object";

  private static final Log      LOGGER                    = LogFactory
                                                              .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                                  + ".DicoStorageSystemImplem");

  private static final String   ERR_MSG1                  = "While adding term in index: ";

  private Document              documentObject            = null;
  private Field                 fieldObjectId             = null;
  private Field                 fieldObjectValue          = null;

  private Document              documentData              = null;
  private Field                 fieldDataId               = null;
  private Field                 fieldDataValue            = null;
  // it is 20 times faster to search an exact term or phrase in a not analyzed
  // field than search the same term in a analyzed field using QueryParser or
  // PhraseQuery
  private Field                 fieldDataNotAnalyzedValue = null;

  private StandardAnalyzer      analyzer                  = null;

  // used to search in lucene indexes
  private Term                  termId                    = null;
  @SuppressWarnings("unused")
  private Term                  termName                  = null;
  private Term                  termNameNotAnalyzed       = null;

  public DicoStorageSystemImplem() {
    this.analyzer = new StandardAnalyzer(Version.LUCENE_29);

    this.fieldObjectId = new Field(ID_FIELD, "", Field.Store.YES,
        Field.Index.NOT_ANALYZED);
    this.fieldObjectValue = new Field(OBJECT_FIELD, new byte[0],
        Field.Store.YES);
    this.documentObject = new Document();
    this.documentObject.add(this.fieldObjectId);
    this.documentObject.add(this.fieldObjectValue);

    this.fieldDataId = new Field(ID_FIELD, "", Field.Store.YES,
        Field.Index.NOT_ANALYZED);
    this.fieldDataValue = new Field(DATA_FIELD, "", Field.Store.YES,
        Field.Index.ANALYZED);
    this.fieldDataNotAnalyzedValue = new Field(DATA_FIELD_NOT_ANALYZED, "",
        Field.Store.YES, Field.Index.NOT_ANALYZED);
    this.documentData = new Document();
    this.documentData.add(this.fieldDataId);
    this.documentData.add(this.fieldDataValue);
    this.documentData.add(this.fieldDataNotAnalyzedValue);

    this.termId = new Term(ID_FIELD);
    this.termName = new Term(DATA_FIELD);
    this.termNameNotAnalyzed = new Term(DATA_FIELD_NOT_ANALYZED);

  }

  private void checkRamUsage() throws IOException {
    long totMem, freeMem;

    totMem = Runtime.getRuntime().totalMemory();
    freeMem = Runtime.getRuntime().freeMemory();
    if (freeMem < (10 * totMem / 100)) {
      _writer.commit();
      System.gc();
    }
  }

  /**
   * Implementation of DicoStorageSystem interface.
   */
  public void addEntry(String id, String dataField)
      throws DicoStorageSystemException {

    if (_writerOk == false)
      throw new DicoStorageSystemException("DicoStorageSystem not initialised.");
    try {
      this.fieldDataId.setValue(id);
      this.fieldDataValue.setValue(dataField);
      this.fieldDataNotAnalyzedValue.setValue(dataField);
      checkRamUsage();
      _writer.addDocument(this.documentData);
    } catch (Exception e) {
      throw new DicoStorageSystemException(ERR_MSG1 + e);
    }
  }

  /**
   * Implementation of DicoStorageSystem interface.
   */
  public void addBinaryEntry(String id, Object dataField)
      throws DicoStorageSystemException {

    if (_writerOk == false)
      throw new DicoStorageSystemException("DicoStorageSystem not initialised.");
    try {
      byte[] data = Serialization_GeneOntologyTerm.serialize(dataField);
      this.fieldObjectId.setValue(id);
      this.fieldObjectValue.setValue(data);
      checkRamUsage();
      _writer.addDocument(this.documentObject);
    } catch (Exception e) {
      throw new DicoStorageSystemException(ERR_MSG1 + e);
    }
  }

  /**
   * Implementation of DicoStorageSystem interface.
   */
  public DicoTerm getTerm(String id) throws DicoStorageSystemException {
    DicoTerm term = null;
    Document doc;
    Query query = null;
    TopDocs docs = null;
    byte data[] = null;

    if (_readerOk == false)
      throw new DicoStorageSystemException("DicoStorageSystem not initialised.");

    try {
      query = new TermQuery(termId.createTerm(id));
      docs = _searcher.search(query, 1);
      if (docs.totalHits > 0) {
        doc = _searcher.doc(docs.scoreDocs[0].doc);
        data = doc.getBinaryValue(OBJECT_FIELD);

        if (data != null) {
          term = new DicoTerm(id, doc.get(DATA_FIELD),
              Serialization_GeneOntologyTerm.deserialize(data));
        } else {
          term = new DicoTerm(id, doc.get(DATA_FIELD));
        }
      } else {
        return null;
      }

    } catch (Exception e) {
      throw new DicoStorageSystemException("Unable to query index: " + e);
    }
    return term;
  }

  /**
   * Implementation of DicoStorageSystem interface.
   */
  public String getID(String name) throws DicoStorageSystemException {
    Document doc;
    Query query = null;
    TopDocs docs = null;

    if (_readerOk == false)
      throw new DicoStorageSystemException("DicoStorageSystem not initialised.");

    try {
      query = new TermQuery(termNameNotAnalyzed.createTerm(name));

      docs = _searcher.search(query, 1);
      if (docs.totalHits > 0) {
        doc = _searcher.doc(docs.scoreDocs[0].doc);
        return doc.get(ID_FIELD);
      }
      return null;
    } catch (Exception e) {
      throw new DicoStorageSystemException("Unable to query index: " + e);
    }
  }

  /**
   * Implementation of DicoStorageSystem interface.
   */
  public DicoTerm[] getTerms(String[] ids) throws DicoStorageSystemException {
    DicoTerm[] terms = null;
    int i = 0;

    if (_readerOk == false)
      throw new DicoStorageSystemException("DicoStorageSystem not initialised.");

    try {
      terms = new DicoTerm[ids.length];
      for (String id : ids) {
        terms[i] = this.getTerm(id);
        i++;
      }
    } catch (Exception e) {
      throw new DicoStorageSystemException("Unable to query index: " + e);
    }
    return terms;
  }

  private void findTerm(ArrayList<DicoTerm> path, String idFrom, String idTo)
      throws Exception {
    String data_field, fatherID;
    Integer idx1;
    DicoTerm found = this.getTerm(idFrom);
    if (found != null) {
      path.add(found);
      if (!idTo.equals(idFrom)) {
        data_field = found.getDataField();

        // if data_field contains a pipe, the first data is the father id and
        // the next is other data (depending to classification)
        if (data_field.contains("|")) {
          idx1 = data_field.indexOf('|');

          fatherID = data_field.substring(0, idx1 - 1).trim();
        } else {
          fatherID = data_field.trim();
        }

        findTerm(path, fatherID, idTo);
      }
    } else {
      throw new DicoStorageSystemException("Term not found : " + idFrom);
    }

  }

  /**
   * Implementation of DicoStorageSystem interface.
   */
  public DicoTerm[] getTerms(String idFrom, String idTo)
      throws DicoStorageSystemException {
    ArrayList<DicoTerm> path;
    DicoTerm[] terms = null;

    if (_readerOk == false)
      throw new DicoStorageSystemException("DicoStorageSystem not initialised.");

    try {
      path = new ArrayList<DicoTerm>();
      findTerm(path, idFrom, idTo);
      if (path.isEmpty() == false) {
        terms = new DicoTerm[path.size()];
        int j = 0;
        for (int i = path.size() - 1; i >= 0; i--) {
          terms[j] = path.get(i);
          j++;
        }
      }
    } catch (DicoStorageSystemException e) {
      terms = null;
    } catch (Exception e) {
      throw new DicoStorageSystemException("Unable to query index: " + e);
    }
    return terms;
  }

  /**
   * Closes a Lucene writer.
   */
  protected boolean closeWriter() {
    boolean bRet = true;
    if (_writerOk == false)
      return true;
    try {
      _writer.commit();
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
      _writer = new IndexWriter(FSDirectory.open(new File(_indexName)),
          this.analyzer, true, MaxFieldLength.UNLIMITED);
      _writerOk = true;

    } catch (IOException e) {
      LoggerCentral.error(LOGGER, "Unable to open index (write mode): "
          + _indexName + ": " + e);
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
      if (_readerFsDir != null) {
        _readerFsDir.close();
      }
      _searcher.close();
      _readerFsDir = null;
      _searcher = null;
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
      _readerFsDir = FSDirectory.open(new File(_indexName));
      _searcher = new IndexSearcher(_readerFsDir, true);
      _readerOk = true;
    } catch (IOException e) {
      LoggerCentral.error(LOGGER, "Unable to open index (read mode): "
          + _indexName + ": " + e);
      bRet = false;
    }
    return bRet;
  }

  /**
   * Implementation of DicoStorageSystem interface.
   */
  public boolean close() {
    boolean bRet = true;

    switch (_mode) {
      case DicoStorageSystem.WRITE_MODE:
        bRet = closeWriter();
        break;
      case DicoStorageSystem.READ_MODE:
        bRet = closeReader();
        break;
    }
    _readerOk = false;
    return bRet;
  }

  /**
   * Implementation of DicoStorageSystem interface.
   */
  public boolean open(String name, int mode, boolean loadInRAM) {
    boolean bRet = true;

    _mode = mode;
    _indexName = name;
    switch (mode) {
      case DicoStorageSystem.WRITE_MODE:
        bRet = openWriter();
        break;
      case DicoStorageSystem.READ_MODE:
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

  @Override
  public int compareTo(Object o) {
    if (o instanceof DicoStorageSystemImplem) {
      return this._indexName.compareToIgnoreCase(((DicoStorageSystemImplem) o)
          .getIndexName());
    } else {
      return this._indexName.compareToIgnoreCase(o.toString());
    }
  }

  public String getIndexName() {
    return this._indexName;
  }

  @SuppressWarnings("deprecation")
  @Override
  public List<DicoTerm> getApprochingTerms(String term)
      throws DicoStorageSystemException {
    List<DicoTerm> result = new ArrayList<DicoTerm>();

    if (StringUtils.isBlank(term)) {
      return result;
    }

    // remove lucene special chars but keep "*"
    term = removeSpecialChars(term, "+", "-", "&&", "||", "!", "(", ")", "{",
        "}", "[", "]", "^", "\"", "~", "?", ":", "\\");
    // except if "*" is the first char
    if (term.startsWith("*")) {
      term = term.substring(1);
    }

    // add ~ to use approximate search
    // (http://lucene.apache.org/core/2_9_4/queryparsersyntax.html)
    term += " OR " + term.replace("*", "") + "~";

    QueryParser parser = new QueryParser(/* Version.LUCENE_29, */DATA_FIELD,
        this.analyzer);
    try {
      Query query = parser.parse(term);
      Document doc;
      String id;
      // only the 100 first results
      ScoreDoc[] hits = _searcher.search(query, null, 10000).scoreDocs;
      if (!ArrayUtils.isEmpty(hits)) {
        for (int i = 0; i < hits.length; i++) {
          doc = _searcher.doc(hits[i].doc);
          id = doc.get(ID_FIELD);
          if (id.startsWith(DicoUtils.TAX_ID_NAME_PREFIX)) {
            result.add(new DicoTerm(id
                .replace(DicoUtils.TAX_ID_NAME_PREFIX, ""),
                doc.get(DATA_FIELD), hits[i].score));
          }
        }
      }
    } catch (ParseException e) {
      throw new DicoStorageSystemException("Unable to parse query '" + term
          + "' : " + e.getMessage());
    } catch (IOException e) {
      throw new DicoStorageSystemException("Unable to search term '" + term
          + "' : " + e.getMessage());
    }
    return result;
  }

  /**
   * Removes special chars from the input string
   * 
   * @param input
   * @return the input cleaned
   */
  private String removeSpecialChars(String input, String... specialChars) {

    if (!ArrayUtils.isEmpty(specialChars)) {
      for (String c : specialChars) {
        input = input.replace(c, "");
      }
    }

    return input.trim();
  }
}