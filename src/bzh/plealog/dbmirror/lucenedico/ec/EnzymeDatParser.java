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
package bzh.plealog.dbmirror.lucenedico.ec;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.indexer.ParserMonitor;
import bzh.plealog.dbmirror.lucenedico.DicoParsable;
import bzh.plealog.dbmirror.lucenedico.DicoParserException;
import bzh.plealog.dbmirror.lucenedico.DicoStorageSystem;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * This class defines a parser for Enzyme Commission data file. Reference:
 * ftp://ftp.expasy.org/databases/enzyme/, get file: enzyme.dat
 * 
 * @author Patrick G. Durand
 */
public class EnzymeDatParser implements DicoParsable {

  private int              _entries;
  private boolean          _verbose;
  private ParserMonitor    _pMonitor;

  private static final Log LOGGER = LogFactory
                                      .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                          + ".EnzymeDatParser");

  public EnzymeDatParser() {
  }

  /**
   * Implementation of DicoParsable interface.
   */
  public void setVerbose(boolean verbose) {
    _verbose = verbose;
  }

  /**
   * Implementation of DicoParsable interface.
   */
  public void setParserMonitor(ParserMonitor pm) {
    _pMonitor = pm;
  }

  private String processName(String line) {
    int idx = line.indexOf('.');// remove terminal . if any
    if (idx != -1)
      return line.substring(2, idx).trim();
    else
      return line.substring(2).trim();
  }

  /**
   * Implementation of DicoParsable interface.
   */
  public void parse(String file, DicoStorageSystem ss)
      throws DicoParserException {
    BufferedReader reader = null;
    String line;
    String id, name;
    int endOfLineSize;
    long curPos = 0;

    try {
      endOfLineSize = Utils.getLineTerminatorSize(file);
      _entries = 0;
      reader = new BufferedReader(new InputStreamReader(new FileInputStream(
          file), "UTF-8"));
      if (_pMonitor != null) {
        _pMonitor.startProcessingFile(file, new File(file).length());
      }
      id = name = null;
      while ((line = reader.readLine()) != null) {
        /*
         * if (_entries>5) break;
         */
        if (line.startsWith("ID")) {
          id = line.substring(2).trim();
        } else if (line.startsWith("DE")) {
          // first time we find DE : init name
          if (name == null) {
            name = processName(line);
          } else {// continue name;
            // code not optimal, but we dont have lots of this situation in EC
            // file
            name += (" " + processName(line));
          }
        } else if (line.startsWith("//") && id != null && name != null) {
          if (_pMonitor != null) {
            _pMonitor.seqFound(id, name, file, curPos, curPos, false);
          }
          if (ss != null) {
            ss.addEntry(id, name);
          }
          /*
           * if (_verbose) System.out.println(id+","+name);
           */
          _entries++;
          if (_verbose && (_entries % 10000) == 0) {
            System.out.println(_entries + ", current term: " + id + "," + name);
          }
          id = name = null;
        }
        curPos += (long) (line.length() + endOfLineSize);
      }

    } catch (Exception e) {
      String msg = "Error while parsing Enzyme entry no. " + (_entries + 1);
      LOGGER.warn(msg + ": " + e);
      throw new DicoParserException(msg + ": " + e.getMessage());
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
        }
      }
      if (_pMonitor != null) {
        _pMonitor.stopProcessingFile(file, _entries);
      }
    }
    if (_entries == 0)
      throw new DicoParserException("Data file does not contain any terms.");
    LoggerCentral.info(LOGGER, file + " contains " + _entries + " entries");
  }

  /**
   * Implementation of DicoParsable interface.
   */
  public int getTerms() {
    return _entries;
  }
}
