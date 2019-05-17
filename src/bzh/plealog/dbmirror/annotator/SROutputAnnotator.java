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
package bzh.plealog.dbmirror.annotator;

import java.io.File;
import java.io.FileInputStream;
import java.util.Date;
import java.util.StringTokenizer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.bioinfo.api.core.config.CoreSystemConfigurator;
import bzh.plealog.bioinfo.api.data.feature.FeatureTable;
import bzh.plealog.bioinfo.api.data.searchresult.SRHit;
import bzh.plealog.bioinfo.api.data.searchresult.SRHsp;
import bzh.plealog.bioinfo.api.data.searchresult.SRIteration;
import bzh.plealog.bioinfo.api.data.searchresult.SROutput;
import bzh.plealog.bioinfo.data.feature.IFeatureTable;
import bzh.plealog.dbmirror.lucenedico.DicoTermQuerySystem;
import bzh.plealog.dbmirror.reader.DBUtils;
import bzh.plealog.dbmirror.reader.PQueryMirrorBase;
import bzh.plealog.dbmirror.reader.PSequence;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.conf.DBMirrorConfig;
import bzh.plealog.dbmirror.util.xref.DBXrefInstancesManager;

/**
 * This class can be used to annotate Blast results using KDMS.
 * 
 * @author Patrick G. Durand
 */
public class SROutputAnnotator {
  private DicoTermQuerySystem _dicoConnector;
  private DBMirrorConfig      _config;
  private String              _confFile;
  private boolean             _annotatorOk;
  private boolean             _includeBC;
  
