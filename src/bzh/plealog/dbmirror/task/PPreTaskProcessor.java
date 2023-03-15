/* Copyright (C) 2021 Patrick G. Durand
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
 */package bzh.plealog.dbmirror.task;

import java.util.StringTokenizer;

import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.fetcher.DBServerConfig;
import bzh.plealog.dbmirror.fetcher.UserProcessingMonitor;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * Properly executes pre-processing tasks.
 * 
 * @author Patrick G. Durand
 * */
public class PPreTaskProcessor extends Thread {
  private UserProcessingMonitor _userMonitor;
  private DBServerConfig        _dbsc;
  private PTaskEngine           _taskEngine;
  private long                  _waitTime;
  
  private static String WAIT_MSG =
      "Waiting for pre-processing task to complete...";
  
  /**
   * No no-arguments constructor available.
   */
  @SuppressWarnings("unused")
  private PPreTaskProcessor() {}
  
  /**
   * Constructor.
   * 
   * @param dbsc bank descriptor under processing
   * @param taskEngine ready-to-use task engine
   * @param waitTime time used to let this thread waiting until pre-processing
   * tasks are done.
   */
  public PPreTaskProcessor(DBServerConfig dbsc, PTaskEngine taskEngine, long waitTime) {
    _dbsc = dbsc;
    _taskEngine = taskEngine;
    _waitTime = waitTime;
    stackTasks();
  }
  
  /**
   * Set a user monitor. 
   * 
   * @param userMonitor user monitor
   * */
  public void setUserProcessingMonitor(UserProcessingMonitor userMonitor) {
    _userMonitor = userMonitor;
  }
  
  private String getTaskParameters(String allTasks, String curTask) {
    int idx, idx2;

    idx = allTasks.indexOf(curTask) + curTask.length();
    if (idx >= allTasks.length())
      return null;
    if (allTasks.charAt(idx) != '(')
      return null;
    idx2 = allTasks.indexOf(')', idx);
    if (idx2 == -1)
      return null;
    return allTasks.substring(idx + 1, idx2);
  }
  
  /**
   * It is worth noting that only one script task is currently supported by this 
   * code.
   */
  private void stackTasks() {
    String tasks = _dbsc.getGlobalPreTasks();
    StringTokenizer tokenizer = new StringTokenizer(tasks, ",");
    while (tokenizer.hasMoreTokens()) {
      String task = tokenizer.nextToken();
      if (task.indexOf(PTask.TASK_G_EXTSCRIPT) >= 0) {
        PTaskExecScript execTask = new PTaskExecScript(_dbsc.getLocalTmpFolder(), null,
            _dbsc.getName(), _dbsc.getTypeCode());
        execTask.setParameters(getTaskParameters(task, PTask.TASK_G_EXTSCRIPT));
        _taskEngine.addTask(execTask, _dbsc.getName());
      }
    }
  }
  
  @Override
  public void run() {
    boolean initThread=true;
    while (true) {
      //force exit from this loop if no more tasks under processing
      //or user request to cancel bank installation (UI only)
      if (  (!_taskEngine.hasTasks() && !_taskEngine.isExeInProgress())
          ||(_userMonitor != null && _userMonitor.jobCancelled()) )  {
        break;
      }
      try {
        if (initThread) {//display this message one times
          initThread = false;
          LoggerCentral.info(
            LogFactory.getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                + ".PPreTaskProcessor"), WAIT_MSG);
        }
        sleep(_waitTime);
      } catch (InterruptedException e) {
      }
    }
  }

}
