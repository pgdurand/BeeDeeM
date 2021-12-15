/* Copyright (C) 2007-2021 Patrick G. Durand
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
package bzh.plealog.dbmirror.task;

import java.io.File;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import bzh.plealog.dbmirror.fetcher.UserProcessingMonitor;
import bzh.plealog.dbmirror.indexer.LuceneUtils;
import bzh.plealog.dbmirror.util.ant.PAntTasks;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * A task capable of merging alltogether several Lucene index files.
 * 
 * @author Patrick G. Durand
 */
public class PTaskLuceneDirMerge extends PAbstractTask {

  private List<String>          _indexPaths;
  private String                _indexName;
  private String                _errMsg;
  private UserProcessingMonitor _monitor;

  private static final Logger      LOGGER = LogManager.getLogger(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                               + ".PTaskEngine");

  public PTaskLuceneDirMerge(String mainIndex, List<String> indexPaths) {
    _indexName = mainIndex;
    _indexPaths = indexPaths;
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getName() {
    return "LuceneDirMerge";
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getUserFriendlyName() {
    return "merging data index";
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getErrorMsg() {
    return _errMsg;
  }

  /**
   * Implementation of KLTask interface.
   */
  public boolean execute() {
    String idxName;

    if (_indexName == null) {
      _errMsg = "index name is unknown";
      return false;
    }

    LoggerCentral.info(LOGGER, getName() + " started for " + _indexName);

    // check whether index already exists
    idxName = _indexName + LuceneUtils.IDX_OK_FEXT;
    if (new File(idxName).exists()) {
      // index exists and not empty
      if (LuceneUtils.getSize(idxName) != 0) {
        LoggerCentral.info(LOGGER, getName() + ": " + idxName
            + ": index skipped: arleady exists");
        return true;
      }
      // empty : consider invalid and retry to create
      PAntTasks.deleteDirectory(idxName);
    }
    try {
      // checker whether tmp index exists
      idxName = _indexName + LuceneUtils.IDX_TMP_FEXT;
      if (new File(idxName).exists()) {
        if (!PAntTasks.deleteDirectory(idxName)) {
          throw new Exception("unable to delete old index: " + idxName);
        }
      }
      // merge index
      if (!LuceneUtils.mergeIndex(idxName, _indexPaths, this._monitor)) {
        throw new Exception("unable to merge all indexes within: " + idxName);
      }
      // rename index to final name
      if (!PAntTasks.movefile(idxName, _indexName + LuceneUtils.IDX_OK_FEXT)) {
        throw new Exception("unable to rename final index: " + idxName);
      }
    } catch (Exception ex) {
      _errMsg = " index merging of " + _indexName + " failed: " + ex;
      return false;
    }
    return true;
  }

  public void setParameters(String params) {
  }

  public void setUserProcessingMonitor(UserProcessingMonitor monitor) {
    this._monitor = monitor;

  }

}
