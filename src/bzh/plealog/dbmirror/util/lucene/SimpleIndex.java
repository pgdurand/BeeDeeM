/* Copyright (C) 2007-2021 Ludovic Antin
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
package bzh.plealog.dbmirror.util.lucene;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;

import bzh.plealog.dbmirror.indexer.LuceneUtils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;

/**
 * Storage used to store and retrieve objects on from disk.
 * 
 * @author Ludovic Antin
 * 
 */
public class SimpleIndex<T> {

  private static final Logger LOGGER = LogManager.getLogger(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY + ".SimpleIndex");
  private static final String DATA_FIELD_NAME = "data";
  private static final String ID_FIELD_NAME   = "id";

  // where is located this storage ?
  private File                directory;

  // T class for the serializer
  private Class<T>            type;

  private StandardAnalyzer    analyzer        = null;
  private IObjectSerializer   serializer      = null;

  private IndexWriter         writer          = null;
  private IndexSearcher       searcher        = null;

  private Document            document        = null;
  private Field               fieldId         = null;
  private Field               fieldData       = null;

  // indicates if the type T is an object => implies serialization and byte[]
  // storage
  private boolean             objectStorage   = false;

  private int                 nbAdded         = 0;
  private int                 limitCommit     = 500000;
  private int                 size            = 0;

  // to search by id
  private Term                termId          = null;

  public SimpleIndex(String directoryPath, Class<T> type,
      IObjectSerializer serializer) {
    this.directory = new File(directoryPath);
    this.type = type;
    this.analyzer = new StandardAnalyzer(Version.LUCENE_29);
    this.serializer = serializer;
    if (type.isPrimitive() || type.getName().equals("java.lang.String")) {
      this.fieldData = new Field(DATA_FIELD_NAME, "", Field.Store.YES,
          Field.Index.NOT_ANALYZED);
    } else {
      this.objectStorage = true;
      // store an object
      this.fieldData = new Field(DATA_FIELD_NAME, new byte[0], Field.Store.YES);
    }
    this.fieldId = new Field(ID_FIELD_NAME, "", Field.Store.YES,
        Field.Index.NOT_ANALYZED);
    this.termId = new Term(ID_FIELD_NAME);
    this.document = new Document();
    this.document.add(this.fieldData);
    this.document.add(this.fieldId);

  }

  public File getDirectory() {
    return directory;
  }

  public void open() throws CorruptIndexException, LockObtainFailedException,
      IOException {
    if (this.writer == null) {
      /*Use a SimpleFSLockFactory to solve pb when using Lustre FS*/
    	this.writer = new IndexWriter(LuceneUtils.getDirectory(this.directory),
          this.analyzer, !this.directory.exists(), MaxFieldLength.UNLIMITED);
    }
  }

  public void close() {
    try {
      this.commit();
      if (this.writer != null) {
        this.writer.optimize();
        this.writer.close();
        this.writer = null;
      }
    } catch (Exception e) {

    }
  }

  public void commit() throws CorruptIndexException, IOException {
    if (this.writer != null) {
      this.writer.commit();
    }
    if (this.searcher != null) {
      this.searcher.close();
      this.searcher = null;
    }
    nbAdded = 0;
  }

  public Iterator<T> getAll() throws CorruptIndexException, IOException {
    if (this.writer != null) {
      this.writer.commit();
    }

    return new Iterator<T>() {
      IndexSearcher index         = null;
      boolean       isInitialized = false;
      int           totalObjects  = 0;
      int           curIdx        = 0;

      private void initialize() {
        try {
          this.index = new IndexSearcher(LuceneUtils.getDirectory(directory), true);
          totalObjects = this.index.maxDoc();
        } catch (IOException ex) {
          LOGGER.warn("Unable to read max doc for storage '" + getDirectory()
              + "' : " + ex.getMessage());
        }
        curIdx = 0;

        isInitialized = true;
      }

      @Override
      public boolean hasNext() {
        if (!isInitialized) {
          initialize();
        }

        if (curIdx < totalObjects) {
          return true;
        } else {
          try {
            this.index.close();
          } catch (Exception e) {
            LOGGER.warn("Unable to close index reader from storage '"
                + getDirectory() + "' : " + e.getMessage());
          }
          return false;
        }
      }

      @SuppressWarnings("unchecked")
      @Override
      public T next() {
        try {
          if (objectStorage) {
            return serializer.readObject(
                this.index.doc(curIdx).getBinaryValue(DATA_FIELD_NAME), type);
          } else {
            return (T) this.index.doc(curIdx).get(DATA_FIELD_NAME);
          }
        } catch (Exception e) {
          LOGGER.warn("Unable to load object from storage '" + getDirectory()
              + "' at index " + curIdx);
          return null;
        } finally {
          curIdx++;
        }
      }

      @Override
      public void remove() {
      }
    };

  }

