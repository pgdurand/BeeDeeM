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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.indexer.ParserMonitor;
import bzh.plealog.dbmirror.lucenedico.DicoParsable;
import bzh.plealog.dbmirror.lucenedico.DicoParserException;
import bzh.plealog.dbmirror.lucenedico.DicoStorageSystem;
import bzh.plealog.dbmirror.lucenedico.DicoStorageSystemException;
import bzh.plealog.dbmirror.lucenedico.DicoTerm;
import bzh.plealog.dbmirror.lucenedico.DicoUtils;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * This class defines a parser for NCBI Taxonomy data file that contains the
 * list of taxon scientific names and their identifiers. Please note that this
 * parser (1) is used to index the files names.dat from NCBI Taxonomy and (2)
 * prefix all NCBI Taxon ID with the letter 'n' so that all taxon IDs from
 * names.dat and nodes.dat can be put within a single index. Reference:
 * ftp://ftp.ncbi.nih.gov/pub/taxonomy/. Data file: taxdump.tar.gz.
 * 
 * @author Patrick G. Durand
 */
public class NcbiTaxonomyTaxNamesParser implements DicoParsable {

  private int              _entries;
  private boolean          _verbose;
  private ParserMonitor    _pMonitor;

  private static final Log LOGGER = LogFactory
                                      .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                          + ".NcbiTaxonomyTaxNamesParser");

  public NcbiTaxonomyTaxNamesParser() {
  }

  /**
   * Analyzes a data line from merged.dmp.
   */
  private void splitMergedDataLine(String line, String[] data) {

    int idx1, idx2;
    // to parse this : (merged.dmp from NCBI data repository)
    // 45499   |       45514   |
    // we do not use StringTokenizer to avoid lots of instances of that class
    idx1 = line.indexOf('|');
    idx2 = line.indexOf('|', idx1 + 1);

    // n was added because taxon IDs from names.dat and nodes.dat are put
    // within a single index
    data[0] = DicoUtils.TAX_ID_NAME_PREFIX + line.substring(0, idx1).trim();
    data[1] = DicoTerm.SYNONYM + DicoUtils.TAX_ID_NAME_PREFIX + 
        line.substring(idx1 + 1, idx2).trim();
  }

  /**
   * Analyzes a data line from names.dmp.
   */
  private void splitNamesDataLine(String line, String[] data) {

    int idx1, idx2, idx3, idx4;
    // to parse this : (names.dmp from NCBI data repository)
    // 2 | Bacteria | Bacteria <prokaryote> | scientific name |
    // we do not use StringTokenizer to avoid lots of instances of that class
    idx1 = line.indexOf('|');
    idx2 = line.indexOf('|', idx1 + 1);
    idx3 = line.indexOf('|', idx2 + 1);
    idx4 = line.indexOf('|', idx3 + 1);
    // n was added because taxon IDs from names.dat and nodes.dat are put
    // within a single index
    data[0] = DicoUtils.TAX_ID_NAME_PREFIX + line.substring(0, idx1).trim();
    data[1] = line.substring(idx1 + 1, idx2).trim();
    data[2] = line.substring(idx2 + 1, idx3).trim();
    data[3] = line.substring(idx3 + 1, idx4).trim();
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

  private void parseMerged(String file, DicoStorageSystem ss)
      throws DicoParserException {
    BufferedReader reader = null;
    String line;
    String[] data;
    long curPos = 0;
    int endOfLineSize, entries=0;

    try {
      endOfLineSize = Utils.getLineTerminatorSize(file);
      reader = new BufferedReader(new InputStreamReader(new FileInputStream(
          file), "UTF-8"));
      data = new String[2];
      if (_pMonitor != null) {
        _pMonitor.startProcessingFile(file, new File(file).length());
      }
      while ((line = reader.readLine()) != null) {
        /*
         * if (_entries>5) break;
         */
        splitMergedDataLine(line, data);
        if (_pMonitor != null) {
          _pMonitor.seqFound(data[0], data[1], file, curPos, curPos, false);
        }
        if (ss != null) {
          if (data[0].length() == 0)
            throw new DicoStorageSystemException("taxon ID is missing");
          if (data[1].length() == 0)
            throw new DicoStorageSystemException("taxon synonym ID is missing");
          ss.addEntry(data[0], data[1]);
          entries++;
        }
        curPos += (long) (line.length() + endOfLineSize);
      }

    } catch (Exception e) {
      String msg = "Error while parsing NCBI taxon no. " + (entries + 1);
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
        //set 0 to entries to avoid modifying total number of TaxIDs
        // (see InstallInProduction task)
        _pMonitor.stopProcessingFile(file, 0);
      }
    }
    LoggerCentral.info(LOGGER, file + " contains " + entries + " entries");
  }
  private void parseNames(String file, DicoStorageSystem ss)
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
        splitNamesDataLine(line, data);
        if ("scientific name".equals(data[3])) {
          if (_pMonitor != null) {
            _pMonitor.seqFound(data[0], data[1], file, curPos, curPos, false);
          }
          if (ss != null) {
            if (data[0].length() == 0)
              throw new DicoStorageSystemException("taxon ID is missing");
            if (data[1].length() == 0)
              throw new DicoStorageSystemException("taxon name is missing");
            ss.addEntry(data[0], data[1]);
          }
          /*
           * if (_verbose) System.out.println(data[0]+","+data[1]);
           */
          _entries++;
          if (_verbose && (_entries % 10000) == 0) {
            System.out.println(_entries + ", current taxon: " + data[0] + ","
                + data[1]);
          }
        }
        curPos += (long) (line.length() + endOfLineSize);
      }

    } catch (Exception e) {
      String msg = "Error while parsing NCBI taxon no. " + (_entries + 1);
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
      throw new DicoParserException("Data file does not contain any taxons.");
    LoggerCentral.info(LOGGER, file + " contains " + _entries + " entries");
  }

  /**
   * Implementation of DicoParsable interface.
   */
  public void parse(String file, DicoStorageSystem ss)
      throws DicoParserException {
    
    // This method was originally designed to handle a single file, names.dmp.
    // By design, to avoid huge modification of the existing code, file
    // parameter can now contain two files: names.dmp:merged.dmp.
    // To enable backward compatibility, merged.dmp is optional.
    
    // why ':'? Check out NCBI_Taxonony.dsc configuration file!
    int idx = file.indexOf(':');
    
    if (idx==-1) {
      parseNames(file, ss);
    }
    else {
      String fNames = file.substring(0, idx);
      parseNames(fNames, ss);
      File f = new File(fNames);
      fNames = Utils.terminatePath(f.getParent()) + file.substring(idx+1);
      parseMerged(fNames, ss);
    }
    
  }

  /**
   * Implementation of DicoParsable interface.
   */
  public int getTerms() {
    return _entries;
  }
}
