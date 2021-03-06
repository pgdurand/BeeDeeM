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
package bzh.plealog.dbmirror.task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;

import bzh.plealog.dbmirror.util.ant.PAntTasks;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * A task capable of untaring a TAR file.
 * 
 * @author Patrick G. Durand
 */
public class PTaskUntarTgz extends PAbstractTask {

  private String           _src;
  private String           _dest;
  private String           _errMsg;

  private static final Log LOGGER = LogFactory
                                      .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                          + ".PTaskEngine");

  public PTaskUntarTgz(String srcFile, String destDir) {
    _src = srcFile;
    _dest = destDir;
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getName() {
    return "untar";
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getUserFriendlyName() {
    return "unarchiving file";
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
    if (_dest == null) {
      _errMsg = "destination directory is unknown";
      return false;
    }
    LoggerCentral.info(LOGGER, getName() + " " + _src + " to " + _dest);
    if (!PAntTasks.untartgz(_src, _dest)) {
      _errMsg = "unable to untar " + _src;
      return false;
    }
    return true;
  }

  public static void main(String[] args) {
    PTaskUntarTgz task;

    BasicConfigurator.configure();
    task = new PTaskUntarTgz(args[0], args[1]);
    if (!task.execute()) {
      LoggerCentral.error(LOGGER, "Unable to untartgz");
    }
  }

  public void setParameters(String params) {
  }

}
