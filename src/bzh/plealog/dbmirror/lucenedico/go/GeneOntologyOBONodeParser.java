/* Copyright (C) 2007-2021 Patrick G. Durand
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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import bzh.plealog.dbmirror.indexer.ParserMonitor;
import bzh.plealog.dbmirror.lucenedico.DicoParsable;
import bzh.plealog.dbmirror.lucenedico.DicoParserException;
import bzh.plealog.dbmirror.lucenedico.DicoStorageSystem;
import bzh.plealog.dbmirror.lucenedico.DicoUtils;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;

/**
 * Class which parses obo file and create, for each of terms a GeneOntologyTerm
 * containing the name, the GO id, the ontology and the parents of the term.
 * 
 * @author Patrick G. Durand
 * 
 */
public class GeneOntologyOBONodeParser implements DicoParsable {

  // terms of the obo file
  private HashMap<String, GeneOntologyTerm> _nodes                      = new HashMap<String, GeneOntologyTerm>();
  private ParserMonitor                     _pMonitor;
  private boolean                           _verbose                    = true;
  private int                               _entries;

  private static final Logger               LOGGER                      = LogManager.getLogger(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                                                + ".GeneOntologyOBONodeParser");

  private static final String               ROOT_GO                     = "GO:0003673";
  private static final String               ROOT_NAME                   = "Gene_Ontology";
  private static final String               BIO_PRO_GO                  = "GO:0008150";
  private static final String               BIO_PRO_ONT                 = "biological_process";
  private static final String               MOL_FUNC_GO                 = "GO:0003674";
  private static final String               MOL_FUNC_ONT                = "molecular_function";
  private static final String               CELL_COMP_GO                = "GO:0005575";
  private static final String               CELL_COMP_ONT               = "cellular_component";

  private static final String               OBSO_BIOPRO_GO              = "obsolete_Bp";
  private static final String               OBSO_BIOPRO_NAME            = "obsolete_biological_process";
  private static final String               OBSO_MOLFUNC_GO             = "obsolete_Mf";
  private static final String               OBSO_MOLFUNC_NAME           = "obsolete_molecular_function";
  private static final String               OBSO_CELLCOMP_GO            = "obsolete_Cc";
  private static final String               OBSO_CELLCOMP_NAME          = "obsolete_cellular_component";

  private static final String               TERM                        = "[Term]";
  private static final String               ID                          = "id:";
  private static final String               NAME                        = "name:";
  private static final String               IS_OBSOLETE                 = "is_obsolete:";
  private static final String               NAMESPACE                   = "namespace:";
  private static final String               IS_A                        = "is_a:";
  private static final String               RELATIONSHIP                = "relationship:";
  private static final String               PART_OF                     = "part_of";
  private static final String               REGULATES                   = "regulates";
  private static final String               POSITIVELY_REGULATES        = "positively_regulates";
  private static final String               NEGATIVELY_REGULATES        = "negatively_regulates";
  private static final String               TYPE_DEF                    = "[Typedef]";
  private static final String               ALT_ID                      = "alt_id";

  private static final int                  IS_A_LENGTH                 = IS_A
                                                                            .length();
  private static final int                  PART_OF_LENGTH              = PART_OF
                                                                            .length();
  private static final int                  REGULATES_LENGTH            = REGULATES
                                                                            .length();
  private static final int                  POSITIVELY_REGULATES_LENGTH = POSITIVELY_REGULATES
                                                                            .length();
  private static final int                  NEGATIVELY_REGULATES_LENGTH = NEGATIVELY_REGULATES
                                                                            .length();
  private static final int                  RELATIONSHIP_LENGTH         = RELATIONSHIP
                                                                            .length();

  public GeneOntologyOBONodeParser() {
    _nodes = new HashMap<String, GeneOntologyTerm>();
    _verbose = false;
    _entries = 0;
  }

  /**
   * Implementation of DicoParsable interface.
   */
  public void setParserMonitor(ParserMonitor pm) {
    _pMonitor = pm;
  }

