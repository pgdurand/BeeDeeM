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

/**
 * Node create from a vertex of the graph 
 * 
 */

import bzh.plealog.dbmirror.lucenedico.go.GeneOntologyGraphParser.TYPE_EDGE;

public class GeneOntologyNode {

  String                     _node_id;
  HashMap<String, TYPE_EDGE> _nodes_son;
  HashMap<String, TYPE_EDGE> _nodes_father;

  public GeneOntologyNode(String id) {
    _node_id = id;
  }

  public HashMap<String, TYPE_EDGE> get_nodes_son() {
    return _nodes_son;
  }

  public void set_nodes_son(HashMap<String, TYPE_EDGE> _nodes_son) {
    this._nodes_son = _nodes_son;
  }

  public HashMap<String, TYPE_EDGE> get_nodes_father() {
    return _nodes_father;
  }

  public void set_nodes_father(HashMap<String, TYPE_EDGE> list) {
    this._nodes_father = list;
  }

  public String get_node_id() {
    return _node_id;
  }

}
