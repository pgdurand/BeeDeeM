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
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.io.IOUtils;
import org.apache.commons.net.io.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import bzh.plealog.dbmirror.fetcher.UserProcessingMonitor;
import bzh.plealog.dbmirror.indexer.ParserMonitor;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;
import bzh.plealog.dbmirror.util.runner.DBMSUniqueSeqIdDetector;
import bzh.plealog.dbmirror.util.runner.DBMSUniqueSeqIdRedundantException;

/**
 * This is the task execution engine. It stores a FIFO list of KLTasks that are
 * executed one at a time using a Timer.
 * 
 * @author Patrick G. Durand
 */
public class PTaskEngine {
  private Timer                   _engine;
  private LinkedList<PTask>      _tasks;
  private boolean                 _exeInProgress;
  private UserProcessingMonitor   _userMonitor;
  private DBMSUniqueSeqIdDetector _seqIdDetector;

  public static final long        ENGINE_SCHEDULING_TIMER = 2000;

  public static final String      WORKER_ID               = "TaskEngine";

  private static final Logger        LOGGER                  = LogManager.getLogger(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                                  + ".PTaskEngine");

  /**
   * Constructor.
   * 
   * @param launchTasks
   *          false if the tasks should not be launched automatically by the
   *          timer
   */
  public PTaskEngine(boolean launchTasks) {
    _tasks = new LinkedList<PTask>();
    _engine = new Timer();
    if (launchTasks) {
      _engine.schedule(new TaskStarter(), 1000, ENGINE_SCHEDULING_TIMER);
    }
  }

  /**
   * Constructor.
   */
  public PTaskEngine() {
    this(true);
  }

  /**
   * Constructor.
   * 
   * @param delay
   *          time interval (ms) used to schedule tasks execution.
   */
  public PTaskEngine(long delay) {
    _tasks = new LinkedList<PTask>();
    _engine = new Timer();
    _engine.schedule(new TaskStarter(), 1000,
        delay < 1000 ? ENGINE_SCHEDULING_TIMER : delay);
  }

  public void setUserProcessingMonitor(UserProcessingMonitor userMonitor) {
    _userMonitor = userMonitor;
  }

  /**
   * Adds a new task.
   */
  public synchronized void addTask(PTask task, String dbConfName) {
    if (LoggerCentral.errorMsgEmitted() || LoggerCentral.processAborted())
      return;
    task.setDbConfName(dbConfName);
    _tasks.add(task);
  }

  /**
   * 
   * @return the tasks
   */
  public LinkedList<PTask> getTasks() {
    return this._tasks;
  }

  /**
   * Gets the next task to execute. That tasks will be removed from this
   * scheduler.
   * 
   * @return a task or null if the scheduler does not contain any tasks.
   */
  private synchronized PTask getTask() {
    if (_tasks.size() == 0)
      return null;
    return _tasks.removeFirst();
  }

  /**
   * Checks whether this scheduler contains some tasks.
   */
  public synchronized boolean hasTasks() {
    return _tasks.size() != 0;
  }

  /**
   * Terminate scheduler only if no more tasks are either under execution of
   * contained in the scheduler.
   * 
   * @return true if the scheduler has been terminated, false otherwise.
   */
  public boolean terminate() {
    if (LoggerCentral.processAborted() == false) {
      if (isExeInProgress())
        return false;
      if (hasTasks())
        return false;
    }
    if (hasTasks())
      _tasks.clear();
    _engine.cancel();
    if (_seqIdDetector != null) {
      _seqIdDetector.reset();
      if (_seqIdDetector.hasBeenUsed()) {
        LoggerCentral.info(
            LOGGER,
            "TaskEngine/UniqueSeqId: total working time: "
                + (_seqIdDetector.getWorkingTime() / 1000l) + " s.");
      }
    }
    return true;
  }

  private synchronized void setExeInProgress(boolean val) {
    _exeInProgress = val;
  }

  protected synchronized boolean isExeInProgress() {
    return _exeInProgress;
  }

  private class TaskStarter extends TimerTask {
    public void run() {
      if (isExeInProgress())
        return;
      setExeInProgress(true);
      PTask task = getTask();
      if (task == null) {
        setExeInProgress(false);
        return;
      }
      if (!(LoggerCentral.errorMsgEmitted() || LoggerCentral.processAborted())) {
        TaskRunner taskRunner = new TaskRunner(task);
        taskRunner.start();
      }
    }
  }

  private class TaskRunner extends Thread {
    private PTask _task;

    public TaskRunner(PTask task) {
      _task = task;
    }

