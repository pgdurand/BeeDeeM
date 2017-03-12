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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;
import bzh.plealog.dbmirror.util.runner.DBMSUniqueSeqIdRedundantException;

/**
 * This class defines a parser for Fasta database files. It is worth noting that
 * this parser will set a local id to each sequence reported in a fasta file.
 * Such an id is made of the value of LCl_PREFIX_ID to which is added a
 * incremented number starting from zero.
 * 
 * @author Patrick G. Durand
 */
public class FastaParser implements DBParsable {

  private int                _entries;
  private boolean            _verbose;
  private boolean            _warnError;
  private ParserMonitor      _pMonitor;
  private boolean            _checkNR         = false;
  private boolean            _generateLocalID = false;

  public static final String LCl_PREFIX_ID    = "kd_i";
  public static final String LENGTH_PREFIX    = " [Length=";
  public static final String LENGTH_SUFFIX    = "]";

  private static final Log   LOGGER           = LogFactory
                                                  .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                      + ".FastaParser");

  public FastaParser() {
  }

  public FastaParser(boolean generateLocalID) {
    _generateLocalID = generateLocalID;
  }

  /**
   * Implementation of DBParsable interface.
   */
  public void setVerbose(boolean verbose) {
    _verbose = verbose;
  }

  /**
   * Figures out whether or not this parser should check for sequence ID
   * redundancy. Default behavior is false.
   */
  public void setCheckSeqIdRedundancy(boolean checkNR) {
    _checkNR = checkNR;
  }

  /**
   * When a Fasta entry is not valid one can choose to notify this parser to
   * avoid raising an error. For that purpose, pass true. Default is false, i.e.
   * the parser will raise an exception if the Fasta file is not valid.
   */
  public void setWarnError(boolean warn) {
    _warnError = warn;
  }

  /**
   * Implementation of DBParsable interface.
   */
  public void setParserMonitor(ParserMonitor pm) {
    _pMonitor = pm;
  }

  private void handleData(StorageSystem ss, String idLine, String fName,
      int seqLen, long start, long curPos) {
    String data, lclId, desc, id;

    // skip the '>' which has to be the first char of a Fasta header
    // then use trim() since some sequences contains space chars between ">" and
    // the SeqID
    data = idLine.substring(idLine.indexOf(">") + 1).trim();
    int idx = data.indexOf(' ');
    if (idx == -1)
      idx = data.length();
    id = data.substring(0, idx);

    if (seqLen == 0) {
      if (_warnError) {
        LoggerCentral.info(LOGGER, "Skip Fasta entry no. " + (_entries + 1)
            + ": no sequence");
        return;
      } else {
        throw new StorageSystemException("no sequence provided for entry " + id);
      }
    }
    // JIRA KDMS-20 no use of the desc field anymore
    desc = "";
    if (_generateLocalID) {
      lclId = LCl_PREFIX_ID + _entries;
    } else {
      lclId = id;
    }

    try {
      if (_pMonitor != null) {
        _pMonitor.seqFound(lclId, desc, fName, start, curPos, _checkNR);
      }
      if (ss != null) {
        ss.addEntry(lclId, desc, fName, start, curPos);
      }
      if (_verbose)
        System.out.println(lclId + "," + desc + "," + start + "," + (curPos));
      _entries++;
      if (_verbose && (_entries % 10000) == 0) {
        System.out.println(_entries + ", start: " + start);
      }
    } catch (DBMSUniqueSeqIdRedundantException ex) {
      // JIRA KDMS-22 accept a redundant sequence, just add a log
      LoggerCentral.warn(LOGGER, "Redundant sequence id " + lclId);
    }

  }

  /**
   * Implementation of DBParsable interface.
   */
  public void parse(String file, StorageSystem ss) throws DBParserException {
    BufferedReader reader = null;
    String line, id, fName;
    long curPos = 0, start = 0;
    int endOfLineSize, i, size, seqLen = 0;
    boolean readseq = false;
    char c;

    try {
      endOfLineSize = Utils.getLineTerminatorSize(file);
      _entries = 0;
      reader = new BufferedReader(new InputStreamReader(new FileInputStream(
          file), "UTF-8"));
      fName = file;
      id = null;
      if (_pMonitor != null) {
        _pMonitor.startProcessingFile(fName, new File(file).length());
      }
      while ((line = reader.readLine()) != null) {
        /*
         * if (counter>5) break;
         */
        if (line.startsWith(">")) {
          if (id != null)
            handleData(ss, id, fName, seqLen, start, curPos - 1l);
          start = curPos;
          id = line;
          readseq = true;
          seqLen = 0;
        } else if (readseq) {
          size = line.length();
          for (i = 0; i < size; i++) {
            c = line.charAt(i);
            if (Character.isLetter(c)) {
              seqLen++;
            }
          }
        }

        curPos += (long) (line.length() + endOfLineSize);
      }
      handleData(ss, id, fName, seqLen, start, curPos);
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
      if (_pMonitor != null) {
        _pMonitor.stopProcessingFile(file, _entries);
      }
    }
    LoggerCentral.info(LOGGER, new File(file).getName() + " content: "
        + _entries + " entries");
  }

  /**
   * Implementation of DBParsable interface.
   */
  public int getEntries() {
    return _entries;
  }
}
