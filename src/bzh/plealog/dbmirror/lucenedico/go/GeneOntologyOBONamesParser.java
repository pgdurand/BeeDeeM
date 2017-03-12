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
package bzh.plealog.dbmirror.lucenedico.go;

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
import bzh.plealog.dbmirror.lucenedico.DicoUtils;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * This class defines a parser for GeneOntology OBO release 1-2 database files.
 * Reference: http://www.geneontology.org/GO.downloads.ontology.shtml.
 * ftp://ftp.geneontology.org/pub/go/ontology/gene_ontology.obo
 * 
 * @author Patrick G. Durand
 */
public class GeneOntologyOBONamesParser implements DicoParsable {

  private int              _entries;
  private boolean          _verbose;
  private ParserMonitor    _pMonitor;

  private static final Log LOGGER = LogFactory
                                      .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                          + ".GeneOntologyOBONamesParser");

  public GeneOntologyOBONamesParser() {
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

  private String prepareTerm(String id, String name, String ontology,
      StringBuffer buf) {
    buf.delete(0, buf.length());
    buf.append(DicoUtils.GO_NAME_KEY);
    buf.append(name);
    buf.append("|");
    buf.append(DicoUtils.GO_NAME_KEY);
    if (ontology != null)
      buf.append(ontology);
    else
      buf.append("unknown");
    return buf.toString();
  }

  private void handleData(DicoStorageSystem ss, String fName, String id,
      String name, String ontology, StringBuffer buf, long curPos) {
    if (id != null && name != null) {
      String dataField = prepareTerm(id, name, ontology, buf);
      if (_pMonitor != null) {
        _pMonitor.seqFound(id, dataField, fName, curPos, curPos, false);
      }
      if (ss != null) {
        ss.addEntry(id, dataField);
      }
      if (_verbose)
        System.out.println(id + "," + dataField);
      _entries++;
      if (_verbose && (_entries % 10000) == 0) {
        System.out
            .println(_entries + ", current term: " + id + "," + dataField);
      }
    }
  }

  /**
   * Implementation of DicoParsable interface.
   */
  public void parse(String file, DicoStorageSystem ss)
      throws DicoParserException {
    BufferedReader reader = null;
    String line;
    String id = null, name = null, ontology = null;
    StringBuffer buf;
    int endOfLineSize;
    long curPos = 0;

    try {
      endOfLineSize = Utils.getLineTerminatorSize(file);
      _entries = 0;
      reader = new BufferedReader(new InputStreamReader(new FileInputStream(
          file), "UTF-8"));
      buf = new StringBuffer();
      if (_pMonitor != null) {
        _pMonitor.startProcessingFile(file, new File(file).length());
      }
      while ((line = reader.readLine()) != null) {
        if (line.startsWith("[Term]")) {
          handleData(ss, file, id, name, ontology, buf, curPos);
          id = name = null;
        } else if (line.startsWith("id:")) {
          id = line.substring(3).trim();
          // remove the "GO:" prefix
          // id = id.substring(id.indexOf(':')+1);
        } else if (line.startsWith("name:")) {
          name = line.substring(5).trim();
        } else if (line.startsWith("namespace:")) {
          ontology = line.substring(10).trim();
        }

        curPos += (long) (line.length() + endOfLineSize);
      }
      // handle last term
      handleData(ss, file, id, name, ontology, buf, curPos);

    } catch (Exception e) {
      String msg = "Error while parsing GeneOntology entry no. "
          + (_entries + 1);
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
