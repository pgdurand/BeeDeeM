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

import java.util.ArrayList;

import bzh.plealog.dbmirror.lucenedico.DicoStorageSystem;
import bzh.plealog.dbmirror.lucenedico.DicoStorageSystemImplem;
import bzh.plealog.dbmirror.lucenedico.go.GeneOntologyGenerateTree;
import bzh.plealog.dbmirror.lucenedico.go.GeneOntologyGenerateTree.Path;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;

/**
 * Code snippet to check if the GO branches are correctly created.
 * 
 * @author Patrick G. Durand
 * 
 */
public class GeneratePathTest {

  public static void main(String[] args) {

    ArrayList<Path> firsts_path;
    DicoStorageSystem lss = null;
    GeneOntologyGenerateTree path;
    ArrayList<Path> paths = new ArrayList<Path>();

    try {

      path = new GeneOntologyGenerateTree();

      DBMSAbstractConfig.configureLog4J("GeneOntologyIndexReadTest");
      lss = new DicoStorageSystemImplem();
      lss.open(args[0], DicoStorageSystem.READ_MODE);

      firsts_path = path.initPaths(lss, "GO:0017079");

      if (firsts_path != null) {

        for (int i = 0; i < firsts_path.size(); i++) {
          Path path2 = firsts_path.get(i);
          paths.addAll(GeneOntologyGenerateTree.generatePaths(lss, path2));
        }

        if (paths.size() > 0) {
          printPaths(paths);
        } else {
          throw new Exception("Term unknown");
        }

      } else {
        throw new Exception("Term unknown");
      }

    } catch (Exception e) {
      System.out.println("Error during branch generation : " + e);

    } finally {
      if (lss != null) {
        lss.close();
      }
    }

  }

  public static void printPaths(ArrayList<Path> paths) {
    System.out.println("");
    int cpt = 0;
    for (Path p : paths) {
      for (int i = 0; i < p.get_path().size(); i++) {
        for (int j = 0; j < cpt; j++) {
          System.out.print("\t");
        }
        System.out.println("-" + p.get_path().get(i).get_id() + "|"
            + p.get_path().get(i).get_type_relation());
        cpt++;
      }
      cpt = 0;
      System.out.println(" ");
    }
  }

}
