/* Copyright (C) 2007-2021 Ludovic Antin
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
package bzh.plealog.dbmirror.lucenedico.cdd;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import bzh.plealog.dbmirror.indexer.ParserMonitor;
import bzh.plealog.dbmirror.lucenedico.DicoParsable;
import bzh.plealog.dbmirror.lucenedico.DicoParserException;
import bzh.plealog.dbmirror.lucenedico.DicoStorageSystem;
import bzh.plealog.dbmirror.lucenedico.DicoTerm;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * This class defines a parser for CDD data file. Reference : ftp://
 * ftp.ncbi.nih.gov/pub/mmdb/cdd/README section cddid_all.tbl.gz
 * 
 * @author Ludvic Antin
 * 
 */
public class CddParser implements DicoParsable {

  private static final Logger LOGGER                = LogManager.getLogger(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                            + ".CddParser");

  private static final String DESCRIPTION_SEPARATOR = "@";

  private int                 nbEntries             = 0;
  @SuppressWarnings("unused")
  private boolean             verbose               = false;
  private ParserMonitor       monitor               = null;

  @Override
  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  @Override
  public void setParserMonitor(ParserMonitor pm) {
    this.monitor = pm;
  }

  @Override
  public void parse(String file, DicoStorageSystem ss)
      throws DicoParserException {
    if (StringUtils.isNotBlank(file)) {
      File fileToParse = new File(file);
      int currentPositionInFile = 0;
      if (fileToParse.exists()) {
        FileInputStream fis = null;
        InputStreamReader isr = null;
        BufferedReader reader = null;
        try {

          int endOfLineSize = Utils.getLineTerminatorSize(file);
          String id = null;
          String description = null;
          String cddAccession = null;

          fis = new FileInputStream(file);
          isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
          reader = new BufferedReader(isr);

          if (monitor != null) {
            monitor.startProcessingFile(file, fileToParse.length());
          }

          /*
           * "cddid.tbl.gz" contains summary information about the CD models in
           * this distribution, which are part of the default "cdd" search
           * database and are indexed in NCBI's Entrez database. This is a
           * tab-delimited text file, with a single row per CD model and the
           * following columns:
           * 
           * PSSM-Id (unique numerical identifier) CD accession (starting with
           * 'cd', 'pfam', 'smart', 'COG', 'PRK' or "CHL') CD "short name" CD
           * description PSSM-Length (number of columns, the size of the search
           * model)
           */
          /*
           * Samples: 225718 COG3177 COG3177 Fic family protein [Function
           * unknown] 348 214331 CHL00002 matK maturase K 504 144813 pfam01358
           * PARP_regulatory Poly A polymerase regulatory subunit. 295 240457
           * cd12877 SPRY1_RyR SPRY domain 1 (SPRY1) of ryanodine receptor
           * (RyR). This SPRY domain is the first of three structural repeats in
           * all three isoforms of the ryanodine receptor (RyR), which are the
           * major Ca2+ release channels in the membranes of sarcoplasmic
           * reticulum (SR). There are three RyR genes in mammals; the skeletal
           * RyR1, the cardiac RyR2 and the brain RyR3. The three SPRY domains
           * are located in the N-terminal part of the cytoplasmic region of the
           * RyRs, but no specific function has been found for this first SPRY
           * domain of the RyRs. 151
           */

          String line = null;
          String[] data = null;
          while ((line = reader.readLine()) != null) {
            data = StringUtils.split(line, '\t');
            // interested data are the first and the last ones : id and
            // description
            if (data.length >= 4) {
              id = data[0];
              cddAccession = data[1];
              description = data[3];
              if (StringUtils.isNotBlank(id)) {
                if (ss != null) {
                  ss.addEntry(id.trim(),
                      constructStoredDescription(cddAccession, description));
                }
                if (monitor != null) {
                  monitor.seqFound(id, description, file,
                      currentPositionInFile, currentPositionInFile, false);
                }
                nbEntries++;
              }
            }

            currentPositionInFile += (long) (line.length() + endOfLineSize);
          }
        } catch (FileNotFoundException e) {
          throw new DicoParserException(e.getMessage());
        } catch (IOException e) {
          throw new DicoParserException(e.getMessage());
        } finally {
          IOUtils.closeQuietly(fis);
          IOUtils.closeQuietly(isr);
          IOUtils.closeQuietly(reader);
          if (monitor != null) {
            monitor.stopProcessingFile(file, nbEntries);
          }
        }

        LoggerCentral
            .info(LOGGER, file + " contains " + nbEntries + " entries");

      } else {
        LOGGER.info("The file to parse does not exists: " + file);
      }
    } else {
      LOGGER.info("No file to parse");
    }
  }

  @Override
  public int getTerms() {
    return this.nbEntries;
  }

  /**
   * A CDD description contains 2 data : the CDD accession and the real
   * description. But a lucene dictionary index can contains only one for the
   * moment.
   * 
   * @param cddAccession
   * @param description
   * @return
   */
  public static String constructStoredDescription(String cddAccession,
      String description) {
    return cddAccession.trim() + DESCRIPTION_SEPARATOR + description.trim();
  }

  public static String getCddAccession(DicoTerm term) {
    if (term != null) {
      if (StringUtils.isNotBlank(term.getDataField())) {
        return term.getDataField().substring(0,
            term.getDataField().indexOf(DESCRIPTION_SEPARATOR));
      } else {
        return term.getId();
      }
    }
    return "";
  }

  public static String getDescription(DicoTerm term) {
    if (term != null && StringUtils.isNotBlank(term.getDataField())) {
      return term.getDataField().substring(
          term.getDataField().indexOf(DESCRIPTION_SEPARATOR) + 1);
    }
    return DicoTerm.EMPTY_DESCRIPTION;
  }

}
