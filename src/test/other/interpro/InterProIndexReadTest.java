/* Copyright (C) 2007-2022 Patrick G. Durand
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
package test.other.interpro;

import bzh.plealog.dbmirror.lucenedico.DicoStorageSystem;
import bzh.plealog.dbmirror.lucenedico.DicoStorageSystemImplem;
import bzh.plealog.dbmirror.lucenedico.DicoTerm;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * This code snippet illustrates how to query a Interpro Lucene index.
 * 
 * @author Patrick G. Durand
 */
public class InterProIndexReadTest {

  /**
   * @param args arguments. Only one is expected: [0] is the lucene index
   * of a IP databank.
   */
  public static void main(String[] args) {
    LoggerCentral.configureLog4J("InterProIndexReadTest");
    System.out.println("Index: " + args[0]);
    DicoStorageSystem lss = new DicoStorageSystemImplem();
    lss.open(args[0], DicoStorageSystem.READ_MODE);
    DicoTerm term;

    String[] ids = new String[] { "IPR010978", "IPR021443", "GO:0071289",
        "699400", "721777" };

    DicoTerm[] terms = lss.getTerms(ids);
    int i = 0;
    for (String id : ids) {
      term = terms[i];
      System.out.print(id + " : ");
      if (term != null) {
        System.out.println(term.getDataField());
      } else {
        System.out.println("not found");
      }
      i++;
    }
    lss.close();
  }
}
