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
package bzh.plealog.dbmirror.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.fetcher.DBServerConfig;
import bzh.plealog.dbmirror.fetcher.PFTPLoader;
import bzh.plealog.dbmirror.fetcher.PFTPLoaderDescriptor;
import bzh.plealog.dbmirror.fetcher.PFTPLoaderSystem;
import bzh.plealog.dbmirror.fetcher.PLocalLoader;
import bzh.plealog.dbmirror.fetcher.UserProcessingMonitor;
import bzh.plealog.dbmirror.indexer.LuceneUtils;
import bzh.plealog.dbmirror.task.PIndexerTask;
import bzh.plealog.dbmirror.task.PTask;
import bzh.plealog.dbmirror.task.PTaskEngine;
import bzh.plealog.dbmirror.ui.resources.DBMSMessages;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.conf.DBMirrorConfig;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptorUtils;
import bzh.plealog.dbmirror.util.descriptor.DescriptorEntry;
import bzh.plealog.dbmirror.util.event.DBMirrorEvent;
import bzh.plealog.dbmirror.util.log.LoggerCentral;
import bzh.plealog.dbmirror.util.log.LoggerCentralGateway;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.plealog.genericapp.api.EZEnvironment;

/**
 * This class is used to display the monitoring of databanks installation.
 * 
 * @author Patrick G. Durand
 */
@SuppressWarnings("serial")
public class RunningMirrorPanel extends JPanel {
  private InstallingDescriptorList           _descList;
  private JLabel[]                           _ftpLblMsg;
  private JLabel                             _taskLblMsg;
  private JProgressBar[]                     _ftpProgress;
  private JProgressBar                       _taskProgress;

  private String                             _curFile_ftp;
  private double                             _curFactor_ftp;
  private String                             _curFile_task;
  private double                             _curFactor_task;
  private Hashtable<String, DescriptorEntry> _entriesMap;
  private MyUserProcessingMonitor            _monitor;
  private boolean                            _running;
  private Timer                              _mirrorTimer;
  private ArrayList<FileInfo>                _fileInfo;
  private DBMSLogViewer                      _logViewer;

  public static ImageIcon                    ERROR_ICON = EZEnvironment
                                                            .getImageIcon("sign_warning.png");
  public static ImageIcon                    OK_ICON    = EZEnvironment
                                                            .getImageIcon("ok.png");

