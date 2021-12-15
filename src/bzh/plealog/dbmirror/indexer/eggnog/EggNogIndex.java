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
package bzh.plealog.dbmirror.indexer.eggnog;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.store.LockObtainFailedException;

import bzh.plealog.dbmirror.fetcher.UserProcessingMonitor;
import bzh.plealog.dbmirror.task.PTaskEngine;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;
import bzh.plealog.dbmirror.util.lucene.DefaultSerializer;
import bzh.plealog.dbmirror.util.lucene.SimpleIndex;

/**
 * Index which stores the relations between an eggnog protein (sequence id) and
 * the associated nog with the positions.
 * 
 * An EggNogIndex contains multiple SimpleIndex to optimize the searches
 * 
 * @author Ludovic Antin
 * 
 */
public class EggNogIndex {

  private static final Logger      LOGGER  = LogManager.getLogger(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                + ".EggNogIndex");

  @SuppressWarnings("unchecked")
  private SimpleIndex<String>[] indexes = new SimpleIndex[99];

  public EggNogIndex(File mainDirectory) {

    // init internal indexes
    for (int i = 1; i <= 99; i++) {
      indexes[i - 1] = new SimpleIndex<String>(new File(mainDirectory, i
          + ".ldx").getAbsolutePath(), String.class, new DefaultSerializer());
    }
  }

  /**
   * 
   * @param id
   * 
   * @return the internal index which contains (or must contains) the parameter
   *         id
   */
  private SimpleIndex<String> getIndex(String id) {
    // the internal index is the one corresponding to the first id digit
    int internalIndexNumber = Integer.valueOf(id.substring(0, 2));
    return this.indexes[internalIndexNumber - 1];
  }

  /**
   * Add a new entry in the index
   * 
   * @param id
   * @param item
   * @throws IOException
   */
  public void add(String id, String item) throws IOException {
    this.getIndex(id).add(id, item);
  }

  /**
   * Open all internal indexes
   * 
   * @throws IOException
   * @throws LockObtainFailedException
   * @throws CorruptIndexException
   */
  public void open() throws CorruptIndexException, LockObtainFailedException,
      IOException {
    for (SimpleIndex<String> index : indexes) {
      index.open();
    }
  }

  /**
   * Close all internal indexes
   */
  public void close(UserProcessingMonitor monitor) {
    for (int i = 0; i < indexes.length; i++) {
      if (monitor != null) {
        monitor.processingFile(PTaskEngine.WORKER_ID, "",
            UserProcessingMonitor.PROCESS_TYPE.TASK_EXECUTION,
            "Closing indexes", i + 1, indexes.length);
      }
      LoggerCentral.info(LOGGER, "Closing index " + i + "/" + indexes.length);
      indexes[i].close();
    }
  }

  /**
   * @return the total size (all indexes size sum)
   */
  public int getSize() {
    int result = 0;
    for (SimpleIndex<String> index : indexes) {
      result += index.getSize();
    }
    return result;
  }

  /**
   * 
   * @param id
   * 
   * @return the index's size which contains this id
   */
  public int getSize(String id) {
    return this.getIndex(id).getSize();
  }

  /**
   * 
   * @param id
   * 
   * @return all the index entries which id is the parameter id
   */
  public List<String> getById(String id) {
    // a lot of sequences have just 10 domains, so get the 11 first ones by
    // default
    return this.getIndex(id).getById(id, 13);
  }
}
