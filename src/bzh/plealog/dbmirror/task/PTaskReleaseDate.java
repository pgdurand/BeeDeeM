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
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;
import bzh.plealog.dbmirror.util.runner.DBStampProperties;

/**
 * A task capable of getting bank release date.
 * 
 * @author Patrick G. Durand
 */
public class PTaskReleaseDate extends PAbstractTask {

  private String           _dir;
  private String           _file;
  private String           _errMsg;

  private static final Logger LOGGER = LogManager.getLogger(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                          + ".PTaskEngine");
  
  /**
   * Constructor.
   * 
   * @param dir directory containing an installed bank. Release date is retrieved
   * by getting time stamp of the oldest file contained in the directory.
   */
  public PTaskReleaseDate(String dir) {
    _dir = dir;
  }

  /**
   * Constructor.
   * 
   * @param dir directory containing an installed bank.
   * @param fileName a databank file name. This file is supposed to be located
   * in directory denoted by parameter dir. Release date is retrieved by 
   * getting the time stamp of that file.
   */
  public PTaskReleaseDate(String dir, String fileName) {
    this(dir);
    _file = fileName;
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getName() {
    return "releaseDate";
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getUserFriendlyName() {
    return "getting bank release date";
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
    // directory is mandatory
    if (_dir == null) {
      _errMsg = "directory is unknown";
      return false;
    }
    
    Date d;
    
    // get bank time stamp
    if (_file!=null){
      File f = new File(Utils.terminatePath(_dir)+_file);
      LoggerCentral
        .info(LOGGER, getUserFriendlyName() + " from: " + f.getAbsolutePath());
      d = new Date(f.lastModified());
    }
    else{
      LoggerCentral
      .info(LOGGER, getUserFriendlyName() + " from: " + _dir);
      d = Utils.getOldestFile(_dir);
    }
    
    //save time stamp in an appropriate file
    if (!DBStampProperties.writeReleaseDate(_dir, d)){
      _errMsg = "unable to save bank release date";
      return false;
    }
    return true;
  }

  public void setParameters(String params) {
  }

}
