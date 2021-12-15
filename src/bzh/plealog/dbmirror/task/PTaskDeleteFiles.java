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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import bzh.plealog.dbmirror.util.ant.PAntTasks;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * A task capable of removing some files from the file system.
 * 
 * @author Patrick G. Durand
 */
public class PTaskDeleteFiles extends PAbstractTask {

  private String           _dir;
  private String           _fileFilter;
  private String           _errMsg;
  private boolean          _forceDelete;

  private static final Logger LOGGER = LogManager.getLogger(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                          + ".PTaskEngine");

  public PTaskDeleteFiles(String dir, String fileFilter) {
    _dir = dir;
    _fileFilter = fileFilter;
    _forceDelete = false;
  }

  public PTaskDeleteFiles(String dir, String fileFilter, boolean forceDelete) {
    this(dir, fileFilter);
    _forceDelete = forceDelete;
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getName() {
    return "delfiles";
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getUserFriendlyName() {
    return "deleting temporary files";
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
    if (_dir == null) {
      _errMsg = "directory is unknown";
      return false;
    }
    if (_fileFilter == null) {
      _errMsg = "file filter is unknown";
      return false;
    }
    LoggerCentral
        .info(LOGGER, getName() + ": " + _fileFilter + " from " + _dir);
    if (!_forceDelete
        && (LoggerCentral.errorMsgEmitted() || LoggerCentral.processAborted())) {
      _errMsg = "unable to delete files: warn messages emitted.";
      return false;
    }
    if (!PAntTasks.deleteFiles(_dir, _fileFilter)) {
      _errMsg = "unable to delete files";
      return false;
    }
    return true;
  }

  public void setParameters(String params) {
  }

}
