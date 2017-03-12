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

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;

import bzh.plealog.bioinfo.api.core.config.CoreSystemConfigurator;
import bzh.plealog.bioinfo.api.data.feature.Feature;
import bzh.plealog.bioinfo.api.data.feature.FeatureTable;
import bzh.plealog.bioinfo.api.data.feature.utils.FeatureTableFactory;
import bzh.plealog.bioinfo.api.data.searchresult.SRHit;
import bzh.plealog.bioinfo.api.data.searchresult.SRHsp;
import bzh.plealog.bioinfo.api.data.searchresult.SRIteration;
import bzh.plealog.bioinfo.api.data.searchresult.SROutput;
import bzh.plealog.bioinfo.api.data.sequence.BankSequenceInfo;
import bzh.plealog.dbmirror.lucenedico.DicoTerm;
import bzh.plealog.dbmirror.lucenedico.DicoTermQuerySystem;
import bzh.plealog.dbmirror.lucenedico.Dicos;
import bzh.plealog.dbmirror.util.xref.DBXrefInstancesManager;

public class SRAnnotatorUtils {
  public static final String UNK_DB = "unknown";

  // some databanks such as NCBI_nr contains the taxonomy scientific name within
  // hit definition line
  // and between brackets:
  // >gi|66816243|ref|XP_642131.1| hypothetical protein DDB_G0277827
  // [Dictyostelium discoideum AX4]
  // so, we try to get that information in this code
  private static List<String> getTaxonomyFromDefLine(String hitDef,
      DicoTermQuerySystem dico) {
    ArrayList<String> taxons;
    String tax;
    int from, to;
    if (dico.hasDicoAvailable(Dicos.NCBI_TAXONOMY) == false) {
      return null;
    }
    from = hitDef.indexOf("[");
    to = hitDef.indexOf("]");
    if (from == -1 || to == -1 || to <= from) {
      return null;
    }
    tax = hitDef.substring(from + 1, to).trim();
    tax = dico.getTaxID(tax);
    if (tax == null)
      return null;
    taxons = new ArrayList<String>();
    // note: taxID returned above is prefixed with 'n', so remove that part
    taxons.add(Dicos.NCBI_TAXONOMY.xrefId + "; " + tax.substring(1));
    return taxons;
  }

  public static boolean extractDbXrefFromHitDefline(SROutput bo,
      DicoTermQuerySystem dico) {
    Hashtable<String, BankSequenceInfo> taxons;
    Hashtable<String, String> dbXrefs;
    FeatureTableFactory ftFactory;
    FeatureTable fTable;
    Feature feat;
    SRIteration bi;
    SRHit hit;
    SRHsp hsp;
    List<String> xrefs;
    String hitDef;
    BankSequenceInfo si;
    int i, j, k, size, size2, size3, from, to, strand;
    boolean dbxrefsFound = false, xrefsSpecificFound;

    ftFactory = CoreSystemConfigurator.getFeatureTableFactory();
    size = bo.countIteration();
    taxons = new Hashtable<String, BankSequenceInfo>();
    dbXrefs = new Hashtable<String, String>();
    for (i = 0; i < size; i++) {
      bi = bo.getIteration(i);
      size2 = bi.countHit();
      for (j = 0; j < size2; j++) {
        hit = bi.getHit(j);
        hitDef = hit.getHitDef();
        // try to locate KoriBlast/Fasta-extended special data
        xrefs = DBXrefInstancesManager.getDbXrefs(hitDef);
        if (xrefs.isEmpty()) {
          xrefs = getTaxonomyFromDefLine(hitDef, dico);
          if (xrefs == null)
            continue;
          xrefsSpecificFound = false;
        } else {
          xrefsSpecificFound = true;
        }
        if (!dbxrefsFound)
          dbxrefsFound = true;
        // remove the KoriBlast/Fasta-extended special data
        if (xrefsSpecificFound) {
          hit.setHitDef(hitDef.substring(0,
              hitDef.indexOf(DBXrefInstancesManager.HIT_DEF_LINE_START)));
        }
        // check once for Taxonomy data if any available
        si = prepareSequenceInfo(xrefs, taxons, dico);
        if (si != null) {
          hit.setSequenceInfo(si);
        }
        size3 = hit.countHsp();
        for (k = 0; k < size3; k++) {
          hsp = hit.getHsp(k);
          fTable = ftFactory.getFTInstance();
          from = hsp.getHit().getFrom();
          to = hsp.getHit().getTo();
          strand = (hsp.getHit().getFrame() < 0 ? Feature.MINUS_STRAND
              : Feature.PLUS_STRAND);
          feat = fTable.addFeature(
              DBXrefInstancesManager.DEFAULT_FEATURE_TYPE_XREF,
              Math.min(from, to), Math.max(from, to), strand);
          if (si != null && si.getOrganism() != null) {
            feat.addQualifier(DBXrefInstancesManager.DEFAULT_FEATURE_TYPE_ORG,
                si.getOrganism());
          }
          prepareXrefsFeatureTable(dbXrefs, xrefs, feat, dico);
          hsp.setFeatures(fTable);
        }
      }
    }
    taxons.clear();
    dbXrefs.clear();
    return dbxrefsFound;
  }