  private static final Log                   LOGGER     = LogFactory
                                                            .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                                + ".RunningMirrorPanel");

  public RunningMirrorPanel() {
    _monitor = new MyUserProcessingMonitor();
    _entriesMap = new Hashtable<String, DescriptorEntry>();
    _fileInfo = new ArrayList<FileInfo>();
    createUI();
  }

  public JProgressBar getTaskProgress() {
    return _taskProgress;
  }

  public UserProcessingMonitor getMonitor() {
    return _monitor;
  }

  public boolean canClose() {
    int ret;

    if (_running) {
      ret = JOptionPane.showConfirmDialog(
          JOptionPane.getFrameForComponent(this),
          DBMSMessages.getString("RunningMirrorPanel.msg3"),
          DBMSMessages.getString("RunningMirrorPanel.msg2"),
          JOptionPane.YES_NO_OPTION);
      return (ret == JOptionPane.YES_OPTION);
    }
    if (_mirrorTimer != null) {
      ret = JOptionPane.showConfirmDialog(
          JOptionPane.getFrameForComponent(this),
          DBMSMessages.getString("RunningMirrorPanel.msg5"),
          DBMSMessages.getString("RunningMirrorPanel.msg2"),
          JOptionPane.YES_NO_OPTION);
      return (ret == JOptionPane.YES_OPTION);
    }
    return true;
  }

  public boolean hasJobRunning() {
    return _running;
  }

  public boolean hasJobScheduled() {
    return _mirrorTimer != null;
  }

  protected boolean canStartAJob(ArrayList<DescriptorEntry> entries) {
    if (_running) {
      JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(this),
          DBMSMessages.getString("RunningMirrorPanel.msg1"),
          DBMSMessages.getString("RunningMirrorPanel.msg2"),
          JOptionPane.INFORMATION_MESSAGE);
      return false;
    }
    if (_mirrorTimer != null) {
      int ret = JOptionPane.showConfirmDialog(
          JOptionPane.getFrameForComponent(this),
          DBMSMessages.getString("RunningMirrorPanel.msg4"),
          DBMSMessages.getString("RunningMirrorPanel.msg2"),
          JOptionPane.YES_NO_OPTION);
      if (ret == JOptionPane.NO_OPTION)
        return false;
      _mirrorTimer.cancel();
      _mirrorTimer = null;
    }
    return true;
  }

  private JLabel createLabel() {
    JLabel lbl;

    lbl = new JLabel("-");
    lbl.setOpaque(true);
    lbl.setForeground(EZEnvironment.getSystemTextColor());
    return lbl;
  }

  private JProgressBar createProgressBar() {
    JProgressBar pb;

    pb = new JProgressBar();
    pb.setStringPainted(true);
    pb.setString("");

    return pb;
  }

  private void createUI() {
    FormLayout layout;
    DefaultFormBuilder builder;
    JTabbedPane pane;
    JPanel pnl;
    int i;
    int nbWorkers = DBMSAbstractConfig.getFileCopyWorkers();

    layout = new FormLayout("240dlu", "");
    builder = new DefaultFormBuilder(layout);
    builder.setDefaultDialogBorder();

    _descList = new InstallingDescriptorList();

    builder.appendSeparator(DBMSMessages.getString("RunningMirrorPanel.msg7"));

    _ftpLblMsg = new JLabel[nbWorkers];
    _ftpProgress = new JProgressBar[nbWorkers];
    for (i = 0; i < nbWorkers; i++) {
      _ftpLblMsg[i] = createLabel();
      _ftpProgress[i] = createProgressBar();
      builder.append(_ftpLblMsg[i]);
      builder.nextLine();
      builder.append(_ftpProgress[i]);
    }

    builder.appendSeparator(DBMSMessages.getString("RunningMirrorPanel.msg8"));
    _taskLblMsg = createLabel();
    _taskProgress = createProgressBar();
    builder.append(_taskLblMsg);
    builder.nextLine();
    builder.append(_taskProgress);

    pnl = new JPanel(new BorderLayout());
    pnl.add(_descList, BorderLayout.NORTH);
    pnl.add(builder.getContainer(), BorderLayout.CENTER);

    pane = new JTabbedPane(JTabbedPane.BOTTOM);
    pane.add("Process", pnl);
    if (LoggerCentral.getLogGateway() == null) {
      _logViewer = new DBMSLogViewer();
      if (DBMSAbstractConfig.isUsingDirectConnectionFromLogToLogViewer()) {
        LoggerCentral.setLogGateway(new MyLoggerCentral(_logViewer));
      } else {
        DBMSAbstractConfig.addLogAppender(_logViewer.getAppender());
      }
      pane.add("Logs", _logViewer);
    }
    pane.setFocusable(false);

    this.setLayout(new BorderLayout());
    this.add(pane, BorderLayout.CENTER);
  }

  private void resetUI() {
    int i, nbWorkers = DBMSAbstractConfig.getFileCopyWorkers();
    for (i = 0; i < nbWorkers; i++) {
      _ftpLblMsg[i].setText("-");
      _ftpProgress[i].setValue(0);
    }

    _taskLblMsg.setIcon(null);
    _taskLblMsg.setText("-");

    _taskProgress.setValue(0);
    _curFile_ftp = null;
    _curFactor_ftp = 0d;
    _curFile_task = null;
    _curFactor_task = 0d;
  }

  private boolean hasPreviousAbortedJobs(List<DescriptorEntry> entries) {
    File folder;
    // for unit tests
    if (DBMSAbstractConfig.testMode) {
      return false;
    }
    for (DescriptorEntry de : entries) {
      folder = new File(de.getDescriptor().getLocalFolder()
          + DBMSAbstractConfig.DOWNLOADING_DIR);
      if (folder.exists()) {
        return true;
      }
    }
    return false;
  }

  private boolean checkWritePermission(List<DescriptorEntry> entries) {
    File f, dir;
    PrintWriter writer = null;
    String folder;

    // usually, a Security Manager is not associated to an application
    // so, test access 'manually'
    for (DescriptorEntry de : entries) {
      folder = de.getDescriptor().getLocalFolder();
      f = new File(folder + "ztext.txt");
      try {
        // first time a db is installed, its target dir may not exist yet
        dir = new File(folder);
        if (dir.exists() == false)
          dir.mkdirs();
        // write access
        writer = new PrintWriter(f);
        writer.write("test");
        writer.flush();
        writer.close();
        // dir access
        new File(folder).list();

      } catch (Exception e) {
        JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(this),
            DBMSMessages.getString("RunningMirrorPanel.msg6") + ":\n" + folder,
            DBMSMessages.getString("RunningMirrorPanel.msg2"),
            JOptionPane.WARNING_MESSAGE);
        return false;
      } finally {
        FileUtils.deleteQuietly(f);
        IOUtils.closeQuietly(writer);
      }
    }
    return true;
  }

  /**
   * This method check and add the dependent databanks
   * 
   * @param entry
   * @param entries
   *          list of all entries to process
   */
  private void addEntryToProcess(DescriptorEntry entry,
      List<DescriptorEntry> entries) {
    DescriptorEntry dependsEntry = null;
    // install depends db before this one
    for (File dependDbFile : entry.getDescriptor().getDependsDatabanks()) {
      try {
        // init an entry loaded the entry descriptor file
        dependsEntry = DescriptorEntry.createFrom(dependDbFile);

        // is this entry still in the list ?
        if (entries.contains(dependsEntry)) {
          // yes, so get it
          dependsEntry = entries.get(entries.indexOf(dependsEntry));
        }

        // the dependent databank must be installed before
        dependsEntry.setInstallationOrder(dependsEntry.getInstallationOrder()
            - entry.getInstallationOrder() - 1);

        if (!entries.contains(dependsEntry)) {
          addEntryToProcess(dependsEntry, entries);
        }
      } catch (Exception e) {
        LoggerCentral.warn(LOGGER, "Depend databank '" + dependDbFile.getName()
            + "' will not be installed: " + e.getMessage());
        continue;
      }

    }

    if (!_entriesMap.containsKey(entry.getDescriptor().getName())) {
      _entriesMap.put(entry.getDescriptor().getName(), entry);
      if (!entries.contains(entry)) {
        entries.add(entry);
      }
    }
  }

  private PFTPLoaderDescriptor[] initProcess(List<DescriptorEntry> entries,
      String wkMode, String force) {

    resetUI();
    LoggerCentral.reset();
    _entriesMap.clear();
    for (int i = 0; i < entries.size(); i++) {
      DescriptorEntry entry = entries.get(i);
      entry.setStatus(DescriptorEntry.STATUS.waiting);

      // install this db
      addEntryToProcess(entry, entries);
    }

    // creates the descriptor regarding the installation orders
    Collections.sort(entries);
    int currentInstallOrder = entries.get(0).getInstallationOrder();
    ArrayList<PFTPLoaderDescriptor> descriptors = new ArrayList<PFTPLoaderDescriptor>();
    PFTPLoaderDescriptor currentDescriptor = PFTPLoaderDescriptor.create(
        force, wkMode);
    descriptors.add(currentDescriptor);
    for (DescriptorEntry entry : entries) {
      if (entry.getInstallationOrder() == currentInstallOrder) {
        currentDescriptor.addDbToInstall(entry);
      } else {
        currentDescriptor = PFTPLoaderDescriptor.create(force, wkMode);
        currentDescriptor.addDbToInstall(entry);
        descriptors.add(currentDescriptor);
        currentInstallOrder = entry.getInstallationOrder();
      }
    }

    if (_logViewer != null) {
      _logViewer.clearContent();
    }
    return descriptors.toArray(new PFTPLoaderDescriptor[descriptors.size()]);
  }

  // wkMode: KFTPLoaderDescriptor.MAINTASK_INFO or
  // KFTPLoaderDescriptor.MAINTASK_DOWNLOAD
  public synchronized void startLoadingEntries(List<DescriptorEntry> entries,
      String wkMode) {
    PFTPLoaderDescriptor[] descriptors;
    String forceNewInstall = "true";
    Date scheduleTime;
    boolean closeAfterProcess;

    if (entries.isEmpty())
      return;

    if (!checkWritePermission(entries))
      return;

    // reset the rename id number
    PIndexerTask.firstNbForRename = 0;

    _taskLblMsg.setIcon(null);
    _taskLblMsg.setText("-");

    if (PFTPLoaderDescriptor.MAINTASK_DOWNLOAD.equals(wkMode)) {
      // start the download
      RunnerSchedulerDlg scheduler = new RunnerSchedulerDlg(
          JOptionPane.getFrameForComponent(RunningMirrorPanel.this));
      if (!DBMSAbstractConfig.testMode) {
        scheduler.showDlg();
      }
      scheduleTime = scheduler.getScheduleTime();
      closeAfterProcess = scheduler.closeAppAfterProcessing();
      if (scheduleTime == null) {// cancel
        for (DescriptorEntry de : entries) {
          de.setStatus(DescriptorEntry.STATUS.unknown);
        }
        _descList.clearTimeLalbel();
        return;
      }

      // to add depends entries before checking existed previous job
      descriptors = initProcess(entries, wkMode, forceNewInstall);

      // before starting a new job, check if we can resume a previous aborted
      // one
      if (hasPreviousAbortedJobs(entries)) {
        int ret = JOptionPane.showConfirmDialog(
            JOptionPane.getFrameForComponent(this),
            DBMSMessages.getString("RunningMirrorPanel.msg13"),
            DBMSMessages.getString("RunningMirrorPanel.msg2"),
            JOptionPane.YES_NO_CANCEL_OPTION);
        if (ret == JOptionPane.YES_OPTION) {
          forceNewInstall = "false";
        } else if (ret == JOptionPane.CANCEL_OPTION) {
          return;
        }
      }
      for (PFTPLoaderDescriptor descriptor : descriptors) {
        // set the resume date regarding the previous user choice
        descriptor.setProperty(PFTPLoaderDescriptor.FORCE_KEY, forceNewInstall);
      }
      _descList.setProcessingTime(scheduleTime);

      MirrorRunner runner = new MirrorRunner(descriptors, closeAfterProcess);
      if (!DBMSAbstractConfig.testMode) {
        _mirrorTimer = new Timer();
        _mirrorTimer.schedule(runner, scheduleTime);
      } else {
        runner.run();
      }
    } else {
      descriptors = initProcess(entries, wkMode, forceNewInstall);
      new InfoRunner(entries, descriptors).start();
    }
  }

  public class MyUserProcessingMonitor implements UserProcessingMonitor {
    private String                          workingMode;
    private boolean                         closeAfterProcess;
    private boolean                         continueProcess;
    private boolean                         error;

    private Hashtable<String, JLabel>       workerLabels     = new Hashtable<String, JLabel>();
    private Hashtable<String, JProgressBar> workerProgresess = new Hashtable<String, JProgressBar>();

    public synchronized JProgressBar getTaskProgress() {
      return _taskProgress;
    }

    private synchronized JLabel getFtpLabel(String workerId) {
      JLabel lbl = _ftpLblMsg[0];// default is first lbl

      lbl = workerLabels.get(workerId);
      if (lbl != null)
        return lbl;
      if (workerId.equals(PFTPLoaderSystem.WORKER_ID)
          || workerId.equals(PTaskEngine.WORKER_ID)
          || workerId.equals(PLocalLoader.WORKER_ID)) {
        workerLabels.put(workerId, _ftpLblMsg[0]);
        lbl = _ftpLblMsg[0];
      } else if (workerId.startsWith(PFTPLoader.WORKER_ID)) {
        int pos = workerId.indexOf('-');
        String value = workerId.substring(pos + 1);
        int idx = Integer.valueOf(value);
        lbl = _ftpLblMsg[idx >= DBMSAbstractConfig.getFileCopyWorkers() ? 0
            : idx];
        workerLabels.put(workerId, lbl);
      }
      return lbl;
    }

    public synchronized JProgressBar getFtpProgress(String workerId) {
      JProgressBar lbl = _ftpProgress[0];// default is first lbl

      lbl = workerProgresess.get(workerId);
      if (lbl != null)
        return lbl;
      if (workerId.equals(PFTPLoaderSystem.WORKER_ID)
          || workerId.equals(PTaskEngine.WORKER_ID)
          || workerId.equals(PLocalLoader.WORKER_ID)) {
        workerProgresess.put(workerId, _ftpProgress[0]);
        lbl = _ftpProgress[0];
      } else if (workerId.startsWith(PFTPLoader.WORKER_ID)) {
        int idx = Integer
            .valueOf(workerId.substring(workerId.indexOf('-') + 1));
        lbl = _ftpProgress[idx >= DBMSAbstractConfig.getFileCopyWorkers() ? 0
            : idx];
        workerProgresess.put(workerId, lbl);
      }
      return lbl;
    }

    private void setWorkingMode(String wkMode) {
      workingMode = wkMode;
      continueProcess = false;
      error = false;
    }

    private void setCloseAfterProcess(boolean closeAfterProcess) {
      this.closeAfterProcess = closeAfterProcess;
    }

    private void updateStatus(String dbConfName, DescriptorEntry.STATUS status) {
      if (dbConfName == null)
        return;
      DescriptorEntry de = _entriesMap.get(dbConfName);
      if (de != null) {
        de.setStatus(status);
      }
    }

    /**
     * Implementation of UserProcessingMonitor.
     */
    public boolean jobCancelled() {
      return LoggerCentral.processAborted();
    }

    /**
     * Implementation of UserProcessingMonitor.
     */
    public void processingDone(MSG_TYPE status) {
      DBMirrorConfig mirrorConfig;
      Enumeration<DescriptorEntry> enums;

      this.processingDone();
      resetUI();
      if (status.equals(MSG_TYPE.ERROR)) {
        error = true;
        _taskLblMsg.setForeground(Color.orange);
        if (PFTPLoaderDescriptor.MAINTASK_DOWNLOAD.equals(workingMode)) {
          _taskLblMsg
              .setText(DBMSMessages.getString("RunningMirrorPanel.msg9"));
        } else {
          _taskLblMsg.setText(DBMSMessages
              .getString("RunningMirrorPanel.msg14"));
        }
        _taskLblMsg.setIcon(ERROR_ICON);
        enums = _entriesMap.elements();
        while (enums.hasMoreElements()) {
          enums.nextElement().setStatus(DescriptorEntry.STATUS.error);
        }
      } else {
        error = false;
        _taskLblMsg.setForeground(EZEnvironment.getSystemTextColor());
        if (PFTPLoaderDescriptor.MAINTASK_DOWNLOAD.equals(workingMode)) {
          _taskLblMsg.setText(DBMSMessages
              .getString("RunningMirrorPanel.msg10"));
          _taskLblMsg.setIcon(OK_ICON);
          mirrorConfig = DBDescriptorUtils.getDBMirrorConfig(DBMSAbstractConfig
              .getLocalMirrorConfFile());
          DBMSAbstractConfig.fireMirrorEvent(new DBMirrorEvent(mirrorConfig,
              DBMirrorEvent.TYPE.dbAdded));
          if (closeAfterProcess) {
            LoggerCentral.info(LOGGER,
                "*** Ending application after processing: "
                    + Calendar.getInstance().getTime());
            System.exit(0);
          }
        } else {
          _taskLblMsg.setText(DBMSMessages
              .getString("RunningMirrorPanel.msg11"));
          // display files info data structure
          FileInfoDlg dlg = new FileInfoDlg(
              JOptionPane.getFrameForComponent(RunningMirrorPanel.this),
              _fileInfo);
          dlg.showDlg();
          continueProcess = !dlg.dlgCancelled();
        }
      }
      System.gc();
      LoggerCentral.setRunning(false);
    }

    /**
     * Method called after a filter on the personnal databank panel. In this
     * case, there is no need of continuing an installation (which is the case
     * of the processingDone(MSG_TYPE status) method
     */
    public void processingDone() {
      _descList.stopProcessing();
      _running = false;
      _mirrorTimer = null;
    }

    private void handleProcessingFTP(String workerID, String fName,
        long currentSteps, long totalSteps) {
      JProgressBar progress;
      int value;

      progress = getFtpProgress(workerID);
      if (fName.equals(_curFile_ftp) == false) {
        // new file processing
        _curFile_ftp = fName;
        progress.setMinimum(0);
        _curFactor_ftp = 100.0d / (double) totalSteps;
        progress.setMaximum(100);
      }
      value = (int) ((double) currentSteps * _curFactor_ftp);
      progress.setValue(value);
    }

    private void handleProcessingTASK(String fName, long currentSteps,
        long totalSteps) {
      int value;
      if (fName.equals(_curFile_task) == false) {
        // new file processing
        _curFile_task = fName;
        _taskProgress.setMinimum(0);
        _curFactor_task = 100.0d / (double) totalSteps;
        _taskProgress.setMaximum(100);
      }
      value = (int) ((double) currentSteps * _curFactor_task);
      _taskProgress.setValue(value);
    }

    public void fileTransferInfo(String workerID, DBServerConfig fsc,
        int nFiles, long totalBytes) {
      String tasks;
      int factor;

      tasks = fsc.getUnitPostTasks() + fsc.getGlobalPostTasks();
      // index data ?
      if (tasks.indexOf(PTask.TASK_U_GB_IDX) != -1
          || tasks.indexOf(PTask.TASK_U_GP_IDX) != -1
          || tasks.indexOf(PTask.TASK_U_EM_IDX) != -1
          || tasks.indexOf(PTask.TASK_U_SW_IDX) != -1
          || tasks.indexOf(PTask.TASK_U_DICO_IDX) != -1) {
        // make also a blast databank ?
        if (tasks.indexOf(PTask.TASK_G_FORMATDB) != -1) {
          factor = 10;
        } else {
          factor = 9;
        }
      }
      // only blast databank to create
      else if (tasks.indexOf(PTask.TASK_G_FORMATDB) != -1) {
        factor = 10;
      } else {// install native blast databank
        factor = 4;
      }
      _fileInfo.add(new FileInfo(fsc.getName(), nFiles, totalBytes, totalBytes
          * (long) factor));
      StringBuffer buf = new StringBuffer();
      buf.append(nFiles);
      buf.append(" file");
      if (nFiles > 1)
        buf.append("s");
      buf.append(" to download (");
      Utils.setBytes(buf, totalBytes);
      buf.append(")");
      getFtpLabel(workerID).setText(buf.toString());
    }

    /**
     * Implementation of UserProcessingMonitor.
     */
    public void processingFile(String workerID, String dbConfName,
        PROCESS_TYPE processType, String fName, long currentSteps,
        long totalSteps) {
      if (processType.equals(PROCESS_TYPE.FTP_LOADING)) {
        handleProcessingFTP(workerID, fName, currentSteps, totalSteps);
      } else {
        handleProcessingTASK(fName, currentSteps, totalSteps);
      }
    }

    /**
     * Implementation of UserProcessingMonitor.
     */
    public void processingMessage(String workerID, String dbConfName,
        PROCESS_TYPE processType, MSG_TYPE msgType, String message) {
      this.processingMessage(null, workerID, dbConfName, processType, msgType,
          message);
    }

    /**
     * Implementation of UserProcessingMonitor.
     */
    public void processingMessage(ImageIcon icon, String workerID,
        String dbConfName, PROCESS_TYPE processType, MSG_TYPE msgType,
        String message) {

      JLabel curLbl;
      Color clr;

      if (processType.equals(PROCESS_TYPE.FTP_LOADING)) {
        getFtpProgress(workerID).setValue(0);
        processingFileMessage(workerID, processType, "");
        curLbl = getFtpLabel(workerID);
      } else {

        _taskProgress.setValue(0);
        processingFileMessage(workerID, processType, "");
        curLbl = _taskLblMsg;
      }
      if (msgType.equals(MSG_TYPE.ERROR)) {
        clr = Color.orange;
        updateStatus(dbConfName, DescriptorEntry.STATUS.error);
      } else {
        clr = EZEnvironment.getSystemTextColor();
      }

      if (icon != null) {
        curLbl.setIcon(icon);
      } else {
        curLbl.setIcon(null);
      }
      curLbl.setForeground(clr);
      curLbl.setText(message);
    }

    public void processingFileMessage(String workerID,
        PROCESS_TYPE processType, String message) {
      JProgressBar currentProgressBar = null;
      if (processType.equals(PROCESS_TYPE.FTP_LOADING)) {
        currentProgressBar = getFtpProgress(workerID);
      } else {
        currentProgressBar = _taskProgress;
      }
      currentProgressBar.setString(message);
    }

    public void startProcessing(String dbConfName) {
      updateStatus(dbConfName, DescriptorEntry.STATUS.running);
    }

    public void endProcessing(String dbConfName) {
      updateStatus(
          dbConfName,
          (LoggerCentral.errorMsgEmitted() || LoggerCentral.processAborted()) ? DescriptorEntry.STATUS.error
              : DescriptorEntry.STATUS.ok);
    }

    /**
     * Implementation of UserProcessingMonitor.
     */
    public void processingStarted() {
      _running = true;
      _fileInfo.clear();
      workerLabels.clear();
      workerProgresess.clear();
      _descList.startProcessing();
    }

  }

  private class InfoRunner extends Thread {
    private PFTPLoaderDescriptor[] descriptors;
    private List<DescriptorEntry>  entries;

    private InfoRunner(List<DescriptorEntry> entries,
        PFTPLoaderDescriptor[] descriptors) {
      this.descriptors = descriptors;
      this.entries = entries;
    }

    public void run() {
      String forceNewInstall = "true";
      PFTPLoaderSystem loader;
      Date scheduleTime;
      boolean closeAfterProcess;

      // start info task
      for (PFTPLoaderDescriptor descriptor : descriptors) {
        descriptor.setProperty(PFTPLoaderDescriptor.MAINTASK_KEY,
            PFTPLoaderDescriptor.MAINTASK_INFO);
      }
      _monitor.setWorkingMode(PFTPLoaderDescriptor.MAINTASK_INFO);
      _monitor.setCloseAfterProcess(false);
      loader = new PFTPLoaderSystem(descriptors);
      loader.setUserProcessingMonitor(_monitor);
      loader.runProcessing();
      if (_monitor.continueProcess == false) {// error or cancel after viewing
                                              // db info
        if (!_monitor.error) {
          resetUI();
        }
        for (DescriptorEntry de : entries) {
          de.setStatus(DescriptorEntry.STATUS.unknown);
        }
        _descList.clearTimeLalbel();
        return;
      }
      // start the download
      RunnerSchedulerDlg scheduler = new RunnerSchedulerDlg(
          JOptionPane.getFrameForComponent(RunningMirrorPanel.this));
      scheduler.showDlg();
      scheduleTime = scheduler.getScheduleTime();
      closeAfterProcess = scheduler.closeAppAfterProcessing();
      resetUI();
      if (scheduleTime == null) {// cancel
        for (DescriptorEntry de : entries) {
          de.setStatus(DescriptorEntry.STATUS.unknown);
        }
        _descList.clearTimeLalbel();
        return;
      }
      if (hasPreviousAbortedJobs(entries)) {
        int ret = JOptionPane.showConfirmDialog(
            JOptionPane.getFrameForComponent(RunningMirrorPanel.this),
            DBMSMessages.getString("RunningMirrorPanel.msg13"),
            DBMSMessages.getString("RunningMirrorPanel.msg2"),
            JOptionPane.YES_NO_CANCEL_OPTION);
        if (ret == JOptionPane.YES_OPTION) {
          forceNewInstall = "false";
        } else if (ret == JOptionPane.CANCEL_OPTION) {
          for (DescriptorEntry de : entries) {
            de.setStatus(DescriptorEntry.STATUS.unknown);
          }
          _descList.clearTimeLalbel();
          return;
        }
      }
      for (DescriptorEntry de : entries) {
        de.setStatus(DescriptorEntry.STATUS.waiting);
      }

      for (PFTPLoaderDescriptor descriptor : descriptors) {
        descriptor.setProperty(PFTPLoaderDescriptor.MAINTASK_KEY,
            PFTPLoaderDescriptor.MAINTASK_DOWNLOAD);
        descriptor.setProperty(PFTPLoaderDescriptor.FORCE_KEY, forceNewInstall);
      }

      _monitor.setWorkingMode(PFTPLoaderDescriptor.MAINTASK_DOWNLOAD);
      _monitor.setCloseAfterProcess(closeAfterProcess);
      _descList.setProcessingTime(scheduleTime);
      _mirrorTimer = new Timer();
      _mirrorTimer.schedule(new MirrorRunner(descriptors, closeAfterProcess),
          scheduleTime);
    }
  }

  private class MirrorRunner extends TimerTask {
    private PFTPLoaderDescriptor[] descriptors;
    private boolean                closeAfterProcess;

    private MirrorRunner(PFTPLoaderDescriptor[] descriptors,
        boolean closeAfterProcess) {
      this.descriptors = descriptors;
      this.closeAfterProcess = closeAfterProcess;
    }

    public void run() {
      PFTPLoaderSystem loader;
      if (descriptors.length > 0) {
        _monitor.setWorkingMode(descriptors[0].getProperties().getProperty(
            PFTPLoaderDescriptor.MAINTASK_KEY));
      }
      _monitor.setCloseAfterProcess(closeAfterProcess);
      loader = new PFTPLoaderSystem(descriptors);
      loader.setUserProcessingMonitor(_monitor);
      LuceneUtils.closeStorages();
      loader.runProcessing();
    }
  }

  private class MyLoggerCentral implements LoggerCentralGateway {
    private SimpleDateFormat _formatter = new SimpleDateFormat("HH:mm:ss");
    private DBMSLogViewer    _logViewer;

    public MyLoggerCentral(DBMSLogViewer lv) {
      _logViewer = lv;
    }

    private void setMessage(final String msg) {
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          _logViewer.appendText(msg + "\n");
        }
      });

    }

    @Override
    public void debug(String arg0) {
      setMessage(_formatter.format(Calendar.getInstance().getTime())
          + " | DEBUG | " + arg0);
    }

    @Override
    public void error(String arg0) {
      setMessage(_formatter.format(Calendar.getInstance().getTime())
          + " | ERROR | " + arg0);
    }

    @Override
    public void info(String arg0) {
      setMessage(_formatter.format(Calendar.getInstance().getTime())
          + " | INFO | " + arg0);
    }

    @Override
    public void warn(String arg0) {
      setMessage(_formatter.format(Calendar.getInstance().getTime())
          + " | WARN | " + arg0);
    }

  }

}
