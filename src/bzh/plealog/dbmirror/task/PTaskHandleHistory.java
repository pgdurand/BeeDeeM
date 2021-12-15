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
import java.util.ArrayList;
import java.util.Collections;

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
public class PTaskHandleHistory extends PAbstractTask {

  private String           _dir;
  private int              _history;
  private String           _errMsg;

  private static final Logger LOGGER = LogManager.getLogger(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                          + ".PTaskEngine");

  public PTaskHandleHistory(String dir, int history) {
    _dir = dir;
    _history = history;
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getName() {
    return "deldir";
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getUserFriendlyName() {
    return "deleting old databank installation";
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getErrorMsg() {
    return _errMsg;
  }

  private boolean handleHistory() {
    File[] files;
    File f;
    String dirName;
    ArrayList<String> dirNames;
    int i, keep;

    files = new File(_dir).listFiles();
    dirNames = new ArrayList<String>();
    for (i = 0; i < files.length; i++) {
      f = files[i];
      if (f.isFile())
        continue;
      dirName = f.getName();
      if (dirName.startsWith("currentOn") == false)
        continue;
      dirNames.add(dirName);
    }
    if (dirNames.isEmpty())
      return true;
    if (dirNames.size() < _history)
      return true;

    Collections.sort(dirNames);
    keep = dirNames.size() - _history;

    for (i = 0; i < keep; i++) {
      dirName = _dir + dirNames.get(i);
      if (!PAntTasks.deleteDirectory(dirName)) {
        _errMsg = "unable to delete directory";
        return false;
      }
    }
    return true;
  }

  /**
   * Implementation of KLTask interface.
   */
  public boolean execute() {
    if (_dir == null) {
      _errMsg = "directory is unknown";
      return false;
    }

    LoggerCentral.info(LOGGER, getName() + ": " + _dir);
    if (LoggerCentral.errorMsgEmitted()) {
      _errMsg = "unable to delete directory: warn messages emitted.";
      return false;
    } else if (LoggerCentral.processAborted()) {
      _errMsg = "unable to delete directory: process aborted.";
      return false;
    }

    return handleHistory();
  }

  public void setParameters(String params) {
  }

}
