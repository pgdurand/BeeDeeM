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
import bzh.plealog.dbmirror.util.sequence.TaxonMatcherHelper;

/**
 * This class defines a parser for Uniprot/SwissProt database files.
 * 
 * @author Patrick G. Durand
 */
public class SwissProtParser extends TaxonMatcherHelper implements DBParsable {

  public static final String KEYWORD_ID             = "ID";
  public static final String KEYWORD_ACCESSION      = "AC";
  public static final String KEYWORD_ORGANISM       = "OS";
  public static final String KEYWORD_ORGANISM_ID    = "OX";
  public static final String KEYWORD_FEATURE_TABLE  = "FT";
  public static final String KEYWORD_START_SEQUENCE = "SQ";
  public static final String KEYWORD_NB_LETTERS     = "AA";
  public static final String KEYWORD_END_ENTRY      = "//";

  private int                _total;
  private int                _kept;
  private int                _dicarded;
  private boolean            _verbose;
  private ParserMonitor      _pMonitor;
  private boolean            _checkNR               = false;

  private static final Log   LOGGER                 = LogFactory
                                                        .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                            + ".SwissProtParser");

  public SwissProtParser() {
  }

  /**
   * Analyses a data line and return the primary ID.
   */
  private static String getID(int decal, String line, StringBuffer buf) {
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

  /**
   * Analyses a data line and return the primary accession number.
   */
  private String getAC(String line, StringBuffer buf) {
    int i;

    buf.delete(0, buf.length());
    i = 2;
    while (line.charAt(i) == ' ') {
      i++;
    }
    while (line.charAt(i) != ';') {
      buf.append(line.charAt(i));
      i++;
    }
    return buf.toString();
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
    String line, id = null, ac = null, fName;
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
      locusBuf = new StringBuffer();
      fName = file;// new File(file).getName();
      if (_pMonitor != null) {
        _pMonitor.startProcessingFile(fName, new File(file).length());
      }
      while ((line = reader.readLine()) != null) {
        /*
         * if (counter>5) break;
         */
        if (line.startsWith(KEYWORD_ID + " ")) {
          start = curPos;
          id = getID(2, line, locusBuf);
          handleTaxon = true;
          seqOk = true;
        } else if (ac == null && line.startsWith(KEYWORD_ACCESSION)) {
          ac = getAC(line, locusBuf);
        }
        // EMBL ENA: PA instead of AC !
        // not documented anywhere even in official doc:
        // ftp://ftp.ebi.ac.uk/pub/databases/ena/sequence/release/doc/usrman.txt

        // keep for history : DO NOT USE since PA can be redundant !!!!

        /*
         * else if (ac == null && line.startsWith("PA")) { ac = getAC(line,
         * locusBuf); }
         */
        else if (line.startsWith(KEYWORD_START_SEQUENCE)) {
          handleTaxon = false;
        }
        if (hasTaxoConstraints && handleTaxon) {
          isSeqTaxonvalid(line, values);
          seqOk = values[1];
          if (values[0] == true) {
            handleTaxon = false;
          }
        }

        if (line.startsWith(KEYWORD_END_ENTRY)) {
          _total++;
          if (seqOk) {
            try {
              if (ac == null) {
                ac = id;
              }
              if (_pMonitor != null) {
                _pMonitor.seqFound(ac, id, fName, start, curPos + 2l, _checkNR);
              }
              if (ss != null) {
                if (id == null)
                  throw new StorageSystemException("field ID is missing");
                ss.addEntry(ac, id, fName, start, curPos + 2l);
              }
              if (_verbose)
                System.out.println(id + "," + ac + "," + start + ","
                    + (curPos + 2l));
              _kept++;
              if (_verbose && (_kept % 10000) == 0) {
                System.out.println(_kept + ", start: " + start);
              }
            } catch (DBMSUniqueSeqIdRedundantException ex) {
              // JIRA KDMS-22 accept a redundant sequence, just add a log
              LoggerCentral.warn(LOGGER, "Redundant sequence id " + ac);
              _dicarded++;
              if (_verbose)
                System.out.println("discarded: " + id + "," + ac + "," + start
                    + "," + (curPos + 2l));
            }
            ac = null;
          } else {
            _dicarded++;
            if (_verbose)
              System.out.println("discarded: " + id + "," + ac + "," + start
                  + "," + (curPos + 2l));
          }

        }

        curPos += (long) (line.length() + endOfLineSize);
      }
      this.dumpTaxonNotFound(LOGGER);
    } catch (Exception e) {
      String msg = "Error while parsing SW entry no. " + (_kept + 1);
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
