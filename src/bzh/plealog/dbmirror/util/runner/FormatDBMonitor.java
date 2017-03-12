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
import java.io.IOException;
import java.util.Hashtable;

import javax.swing.text.JTextComponent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.task.PTaskEngineAbortException;
import bzh.plealog.dbmirror.util.ant.PAntTasks;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;
import bzh.plealog.dbmirror.util.sequence.SeqIOConvertMonitor;

public abstract class FormatDBMonitor implements SeqIOConvertMonitor {
  Hashtable<String, String>       uniqueIDs;
  private boolean                 jobRunning;
  private String                  errMsg;
  private JTextComponent          txtCompo;
  private boolean                 cancelJob;
  private boolean                 checkNR;
  private int                     totSeq;
  private String                  lastID = "?";
  private DBMSUniqueSeqIdDetector _seqIdDetector;
  private boolean                 _bInit = false;

  private static final Log        LOGGER = LogFactory
                                             .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                 + ".FormatDBMonitor");

  public FormatDBMonitor() {
    uniqueIDs = new Hashtable<String, String>();
  }

  public FormatDBMonitor(JTextComponent txtCompo) {
    this();
    setTxtCompo(txtCompo);
  }

  public void reset() {
    uniqueIDs.clear();
    errMsg = null;
    jobRunning = false;
    cancelJob = false;
    checkNR = false;
    totSeq = 0;
    lastID = "?";
  }

  public void setCheckNR(boolean check) {
    checkNR = check;
  }

  public String getErrMsg() {
    return errMsg;
  }

  public void setErrMsg(String errMsg) {
    this.errMsg = errMsg;
  }

  /**
   * Indicates thet the processing is going to start. Callee may return false to
   * indicate that caller should not continue.
   */
  public boolean setJobRunnig(boolean val) {
    jobRunning = val;
    boolean bRet = true;
    if (val) {
      bRet = prepareSeqIdChecker();
    } else {
      closeSeqIdChecker();
    }
    return bRet;
  }

  public boolean isJobRunning() {
    return jobRunning;
  }

  public void setTxtCompo(JTextComponent txtCompo) {
    this.txtCompo = txtCompo;
  }

  public void setTxtMessage(String msg) {
    if (txtCompo != null)
      txtCompo.setText(msg);
  }

  public String getLastID() {
    return lastID;
  }

  public abstract void jobDone(boolean success);

  private boolean prepareSeqIdChecker() {
    File f = null;
    if (!checkNR) {
      return true;
    }
    if (_bInit)// this is to ensure that _seqIdDetector is created only one
               // times
      return true;
    _bInit = true;
    try {
      f = File.createTempFile("FDBseqIds", ".ldx");
      // f is a file, whereas KDMSUniqueSeqIdDetector will f to create a
      // directory
      f.delete();
    } catch (IOException e) {
      LoggerCentral.error(LOGGER,
          "unable to create UniqueSeqId: " + e.toString());
    }
    if (f != null) {
      _seqIdDetector = new DBMSUniqueSeqIdDetector(f.getAbsolutePath());
    } else {
      _seqIdDetector = null;
    }
    return (_seqIdDetector != null);
  }

  private void closeSeqIdChecker() {
    if (_seqIdDetector == null)
      return;
    // ensure that remaining seqIds are saved in the index
    _seqIdDetector.dumpContent();
    // clean content
    _seqIdDetector.reset();
    if (_seqIdDetector.hasBeenUsed()) {
      LoggerCentral.info(LOGGER, "FormatDB/UniqueSeqId: total working time: "
          + (_seqIdDetector.getWorkingTime() / 1000l) + " s.");
    }
    // close index before deletion
    try {
      _seqIdDetector.closeIndex();
    } catch (Exception ex) {// not bad, so avoid to stop installation here
                            // do not log error with a warn : this is not a bad
                            // error,
                            // so we do not want to stop the overall indexing
                            // process
      LoggerCentral.info(LOGGER,
          "unable to close index (FormatDB/UniqueSeqId): " + ex);
    }
    // remove temporary index
    if (!PAntTasks.deleteDirectory(_seqIdDetector.getIndexPath())) {
      LOGGER.warn("unable to delete UniqueSeqId index: "
          + _seqIdDetector.getIndexPath());
    }
  }

  /**
   * Indicates that the processor has just started the conversion.
   */
  public void startProcessing() {

  }

  /**
   * Indicates that the processor has just stop the conversion.
   * 
   * @param time
   *          the time required to process the conversion.
   */
  public void stopProcessing(long time) {

  }

  /**
   * Can be used to force the processor ending the conversion.
   */
  public boolean interruptProcessing() {
    return cancelJob;
  }

  /**
   * Returns the total number of sequences found during formatdb processing.
   */
  public int getTotalSequences() {
    return totSeq;
  }

  public void seqFound(String seqID) {
    totSeq++;
    lastID = seqID;
    if (LoggerCentral.processAborted()) {
      throw new PTaskEngineAbortException();
    }

    if (!checkNR || _seqIdDetector == null)
      return;

    if (!_seqIdDetector.add(seqID)) {
      totSeq--;
      throw new DBMSUniqueSeqIdRedundantException("redundant sequence ID: "
          + seqID);
    }
  }
}