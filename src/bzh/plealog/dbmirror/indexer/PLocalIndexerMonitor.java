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
package bzh.plealog.dbmirror.indexer;

import javax.swing.text.JTextComponent;

public abstract class PLocalIndexerMonitor implements ParserMonitor {
  private boolean        jobRunning;
  private String         errMsg;
  private JTextComponent txtCompo;

  public PLocalIndexerMonitor() {

  }

  public PLocalIndexerMonitor(JTextComponent txtCompo) {
    setTxtCompo(txtCompo);
  }

  public void reset() {
    errMsg = null;
    jobRunning = false;
  }

  public String getErrMsg() {
    return errMsg;
  }

  public void setErrMsg(String errMsg) {
    this.errMsg = errMsg;
  }

  public void setJobRunnig(boolean val) {
    jobRunning = val;
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

  public abstract void jobDone(boolean success);

}
