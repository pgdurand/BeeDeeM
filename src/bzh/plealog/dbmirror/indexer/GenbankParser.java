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
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;
import bzh.plealog.dbmirror.util.runner.DBMSUniqueSeqIdRedundantException;
import bzh.plealog.dbmirror.util.sequence.TaxonMatcherHelper;

/**
 * This class defines a parser for Genbank database files.
 * 
 * @author Patrick G. Durand
 */
public class GenbankParser extends TaxonMatcherHelper implements DBParsable {

  private int              _total;
  private int              _kept;
  private int              _dicarded;
  private boolean          _verbose;
  private ParserMonitor    _pMonitor;
  private boolean          _checkNR = false;

  private static final Log LOGGER   = LogFactory
                                        .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                            + ".GenbankParser");

  public GenbankParser() {
  }

  /**
   * Analyses a data line and return the locus.
   */
  private static String getLocus(int decal, String line, StringBuffer buf) {
    int i;

    buf.delete(0, buf.length());
    i = decal;
    while (line.charAt(i) == ' ') {
      i++;
    }
    while (line.charAt(i) != ' ') {
      buf.append(line.charAt(i));
      i++;
    }
    return buf.toString();
  }

  private static String getAccession(String line) {
    StringTokenizer tokenizer;
    String acc;
    int idx;

    tokenizer = new StringTokenizer(line, " \n");
    if (tokenizer.countTokens() < 2)
      return null;
    tokenizer.nextToken();
    acc = tokenizer.nextToken();
    idx = acc.indexOf(".");
    if (idx < 0)
      return acc;
    else
      return line.substring(0, idx);
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
   * Implementation of DBParsable interface.
   */
  public void setParserMonitor(ParserMonitor pm) {
    _pMonitor = pm;
  }

  /**
   * Implementation of DBParsable interface.
   */
  public void parse(String file, StorageSystem ss) throws DBParserException {
    BufferedReader reader = null;
    String fName, line, locus = null, gi = null;
    long curPos = 0, start = 0;
    StringBuffer locusBuf;
    int endOfLineSize;
    boolean handleTaxon = true, seqOk = true, hasTaxoConstraints;
    boolean[] values = new boolean[3];

    try {
      // taxonomy filtering if needed
      initTaxonMatcher();
      hasTaxoConstraints = hasTaxoConstraints();
      if (hasTaxoConstraints) {
        if (hasTaxonomyAvailable() == false) {
          throw new Exception(TaxonMatcherHelper.ERR1);
        }
      }
      endOfLineSize = Utils.getLineTerminatorSize(file);
      _kept = _dicarded = _total = 0;
      reader = new BufferedReader(new InputStreamReader(new FileInputStream(
          file), "UTF-8"));
      fName = file;// new File(file).getName();
      if (_pMonitor != null) {
        _pMonitor.startProcessingFile(fName, new File(file).length());
      }
      locusBuf = new StringBuffer();
      while ((line = reader.readLine()) != null) {
        if (line.startsWith("LOCUS")) {
          start = curPos;
          locus = getLocus(5, line, locusBuf);
          handleTaxon = true;
          seqOk = true;
        } else if (line.startsWith("ACCESSION")) {
          gi = getAccession(line);
        } else if (line.startsWith("ORIGIN")) {
          handleTaxon = false;
        }
        if (hasTaxoConstraints && handleTaxon) {
          isSeqTaxonvalid(line, values);
          seqOk = values[1];
          if (values[0] == true) {
            handleTaxon = false;
          }
        }

        if (line.startsWith("//")) {
          _total++;
          if (seqOk) {
            if (locus == null)
              throw new StorageSystemException("field Locus is missing");
            if (gi == null)
              gi = locus;
            try {
              if (_pMonitor != null) {
                _pMonitor.seqFound(gi, locus, fName, start, curPos + 2l,
                    _checkNR);
              }
              if (ss != null) {
                ss.addEntry(gi, locus, fName, start, curPos + 2);
              }
              if (_verbose)
                System.out.println(locus + "," + gi + "," + start + ","
                    + (curPos + 2l));
              _kept++;
            } catch (DBMSUniqueSeqIdRedundantException ex) {
              // JIRA KDMS-22 accept a redundant sequence, just add a log
              LoggerCentral.warn(LOGGER, "Redundant sequence id " + gi);
              _dicarded++;
              if (_verbose)
                System.out.println("discarded: " + gi + "," + locus + ","
                    + start + "," + (curPos + 2l));
            }
          } else {
            _dicarded++;
            if (_verbose)
              System.out.println("discarded: " + gi + "," + locus + "," + start
                  + "," + (curPos + 2l));
          }

        }

        curPos += (long) (line.length() + endOfLineSize);
      }
      this.dumpTaxonNotFound(LOGGER);
    } catch (Exception e) {
      String msg = "Error while parsing GB entry no. " + (_kept + 1);
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
        _pMonitor.stopProcessingFile(file, _kept);
      }
      closeTaxonMatcher();
    }
    LoggerCentral.info(LOGGER, new File(file).getName() + " content: " + _total
        + " entries [" + _kept + " kept in DB, " + _dicarded + " discarded]");
  }

  /**
   * Implementation of DBParsable interface.
   */
  public int getEntries() {
    return _kept;// only return what is really kept
  }

}
