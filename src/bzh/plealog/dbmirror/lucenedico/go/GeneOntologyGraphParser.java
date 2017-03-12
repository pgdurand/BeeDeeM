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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedMultigraph;

import bzh.plealog.dbmirror.lucenedico.DicoParserException;

/**
 * Parse an OBO file and create a graph
 * 
 * @author Patrick G. Durand
 * 
 */
public class GeneOntologyGraphParser {

  private DirectedMultigraph<String, Edge> _GOGraph;
  private int                              _nbVertex;

  public static enum TYPE_EDGE {
    P, I, R, O, U, N, K
  };

  /**
   * Initialize the root of the gene Ontology graph and the graph [R O O T] / |
   * \ bio_pro mol_func cell_comp | | | obso_bp obso_mf obso_cc
   */
  public GeneOntologyGraphParser() {

    _GOGraph = new DirectedMultigraph<String, Edge>(Edge.class);

    _GOGraph.addVertex("GO:0003673");
    _GOGraph.addVertex("obsolete_Bp");
    _GOGraph.addVertex("GO:0008150");
    _GOGraph.addVertex("obsolete_Mf");
    _GOGraph.addVertex("GO:0003674");
    _GOGraph.addVertex("obsolete_Cc");
    _GOGraph.addVertex("GO:0005575");

    _GOGraph.addEdge("GO:0008150", "GO:0003673", new Edge(TYPE_EDGE.K));
    _GOGraph.addEdge("GO:0003674", "GO:0003673", new Edge(TYPE_EDGE.K));
    _GOGraph.addEdge("GO:0005575", "GO:0003673", new Edge(TYPE_EDGE.K));
    _GOGraph.addEdge("obsolete_Bp", "GO:0008150", new Edge(TYPE_EDGE.K));
    _GOGraph.addEdge("obsolete_Mf", "GO:0003674", new Edge(TYPE_EDGE.K));
    _GOGraph.addEdge("obsolete_Cc", "GO:0005575", new Edge(TYPE_EDGE.K));
  }

  /**
   * Create and inquire the differents fields of the GeneOntologyTerm created
   * from parsed term.
   * 
   * @param Go_IdSource
   *          GO id of the current term
   * @param Go_IdParent
   *          Go ids of the fathers
   * @param typeEdge
   *          types of the link between current term ant its fathers
   */
  private void handleData(String Go_IdSource, ArrayList<String> Go_IdParent,
      ArrayList<TYPE_EDGE> typeEdge) {

    if (Go_IdSource != null && !Go_IdSource.equals("GO:0003673")) {

      if (!_GOGraph.containsVertex(Go_IdSource)) {
        _GOGraph.addVertex(Go_IdSource);
      }

      for (int i = 0; i < Go_IdParent.size(); i++) {
        if (!_GOGraph.containsVertex(Go_IdParent.get(i))) {
          _GOGraph.addVertex(Go_IdParent.get(i));
        }

        _GOGraph.addEdge(Go_IdSource.toString(), Go_IdParent.get(i).toString(),
            new Edge(typeEdge.get(i)));

      }
      _nbVertex++;
    }
  }

  /**
   * Implementation of DicoParsable interface.
   */
  public void parse(String file) {
    BufferedReader reader = null;
    String line;
    String go_iD = null;
    ArrayList<String> go_parent = new ArrayList<String>();
    ArrayList<TYPE_EDGE> typeEdge = new ArrayList<GeneOntologyGraphParser.TYPE_EDGE>();
    String ontology = null;
    boolean stop = false;

    try {
      _nbVertex = 0;
      reader = new BufferedReader(new InputStreamReader(new FileInputStream(
          file), "UTF-8"));
      while ((line = reader.readLine()) != null && !stop) {
        if (line.startsWith("[Term]")) {
          handleData(go_iD, go_parent, typeEdge);
          go_iD = ontology = null;
          go_parent.clear();
          typeEdge.clear();
        } else if (line.startsWith("id:")) {
          go_iD = line.substring(3).trim();
          // remove the "GO:" prefix
          // id = id.substring(id.indexOf(':')+1);
        }
        // else if(line.startsWith("name:")){
        // name = line.substring(5).trim();
        // }
        else if (line.startsWith("namespace:")) {
          ontology = line.substring(10).trim();

        } else if (line.startsWith("is_obsolete:")) {

          typeEdge.add(TYPE_EDGE.O);

          if (ontology.equals("biological_process")) {
            go_parent.add("obsolete_Bp");
          } else if (ontology.equals("molecular_function")) {
            go_parent.add("obsolete_Mf");
          } else if (ontology.equals("cellular_component")) {
            go_parent.add("obsolete_Cc");
          }

        } else if (line.startsWith("is_a:")) {
          typeEdge.add(TYPE_EDGE.I);
          String type = line.substring(5).trim().split("!")[0].trim();
          go_parent.add(type);

        } else if (line.startsWith("relationship:")) {
          String type = line.substring(13).trim().split("!")[0].trim();

          if (type.substring(0, 7).equals("part_of")) {
            typeEdge.add(TYPE_EDGE.P);
            go_parent.add(type.substring(8, 18));
          } else if (type.substring(0, 9).equals("regulates")) {
            typeEdge.add(TYPE_EDGE.R);
            go_parent.add(type.substring(10, 20));
          } else if (type.substring(0, 20).equals("positively_regulates")) {
            typeEdge.add(TYPE_EDGE.U);
            go_parent.add(type.substring(21, 31));
          } else if (type.substring(0, 20).equals("negatively_regulates")) {
            typeEdge.add(TYPE_EDGE.N);
            go_parent.add(type.substring(21, 31));
          }
        } else if (line.startsWith("[Typedef]")) {
          stop = true;
        }
      }
      // handle last term
      handleData(go_iD, go_parent, typeEdge);

    } catch (Exception e) {
      String msg = "Error while parsing GeneOntology entry no. "
          + (_nbVertex + 1);
      throw new DicoParserException(msg + ": " + e.getMessage());
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
        }
      }
    }
    if (_nbVertex == 0)
      throw new DicoParserException("Data file does not contain any terms.");
  }

  public DirectedMultigraph<String, Edge> getGraph() {
    return _GOGraph;
  }

  @SuppressWarnings("serial")
  public class Edge extends DefaultEdge {

    TYPE_EDGE type;

    public Edge(TYPE_EDGE t) {
      type = t;

    }

    public TYPE_EDGE getType() {
      return type;
    }
  }

}
