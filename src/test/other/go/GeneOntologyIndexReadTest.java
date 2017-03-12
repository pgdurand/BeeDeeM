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
package test.other.go;

import bzh.plealog.dbmirror.lucenedico.DicoStorageSystem;
import bzh.plealog.dbmirror.lucenedico.DicoStorageSystemImplem;
import bzh.plealog.dbmirror.lucenedico.DicoTerm;
import bzh.plealog.dbmirror.lucenedico.go.GeneOntologyTerm;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;

/**
 * This code snippet illustrates how to query a GeneOntology Lucene index.
 * 
 * @author Patrick G. Durand
 */
public class GeneOntologyIndexReadTest {

  /**
   * @param args
   *          arguments. Only one is expected: [0] is the lucene index of a GO
   *          databank.
   */
  public static void main(String[] args) {
    DBMSAbstractConfig.configureLog4J("GeneOntologyIndexReadTest");
    System.out.println("Index: " + args[0]);
    DicoStorageSystem lss = new DicoStorageSystemImplem();
    lss.open(args[0], DicoStorageSystem.READ_MODE);
    DicoTerm term;

    String[] ids = new String[] { "GO:0003673", "GO:0008150", "GO:0003674",
        "GO:0005575", "obsolete_Bp", "obsolete_Mf", "obsolete_Cc",
        "GO:0008152", "GO:0022610" };

    DicoTerm[] terms = lss.getTerms(ids);
    int i = 0;
    for (String id : ids) {
      term = terms[i];
      System.out.print(id + " : ");
      if (term != null) {
        GeneOntologyTerm goTerm = (GeneOntologyTerm) term.get_dataObject();
        System.out.println(goTerm);
        System.out.println("id: " + goTerm.get_node_id() + " name: "
            + goTerm.get_node_name() + " ontology: "
            + goTerm.get_node_ontology() + " nb. father: "
            + goTerm.get_node_father().size() + " nb. son: "
            + goTerm.get_node_sons().size());
      } else {
        System.out.println("not found");
      }
      i++;
    }
    lss.close();
  }
}
