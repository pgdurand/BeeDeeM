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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import bzh.plealog.dbmirror.indexer.BOLDParser;
import bzh.plealog.dbmirror.indexer.ParserMonitor;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * A task capable of converting a BOLD file to a Genbank one.
 * 
 * @author Patrick G. Durand
 */
public class PTaskBold2Genbank extends PAbstractTask implements PParserTask {

  private String           _src;
  private String           _errMsg;
  private ParserMonitor    _monitor;

  private static final Logger LOGGER = LogManager.getLogger(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                          + ".PTaskEngine");

  public PTaskBold2Genbank(String srcFile) {
    _src = srcFile;
  }

  public String getSrc() {
    return this._src;
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getName() {
    return "Bold2Genbank";
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getUserFriendlyName() {
    return "converting BOLD to Genbank file";
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
    BOLDParser gp;
    String idxName;

    if (_src == null) {
      _errMsg = "source file is unknown";
      return false;
    }

    LoggerCentral.info(LOGGER, getName() + " started on " + _src);

    // check whether index already exists
    idxName = _src + BOLDParser.GB_FILE_EXT;
    if (new File(idxName).exists()) {
      LoggerCentral.info(LOGGER, getName() + ": " + idxName
          + ": file skipped: arleady exists");
      return true;
    }
    try {
      gp = new BOLDParser();
      gp.setParserMonitor(_monitor);
      gp.parse(_src, null);
      // if index is empty: ok, but remove it. This has been introduced to
      // enable
      // a clean resume of file processing, especially when playing with
      // constraints
      // based sequence subsets (taxonomy, etc).
      if (gp.getEntries() == 0) {
        LoggerCentral.info(LOGGER, getName() + ": " + idxName
            + ": file removed: empty");
        new File(idxName).delete();
      }
    } catch (Exception ex) {
      _errMsg = " conversion of " + _src + " failed: " + ex;
      return false;
    }
    return true;
  }

  public void setParameters(String params) {
    // do not add optional "checkNR" parameter. This parser must always do that.
  }

  public void setParserMonitor(ParserMonitor pm) {
    _monitor = pm;
  }

}
