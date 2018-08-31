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
package bzh.plealog.dbmirror.lucenedico;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.collections.OrderedMap;
import org.apache.commons.collections.map.LinkedMap;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.lucenedico.go.GeneOntologyGenerateTree;
import bzh.plealog.dbmirror.lucenedico.go.GeneOntologyGenerateTree.Path;
import bzh.plealog.dbmirror.lucenedico.go.GeneOntologyTerm;
import bzh.plealog.dbmirror.lucenedico.go.GeneOntologyTermRelationship;
import bzh.plealog.dbmirror.lucenedico.tax.TaxonomyRank;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.conf.DBMirrorConfig;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptor;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptorUtils;
import bzh.plealog.dbmirror.util.descriptor.IdxDescriptor;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * This class manages an access to KDMS Lucene Dictionary that can be used to
 * annotate Blast results during the search.
 * 
 * @author Patrick G. Durand
 */
public class DicoTermQuerySystem {
  private GeneOntologyGenerateTree          _goTree;
  private boolean                           isClosed = false;

  private HashMap<Dicos, DicoStorageSystem> storages = new HashMap<Dicos, DicoStorageSystem>();

  private static DicoTermQuerySystem        _dicoSystem;
  private static final Object               SEM      = new Object();
  private static final Log                  LOGGER   = LogFactory
                                                         .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                             + ".DicoStorageSystemImplem");

  private DicoTermQuerySystem() {
  }

  /**
   * Constructor. Please note that this constructor will open Lucene indexes
   * (when available) in read mode. See close() method.
   */
  private DicoTermQuerySystem(DBMirrorConfig mirrorCfg) {
    List<IdxDescriptor> dbs;

    dbs = DBDescriptorUtils.prepareIndexDBList(mirrorCfg);
    for (IdxDescriptor desc : dbs) {
      if (desc.getType().equals(DBDescriptor.TYPE.dico) == false)
        continue;
      Dicos dico = Dicos.getAssociatedDico(desc.getReader());
      if (dico != null) {
        storages.put(dico, dico.getDicoQuery(desc.getCode()));
      }
    }
    _goTree = new GeneOntologyGenerateTree();
  }

  private DicoTermQuerySystem(Map<String, String> dicos) {
    Iterator<String> readers = dicos.keySet().iterator();
    while(readers.hasNext()) {
      // reader: one of DicoUtils.READER_xxx
      String reader = readers.next();
      Dicos dico = Dicos.getAssociatedDico(reader);
      if (dico != null) {
        storages.put(dico, dico.getDicoQuery(dicos.get(reader)));
      }
      else {
        LoggerCentral.warn(LOGGER, "unable to find reader: "+reader);
      }
    }
    _goTree = new GeneOntologyGenerateTree();
  }

  /**
   * Instantiate a fresh DicoTermQuerySystem.
   * 
   * @param dicos map where keys are reader types for dictionary and values are absolute paths
   * to corresponding Lucene index. Readers must be one of DicoUtils.READER_xxx constants targetting
   * dicos: GO, Enzyme, etc.
   */
  public static DicoTermQuerySystem getDicoTermQuerySystem(
      Map<String, String> dicos) {
    synchronized (SEM) {
      if ((_dicoSystem == null) || (_dicoSystem.isClosed)
          || (_dicoSystem.storages.size() == 0)) {
        _dicoSystem = new DicoTermQuerySystem(dicos);
      }
      return _dicoSystem;
    }
  }

  /**
   * Instantiate a fresh DicoTermQuerySystem.
   * 
   * @param mirrorCfg a DBMirrorConfig object containing BeeDeeM configuration
   */
  public static DicoTermQuerySystem getDicoTermQuerySystem(
      DBMirrorConfig mirrorCfg) {
    synchronized (SEM) {
      if ((_dicoSystem == null) || (_dicoSystem.isClosed)
          || (_dicoSystem.storages.size() == 0)) {
        _dicoSystem = new DicoTermQuerySystem(mirrorCfg);
      }
      return _dicoSystem;
    }
  }

  public static void closeDicoTermQuerySystem() {
    synchronized (SEM) {
      if (_dicoSystem != null) {
        _dicoSystem.close();
        _dicoSystem = null;
      }
    }
  }

  /**
   * Do not forget to call this method when annotation process is finished.
   */
  private void close() {
    for (DicoStorageSystem storage : storages.values()) {
      if (storage != null) {
        storage.close();
      }
    }
    storages = new HashMap<Dicos, DicoStorageSystem>();
    _goTree = null;
    // needed since we use a RAMDirectory: ask JRE to free the data loaded by
    // Lucene
    System.gc();
    this.isClosed = true;
  }

  /**
   * Figures out if a particular dictionary is available.
   */
  public boolean hasDicoAvailable(Dicos dico) {
    return (this.storages.containsKey(dico))
        && (this.storages.get(dico) != null);
  }

  public DicoStorageSystem getStorage(Dicos dico) {
    return this.storages.get(dico);
  }

  /**
   * Get a term given its dictionary type and ID. <br>
   * <br>
   * Parameter id must be as follows. <br>
   * <br>
   * For EC number, id must be formatted as 'A.B.C.D' (wihtout quotes) where
   * A..D are numbers. For some EC codes B, C or D can be missing (EC master
   * classes, for example: 1.1. Acting on the CH-OH group of donors.). In such
   * cases, pass an hyphen (-): 1.1.-.- in our example. <br>
   * <br>
   * For Taxonomy, id is a number. For example, use 9606 to get Homo sapiens. <br>
   * <br>
   * For GO, id must be formatted as 'GO:A' (without quotes) where 'GO:' is a
   * constant and A is a number (example: GO:0004599). <br>
   * <br>
   * For Interpro, id must be formatted as 'IPRA' (without quotes) where IPR (a
   * constant) is immediately followed by A, a number (example: IPR000038). <br>
   * <br>
   * For PFam, id must be formatted as 'PFA' (without quotes) where PF (a
   * constant) is immediately followed by A, a number (example: PF04720).
   */
  public DicoTerm getTerm(Dicos dico, String id) {
    if (dico.equals(Dicos.NCBI_TAXONOMY)) {
      id = DicoUtils.TAX_ID_NAME_PREFIX + id;
    }

    try {
      return storages.get(dico).getTerm(id);
    } catch (NullPointerException ex) {
      // use a catch instead of a storages.containsKey() to speed up
      // dico is not available.
      return null;
    }

  }

  public ArrayList<String> getGoPath(String id) {
    ArrayList<Path> tree = new ArrayList<Path>();
    ArrayList<String> pathsFormated = new ArrayList<String>();
    Vector<GeneOntologyTermRelationship> pathsList = new Vector<GeneOntologyTermRelationship>();
    GeneOntologyTerm goTermLuc = null;
    DicoTerm term;
    String idTerm, name;
    GeneOntologyTermRelationship.TYPE_EDGE typeEdge;
    StringBuffer pathFormated;

    if (hasDicoAvailable(Dicos.GENE_ONTOLOGY)) {
      tree = _goTree.createTree(id, storages.get(Dicos.GENE_ONTOLOGY), false);
      for (Path path : tree) {
        pathFormated = new StringBuffer("");
        pathsList = path.get_path();
        for (GeneOntologyTermRelationship goTerm : pathsList) {
          term = getTerm(Dicos.GENE_ONTOLOGY, goTerm.get_id());
          if (term != null) {
            idTerm = goTerm.get_id();
            goTermLuc = (GeneOntologyTerm) term.get_dataObject();
            name = goTermLuc.get_node_name();
            typeEdge = goTerm.get_type_relation();
            pathFormated.append("[");
            pathFormated.append(typeEdge);
            pathFormated.append("] ");
            pathFormated.append(idTerm);
            pathFormated.append(" ");
            pathFormated.append(name);
            pathFormated.append(";");
          }
        }
        if (StringUtils.isNotBlank(pathFormated.toString())) {
          pathFormated = new StringBuffer(pathFormated.substring(0,
              pathFormated.length() - 1));
          pathsFormated.add(pathFormated.toString());
        }
      }
    }
    return pathsFormated;
  }

  public ArrayList<String> getGoPathId(String id) {
    ArrayList<Path> tree = new ArrayList<Path>();
    ArrayList<String> pathsFormated = new ArrayList<String>();
    Vector<GeneOntologyTermRelationship> paths = new Vector<GeneOntologyTermRelationship>();
    StringBuffer pathFormated;

    if (hasDicoAvailable(Dicos.GENE_ONTOLOGY)) {
      tree = _goTree.createTree(id, storages.get(Dicos.GENE_ONTOLOGY), false);
      for (Path path : tree) {
        pathFormated = new StringBuffer("");
        paths = path.get_path();
        for (GeneOntologyTermRelationship goTerm : paths) {
          pathFormated.append(goTerm.get_id());
          pathFormated.append(";");
        }
        if (StringUtils.isNotBlank(pathFormated.toString())) {
          pathFormated = new StringBuffer(pathFormated.substring(0,
              pathFormated.length() - 1));
          pathsFormated.add(pathFormated.toString());
        }
      }
    }
    return pathsFormated;
  }

  public String getEnzymePath(String id) {
    DicoTerm term = null;
    try {
      term = this.getTerm(Dicos.ENZYME, id);
    } catch (DicoStorageSystemException ex) {
      LoggerCentral.warn(LOGGER, "Unable to get enzyme term for '" + id
          + "' : " + ex.getMessage());
      return null;
    }
    if (term != null) {
      // it is a real enzyme term formatted like a.b.c.d
      // so now, get all terms for each part
      // a.b.c then a.b then a
      StringBuilder result = new StringBuilder();
      result.append(term.getId().trim());
      result.append(DicoUtils.ENZYME_ID_LABEL_SEPARATOR);
      result.append(term.getDataField().trim());
      int indexDot = id.lastIndexOf(".");
      while (indexDot > 0) {
        id = id.substring(0, indexDot);

        try {
          term = this.getTerm(Dicos.ENZYME, id);
        } catch (DicoStorageSystemException ex) {
          LoggerCentral.warn(LOGGER, "Unable to get enzyme term for '" + id
              + "' : " + ex.getMessage());
          return null;
        }
        if (term != null) {
          // insert a new 'node' at the beginning of the path
          result.insert(0, DicoUtils.ENZYME_NODE_SEPARATOR);
          result.insert(0, term.getDataField().trim());
          result.insert(0, DicoUtils.ENZYME_ID_LABEL_SEPARATOR);
          result.insert(0, term.getId().trim());
        }

        indexDot = id.lastIndexOf(".");
      }

      return result.toString();
    } else {
      return null;
    }
  }

  /**
   * Get a full taxonomic path of strings given an organism ID. Returns null if
   * not found.
   */
  public String getTaxPath(String id) {
    return getTaxPath(id, true, false, false);
  }

  public String getTaxPath(String id, boolean simplified) {
    return getTaxPath(id, simplified, false, false);
  }
  
  public String getTaxPath(String id, boolean simplified, boolean includeOrganism, boolean tagMissingRank) {
    StringBuffer ids = new StringBuffer();

    OrderedMap taxonPath = getTaxPath(id, simplified, null);

    if (taxonPath != null) {
      TaxonomyRank[] taxRanks = TaxonomyRank.values();
      if (tagMissingRank) {
        DicoTerm term;
        term = (DicoTerm) taxonPath.firstKey();
        TaxonomyRank rank = (TaxonomyRank) taxonPath.get(term);
        for(TaxonomyRank taxRank : taxRanks) {
          if (rank != null && rank.getLevel()==taxRank.getLevel()) {
            String termId = term.getId().substring(1);
            if (termId.equals(id) && !includeOrganism)
              break; // skip this id: the organism
            ids.append(rank.getLevelCode());
            ids.append(this.storages.get(Dicos.NCBI_TAXONOMY)
                .getTerm(DicoUtils.TAX_ID_NAME_PREFIX + termId).getDataField());
            term = (DicoTerm) taxonPath.nextKey(term);
            rank = (TaxonomyRank) taxonPath.get(term);
          }
          else {
            ids.append(taxRank.getLevelCode());
            ids.append("unknown");
          }
          ids.append(";");
        }
      }
      else {
        for (Object o : taxonPath.keySet()) {
          if (o instanceof DicoTerm) {
            DicoTerm term = (DicoTerm) o;
            String termId = term.getId().substring(1);
            if (termId.equals(id) && !includeOrganism)
              break; // skip this id: the organism
            ids.append(((TaxonomyRank) taxonPath.get(term)).getLevelCode());
            ids.append(this.storages.get(Dicos.NCBI_TAXONOMY)
                .getTerm(DicoUtils.TAX_ID_NAME_PREFIX + termId).getDataField());
            ids.append(";");
          }
        }
      }
    }

    if (StringUtils.isNotBlank(ids.toString())) {
      return ids.toString();
    } else {
      return null;
    }
  }

  /**
   * Same as getTaxPath but returns a path made of taxon ID.
   */
  public String getTaxPathIds(String id) {
    return getTaxPathIds(id, true);
  }

  public String getTaxPathIds(String id, boolean simplified) {
    return getTaxPathIds(id, simplified, false);
  }
  /**
   * Same as getTaxPath but returns a path made of taxon ID.
   */
  public String getTaxPathIds(String id, boolean simplified, boolean includeOrganism) {
    StringBuffer ids = new StringBuffer();

    OrderedMap taxonPath = getTaxPath(id, simplified, null);

    if (taxonPath != null) {
      for (Object o : taxonPath.keySet()) {
        if (o instanceof DicoTerm) {
          DicoTerm term = (DicoTerm) o;
          String termId = term.getId().substring(1);
          if (termId.equals(id) && !includeOrganism)
            break; // skip this id: the organism

          ids.append(DicoUtils.TAX_ID_NAME_PREFIX + termId);
          ids.append(";");
        }
      }
    }

    if (StringUtils.isNotBlank(ids.toString())) {
      return ids.toString();
    } else {
      return null;
    }
  }

  /**
   * Given a tax id we search its parent as given rank
   */
  public String getTaxIdForRank(String id, TaxonomyRank taxRank) {
    OrderedMap taxonPath = getTaxPath(id, true, taxRank);

    if (taxonPath != null) {
      for (Object o : taxonPath.keySet()) {
        if (o instanceof DicoTerm) {
          DicoTerm term = (DicoTerm) o;
          if (taxonPath.get(term).equals(taxRank)) {
            return term.getId().substring(1);
          }
        }
      }
    }

    return "";
  }

  /**
   * Get a map of taxonomy DicoTerms associated with her TaxonomyRank
   */
  @SuppressWarnings("unchecked")
  private OrderedMap getTaxPath(String id, boolean simplified,
      TaxonomyRank taxRank) {
    OrderedMap taxonPath = new LinkedMap();
    TaxonomyRank taxRankFound;
    DicoTerm[] terms;
    DicoTerm taxonTerm;
    String dicoRankName;

    // 1: get taxon term : keep ??????
    taxonTerm = getTerm(Dicos.NCBI_TAXONOMY, id);
    if (taxonTerm == null)
      return null;

    // 2: get taxon term path
    terms = this.storages.get(Dicos.NCBI_TAXONOMY).getTerms(
        DicoUtils.TAX_ID_NODE_PREFIX + id, "o1");
    if (terms == null || terms.length <= 1)// no result
      return null;

    // First pass : we search one term by taxonomy rank (level name)
    for (DicoTerm term : terms) {
      dicoRankName = getTaxRankFromDataField(term.getDataField());
      taxRankFound = TaxonomyRank.getTaxonomyRank(dicoRankName, true);
      taxonPath.put(term, taxRankFound);
    }

    // Second pass (only for simplified)
    if (simplified) {
      for (DicoTerm term : terms) {

        // already treated ?
        if (taxonPath.get(term) != null)
          continue;

        // if simplified we search one term by taxonomy rank (synonym names)
        dicoRankName = getTaxRankFromDataField(term.getDataField());
        taxRankFound = TaxonomyRank.getTaxonomyRank(dicoRankName, false);

        if (taxRankFound != null && !taxonPath.containsValue(taxRankFound)) {
          // added only if this taxonomic rank is not already present
          taxonPath.put(term, taxRankFound);
        } else {
          // if no taxonomic rank founded, we remove this term
          taxonPath.remove(term);
        }

      }
    }

    return taxonPath;
  }

  /**
   * returns a tax id given his name.
   * 
   * @param termName
   *          taxonomy scientific name
   * @return a taxonomy ID
   */
  public String getTaxID(String termName) {
    try {
      return storages.get(Dicos.NCBI_TAXONOMY).getID(termName);
    } catch (NullPointerException ex) {
      return null;
    }
  }

  private String getTaxRankFromDataField(String dataField) {
    int idx1;
    if (dataField != null && (idx1 = dataField.indexOf('|')) != -1) {
      return dataField.substring(idx1 + 1).trim();
    } else {
      return null;
    }
  }

  /**
   * returns a TaxonomyRank given his name.
   * 
   * @param id
   * @return TaxonomyRank
   */
  public TaxonomyRank getSimplifiedTaxRank(String id) {
    return TaxonomyRank.getTaxonomyRank(getRealTaxRank(id));
  }

  /**
   * returns a tax rank given his name.
   * 
   * @param id
   * @return rank
   */
  public String getRealTaxRank(String id) {
    DicoTerm term;
    id = "o" + id;
    term = this.storages.get(Dicos.NCBI_TAXONOMY).getTerm(id);
    if (term == null) {
      return null;
    }
    return getTaxRankFromDataField(term.getDataField());
  }

  /**
   * 
   * @param term
   * @return
   * @throws DicoStorageSystemException
   * @see DicoStorageSystem.getApprochingTerms
   */
  public List<DicoTerm> getApprochingTerms(String term)
      throws DicoStorageSystemException {
    try {
      return storages.get(Dicos.NCBI_TAXONOMY).getApprochingTerms(term);
    } catch (NullPointerException ex) {
      return null;
    }
  }
}
