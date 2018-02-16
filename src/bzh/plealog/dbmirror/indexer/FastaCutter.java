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
package bzh.plealog.dbmirror.indexer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * This class defines a Fasta file cutter. Cutter means that this class parses a
 * Fasta files and only retains entries for which sequence entry number is
 * included within a range of entry numbers.
 * 
 * Use SequenceFileManager instead
 * 
 * @author Patrick G. Durand
 */

@Deprecated
/*
 * see SequenceFileManager in package util.sequence for a more generic method.
 */
public class FastaCutter {

  private int              _entries;
  private ParserMonitor    _pMonitor;

  private static final Log LOGGER = LogFactory
                                      .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                          + ".FastaCutter");

  public FastaCutter() {
  }

  /**
   * Provide a monitor to this parser.
   */
  public void setParserMonitor(ParserMonitor pm) {
    _pMonitor = pm;
  }

  private void handleData(String idLine, String fName) {
    String desc, id;

    int idx = idLine.indexOf(' ');
    if (idx == -1) {
      id = idLine;
      desc = "";
    } else {
      id = idLine.substring(1, idx);
      desc = idLine.substring(idx + 1);
    }

    if (_pMonitor != null) {
      _pMonitor.seqFound(id, desc, fName, -1, -1, false);
    }
    _entries++;
  }

  /**
   * Parse a Fasta file and retains all entries for which sequence entry number
   * is included within a range of entry numbers.
   * 
   * @param file
   *          the file to parse
   * @param os
   *          where to write entries for which location in file is between from
   *          and to (inclusive)
   * @param from
   *          starting index of the range
   * @param to
   *          ending index of the range
   */
  public void parse(String file, OutputStream os, int from, int to)
      throws DBParserException {
    BufferedReader reader = null;
    BufferedWriter writer = null;
    String line, fName;
    int curIdx = -1;
    boolean readseq = false;

    try {
      _entries = 0;
      reader = new BufferedReader(new InputStreamReader(new FileInputStream(
          file), "UTF-8"));
      fName = file;
      if (_pMonitor != null) {
        _pMonitor.startProcessingFile(fName, new File(file).length());
      }
      writer = new BufferedWriter(new OutputStreamWriter(os));
      while ((line = reader.readLine()) != null) {
        if (line.startsWith(">")) {
          curIdx++;
          if (curIdx >= from && curIdx <= to) {
            handleData(line, fName);
            writer.write(line);
            writer.write("\n");
            readseq = true;
          } else {
            readseq = false;
          }
        } else if (readseq) {
          writer.write(line);
          writer.write("\n");
        }
      }
    } catch (Exception e) {
      String msg = "Error while parsing Fasta entry no. " + (_entries + 1);
      LOGGER.warn(msg + ": " + e);
      throw new DBParserException(msg + ": " + e.getMessage());
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
        }
      }
      if (writer != null) {
        try {
          writer.flush();
        } catch (IOException e) {
        }
      }
      if (_pMonitor != null) {
        _pMonitor.stopProcessingFile(file, _entries);
      }
    }
    LoggerCentral.info(LOGGER, new File(file).getName() + ": " + _entries
        + " entries retained");
  }

  /**
   * Number of entries retained in the parsed Fasta file.
   */
  public int getEntries() {
    return _entries;
  }
}