  public static BankSequenceInfo prepareSequenceInfo(List<String> xrefs,
      Hashtable<String, BankSequenceInfo> taxons, DicoTermQuerySystem dico) {
    DicoTerm term;
    String id, taxo;
    BankSequenceInfo si = null, knwonSI;

    for (String xref : xrefs) {
      // string tests for taxon, etc.: see dbxrefsForFasta.config
      if (xref.startsWith(Dicos.NCBI_TAXONOMY.xrefId)) {
        id = xref.substring(xref.indexOf(';') + 2);
        knwonSI = taxons.get(id);
        if (knwonSI != null) {
          si = CoreSystemConfigurator.getBankSequenceInfoFactory().getInstance();
          si.setOrganism(knwonSI.getOrganism());
          si.setTaxonomy(knwonSI.getTaxonomy());
        } else {
          term = dico.getTerm(Dicos.NCBI_TAXONOMY, id);
          if (term != null) {
            si = CoreSystemConfigurator.getBankSequenceInfoFactory().getInstance();
            si.setOrganism(Dicos.NCBI_TAXONOMY.getData(term));
            taxo = dico.getTaxPath(id);
            if (taxo != null)
              si.setTaxonomy(taxo);
            taxons.put(id, si);
          }
        }
        break;
      }
    }
    return si;
  }

  private static void prepareXrefsFeatureTable(
      Hashtable<String, String> dbXrefs, List<String> xrefs, Feature feat,
      DicoTermQuerySystem dicoQuerySystem) {
    DicoTerm term;
    String id, value;
    Dicos xrefDico = null;

    for (String xref : xrefs) {
      id = xref.substring(xref.indexOf(';') + 2);
      value = dbXrefs.get(id);
      xrefDico = null;

      for (Dicos dico : Dicos.values()) {
        if (dico != Dicos.NCBI_TAXONOMY) {
          if (xref.startsWith(dico.xrefId)) {
            xrefDico = dico;
            break;
          }
        }
      }

      if (xrefDico != null) {
        term = dicoQuerySystem.getTerm(xrefDico, id);
        if (term != null) {
          value = xref + "; " + xrefDico.getData(term);
          dbXrefs.put(id, value);
        }
      }

      if (value == null) {
        value = xref;
      }
      feat.addQualifier(DBXrefInstancesManager.DEFAULT_QUAL_TYPE_XREF, value);
    }
  }

  private static String analyseID(String defLine, String[] ids) {
    for (int i = 0; i < ids.length; i++) {
      if (defLine.indexOf(ids[i] + "|") >= 0) {
        return ids[i];
      }
    }
    return UNK_DB;
  }

  private static String getID(String db, String defLine) {
    StringTokenizer tokenizer;
    int idx;
    String str;

    idx = defLine.indexOf(db + "|");
    tokenizer = new StringTokenizer(defLine.substring(idx), "| ");
    str = tokenizer.nextToken();
    str = tokenizer.nextToken();
    return (str);
  }

  public static String[] getIdAndDb(SRHit hit, String[] ids) {
    String db, id;
    int idx;

    // Try to retrieve ID from various sources
    db = analyseID(hit.getHitId(), ids);
    if (db.equals(UNK_DB)) {
      db = analyseID(hit.getHitAccession(), ids);
      if (db.equals(UNK_DB)) {
        db = analyseID(hit.getHitDef(), ids);
        if (db.equals(UNK_DB)) {
          id = hit.getHitId();
        } else {
          id = getID(db, hit.getHitDef());
        }
      } else {
        id = getID(db, hit.getHitAccession());
      }
    } else {
      id = getID(db, hit.getHitId());
    }
    // this is for Local Blast and/or seqDB formatted with formatdb
    // that report local IDs.
    idx = id.indexOf("BL_ORD_ID");
    if (idx > 0) {
      id = hit.getHitDef().trim();
      // try to retrieve some ID that should be the first string of the
      // Definition line
      idx = id.indexOf(' ');
      if (idx > 0) {
        id = id.substring(0, idx);
      }
      // then, figure out if there is multiple IDs separated by a |
      idx = id.indexOf('|');
      if (idx > 0) {
        id = id.substring(0, idx);
      }
    }
    // figure out whether or not the id contains a version number.
    // If true, remove it!
    idx = id.indexOf('.');
    if (idx > 0) {
      id = id.substring(0, idx);
    }
    return new String[] { db, id };
  }

}