  /**
   * Initialize the root of the gene Ontology graph. [R O O T] / | \ bio_pro
   * mol_func cell_comp | | | obso_bp obso_mf obso_cc
   */
  private void initStructure() {
    GeneOntologyTerm root = new GeneOntologyTerm(ROOT_GO, ROOT_NAME, null);
    _nodes.put(ROOT_GO, root);
    GeneOntologyTerm bio_p = new GeneOntologyTerm(BIO_PRO_GO, BIO_PRO_ONT,
        BIO_PRO_ONT);
    bio_p.add_parent(root, GeneOntologyTermRelationship.TYPE_EDGE.K);
    _nodes.put(BIO_PRO_GO, bio_p);
    GeneOntologyTerm mol_fun = new GeneOntologyTerm(MOL_FUNC_GO, MOL_FUNC_ONT,
        MOL_FUNC_ONT);
    mol_fun.add_parent(root, GeneOntologyTermRelationship.TYPE_EDGE.K);
    _nodes.put(MOL_FUNC_GO, mol_fun);
    GeneOntologyTerm cell_comp = new GeneOntologyTerm(CELL_COMP_GO,
        CELL_COMP_ONT, CELL_COMP_ONT);
    cell_comp.add_parent(root, GeneOntologyTermRelationship.TYPE_EDGE.K);
    _nodes.put(CELL_COMP_GO, cell_comp);
    GeneOntologyTerm o_bp = new GeneOntologyTerm(OBSO_BIOPRO_GO,
        OBSO_BIOPRO_NAME, BIO_PRO_ONT);
    o_bp.add_parent(bio_p, GeneOntologyTermRelationship.TYPE_EDGE.K);
    _nodes.put(OBSO_BIOPRO_GO, o_bp);
    GeneOntologyTerm o_mf = new GeneOntologyTerm(OBSO_MOLFUNC_GO,
        OBSO_MOLFUNC_NAME, MOL_FUNC_ONT);
    o_mf.add_parent(mol_fun, GeneOntologyTermRelationship.TYPE_EDGE.K);
    _nodes.put(OBSO_MOLFUNC_GO, o_mf);
    GeneOntologyTerm o_cc = new GeneOntologyTerm(OBSO_CELLCOMP_GO,
        OBSO_CELLCOMP_NAME, CELL_COMP_ONT);
    o_cc.add_parent(cell_comp, GeneOntologyTermRelationship.TYPE_EDGE.K);
    _nodes.put(OBSO_CELLCOMP_GO, o_cc);
    _entries = 6;
  }

  /**
   * Create and inquire the differents fields of the GeneOntologyTerm created
   * from parsed term.
   * 
   * @param file
   *          the file ro process
   * @param goIdTerm
   *          GO id of the term obtained by the "id" field
   * @param nameAndOntology
   *          of the term obtained by the "name" field
   * @param id_parent
   *          fathers of the term obtained by the "is_a/part_of/regulate" field
   * @param edge
   *          type of the link between a term's parent and the term. depends on
   *          "is_a/part_of/regulate" field
   * @param alternative_id
   *          alternative GO id of the term obtained by the "alt_id" field
   * @param curPos
   *          current position in the file (butes)
   */
  private void handleData(String file, String goIdTerm, String nameAndOntology,
      ArrayList<String> id_parent,
      ArrayList<GeneOntologyTermRelationship.TYPE_EDGE> edge,
      ArrayList<String> alternative_id, long curPos) {
    GeneOntologyTerm son;
    GeneOntologyTerm father;

    // Ignore the root to avoid bugs
    if (goIdTerm != null && !goIdTerm.equals(ROOT_GO)
        && nameAndOntology != null) {
      if (!_nodes.containsKey(goIdTerm)) {
        son = new GeneOntologyTerm(goIdTerm, nameAndOntology);
        _nodes.put(goIdTerm, son);
      } else if (_nodes.get(goIdTerm).get_node_name() == null) {
        son = _nodes.get(goIdTerm);
        son.set_node_ontology(nameAndOntology);
        son.set_node_name(nameAndOntology);
        son.set_node_nameAndOntology(nameAndOntology);
      } else {
        son = _nodes.get(goIdTerm);
      }

      if (_pMonitor != null) {
        _pMonitor.seqFound(goIdTerm, nameAndOntology, file, curPos, curPos,
            false);
      }
      // For each father of the term scanned
      for (int i = 0; i < id_parent.size(); i++) {
        if (!_nodes.containsKey(id_parent.get(i))) {
          father = new GeneOntologyTerm(id_parent.get(i));
          son.add_parent(father, edge.get(i));
          _nodes.put(father.get_node_id(), father);
        } else {
          father = _nodes.get(id_parent.get(i));
          // add the father to the fatherlist of the current term and add the
          // current term as son in the sonlist of the father term
          son.add_parent(father, edge.get(i));
        }
      }

      if (!alternative_id.isEmpty()) {
        for (String alt_id : alternative_id) {
          _nodes.put(alt_id, son);
        }
      }

      if (_verbose && (_entries % 5000) == 0) {
        System.out.println("Nb term parsed: " + _entries);
      }
    }
  }

  public static String prepareTerm(String name, String ontology) {
    StringBuffer buf = new StringBuffer();
    buf.delete(0, buf.length());
    buf.append(DicoUtils.GO_NAME_KEY);
    buf.append(name);
    buf.append("|");
    buf.append(DicoUtils.GO_ONTO_KEY);
    if (ontology != null)
      buf.append(ontology);
    else
      buf.append("unknown");
    return buf.toString();
  }

