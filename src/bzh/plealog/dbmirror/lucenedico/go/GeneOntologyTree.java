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

import java.util.HashMap;

import bzh.plealog.dbmirror.lucenedico.go.GeneOntologyGraphParser.Edge;
import bzh.plealog.dbmirror.lucenedico.go.GeneOntologyGraphParser.TYPE_EDGE;

/**
 * Create a data structure from a graph
 * 
 * @author Patrick G. Durand
 * 
 */
public class GeneOntologyTree {

  GeneOntologyGraphParser           _graph;

  // String=GoId
  HashMap<String, GeneOntologyNode> _nodes;

  public GeneOntologyTree(String oboFile) {
    _nodes = new HashMap<String, GeneOntologyNode>();
    _graph = new GeneOntologyGraphParser();
    _graph.parse(oboFile);
  }

  /**
   * Create a node of the graph which a specific structure
   * 
   */
  public void createNode() {
    GeneOntologyNode node;
    HashMap<String, TYPE_EDGE> son_list;
    HashMap<String, TYPE_EDGE> father_list;

    // For each node of the graph
    for (String vertex : _graph.getGraph().vertexSet()) {
      node = new GeneOntologyNode(vertex);
      son_list = new HashMap<String, TYPE_EDGE>();
      father_list = new HashMap<String, TYPE_EDGE>();

      // We recover its sons
      for (Edge edge : _graph.getGraph().incomingEdgesOf(vertex)) {
        son_list.put(_graph.getGraph().getEdgeSource(edge), edge.getType());
      }

      // Then we recover its fathers
      for (Edge edge : _graph.getGraph().outgoingEdgesOf(vertex)) {
        father_list.put(_graph.getGraph().getEdgeTarget(edge), edge.getType());
      }

      node.set_nodes_father(father_list);
      node.set_nodes_son(son_list);
      _nodes.put(vertex, node);
    }
  }

  /**
   * Display the parents of the term
   * 
   * @param node
   *          the GO id of the node
   */
  public void checkTree(String node) {
    GeneOntologyNode nod = _nodes.get(node);
    System.out.print("fathers: ");
    for (int i = 0; i < nod.get_nodes_father().size(); i++) {
      System.out.print(nod.get_nodes_father().keySet().toArray()[i] + " ");
    }
    System.out.println("");

    System.out.print("sons: ");
    for (int i = 0; i < nod.get_nodes_son().size(); i++) {
      System.out.print(nod.get_nodes_son().keySet().toArray()[i] + "  ");
    }

  }
}
