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
package bzh.plealog.dbmirror.lucenedico.pfam;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import bzh.plealog.dbmirror.indexer.ParserMonitor;
import bzh.plealog.dbmirror.lucenedico.DicoParsable;
import bzh.plealog.dbmirror.lucenedico.DicoParserException;
import bzh.plealog.dbmirror.lucenedico.DicoStorageSystem;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * This class defines a parser for Pfam data file. Reference:
 * ftp://ftp.sanger.ac.uk/pub/databases/Pfam/current_release/database_files/,
 * get the file: pfamA.txt.gz
 * 
 * @author Patrick G. Durand
 */
public class PfamNamesParser implements DicoParsable {

  private int              _entries;
  private boolean          _verbose;
  private ParserMonitor    _pMonitor;

  private static final Logger LOGGER = LogManager.getLogger(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                          + ".PfamNamesParser");

  public PfamNamesParser() {
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

  private String getDataWithoutQuotes(String data, int delta) {
    int idx1, idx2;

    idx1 = data.indexOf('\'', delta);
    if (idx1 == -1)
      return null;
    idx2 = data.indexOf('\'', idx1 + 1);
    if (idx2 == -1)
      return null;
    return data.substring(idx1 + 1, idx2);
  }

  /**
   * Get data if there are no quotes
   * 
   * @param data
   *          line of the file parsed
   * @param delta
   *          index where the parser look
   * @return the data contains between two '\t'
   */
  private String getData(String data, int delta) {
    int idx1, idx2;

    idx1 = data.indexOf('\t', delta);
    if (idx1 == -1)
      return null;
    idx2 = data.indexOf('\t', idx1 + 1);
    if (idx2 == -1)
      return null;
    return data.substring(idx1 + 1, idx2);
  }

  /**
   * Implementation of DicoParsable interface.
   */
  public void parse(String file, DicoStorageSystem ss)
      throws DicoParserException {
    BufferedReader reader = null;
    String line;
    String id, name;
    int endOfLineSize, idx;
    long curPos = 0;
    boolean withQuotes = true;

    try {
      endOfLineSize = Utils.getLineTerminatorSize(file);
      _entries = 0;
      reader = new BufferedReader(new InputStreamReader(new FileInputStream(
          file), "UTF-8"));
      if (_pMonitor != null) {
        _pMonitor.startProcessingFile(file, new File(file).length());
      }
      while ((line = reader.readLine()) != null) {
        /*
         * if (_entries>5) break;
         */
        idx = line.indexOf('\t');
        if (idx == -1)
          continue;
        // skip line number and get Pfam ID
        if (line.startsWith("'")) {
          id = getDataWithoutQuotes(line, idx + 1);
          withQuotes = true;
        } else {
          id = getData(line, idx);
          withQuotes = false;
        }
        // jump over ID, and two short names to get full name
        idx = line.indexOf('\t', idx + 1);
        if (idx == -1)
          continue;
        idx = line.indexOf('\t', idx + 1);
        if (idx == -1)
          continue;
        idx = line.indexOf('\t', idx + 1);
        if (idx == -1)
          continue;
        if (withQuotes) {
          name = getDataWithoutQuotes(line, idx + 1);
        } else {
          name = getData(line, idx);
        }
        if (id == null || name == null) {
          continue;
        }
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
        curPos += (long) (line.length() + endOfLineSize);
      }

    } catch (Exception e) {
      String msg = "Error while parsing Pfam entry no. " + (_entries + 1);
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
