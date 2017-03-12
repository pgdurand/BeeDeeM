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
package bzh.plealog.dbmirror.util.descriptor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.JComponent;

import org.apache.commons.lang.StringUtils;

import bzh.plealog.dbmirror.fetcher.DBServerConfig;
import bzh.plealog.dbmirror.task.PTask;

public class DescriptorEntry implements Comparable<DescriptorEntry> {
  private DBServerConfig _descriptor;
  private String         _file;
  private STATUS         _status           = STATUS.unknown;
  private JComponent     _holder;

  // indicates that the installation process order if a bank must be installed
  // before another one
  // the install process will install the higher ones before
  private Integer        installationOrder = 0;

  public enum STATUS {
    unknown, waiting, running, error, ok
  }

  public DescriptorEntry(DescriptorEntry srcEntry) {
    this._descriptor = srcEntry.getDescriptor();
    this._file = srcEntry.getFile();
    this._status = srcEntry.getStatus();
    this._holder = srcEntry.getHolder();
  }

  public DescriptorEntry(DBServerConfig descriptor, String file) {
    super();
    this._descriptor = descriptor;
    this._file = file;
  }

  /**
   * A simple init using an existed dsc file
   * 
   * @param dscFile
   * @throws IOException
   * @throws FileNotFoundException
   */
  public static DescriptorEntry createFrom(File dscFile)
      throws FileNotFoundException, IOException {
    DBServerConfig desc = new DBServerConfig();
    desc.load(dscFile.getAbsolutePath());
    return new DescriptorEntry(desc, dscFile.getAbsolutePath());
  }

  public void setHolder(JComponent holder) {
    _holder = holder;
  }

  public JComponent getHolder() {
    return _holder;
  }

  public String getFile() {
    return _file;
  }

  public DBServerConfig getDescriptor() {
    return _descriptor;
  }

  public void setDescriptor(DBServerConfig descriptor) {
    _descriptor = descriptor;
  }

  public String getName() {
    return _descriptor.getName();
  }

  public String getDescription() {
    return _descriptor.getDescription();
  }

  public STATUS getStatus() {
    return _status;
  }

  public void setStatus(STATUS status) {
    this._status = status;
    if (_holder != null) {
      _holder.repaint();
    }
  }

  public String toString() {
    if (_status == STATUS.unknown)
      return getName();
    else
      return getName() + ":" + _status;
  }

  public boolean isDictionary() {
    if (this.getDescriptor().isDictionary()) {
      // check indexing tasks
      String tasks = this.getDescriptor().getUnitPostTasks().toLowerCase();
      tasks += " " + this.getDescriptor().getGlobalPostTasks().toLowerCase();

      if ((tasks.contains(PTask.TASK_U_SW_IDX))
          || (tasks.contains(PTask.TASK_U_GP_IDX))
          || (tasks.contains(PTask.TASK_U_EM_IDX))
          || (tasks.contains(PTask.TASK_U_NOG_IDX))
          || (tasks.contains(PTask.TASK_U_FAS_IDX))
          || (tasks.contains(PTask.TASK_U_GB_IDX))) {
        return false;
      }

      int nbIdxDico = StringUtils.countMatches(tasks, PTask.TASK_U_DICO_IDX);
      return nbIdxDico > 0; // at least one dico indexing task

    }

    return false;
  }

  public Integer getInstallationOrder() {
    return installationOrder;
  }

  public void setInstallationOrder(Integer installationOrder) {
    this.installationOrder = installationOrder;
  }

  @Override
  public int compareTo(DescriptorEntry o) {
    return this.installationOrder.compareTo(o.getInstallationOrder());
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof DescriptorEntry) {
      return this.getDescriptor().getName()
          .equalsIgnoreCase(((DescriptorEntry) o).getDescriptor().getName());
    }
    return super.equals(o);
  }
}