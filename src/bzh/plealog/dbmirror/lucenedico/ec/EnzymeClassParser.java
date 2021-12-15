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
package bzh.plealog.dbmirror.lucenedico.ec;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

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
 * This class defines a parser for Enzyme Commission data file. Reference:
 * ftp://ftp.expasy.org/databases/enzyme/, get file: enzclass.txt
 * 
 * @author Patrick G. Durand
 */
public class EnzymeClassParser implements DicoParsable {

  private int              _entries;
  private boolean          _verbose;
  private ParserMonitor    _pMonitor;

  private static final Logger LOGGER = LogManager.getLogger(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                          + ".EnzymeClassParser");

  public EnzymeClassParser() {
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

  private String processName(String line, int start) {
    int idx = line.indexOf('.', start);// remove terminal . if any
    if (idx != -1)
      return line.substring(start, idx).trim();
    else
      return line.substring(start).trim();
  }

  public static String[] getIds(String data) {
    StringBuffer buf1, buf2;
    StringTokenizer tokenizer;
    String token;

    buf1 = new StringBuffer();
    buf2 = new StringBuffer();
    tokenizer = new StringTokenizer(data, ".");

    if (tokenizer.hasMoreTokens() == false)
      return null;
    // 1.
    token = tokenizer.nextToken().trim();
    buf1.append(token);
    buf2.append(token);
    // 1.1.
    if (tokenizer.hasMoreTokens() == false)
      return new String[] { buf1.toString(), buf2.toString() };
    buf1.append(".");
    token = tokenizer.nextToken().trim();
    buf1.append(token);
    if (token.equals("-") == false) {
      buf2.append(".");
      buf2.append(token);
    }
    // 1.1.1.
    if (tokenizer.hasMoreTokens() == false)
      return new String[] { buf1.toString(), buf2.toString() };
    buf1.append(".");
    token = tokenizer.nextToken().trim();
    buf1.append(token);
    if (token.equals("-") == false) {
      buf2.append(".");
      buf2.append(token);
    }
    // 1.1.1.1.
    if (tokenizer.hasMoreTokens() == false)
      return new String[] { buf1.toString(), buf2.toString() };
    buf1.append(".");
    token = tokenizer.nextToken().trim();
    buf1.append(token);
    if (token.equals("-") == false) {
      buf2.append(".");
      buf2.append(token);
    }
    return new String[] { buf1.toString(), buf2.toString() };
  }

  /**
   * Implementation of DicoParsable interface.
   */
  public void parse(String file, DicoStorageSystem ss)
      throws DicoParserException {
    BufferedReader reader = null;
    String line;
    String id, name;
    String[] ids;
    int endOfLineSize, idx;
    long curPos = 0;

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
        idx = line.indexOf(".- ");
        if (idx == -1 || line.length() < 1
            || !Character.isDigit(line.charAt(0)))
          continue;
        id = line.substring(0, idx + 2);
        name = processName(line, idx + 3);
        // id: remove spaces and enter in index two ids: with and without -
        ids = getIds(id);

        if (_pMonitor != null) {
          _pMonitor.seqFound(ids[0], name, file, curPos, curPos, false);
        }
        if (ss != null) {
          ss.addEntry(ids[0], name);
          ss.addEntry(ids[1], name);
        }
        if (_verbose)
          System.out.println(ids[0] + "/" + ids[1] + "," + name);
        _entries++;
        if (_verbose && (_entries % 10000) == 0) {
          System.out.println(_entries + ", current term: " + ids[0] + "/"
              + ids[1] + "," + name);
        }
        id = name = null;
        curPos += (long) (line.length() + endOfLineSize);
      }

    } catch (Exception e) {
      String msg = "Error while parsing Enzyme class no. " + (_entries + 1);
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
