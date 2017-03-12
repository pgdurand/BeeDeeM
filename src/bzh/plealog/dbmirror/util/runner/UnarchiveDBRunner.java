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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.task.PTask;
import bzh.plealog.dbmirror.task.PTaskGunzip;
import bzh.plealog.dbmirror.task.PTaskUntar;
import bzh.plealog.dbmirror.task.PTaskUnzip;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

public class UnarchiveDBRunner extends Thread {
  private UnarchiveDBMonitor _monitor;
  private List<String>       _files;
  private ArrayList<String>  _processMsg;
  private String             _targetDir;
  private boolean            _success = false;
  private LinkedList<PTask> _tasks;

  public static final String GZ_FEXT  = "gz";
  public static final String TAR_FEXT = "tar";
  public static final String TGZ_FEXT = "tgz";
  public static final String ZIP_FEXT = "zip";

  private static final Log   LOGGER   = LogFactory
                                          .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                              + ".UnarchiveDBRunner");

  public UnarchiveDBRunner(List<String> files, String targetDir,
      UnarchiveDBMonitor monitor) {
    _files = files;
    _monitor = monitor;
    _targetDir = Utils.terminatePath(targetDir);
    _tasks = new LinkedList<PTask>();
    _processMsg = new ArrayList<String>();
  }

  /**
   * Adds a new task.
   */
  private void addTask(PTask task) {
    _tasks.add(task);
  }

  /**
   * Gets the next task to execute. That tasks will be removed from this
   * scheduler.
   * 
   * @return a task or null if the scheduler does not contain any tasks.
   */
  private PTask getTask() {
    if (_tasks.size() == 0)
      return null;
    return _tasks.removeFirst();
  }

  /**
   * Checks whether this scheduler contains some tasks.
   */
  private boolean hasTasks() {
    return _tasks.size() != 0;
  }

  private void prepareTasks() {
    PTask task;
    String fName, str;
    int i, size;

    size = _files.size();
    for (i = 0; i < size; i++) {
      fName = _files.get(i).toString();
      str = new File(fName).getName();
      // gunzip a tgz
      if (fName.endsWith(TGZ_FEXT)) {
        str = str.substring(0, str.length() - (TGZ_FEXT.length() + 1)) + "."
            + TAR_FEXT;
        str = _targetDir + str;
        task = new PTaskGunzip(fName, str, true);
        addTask(task);
        _processMsg.add(task.getName() + " " + fName);
        fName = str;
      }
      // gunzip a gz (possibly a tar.gz)
      else if (fName.endsWith(GZ_FEXT)) {
        str = str.substring(0, str.length() - (GZ_FEXT.length() + 1));
        str = _targetDir + str;
        task = new PTaskGunzip(fName, str, true);
        addTask(task);
        _processMsg.add(task.getName() + " " + fName);
        fName = str;
      }
      // unzip a zip
      else if (fName.endsWith(ZIP_FEXT)) {
        task = new PTaskUnzip(fName, _targetDir, true);
        addTask(task);
        _processMsg.add(task.getName() + " " + fName);
      }
      // need to untar (possibly after a gunzip) ?
      if (fName.endsWith(TAR_FEXT)) {
        task = new PTaskUntar(fName, _targetDir);
        addTask(task);
        _processMsg.add(task.getName() + " " + fName);
      }
    }
  }

  private boolean startProcessing() {
    PTask task;
    int i = 0;
    boolean bRet = true;
    while (hasTasks()) {
      task = getTask();
      _monitor.setTxtMessage(_processMsg.get(i));
      i++;
      if (!task.execute()) {
        _monitor.setErrMsg(task.getErrorMsg());
        bRet = false;
        break;
      }
      if (_monitor.interruptProcessing()) {
        _monitor.setErrMsg("job aborted");
        bRet = false;
        break;
      }
    }
    return bRet;
  }

  private void doJob() {
    prepareTasks();
    _success = startProcessing();
    // end process
    System.gc();
    _monitor.setTxtMessage("");
  }

  public void run() {
    _monitor.setJobRunnig(true);
    LoggerCentral.info(LOGGER, "Start unarchive processing");
    doJob();
    LoggerCentral.info(LOGGER, "Done unarchive processing");
    _monitor.setJobRunnig(false);
    _monitor.jobDone(_success);
  }
}
