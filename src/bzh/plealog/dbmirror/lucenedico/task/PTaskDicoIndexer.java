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
package bzh.plealog.dbmirror.lucenedico.task;

import java.io.File;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.indexer.LuceneUtils;
import bzh.plealog.dbmirror.indexer.ParserMonitor;
import bzh.plealog.dbmirror.lucenedico.DicoParsable;
import bzh.plealog.dbmirror.lucenedico.DicoStorageSystem;
import bzh.plealog.dbmirror.lucenedico.DicoStorageSystemImplem;
import bzh.plealog.dbmirror.lucenedico.Dicos;
import bzh.plealog.dbmirror.task.PAbstractTask;
import bzh.plealog.dbmirror.task.PParserTask;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.ant.PAntTasks;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * A task capable of indexing a database file containing some dictionary. For
 * now, GeneOnotology Terms, InterPro names and NCBI Taxonomy Scientific Names
 * can be indexed with this class.
 * 
 * @author Patrick G. Durand
 */
public class PTaskDicoIndexer extends PAbstractTask implements PParserTask {
  private ParserMonitor      _monitor;
  private String             _src;
  private String             _errMsg;
  private String             _dicoType;
  private String             _dataFile;

  private static final Log   LOGGER    = LogFactory
                                           .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                               + ".PTaskEngine");
  public static final String DICO_TYPE = "type";
  public static final String FILE_NAME = "file";

  public PTaskDicoIndexer(String srcFile) {
    _src = srcFile;
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getName() {
    return "KLTaskDicoIndexer";
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getUserFriendlyName() {
    return "indexing term file";
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getErrorMsg() {
    return _errMsg;
  }

  public void setParameters(String params) {
    // first parameter is mandatory : dico type
    // error will be handled during execute()
    Map<String, String> args;
    String value;

    if (params == null)
      return;

    args = Utils.getTaskArguments(params);
    value = args.get(DICO_TYPE);
    if (value != null)
      _dicoType = value;
    value = args.get(FILE_NAME);
    if (value != null)
      _dataFile = value;
  }

  /**
   * Implementation of KLTask interface.
   */
  public boolean execute() {
    DicoStorageSystemImplem lss = null;
    DicoParsable dicoParser;
    String src, idxName;

    if (_src == null) {
      _errMsg = "source file is unknown";
      return false;
    }

    if (_dataFile != null) {
      src = new File(_src).getParent();
      if (src == null) {// no parent
        src = _dataFile;
      } else {
        src += File.separator;
        src += _dataFile;
      }
    } else {
      src = _src;
    }

    LoggerCentral.info(LOGGER, getName() + " for " + _dicoType + " started on "
        + src);

    // check whether index already exists
    idxName = src + LuceneUtils.DIR_OK_FEXT;
    if (new File(idxName).exists()) {
      LoggerCentral.info(LOGGER, getName() + ": " + idxName
          + ": index skipped: arleady exists");
      return true;
    }
    try {
      // check whether tmp index exists
      idxName = src + LuceneUtils.DIR_TMP_FEXT;
      if (new File(idxName).exists()) {
        if (!PAntTasks.deleteDirectory(idxName)) {
          throw new Exception("unable to delete old index: " + idxName);
        }
      }
      dicoParser = Dicos.getParser(_dicoType);
      if (dicoParser == null) {
        throw new Exception("dictionary type unknown: " + _dicoType);
      }
      dicoParser.setParserMonitor(_monitor);
      lss = new DicoStorageSystemImplem();
      lss.open(idxName, DicoStorageSystem.WRITE_MODE);
      dicoParser.parse(src, lss);
      lss.close();
      lss = null;
      // index successfully created
      if (!PAntTasks.movefile(idxName, src + LuceneUtils.DIR_OK_FEXT)) {
        throw new Exception("unable to rename index: " + idxName);
      }
    } catch (Exception ex) {
      if (lss != null)
        lss.close();
      _errMsg = " indexing of " + src + " failed: " + ex;
      return false;
    }
    return true;
  }

  public void setParserMonitor(ParserMonitor pm) {
    _monitor = pm;
  }
}
