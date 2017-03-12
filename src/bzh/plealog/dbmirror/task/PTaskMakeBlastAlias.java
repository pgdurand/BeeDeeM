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
package bzh.plealog.dbmirror.task;

import java.io.File;
import java.io.PrintWriter;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * A task capable of preparing a Blast alias files.
 * 
 * @author Patrick G. Durand
 */
public class PTaskMakeBlastAlias extends PAbstractTask {

  private String              _dbPathName;
  private String              _errMsg;
  private boolean             _isNucleic;
  private boolean             _useFullPath;

  private static final String USE_FULL_PATH = "fullPath";

  private static final Log    LOGGER        = LogFactory
                                                .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                    + ".PTaskEngine");

  public PTaskMakeBlastAlias(String dbPathName, boolean isNucleic) {
    _isNucleic = isNucleic;
    _dbPathName = dbPathName;
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getName() {
    return "Make Blast DB alias";
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getUserFriendlyName() {
    return "preparing Blast databank alias";
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getErrorMsg() {
    return _errMsg;
  }

  private boolean prepareAliasFile(String path, String dbName, boolean isNucleic) {
    String fExt1, fExt2, fExt3;
    File[] files;
    File f;
    PrintWriter writer = null;
    String fName;
    boolean bRet = false, bWrite = false;
    int i, pos;

    fExt1 = (isNucleic ? ".nin" : ".pin");
    fExt2 = (isNucleic ? ".nal" : ".pal");
    fExt3 = ".msk";// Blast DBs relying on other Blast DBs

    try {
      writer = new PrintWriter(path + fExt2);
      writer.print("TITLE ");
      writer.println(dbName);
      writer.print("DBLIST ");
      files = new File(path).getParentFile().listFiles();
      for (i = 0; i < files.length; i++) {
        f = files[i];
        if (!f.isFile())
          continue;
        if (_useFullPath) {
          fName = f.getAbsolutePath();
          fName = fName.replace(DBMSAbstractConfig.DOWNLOADING_DIR, "current");
        } else {
          fName = f.getName();
        }
        pos = fName.indexOf(fExt1);// standard blast file
        if (pos >= 0) {
          writer.print(fName.substring(0, pos) + " ");
          bWrite = true;
        } else {// special handling of ".msk" files
          pos = fName.indexOf(fExt3);
          if (pos >= 0) {
            writer.print(fName.substring(0, pos) + " ");
            bWrite = true;
          }
        }
      }
      writer.println();
      writer.flush();
      writer.close();
      if (!bWrite) {
        throw new Exception("unable to find " + fExt1
            + " or .msk files to prepare Blast DB alias");
      }
      bRet = true;
    } catch (Exception e) {
      _errMsg = "unable to create alias file: " + e;
    } finally {
      IOUtils.closeQuietly(writer);
    }
    return bRet;
  }

  /**
   * Implementation of KLTask interface.
   */
  public boolean execute() {

    LoggerCentral.info(LOGGER,
        getName() + " for " + new File(_dbPathName).getName());

    return prepareAliasFile(_dbPathName, new File(_dbPathName).getName(),
        _isNucleic);
  }

  public void setParameters(String params) {
    Map<String, String> args;
    String value;

    if (params == null)
      return;

    args = Utils.getTaskArguments(params);

    // make alias task accepts an optional argument that can force the wrtite of
    // full path name
    // within the alias file
    value = args.get(USE_FULL_PATH);
    if (value != null)
      _useFullPath = Boolean.TRUE.toString().equals(value);
  }

}
