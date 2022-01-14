/* Copyright (C) 2007-2022 Patrick G. Durand
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * A task capable of extracting the contant of a GZipped file.
 * 
 * @author Patrick G. Durand
 */
public class PTaskGunzip extends PAbstractTask {

  private String           _src;
  private String           _dest;
  private String           _errMsg;
  private boolean          _deleteOld = false;
  private static final Log LOGGER     = LogFactory
                                          .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                              + ".PTaskEngine");

  public PTaskGunzip(String srcFile, String destDir) {
    _src = srcFile;
    _dest = destDir;
  }

  public PTaskGunzip(String srcFile, String destDir, boolean forceDelOld) {
    _src = srcFile;
    _dest = destDir;
    _deleteOld = forceDelOld;
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getName() {
    return "gunzip";
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getUserFriendlyName() {
    return "uncompressing file";
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
    if (_src == null) {
      _errMsg = "source file is unknown";
      return false;
    }
    if (PAbstractTask.testTaskOkForFileExists(_src)) {
      LoggerCentral.info(LOGGER, "skip task: " + _src + ": already gunzipped");
      return true;
    }
    // we suppose here that a gzipped file already has the extension .gz
    // sizing 3 characters...
    File f = new File(_src.substring(0, _src.length() - 3));
    if (f.exists()) {
      if (!_deleteOld) {
        LoggerCentral.info(LOGGER, getName() + ": skip " + f.getAbsolutePath()
            + ": already exists.");
        return true;
      }
      f.delete();
    }
    if (_dest == null) {
      _errMsg = "destination directory is unknown";
      return false;
    }
    LoggerCentral.info(LOGGER, getName() + " " + _src + " to " + _dest);

    if (Utils.gunzipFile(_src) == null) {
      _errMsg = "unable to gunzip " + _src;
      return false;
    }
    PAbstractTask.setTaskOkForFile(_src);
    return true;
  }

  public static void main(String[] args) {
    PTaskGunzip task;

    LoggerCentral.configure();
    task = new PTaskGunzip(args[0], args[1], true);
    if (!task.execute()) {
      LoggerCentral.error(LOGGER, "Unable to gunzip");
    }
  }

  public void setParameters(String params) {
  }

}