    public void run() {
      if (_userMonitor != null) {
        _userMonitor.processingMessage(WORKER_ID, _task.getDbConfName(),
            UserProcessingMonitor.PROCESS_TYPE.TASK_EXECUTION,
            UserProcessingMonitor.MSG_TYPE.OK, _task.getUserFriendlyName());
      } else {
        LOGGER.debug("starting task : " + _task.getUserFriendlyName());
      }
      if (_task instanceof PParserTask) {
        ((PParserTask) _task).setParserMonitor(new MyParserMonitor(_task
            .getDbConfName(), _userMonitor));
      }
      if (_task instanceof PTaskLuceneDirMerge) {
        ((PTaskLuceneDirMerge) _task).setUserProcessingMonitor(_userMonitor);
      }
      else if (_task instanceof PTaskExecScript) {
        ((PTaskExecScript) _task).setUserProcessingMonitor(_userMonitor);
      }
      boolean bOk = true;
      try {
        // this has been added to secure code when running KDMS UI
        if (!_task.execute()) {
          LoggerCentral.error(
              LOGGER,
              "Unable to execute task: " + _task.getName() + ": "
                  + _task.getErrorMsg());
          bOk = false;
        }
      } catch (Exception e) {
        LoggerCentral.error(LOGGER, "Unexpected error: " + e.toString());
        bOk = false;
      }
      if (!bOk) {// end all processing if error
        _tasks.clear();
        _engine.cancel();
      }
      // lan : do not do this anymore because the monitor is needed by the next
      // tasks (example : LuceneFastaIndexer => FastaRenamer)
      // if (_task instanceof KLParserTask)
      // {
      // ((KLParserTask) _task).setParserMonitor(null);
      // }
      if (_task instanceof PTaskEndProcessing && _userMonitor != null) {
        _userMonitor.endProcessing(_task.getDbConfName());
      }
      setExeInProgress(false);
    }
  }

  public class MyParserMonitor implements ParserMonitor {
    private long                  fileSize;
    private long                  stepSize;
    private long                  step                   = 1;
    private String                fName;
    private String                dbConfName;
    private boolean               redundantSequenceFound = false;
    private UserProcessingMonitor upMonitor;

    public MyParserMonitor(String dbConfName, UserProcessingMonitor mon) {
      upMonitor = mon;
      this.dbConfName = dbConfName;
    }

    public void seqFound(String id, String name, String fName, long start,
        long stop, boolean checkRedundancy) {

      if (checkRedundancy) {
        if (!_seqIdDetector.add(id)) {
          this.redundantSequenceFound = true;
          throw new DBMSUniqueSeqIdRedundantException("redundant sequence ID: "
              + id);
        }
      }
      // the following is used to cancel operations made by all parser (SW, GB,
      // Fasta and Bold2Gb)
      if (LoggerCentral.processAborted()) {
        throw new PTaskEngineAbortException();
      }
      if (upMonitor == null)
        return;
      if (start >= (step * stepSize) || start == fileSize) {
        upMonitor.processingFile(WORKER_ID, dbConfName,
            UserProcessingMonitor.PROCESS_TYPE.TASK_EXECUTION, fName, start,
            fileSize);
        step++;
      }
    }

    public void startProcessingFile(String fName, long fSize) {
      // the first time a file is going to be parsed, we create the
      // KDMSUniqueSeqIdDetector object
      // it'll be used for all other parsed files to check for seqIds redundancy
      // across multiple files
      if (_seqIdDetector == null) {
        _seqIdDetector = new DBMSUniqueSeqIdDetector(
            new File(fName).getParent() + File.separator + "seqIds.ldx");
      } else {
        _seqIdDetector.openIndex();
      }

      if (upMonitor == null)
        return;
      fileSize = fSize;
      this.fName = fName;
      this.stepSize = fileSize / 20l;
      if (this.stepSize < Util.DEFAULT_COPY_BUFFER_SIZE)
        this.stepSize = Util.DEFAULT_COPY_BUFFER_SIZE;
    }

    public void stopProcessingFile(String file, int entries) {
      // ensure that remaining seqIds for "file" are saved in the index
      _seqIdDetector.dumpContent();
      try {
        _seqIdDetector.closeIndex();
      } catch (Exception ex) {// not bad, so avoid to stop here
        // do not log error with a warn : this is not a bad error,
        // so we do not want to stop the overall indexing process
        LoggerCentral.info(LOGGER,
            "unable to close index (TaskEngine/UniqueSeqId): " + ex);
      }
      // this is required since seqFound does not specify that we have reached
      // the end of the file
      if (upMonitor != null) {
        upMonitor.processingFile(WORKER_ID, dbConfName,
            UserProcessingMonitor.PROCESS_TYPE.TASK_EXECUTION, fName, fileSize,
            fileSize);
      }
      PrintWriter writer = null;
      String f;

      f = file + DBMSAbstractConfig.FEXT_NUM;
      try {
        writer = new PrintWriter(new FileWriter(new File(f)));
        writer.write(String.valueOf(entries));
        writer.flush();
      } catch (IOException e) {
        // do not log error with a warn : this is not a bad error,
        // so we do not want to stop the overall indexing process
        LoggerCentral.info(LOGGER, "unable to save databank entries in: " + f
            + ": " + e);
      } finally {
        IOUtils.closeQuietly(writer);
      }
    }

    public boolean redundantSequenceFound() {
      return this.redundantSequenceFound;
    }

    /**
     * This method is called to transmit a message to the user monitor.
     */
    public void processingMessage(String message) {
      if (upMonitor != null) {
        this.upMonitor.processingMessage(WORKER_ID, dbConfName,
            UserProcessingMonitor.PROCESS_TYPE.TASK_EXECUTION,
            UserProcessingMonitor.MSG_TYPE.OK, message);
      }
    }
  }

}
