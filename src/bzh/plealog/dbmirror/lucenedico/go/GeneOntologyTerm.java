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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import bzh.plealog.dbmirror.lucenedico.DicoUtils;

/**
 * Structure of a Gene Ontology Term
 * 
 * @author Patrick G. Durand
 * 
 */
public class GeneOntologyTerm implements Serializable {

  private static final long                  serialVersionUID = -9215060339049262201L;

  private String                             _node_id;
  private String                             _node_name;
  private String                             _node_ontology;
  private String                             _node_nameAndOntology;
  private List<GeneOntologyTermRelationship> _node_sons;
  private List<GeneOntologyTermRelationship> _node_father;

  public GeneOntologyTerm() {
  }

  /**
   * Constructor
   * 
   * @param id
   *          the Go id of the term
   */
  public GeneOntologyTerm(String id) {
    _node_id = id;
    _node_father = new ArrayList<GeneOntologyTermRelationship>();
    _node_sons = new ArrayList<GeneOntologyTermRelationship>();
  }

  /**
   * Constructor
   * 
   * @param id
   *          the Go id of the term
   * @param name
   *          the name of the term
   * @param ontology
   *          the ontology of the term
   */
  public GeneOntologyTerm(String id, String name, String ontology) {
    _node_id = id;
    _node_name = name;
    _node_ontology = ontology;
    _node_father = new ArrayList<GeneOntologyTermRelationship>();
    _node_sons = new ArrayList<GeneOntologyTermRelationship>();
    _node_nameAndOntology = GeneOntologyOBONodeParser.prepareTerm(name,
        ontology);

  }

  /**
   * Constructor
   * 
   * @param id
   *          the Go id of the term
   * @param nameAndOntology
   *          the Go ontology and the name of the term formated as this way :
   *          "na="name"|na="ontology
   */
  public GeneOntologyTerm(String id, String nameAndOntology) {
    String name = null;
    String ontology = null;
    String[] nameAndOntologySplitted;

    _node_id = id;
    _node_nameAndOntology = nameAndOntology;

    nameAndOntologySplitted = unFormateNameAndOntology(nameAndOntology);
    if (nameAndOntologySplitted != null) {
      name = nameAndOntologySplitted[0];
      ontology = nameAndOntologySplitted[1];
    }

    _node_name = name;
    _node_ontology = ontology;

    _node_father = new ArrayList<GeneOntologyTermRelationship>();
    _node_sons = new ArrayList<GeneOntologyTermRelationship>();

  }

  /**
   * Add a father for the current term and add the current term as a son for the
   * father term passed in param
   * 
   * @param nodeFather
   *          the father of the current term
   * @param edge
   *          type of the link between current term and its father
   */
  public void add_parent(GeneOntologyTerm nodeFather,
      GeneOntologyTermRelationship.TYPE_EDGE edge) {
    GeneOntologyTermRelationship trs = new GeneOntologyTermRelationship(
        this.get_node_id(), edge);
    GeneOntologyTermRelationship trf = new GeneOntologyTermRelationship(
        nodeFather.get_node_id(), edge);
    _node_father.add(trf);
    nodeFather.get_node_sons().add(trs);

  }

  public String[] unFormateNameAndOntology(String nameAndOntology) {
    int index1 = 0;
    int index2 = 0;
    int indexSeparator = 0;
    // [0]=name [1]=ontology
    String[] nameAndOntologySplited = new String[2];

    indexSeparator = nameAndOntology.indexOf("|");
    if (indexSeparator != -1) {
      index1 = DicoUtils.GO_NAME_KEY.toCharArray().length;
      index2 = indexSeparator;
      nameAndOntologySplited[0] = nameAndOntology.substring(index1, index2);
      index1 = DicoUtils.GO_ONTO_KEY.toCharArray().length + index2 + 1;
      index2 = nameAndOntology.toCharArray().length;
      nameAndOntologySplited[1] = nameAndOntology.substring(index1, index2);
      return nameAndOntologySplited;
    } else {
      return null;
    }
  }

  public void set_node_id(String _node_id) {
    this._node_id = _node_id;
  }

  public String getFormatedNameAndOntology() {
    return _node_nameAndOntology;
  }

  public String get_node_name() {
    return _node_name;
  }

  public void set_node_name(String _node_name) {
    String[] nameSplited;

    nameSplited = unFormateNameAndOntology(_node_name);
    if (nameSplited != null) {
      this._node_name = nameSplited[0];
    } else {
      this._node_name = _node_name;
    }
  }

  public String get_node_ontology() {
    return _node_ontology;
  }

  public String get_node_ontology_code() {
    if (_node_ontology.charAt(0)=='c') {
      // component
      return "C";
    }
    else if (_node_ontology.charAt(0)=='m') {
      // molecular_function
      return "F";
    }
    else if (_node_ontology.charAt(0)=='b') {
      // biological_process
      return "P";
    }
    else {
      return "?";
    }
  }
  
  public void set_node_ontology(String _node_ontology) {
    String[] ontologySplited;

    ontologySplited = unFormateNameAndOntology(_node_ontology);
    if (ontologySplited != null) {
      this._node_ontology = ontologySplited[1];
    } else {
      this._node_ontology = _node_ontology;
    }
  }

  public void set_node_nameAndOntology(String node_nameAndOntology) {
    this._node_nameAndOntology = node_nameAndOntology;
  }

  public String get_node_id() {
    return _node_id;
  }

  public List<GeneOntologyTermRelationship> get_node_sons() {
    return _node_sons;
  }

  public List<GeneOntologyTermRelationship> get_node_father() {
    return _node_father;
  }

}
