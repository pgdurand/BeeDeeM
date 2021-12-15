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
package bzh.plealog.dbmirror.lucenedico.tax;

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
import bzh.plealog.dbmirror.lucenedico.DicoStorageSystemException;
import bzh.plealog.dbmirror.lucenedico.DicoUtils;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * This class defines a parser for NCBI Taxonomy data file that contains the
 * list of taxon nodes. Please note that this parser (1) is used to index the
 * files nodes.dmp from NCBI Taxonomy and (2) prefix all NCBI Taxon ID with the
 * letter 'o' so that all taxon IDs from names.dat and nodes.dat can be put
 * within a single index. Reference: ftp://ftp.ncbi.nih.gov/pub/taxonomy/. Data
 * file: taxdump.tar.gz.
 * 
 * @author Patrick G. Durand
 */
public class NcbiTaxonomyTaxNodesParser implements DicoParsable {

  private int              _entries;
  private boolean          _verbose;
  private ParserMonitor    _pMonitor;
  private boolean          _rootFound = false;

  private static final Logger LOGGER     = LogManager.getLogger(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                              + ".NcbiTaxonomyTaxNodesParser");

  public NcbiTaxonomyTaxNodesParser() {
  }

  /**
   * Analyzes a data line.
   */
  private void splitDataLine(String line, String[] data) {
    int idx1, idx2, idx3;

    // to parse this : (nodes.dmp from NCBI data repository)
    // 1 | 1 | no rank | | 8 | 0 | 1 | 0 | 0 | 0 | 0 | 0 | |
    // we do not use StringTokenizer to avoid lots of instances of that class
    // before and after | : tab character to remove
    idx1 = line.indexOf('|');
    idx2 = line.indexOf('|', idx1 + 1);
    idx3 = line.indexOf('|', idx2 + 1);

    // in the following lines, +1 and -1 are used to remove tab chars

    // o was added because taxon IDs from names.dat and nodes.dat are put
    // within a single index
    data[0] = DicoUtils.TAX_ID_NODE_PREFIX + line.substring(0, idx1 - 1);
    if (!_rootFound && data[0].equals("o1")) {
      data[1] = "1|root";// this is to avoid that "root" be discarded when
                         // dealing with simplified taxonomy
      _rootFound = true;
    } else {
      data[1] = DicoUtils.TAX_ID_NODE_PREFIX
          + line.substring(idx1 + 1, idx3 - 1).trim();
    }
  }

  /**
   * Implementation of DBParsable interface.
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

  /**
   * Implementation of DicoParsable interface.
   */
  public void parse(String file, DicoStorageSystem ss)
      throws DicoParserException {
    BufferedReader reader = null;
    String line;
    String[] data;
    long curPos = 0;
    int endOfLineSize;

    try {
      endOfLineSize = Utils.getLineTerminatorSize(file);
      _entries = 0;
      reader = new BufferedReader(new InputStreamReader(new FileInputStream(
          file), "UTF-8"));
      data = new String[4];
      if (_pMonitor != null) {
        _pMonitor.startProcessingFile(file, new File(file).length());
      }
      while ((line = reader.readLine()) != null) {
        /*
         * if (_entries>5) break;
         */
        splitDataLine(line, data);
        if (_pMonitor != null) {
          _pMonitor.seqFound(data[0], data[1], file, curPos, curPos, false);
        }
        if (ss != null) {
          if (data[0].length() == 0)
            throw new DicoStorageSystemException("taxon node ID is missing");
          if (data[1].length() == 0)
            throw new DicoStorageSystemException(
                "parent taxon node ID is missing");
          ss.addEntry(data[0], data[1]);
        }
        /*
         * if (_verbose) System.out.println("["+data[0]+"],["+data[1]+"]");
         */
        _entries++;
        if (_verbose && (_entries % 10000) == 0) {
          System.out.println(_entries + ", current node: " + data[0] + ","
              + data[1]);
        }
        curPos += (long) (line.length() + endOfLineSize);
      }

    } catch (Exception e) {
      String msg = "Error while parsing NCBI taxon node no. " + (_entries + 1);
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
      throw new DicoParserException(
          "Data file does not contain any taxon nodes.");
    LoggerCentral.info(LOGGER, file + " contains " + _entries + " entries");
  }

  /**
   * Implementation of DicoParsable interface.
   */
  public int getTerms() {
    return _entries;
  }
}