  protected static final Log  LOGGER = LogFactory
                                         .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                             + ".SROutputAnnotator");

  /**
   * Constructor.
   */
  public SROutputAnnotator() {
    this(false);
  }

  /**
   * Constructor.
   */
  public SROutputAnnotator(boolean includeBC) {
    initialize(includeBC);
  }


  public SROutputAnnotator(DBMirrorConfig conf) {
    _config = conf;
    _dicoConnector = DicoTermQuerySystem.getDicoTermQuerySystem(conf);
    _annotatorOk = true;
  }

  /**
   * A call to this method is required when the annotator is no longer used to
   * close read access to KDMS repository.
   */
  public void close() {
    DicoTermQuerySystem.closeDicoTermQuerySystem();
  }

  private void initialize(boolean includeBC) {
    FileInputStream fis = null;
    DBMirrorConfig conf;
    File f;

    _annotatorOk = false;
    _includeBC = includeBC;
    
    // factories for Sequence package
    CoreSystemConfigurator.initializeSystem();

    // Open access to dictionaries
    _confFile = DBMSAbstractConfig.getLocalMirrorConfFile();
    f = new File(_confFile);
    try {
      fis = new FileInputStream(f);
      conf = new DBMirrorConfig();
      if (!conf.load(fis)) {
        conf = null;
      }
    } catch (Exception e) {
      LOGGER.debug("Unable to read DB Mirror config file: " + _confFile + ": "
          + e);
      conf = null;
    } finally {
      IOUtils.closeQuietly(fis);
    }

    if (conf != null) {
      _config = conf;
      _dicoConnector = DicoTermQuerySystem.getDicoTermQuerySystem(conf);
      _annotatorOk = true;
    }
  }

  /**
   * Annotate Blast results with biological classifications only.
   * 
   * @return true if processing was ok.
   * 
   */
  public boolean doClassificationAnnotation(SROutput output) {
    if (_annotatorOk == false) {
      return false;
    }
    else {
      boolean bRet = SRAnnotatorUtils.extractDbXrefFromHitDefline(output, _dicoConnector);
      if (bRet && _includeBC) {
        output.setClassification(SRAnnotatorUtils.prepareClassification(output, _dicoConnector));
      }
      return bRet;
    }
  }

  /**
   * Annotate Blast results with full feature tables.
   * 
   * @return true if processing was ok.
   * 
   */
  public boolean doFullAnnotation(SROutput output) {
    if (_annotatorOk == false)
      return false;

    if (_config == null) {
      return false;
    }

    PQueryMirrorBase qm;
    PSequence[] kseqs;
    SRIteration bi;
    SRHit hit;
    SRHsp hsp;
    String db, gi, today, hitDef;
    String[] ids, vals;
    int blastType, nbSRIteration, nbHit, idx, nbHSP, from, to, start, stop;
    boolean updateHitData;

    blastType = output.getBlastType();
    nbSRIteration = output.countIteration();

    today = DBUtils.getDateFormatter().format(new Date());

    ids = tokenize(_config.getIdKeyString());
    // Get blastType
    if (blastType == SROutput.BLASTP || blastType == SROutput.SCANPS
        || blastType == SROutput.PSIBLAST || blastType == SROutput.BLASTX) {
      db = DBMirrorConfig.PROTEIC_IDX;
    } else {
      db = DBMirrorConfig.NUCLEOTIDEC_IDX;
    }
    qm = new PQueryMirrorBase();
    // Start to loop on SRIterations
    for (int j = 0; j < nbSRIteration; j++) {
      bi = output.getIteration(j);
      nbHit = bi.countHit();

      // Start to loop on SRHits
      for (int l = 0; l < nbHit; l++) {
        hit = bi.getHit(l);
        vals = SRAnnotatorUtils.getIdAndDb((SRHit) hit, ids);
        gi = vals[1];

        nbHSP = hit.countHsp();
        updateHitData = true;

        hitDef = hit.getHitDef();
        // skip special KoriBlast Classifications IDs
        idx = hitDef.indexOf(DBXrefInstancesManager.HIT_DEF_LINE_START);
        if (idx != -1) {
          hit.setHitDef(hitDef.substring(0, idx));
        }
        // Start to loop on HSP
        for (int k = 0; k < nbHSP; k++) {
          hsp = (SRHsp) hit.getHsp(k);
          // the following code has been added for translated Blast. In this
          // case
          // the first coord refers to the starting position of a 'codon'
          // whereas
          // the last coord refers to the ending position of the last 'codon'.
          // However
          // since we display a protein, the last coordinate is computed by
          // KLBlater
          // and corresponds to the beginning of the last codon.
          if (blastType == SROutput.TBLASTN || blastType == SROutput.TBLASTX) {
            int a, b, c;

            a = Math.min(hsp.getHit().getFrom(), hsp.getHit().getTo());
            b = Math.max(hsp.getHit().getFrom(), hsp.getHit().getTo());

            c = (b - a + 1);// nb. letters
            c = c / 3; // nb. codons
            c--;// remove one codon
            c *= 3;// back to nb. letters
            if (hsp.getHit().getFrame() < 0) {
              a = b;
              b = a - c;
            } else {
              b = a + c;
            }
            from = a;
            to = b;
          } else {
            from = hsp.getHit().getFrom();
            to = hsp.getHit().getTo();
          }

          if (from >= 0 && to >= 0) {
            if (from <= to) {
              start = from;
              stop = to;
            } else {
              start = to;
              stop = from;
            }
          } else {
            start = stop = 0;
          }

          try {
            // get the databank
            if (_confFile != null) {
              kseqs = qm.executeJob(db, gi, start, stop, Boolean.FALSE, "insd",
                  null, _confFile);
            } else if (_config != null) {
              kseqs = qm.executeJob(db, gi, start, stop, Boolean.FALSE, "insd",
                  null, _config);
            } else {
              kseqs = null;
            }
            if (qm.terminateWithError()) {
              IFeatureTable ift = new IFeatureTable();
              ift.setMessage(qm.getErrorMessage());
              ift.setStatus(FeatureTable.ERROR_STATUS);
              ift.setDate(today);
              hsp.setFeatures(ift);
            } else if (kseqs != null && kseqs.length != 0 && kseqs[0] != null) {
              // set the feature table into the corresponding HSP
              hsp.setFeatures(kseqs[0].getFeatTable());
              // set the sequence information (taxonomy, etc) into the
              // corresponding HIT
              if (updateHitData) {
                hit.setSequenceInfo(kseqs[0].getSeqInfo());
                updateHitData = false;
              }
            }
          } catch (Exception e) {
            LOGGER
                .debug("KDMS: Error while handling sequence entry: " + gi + e);
            break;
          }
        }// end HSP loop
      }// end Hit loop
    }// end Iteration loop

    if (_includeBC) {
      output.setClassification(SRAnnotatorUtils.prepareClassification(output, _dicoConnector));
    }
    return true;

  }

  public PSequence getSequence(String seqId, boolean isProtein) {
    return getSequence(seqId, 0, 0, isProtein);
  }

  public PSequence getSequence(String seqId, int from, int to,
      boolean isProtein) {
    PSequence seq = null;
    PQueryMirrorBase qm;
    PSequence[] kseqs;
    String db;

    qm = new PQueryMirrorBase();
    if (isProtein) {
      db = DBMirrorConfig.PROTEIC_IDX;
    } else {
      db = DBMirrorConfig.NUCLEOTIDEC_IDX;
    }
    try {
      // get the databank
      if (_confFile != null) {
        kseqs = qm.executeJob(db, seqId, from, to, Boolean.FALSE, "fas", null,
            _confFile);
      } else if (_config != null) {
        kseqs = qm.executeJob(db, seqId, from, to, Boolean.FALSE, "fas", null,
            _config);
      } else {
        kseqs = null;
      }
      if (qm.terminateWithError()) {
        throw new RuntimeException(qm.getErrorMessage());
      } else if (kseqs != null && kseqs.length != 0 && kseqs[0] != null) {
        seq = kseqs[0];
      }
    } catch (Exception e) {
      LOGGER.debug("KDMS: Error while handling sequence entry: " + seqId + ": "
          + e);
    }
    return seq;
  }

  public String[] tokenize(String input) {
    return tokenize(input, ",\t\n\r\f");
  }

  public String[] tokenize(String input, String delim) {
    StringTokenizer tokenizer;
    String str[];
    int i = 0;

    if (input == null)
      return new String[] {};

    tokenizer = new StringTokenizer(input, delim);

    if (tokenizer.countTokens() == 0)
      return new String[] {};

    str = new String[tokenizer.countTokens()];
    while (tokenizer.hasMoreTokens()) {
      str[i] = (String) tokenizer.nextToken().trim();
      i++;
    }

    return str;
  }

}
