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

import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * This class research a list of files in directories. These files must be into
 * the computer.
 * 
 * @author Patrick G. Durand
 * 
 */
public class PLocalFileListLoader extends FileListLoader {

  private PLocalLoader    _kLoader;
  private ArrayList<File> _file;

  /**
   * Constructor of the class
   * 
   * @param dbsc
   *          The DBServerConfig containing the descriptor files
   * @param files
   *          the list of the files which has been found in the directories
   */
  public PLocalFileListLoader(DBServerConfig dbsc, ArrayList<File> files) {
    super(dbsc);
    _kLoader = new PLocalLoader(dbsc);
    _file = files;
  }

  public void setUserProcessingMonitor(UserProcessingMonitor userMonitor) {
    _kLoader.setUserProcessingMonitor(userMonitor);
  }

  /**
   * Done the exploration
   */
  public void run() {
    // return true if the method has found files in the directories
    set_ok(_kLoader.initFilesList(_file));

    if (!listingOk()) {// failure? Report error now!
      LoggerCentral.error(
          LogFactory.getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
              + ".PLocalLoader"),
          "An error has been generated during the file research.");// TODO
                                                                   // changer
                                                                   // l'erreur
    }
  }

  /**
   * KLocalLoader Getter
   * 
   * @return the KLocalLoader use to explore directories
   */
  protected PLocalLoader get_kLoader() {
    return _kLoader;
  }

  /**
   * KLocalLoader Setter
   * 
   * @param kLoader
   *          the KLocalLoader use to explore directories
   */
  protected void set_kLoader(PLocalLoader kLoader) {
    this._kLoader = kLoader;
  }

  /**
   * ArrayList<File> Getter
   * 
   * @return the list used to store files found in the directory
   */
  protected ArrayList<File> get_file() {
    return _file;
  }

  /**
   * ArrayList<File> Setter
   * 
   * @param file
   *          the list used to store files found in the directory
   */
  protected void set_file(ArrayList<File> file) {
    this._file = file;
  }
}
