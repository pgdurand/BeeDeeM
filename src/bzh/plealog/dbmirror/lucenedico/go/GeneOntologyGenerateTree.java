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

import java.util.ArrayList;
import java.util.Vector;

import bzh.plealog.dbmirror.lucenedico.DicoStorageSystem;
import bzh.plealog.dbmirror.lucenedico.DicoTerm;

/**
 * This class implements function to create branch of the GeneOntology graph
 * from an Term given
 * 
 * @author Patrick G. Durand
 * 
 */
public class GeneOntologyGenerateTree {

  /**
   * For each of idGo's son passed in parameter, the function creates a new path
   * which contains a couple <idGo,son1>
   * 
   * @param lss
   *          the dicoStorageSystem which allow access to the GO data
   * @param idGo
   *          the id of the GO term whose we want to obtain the branch
   * 
   * @return the list of the path [idGo,son]
   */
  public ArrayList<Path> initPaths(DicoStorageSystem lss, String idGo) {
    DicoTerm term;
    GeneOntologyTerm goTerm = null;
    ArrayList<Path> first_paths;

    term = lss.getTerm(idGo);
    first_paths = new ArrayList<Path>();

    if (term != null) {
      goTerm = (GeneOntologyTerm) term.get_dataObject();
    }
    /*
     * else{ System.out.println(idGo+" not found in the index."); }
     */

    if (goTerm != null) {
      // if the term has no son we return a new path with just one node
      if (goTerm.get_node_sons().size() == 0) {
        GeneOntologyTermRelationship[] gotr = new GeneOntologyTermRelationship[1];
        gotr[0] = new GeneOntologyTermRelationship(idGo,
            GeneOntologyTermRelationship.TYPE_EDGE.K);
        first_paths.add(new Path(gotr));
        return first_paths;
      } else {
        // generate a new path which contains two node idGO and one son of it
        for (int i = 0; i < goTerm.get_node_sons().size(); i++) {
          GeneOntologyTermRelationship[] gotr = new GeneOntologyTermRelationship[2];
          gotr[0] = new GeneOntologyTermRelationship(idGo,
              GeneOntologyTermRelationship.TYPE_EDGE.K);
          gotr[1] = goTerm.get_node_sons().get(i);
          first_paths.add(new Path(gotr));
        }
        return first_paths;
      }
    } else {
      // System.out.println("Term not found");
      return null;
    }

  }

  /**
   * For each root of the path, we search its fathers that we add to the path
   * and we restart the operation for the new root until there aren't no more
   * father. This function is called after the function initPaths
   * 
   * @param lss
   *          the dicoStorageSystem which allow access to the GO data
   * @param path_withoutFather
   *          the path whose want to add fathers
   * @return the list of the branch possible to go from one Goid given to the
   *         root
   */
  public static ArrayList<Path> generatePaths(DicoStorageSystem lss,
      Path path_withoutFather) {
    ArrayList<Path> path_return = new ArrayList<Path>();
    ArrayList<GeneOntologyTermRelationship> fathers;
    DicoTerm term;
    GeneOntologyTerm goTerm = null;
    String rootId;

    Path path_withFather = path_withoutFather.copy();
    rootId = path_withoutFather.get_root().get_id();
    term = lss.getTerm(rootId);

    if (term != null) {
      goTerm = (GeneOntologyTerm) term.get_dataObject();
    }
    /*
     * else{ System.out.println(rootId+" term not found in index"); }
     */

    if (goTerm != null) {
      fathers = (ArrayList<GeneOntologyTermRelationship>) goTerm
          .get_node_father();

      if (fathers.size() == 0) {
        path_return.add(path_withFather);
        return path_return;
      } else {
        for (int i = 0; i < fathers.size(); i++) {
          path_withFather.addFather(fathers.get(i));

          path_return.addAll(generatePaths(lss, path_withFather));

          path_withFather = path_withoutFather.copy();
        }
        return path_return;
      }
    } else {
      // System.out.println("Term not found");
      return null;
    }
  }

  public ArrayList<Path> createTree(String goTermId, DicoStorageSystem lss,
      boolean withSons) {

    ArrayList<Path> firsts_path;
    Path pathEnCour;
    ArrayList<Path> paths = new ArrayList<Path>();

    try {
      if (withSons) {
        firsts_path = this.initPaths(lss, goTermId);
      } else {
        firsts_path = new ArrayList<Path>();
        GeneOntologyTermRelationship[] gotr = new GeneOntologyTermRelationship[1];
        gotr[0] = new GeneOntologyTermRelationship(goTermId,
            GeneOntologyTermRelationship.TYPE_EDGE.K);
        firsts_path.add(new Path(gotr));
      }

      if (firsts_path != null) {
        for (int i = 0; i < firsts_path.size(); i++) {
          pathEnCour = firsts_path.get(i);
          paths.addAll(GeneOntologyGenerateTree.generatePaths(lss, pathEnCour));
        }

        if (paths.size() > 0) {
          return paths;
        } else {
          throw new Exception("Term unknown");
        }
      } else {
        throw new Exception("Term unknown");
      }
    } catch (Exception e) {
      lss.close();
      // System.out.println("Error during branch generation : "+e);
    }

    return paths;
  }

  /**
   * This class creates a path which is characterized by a vector of
   * GeneOntologyRelationship
   * 
   * @author Patrick G. Durand
   * 
   */
  public class Path {

    Vector<GeneOntologyTermRelationship> path;

    public Path(GeneOntologyTermRelationship[] nodes) {

      path = new Vector<GeneOntologyTermRelationship>();

      for (int i = 0; i < nodes.length; i++) {
        path.add(nodes[i]);
      }
    }

    /**
     * 
     * Create a path from another path Used to copy a path.
     * 
     * @param path
     *          the path copied
     */
    public Path(Path path) {
      this.path = new Vector<GeneOntologyTermRelationship>();
      for (GeneOntologyTermRelationship gotr : path.get_path()) {
        this.path.add(new GeneOntologyTermRelationship(gotr.get_id(), gotr
            .get_type_relation()));
      }

      if (path.get_path().size() < 3) {
        this.path.set(0, new GeneOntologyTermRelationship(path.get_root()
            .get_id(), path.get_root().get_type_relation()));
      }
    }

    /**
     * Return the root of the path
     * 
     * @return a GeneOntologyTermRelationship which is the root of the path
     */
    public GeneOntologyTermRelationship get_root() {
      return path.get(0);
    }

    public Vector<GeneOntologyTermRelationship> get_path() {
      return path;
    }

    /**
     * Copy a path without use references to the value of the copied path
     * 
     * @return a new path copy of the path passed in parameter
     */
    public Path copy() {
      return new Path(this);
    }

    /**
     * Add a new GeneOntologyTermRelationship in a path (change its root) Change
     * the type of the edge between the old root and the new root. The edge type
     * of the node is defined by the edge type which leave the son to go to the
     * father.
     * 
     * @param father
     *          the GeneOntologyTermRelationship which become the new root of
     *          the path
     */
    public void addFather(GeneOntologyTermRelationship father) {
      path.add(
          0,
          new GeneOntologyTermRelationship(father.get_id(), father
              .get_type_relation()));
      if (path.size() >= 2) {
        path.get(1).set_type_relation(father.get_type_relation());
        path.get(0).set_type_relation(GeneOntologyTermRelationship.TYPE_EDGE.K);
      }
    }

    @Override
    public String toString() {
      String result = "";
      for (GeneOntologyTermRelationship gotr : path) {
        result += "[" + gotr.get_id() + " " + gotr.get_type_relation() + "]";
      }
      return result;
    }
  }

}
