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

import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * This class is used to detect sequence redundancy. It is very important to
 * note that this checking is only done on sequence IDs, as they appear in
 * sequence files.
 * 
 * @author Patrick Durand
 */
public class DBMSUniqueSeqIdDetector {

  // use a more optimal implementation of HashSet than the Java standard API
  // see http://java-performance.info/primitive-types-collections-trove-library/
  // and trove4j.sourceforge.net

  // notice: this class uses a Light Edition of the Trove Library made by
  // Plealog.
  // see //olorin/common/libraries/Trove-3.0.3/le

  /*
   * This class works as follows. A HashSet is used to store at most set of
   * MAX_SEQID seqquence IDs within RAM. The hashset is used to check whether or
   * not a seqID has been seen. If not, it is added to the HasSet. Each time a
   * set of MAX_SEQID seqquence IDs is reached, the content of the set is
   * appended to a Lucene index. seqID check is also done against the Lucene
   * index. This system enables to save chunks of seqIds within a Lucene index,
   * instead of writing them one at a time.
   */

  // private TCustomHashSet<String> _seqIdsFilter; // the HashSet
  private HashSet<String>      _seqIdsFilter;
  private DBMSUniqueSeqIdIndex _index;
  private String               _indexPath;
  private boolean              _indexOk;
  private long                 _workingTime;
  private boolean              _used             = false;

  // public and not final for tests
  public static int            MAX_SEQID         = 5000000;
  private static final Object  SEM               = new Object();
  private static final Log     LOGGER            = LogFactory
                                                     .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                         + ".PDMSUniqueSeqIdDetector");
  public static int            CHECK_MEMORY_STEP = 10;

  private DBMSUniqueSeqIdDetector() {
    // we ensure that no rehash will ever occur ; see Java Hashmap definition of
    // load factor
    // _seqIdsFilter = new TCustomHashSet<String>(new myStringHashingStrategy(),
    // (int) ((float) MAX_SEQID / LFACTOR) + 1, LFACTOR);
    _seqIdsFilter = new HashSet<String>();
    _workingTime = 0l;
    _indexOk = false;
  }

  /**
   * Return the absolute path to the Lucence index used by this class to
   * permanently store seIds.
   */
  public String getIndexPath() {
    return _indexPath;
  }

  /**
   * Constructor.
   * 
   * @param indexPath
   *          the absolute path to the Lucene index that will be filled in by
   *          this class with unique sequence IDs.
   */
  public DBMSUniqueSeqIdDetector(String indexPath) {
    this();

    _indexPath = indexPath;

    // needed to use WRITE_APPEND_MODE elsewhere in this code
    _index = new DBMSUniqueSeqIdIndex();
    LoggerCentral.info(LOGGER, "Create UniqueSeqId index: " + indexPath);

    // verifier si _indexPath existe et si oui, virer le "write.lock" (si
    // reprise d'installation)
    _indexOk = _index.open(_indexPath, false);
    if (!_indexOk) {
      LoggerCentral.error(LOGGER, "failed !");
      return;
    }
  }

  /**
   * Reset the set of seqIds contains in this object.
   */
  public void reset() {
    synchronized (SEM) {
      _seqIdsFilter.clear();
    }
  }

  /**
   * Return the working time.
   */
  public long getWorkingTime() {
    return _workingTime;
  }

  /**
   * Returns true if this class has been used, i.e. method add has been called
   * at least one times.
   */
  public boolean hasBeenUsed() {
    return _used;
  }

  /**
   * Close the Lucene index. Can be called to ensure there is no open files.
   */
  public void closeIndex() throws DBMSUniqueSeqIdIndexException {
    synchronized (SEM) {
      if (_indexOk) {
        _indexOk = _index.close();
        if (!_indexOk) {
          throw new DBMSUniqueSeqIdIndexException(
              "Failed to close index (closeIndex)");
        }
      }
      _seqIdsFilter.clear();
      System.gc();
    }
  }

  /**
   * Reopen the Lucene index .
   */
  public void openIndex() throws DBMSUniqueSeqIdIndexException {
    synchronized (SEM) {
      if (_indexOk) {
        _indexOk = _index.open(_indexPath, false);
        if (!_indexOk) {
          throw new DBMSUniqueSeqIdIndexException(
              "Failed to re-open index read mode (openIndexReadMode)");
        }
      }
    }
  }

  /**
   * Adds a new seqId only if it is not already contained in this object.
   * 
   * @param seqId
   *          the new sequence ID to add
   * 
   * @return false if the seqId was not added because it is already known, true
   *         otherwise. So, when this method returns false it means that seqId
   *         is redundant.
   */
  public boolean add(String seqId) {
    synchronized (SEM) {
      if (!_used) {
        _used = true;
      }

      long time = System.currentTimeMillis();

      // this is to secure the HashSet size in case index is not available
      // it means that redundancy will only be tested on the first MAX_SEQID
      // from a data set
      if (_seqIdsFilter.size() <= MAX_SEQID) {
        if (!_seqIdsFilter.add(seqId)) {
          _workingTime += (System.currentTimeMillis() - time);
          return false;
        }

        // check memory 10 times before a dump
        int checkStep = (MAX_SEQID / CHECK_MEMORY_STEP);
        if (checkStep != 0) {
          if (_seqIdsFilter.size() % checkStep == 0) {
            if (Runtime.getRuntime().freeMemory() < (10 * Runtime.getRuntime()
                .totalMemory() / 100)) {
              this.dumpSeqIdsInLuceneIndex();
            }
          }
        }
      }
      boolean result = true;
      if (_indexOk && _index.contains(seqId)) {// add search time; may take some
                                               // time (Lucene)
        _workingTime += (System.currentTimeMillis() - time);
        result = false;
      }
      if (_indexOk && _seqIdsFilter.size() > MAX_SEQID) {
        dumpSeqIdsInLuceneIndex();
      }
      // compute time only when everything is working fine
      _workingTime += (System.currentTimeMillis() - time);

      return result;
    }
  }

  private void dumpSeqIdsInLuceneIndex() {
    long time = System.currentTimeMillis();
    try {
      dumpSeqIds();
    } catch (DBMSUniqueSeqIdIndexException e) {
      LoggerCentral.error(
          LOGGER,
          "Failed to dump sequence IDs within Lucene storage : "
              + e.getMessage());
      _indexOk = false;
    }
    // in case of error with Lucene storage, use the available seqIds in memory
    if (_indexOk) {
      _seqIdsFilter.clear();
    }
    // compute time only when everything is working fine
    _workingTime += (System.currentTimeMillis() - time);
  }

  /**
   * Forces this class to dump the internal set of seqIds within a Lucene index.
   * This method should be used by the caller to ensure that everything is
   * written in the index, since this class cannot ensure that.
   */
  public void dumpContent() {
    synchronized (SEM) {
      dumpSeqIdsInLuceneIndex();
    }
  }

  /**
   * Utility method that dump the internal set of seqIds within a Lucene index.
   */
  private void dumpSeqIds() throws DBMSUniqueSeqIdIndexException {
    if (_seqIdsFilter.size() == 0)
      return;

    // add all seqIds contained in the HashSet
    for (String seqId : _seqIdsFilter) {
      _index.addSeqID(seqId);// throws exception if problem
    }
    // commit added data at once
    _index.commit();// throws exception if problem

  }

}