  /**
   * Parse the obo file
   */
  public void parse(String file, DicoStorageSystem ss)
      throws DicoParserException {
    BufferedReader reader = null;
    String line;
    String go_iD = null;
    String go_name = null;
    String type = null;
    String termPrepared;
    String alt_id_term;
    String ontology = null;
    ArrayList<String> go_parent = new ArrayList<String>();
    ArrayList<GeneOntologyTermRelationship.TYPE_EDGE> typeEdge = new ArrayList<GeneOntologyTermRelationship.TYPE_EDGE>();
    ArrayList<String> alt_id = new ArrayList<String>();
    int endOfLineSize, termCounter = 0;
    long curPos = 0;
    boolean stop = false;

    try {
      _entries = 0;
      initStructure();
      endOfLineSize = Utils.getLineTerminatorSize(file);
      reader = new BufferedReader(new InputStreamReader(new FileInputStream(
          file), "UTF-8"));
      if (_pMonitor != null) {
        _pMonitor.startProcessingFile(file, new File(file).length());
      }
      while ((line = reader.readLine()) != null && !stop) {
        if (line.startsWith(TERM)) {
          termCounter++;
          termPrepared = prepareTerm(go_name, ontology);
          handleData(file, go_iD, termPrepared, go_parent, typeEdge, alt_id,
              curPos);
          go_iD = ontology = go_name = type = null;
          go_parent.clear();
          typeEdge.clear();
          alt_id.clear();
        } else if (line.startsWith(ID)) {
          go_iD = line.substring(3).trim();
        } else if (line.startsWith(NAME)) {
          go_name = line.substring(5).trim();
        } else if (line.startsWith(NAMESPACE)) {
          ontology = line.substring(10).trim();

        } else if (line.startsWith(IS_OBSOLETE)) {

          typeEdge.add(GeneOntologyTermRelationship.TYPE_EDGE.O);

          if (ontology.equals(BIO_PRO_ONT)) {
            go_parent.add(OBSO_BIOPRO_GO);
          } else if (ontology.equals(MOL_FUNC_ONT)) {
            go_parent.add(OBSO_MOLFUNC_GO);
          } else if (ontology.equals(CELL_COMP_GO)) {
            go_parent.add(OBSO_CELLCOMP_GO);
          }

        } else if (line.startsWith(IS_A)) {
          typeEdge.add(GeneOntologyTermRelationship.TYPE_EDGE.I);
          type = line.substring(IS_A_LENGTH + 1).trim().split("!")[0].trim();
          go_parent.add(type);

        } else if (line.startsWith(RELATIONSHIP)) {
          type = line.substring(RELATIONSHIP_LENGTH + 1).trim().split("!")[0]
              .trim();

          if (type.startsWith(PART_OF)) {
            typeEdge.add(GeneOntologyTermRelationship.TYPE_EDGE.P);
            go_parent.add(type.substring(PART_OF_LENGTH + 1));
          } else if (type.startsWith(REGULATES)) {
            typeEdge.add(GeneOntologyTermRelationship.TYPE_EDGE.R);
            go_parent.add(type.substring(REGULATES_LENGTH + 1));
          } else if (type.startsWith(POSITIVELY_REGULATES)) {
            typeEdge.add(GeneOntologyTermRelationship.TYPE_EDGE.U);
            go_parent.add(type.substring(POSITIVELY_REGULATES_LENGTH + 1));
          } else if (type.startsWith(NEGATIVELY_REGULATES)) {
            typeEdge.add(GeneOntologyTermRelationship.TYPE_EDGE.N);
            go_parent.add(type.substring(NEGATIVELY_REGULATES_LENGTH + 1));
          }
        } else if (line.startsWith(TYPE_DEF)) {
          stop = true;
        } else if (line.startsWith(ALT_ID)) {
          alt_id_term = line.substring(7).trim();
          alt_id.add(alt_id_term);
        }

        curPos += (long) (line.length() + endOfLineSize);

      }
      // handle last term
      termPrepared = prepareTerm(go_name, ontology);
      handleData(file, go_iD, termPrepared, go_parent, typeEdge, alt_id, curPos);

      // add Term in a Lucene index
      indexingSequences(ss);

    } catch (Exception e) {
      String msg = "Error while parsing GeneOntology entry no. "
          + (termCounter + 1);
      LOGGER.warn(msg + ": " + e);
      throw new DicoParserException(msg + ": " + e.getMessage());
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
        }
      }
      if (_pMonitor != null) {
        _pMonitor.stopProcessingFile(file, _entries);
      }
    }
    if (_entries == 0)
      throw new DicoParserException("Data file does not contain any terms.");
  }

  public HashMap<String, GeneOntologyTerm> get_nodes() {
    return _nodes;
  }

  /**
   * For each term found, we add it in a Lucene index
   * 
   * @param ss
   *          link to index
   */
  private void indexingSequences(DicoStorageSystem ss) {
    GeneOntologyTerm term;
    Object[] keys = _nodes.keySet().toArray();
    int size;

    size = keys.length;
    _entries += size;
    for (int i = 0; i < size; i++) {
      term = _nodes.get(keys[i]);

      if (ss != null) {
        ss.addBinaryEntry((String) keys[i], term);
      }
    }
  }

  /**
   * Implementation of DicoParsable interface.
   */
  public void setVerbose(boolean verbose) {
    _verbose = verbose;
  }

  public int getTerms() {
    return _entries;
  }
}
