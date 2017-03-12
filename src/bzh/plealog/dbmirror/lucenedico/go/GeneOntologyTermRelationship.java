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

/**
 * This class describe the relation between two term
 * 
 * @author Patrick G. Durand
 * 
 */
public class GeneOntologyTermRelationship implements Serializable {

  // type of the edge between two term
  // P-part of I-is_a R-regulate O-obsolete U-positively-regulate
  // N-negatively-regulate K-unknown
  public static enum TYPE_EDGE {
    P, I, R, O, U, N, K
  }

  private static final long                      serialVersionUID = 7176503273296360838L;

  private GeneOntologyTermRelationship.TYPE_EDGE _type_relation;
  private String                                 _id;

  public GeneOntologyTermRelationship() {
  }

  /**
   * Construct a relation between the term which invoke the object and the term
   * describe by the id attribute
   * 
   * @param id
   *          the GO id of a term
   * @param relation
   *          the type of the relation
   */
  public GeneOntologyTermRelationship(String id,
      GeneOntologyTermRelationship.TYPE_EDGE relation) {
    if (relation.equals(GeneOntologyTermRelationship.TYPE_EDGE.I)) {
      _type_relation = GeneOntologyTermRelationship.TYPE_EDGE.I;
    } else if (relation.equals(GeneOntologyTermRelationship.TYPE_EDGE.K)) {
      _type_relation = GeneOntologyTermRelationship.TYPE_EDGE.K;
    } else if (relation.equals(GeneOntologyTermRelationship.TYPE_EDGE.N)) {
      _type_relation = GeneOntologyTermRelationship.TYPE_EDGE.N;
    } else if (relation.equals(GeneOntologyTermRelationship.TYPE_EDGE.O)) {
      _type_relation = GeneOntologyTermRelationship.TYPE_EDGE.O;
    } else if (relation.equals(GeneOntologyTermRelationship.TYPE_EDGE.P)) {
      _type_relation = GeneOntologyTermRelationship.TYPE_EDGE.P;
    } else if (relation.equals(GeneOntologyTermRelationship.TYPE_EDGE.R)) {
      _type_relation = GeneOntologyTermRelationship.TYPE_EDGE.R;
    } else if (relation.equals(GeneOntologyTermRelationship.TYPE_EDGE.U)) {
      _type_relation = GeneOntologyTermRelationship.TYPE_EDGE.U;
    }
    _id = id;
  }

  public GeneOntologyTermRelationship(String id) {
    _type_relation = null;
    _id = id;
  }

  public GeneOntologyTermRelationship.TYPE_EDGE get_type_relation() {
    return _type_relation;
  }

  public String get_id() {
    return _id;
  }

  public void set_type_relation(GeneOntologyTermRelationship.TYPE_EDGE type_ed) {
    if (type_ed.equals(GeneOntologyTermRelationship.TYPE_EDGE.I)) {
      _type_relation = GeneOntologyTermRelationship.TYPE_EDGE.I;
    } else if (type_ed.equals(GeneOntologyTermRelationship.TYPE_EDGE.K)) {
      _type_relation = GeneOntologyTermRelationship.TYPE_EDGE.K;
    } else if (type_ed.equals(GeneOntologyTermRelationship.TYPE_EDGE.N)) {
      _type_relation = GeneOntologyTermRelationship.TYPE_EDGE.N;
    } else if (type_ed.equals(GeneOntologyTermRelationship.TYPE_EDGE.O)) {
      _type_relation = GeneOntologyTermRelationship.TYPE_EDGE.O;
    } else if (type_ed.equals(GeneOntologyTermRelationship.TYPE_EDGE.P)) {
      _type_relation = GeneOntologyTermRelationship.TYPE_EDGE.P;
    } else if (type_ed.equals(GeneOntologyTermRelationship.TYPE_EDGE.R)) {
      _type_relation = GeneOntologyTermRelationship.TYPE_EDGE.R;
    } else if (type_ed.equals(GeneOntologyTermRelationship.TYPE_EDGE.U)) {
      _type_relation = GeneOntologyTermRelationship.TYPE_EDGE.U;
    }
  }

}