  public int add(String id, T item) throws IOException {

    String itemId = id;
    if (StringUtils.isBlank(itemId)) {
      itemId = String.valueOf(size);
    }

    // get or set the id from/to the item to add
    if (item instanceof SimpleIndexEntry) {
      String objectId = ((SimpleIndexEntry) item).getId();
      if (objectId == null) {
        ((SimpleIndexEntry) item).setId(itemId);
      } else {
        itemId = objectId;
      }
    }

    if (this.objectStorage) {
      try {
        this.fieldData.setValue(this.serializer.writeObject(item,
            item.getClass()));
      } catch (Exception e) {
        LOGGER.warn(e.getMessage(), e);
      }
    } else {
      this.fieldData.setValue(item.toString());
    }
    this.fieldId.setValue(itemId);
    this.writer.addDocument(this.document);

    nbAdded++;
    size++;
    if (nbAdded == limitCommit) {
      this.commit();
    }
    // substract 1 because of the previous ++
    return size - 1;
  }

  public IndexSearcher getSearcher() {
    if (this.searcher == null) {
      try {
        this.searcher = new IndexSearcher(LuceneUtils.getDirectory(directory), true);
      } catch (Exception e) {
        LOGGER.warn("Unable to create searcher for storage '" + getDirectory()
            + "' : " + e.getMessage());
      }
    }
    return this.searcher;
  }

  public int getSize() {
    if (this.size == 0) {
      if (this.getSearcher() != null) {
        try {
          this.size = this.getSearcher().maxDoc();
        } catch (IOException e) {
          LOGGER.warn("Unable to get size for storage '" + getDirectory()
              + "' : " + e.getMessage());
        }
      }
    }
    return this.size;
  }

  public T get(int i) {
    if (this.getSearcher() != null) {
      try {
        return serializer.readObject(
            this.getSearcher().doc(i).getBinaryValue(DATA_FIELD_NAME), type);
      } catch (Exception e) {
        LOGGER.warn("Unable to load object from storage '" + getDirectory()
            + "' at index " + i + " : " + e.getMessage());
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public List<T> getById(String id, int defaultResultSize) {
    List<T> result = new ArrayList<T>();
    if (this.getSearcher() != null) {
      Query query = null;
      TopDocs docs = null;
      Document doc = null;
      byte data[] = null;

      try {
        query = new TermQuery(termId.createTerm(id));
        docs = this.searcher.search(query, defaultResultSize);
        if (docs != null) {
          int nbHits = docs.totalHits;
          while (nbHits == defaultResultSize) {
            defaultResultSize = defaultResultSize * 2;
            docs = this.searcher.search(query, defaultResultSize);
            nbHits = docs.totalHits;
          }
          for (int i = 0; i < nbHits; i++) {
            doc = this.searcher.doc(docs.scoreDocs[i].doc);
            if (this.objectStorage) {
              data = doc.getBinaryValue(DATA_FIELD_NAME);
              result.add((T) this.serializer.readObject(data, this.type));
            } else {
              result.add((T) doc.get(DATA_FIELD_NAME));
            }
          }
        }
      } catch (Exception e) {
        LOGGER.warn("Unable to load objects for id '" + id + "' from class '"
            + this.type.getName() + "' : " + e.getMessage());
        return result;
      }
    }
    return result;
  }
}
