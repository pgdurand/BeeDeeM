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
package bzh.plealog.dbmirror.fetcher;

import java.io.File;
import java.util.ArrayList;

import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * This class copy all file found previously by the FileListLoader class to a
 * directory.
 * 
 * @author Patrick G. Durand
 * 
 */
public class PLocalLoaderEngine extends LoaderEngine {

  private String          _destDir;
  private ArrayList<File> _files;
  private PLocalLoader    _kloader;

  /**
   * The constructor
   * 
   * @param dbsc
   *          the DBServerConfig containing the descriptor file
   * @param files
   *          the files which must be copied
   * @param destDir
   *          the path to the directory where the files must be copied
   * @param monitor
   *          the task monitor
   */
  public PLocalLoaderEngine(DBServerConfig dbsc, ArrayList<File> files,
      String destDir, LoaderMonitor monitor) {
    super(dbsc, monitor);
    _kloader = new PLocalLoader(dbsc);
    _files = files;
    _destDir = destDir;
  }

  public void setUserProcessingMonitor(UserProcessingMonitor userMonitor) {
    _kloader.setUserProcessingMonitor(userMonitor);
  }

  /**
   * Effected the copy
   */
  public void run() {
    int ret;

    ret = _kloader.copyFiles(_files, _destDir, _monitor);
    if (ret == 3) {
      LoggerCentral.abortProcess();
      _monitor.doneLoading("", LoaderMonitor.STATUS_ABORTED);
    }
    set_ok(ret == 1);
  }

}
