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
package bzh.plealog.dbmirror.lucenedico;

import java.util.Hashtable;

import bzh.plealog.dbmirror.lucenedico.go.GeneOntologyTerm;

/**
 * Utility class of Lucene Dictionary package.
 * 
 * @author Patrick G. Durand
 */
public class DicoUtils {

  public static String                    READER_GENE_ONTOLOGY       = "go";
  public static String                    READER_INTERPRO            = "ipr";
  public static String                    READER_NCBI_TAXONOMY       = "tax";
  public static String                    READER_NCBI_TAXONOMY_NODES = "nodes";
  public static String                    READER_PFAM                = "pfam";
  public static String                    READER_ENZYME              = "ec";
  public static String                    READER_ENZYME_CLASS        = "ecc";
  public static String                    READER_CDD                 = "cdd";
  public static String                    READER_EGGNOG              = "nog";

  public static final String              GO_NAME_KEY                = "na=";
  public static final String              GO_ONTO_KEY                = "on=";
  public static final String              TAX_ID_NAME_PREFIX         = "n";
  public static final String              TAX_ID_NODE_PREFIX         = "o";

  // string use to separate nodes in an enzyme path
  public static final String              ENZYME_NODE_SEPARATOR      = ";";
  // string use to separate id and label (node) in an enzyme path
  public static final String              ENZYME_ID_LABEL_SEPARATOR  = ",";
  // string to indicate that it is an enzyme path
  public static final String              ENZYME_PATH_PREFIX         = "Enzyme_path";

  public static String                    GO_CLASS_CELLULAR          = "cellular_component";
  public static String                    GO_CLASS_MOLECULAR         = "molecular_function";
  public static String                    GO_CLASS_BIOLOGICAL        = "biological_process";
  public static Hashtable<String, String> GO_CLASSES;

  static {
    GO_CLASSES = new Hashtable<String, String>();
    GO_CLASSES.put(GO_CLASS_CELLULAR, "C");
    GO_CLASSES.put(GO_CLASS_MOLECULAR, "F");
    GO_CLASSES.put(GO_CLASS_BIOLOGICAL, "P");
  }

  /**
   * Transform a GO DicoTerm into a nice string term.
   */
  public static String getSimpleGoString(DicoTerm goTerm) {
    StringBuffer buf = new StringBuffer();
    if (goTerm != null) {
      String data, clazz, code;
      GeneOntologyTerm goTermObject;
      int idx;

      // see GeneOntologyOBONodeParser for more info
      goTermObject = (GeneOntologyTerm) goTerm.get_dataObject();
      if (goTermObject == null)
        data = goTerm.getDataField();// old index system which does not contain
                                     // Tree Structure of GO
      else
        data = goTermObject.getFormatedNameAndOntology();// new index system
                                                         // (starting with KB
                                                         // 3.2)
      idx = data.indexOf('|');
      clazz = data.substring(idx + 1 + GO_ONTO_KEY.length());
      code = GO_CLASSES.get(clazz);
      if (code != null)
        buf.append(code);
      else
        buf.append(clazz);
      buf.append(": ");
      buf.append(data.substring(GO_NAME_KEY.length(), idx));
    }
    return buf.toString();
  }

  /**
   * 
   * @param term
   * 
   * @return a clean id (without 'n' or 'o' at the beginning)
   */
  public static String getCleanedNCBITaxonId(DicoTerm term) {
    if (term != null) {
      if (!Character.isDigit(term.getId().charAt(0))) {
        return term.getId().substring(1);
      } else {
        return term.getId();
      }
    }
    return "";
  }
}
