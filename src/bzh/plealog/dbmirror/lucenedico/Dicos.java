/* Copyright (C) 2007-2017 Patrick G. Durand - Ludovic Antin
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

import org.apache.commons.lang.StringUtils;

import bzh.plealog.dbmirror.lucenedico.cdd.CddParser;
import bzh.plealog.dbmirror.lucenedico.ec.EnzymeClassParser;
import bzh.plealog.dbmirror.lucenedico.ec.EnzymeDatParser;
import bzh.plealog.dbmirror.lucenedico.eggnog.EggNogDicoParser;
import bzh.plealog.dbmirror.lucenedico.go.GeneOntologyOBONodeParser;
import bzh.plealog.dbmirror.lucenedico.ipr.InterProNamesParser;
import bzh.plealog.dbmirror.lucenedico.pfam.PfamNamesParser;
import bzh.plealog.dbmirror.lucenedico.tax.NcbiTaxonomyTaxNamesParser;
import bzh.plealog.dbmirror.lucenedico.tax.NcbiTaxonomyTaxNodesParser;
import bzh.plealog.dbmirror.util.runner.DBMSExecNativeCommand;

public enum Dicos {

  // GeneOnotology terms
  GENE_ONTOLOGY(
      DicoUtils.READER_GENE_ONTOLOGY,
      "",
      "GeneOntology_terms",
      "GeneOntology",
      10,
      "Gene Ontology Terms",
      "GO",
      "To install GO, provide the file gene_ontology.obo available at ftp.geneontology.org/pub/go/ontology"),
  // InterPro names
  INTERPRO(
      DicoUtils.READER_INTERPRO,
      "",
      "InterPro_terms",
      "InterPro",
      11,
      "InterPro Terms",
      "InterPro",
      "To install InterPro, provide the file names.dat available at ftp.ebi.ac.uk/pub/databases/interpro"),
  // NCBI taxonomy - tax for scientific names & nodes for tree structure
  NCBI_TAXONOMY(
      DicoUtils.READER_NCBI_TAXONOMY,
      DicoUtils.READER_NCBI_TAXONOMY_NODES,
      "NCBI_Taxonomy",
      "Taxonomy",
      12,
      "NCBI Taxonomy Scientific Terms and Tree Structure",
      "taxon",
      "To install NCBI Taxonomy, provide either the two files nodes.dmp and names.dmp or the file taxdump.tar.gz available at ftp.ncbi.nih.gov/pub/taxonomy"),
  // PFAM domains classification
  PFAM(
      DicoUtils.READER_PFAM,
      "",
      "Pfam_terms",
      "Pfam",
      13,
      "Pfam Terms",
      "Pfam",
      "To install PFam, provide the file pfamA.txt.gz available at ftp.sanger.ac.uk/pub/databases/Pfam/current_release/database_files"),
  // Enzyme commission - ec for 'dat' & ecc for 'class'
  ENZYME(
      DicoUtils.READER_ENZYME,
      DicoUtils.READER_ENZYME_CLASS,
      "Enzyme",
      "Enzyme",
      14,
      "Enzyme nomenclature",
      "EC",
      "To install Enzyme, provide the two files enzclass.txt and enzyme.dat available at ftp.expasy.org/databases/enzyme"),
  // Conserved Domains from NCBI
  CDD(
      DicoUtils.READER_CDD,
      "",
      "Conserved_Domains",
      "CDD",
      15,
      "Conserved Domains Terms",
      "CDD",
      "To install CDD, provide the file cddid.tbl.gz available at ftp.ncbi.nih.gov/pub/mmdb/cdd"),
  // EggNog
  EGGNOG(DicoUtils.READER_EGGNOG, "", "EggNog", "NOG", 16, "EggNog Terms",
      "NOG", "To install EggNog, please use the public databank panel");

  public String readerId           = "";
  public String additionalReaderId = "";
  public String name               = "";
  public String label              = "";
  public int    format             = 0;
  public String description        = "";
  public String xrefId             = "";
  public String helpInstall        = "";

  Dicos(String readerId, String additionalReaderId, String name, String label,
      int format, String description, String xrefId, String help) {
    this.readerId = readerId;
    this.additionalReaderId = additionalReaderId;
    this.name = name;
    this.label = label;
    this.format = format;
    this.description = description;
    this.helpInstall = help;
    this.xrefId = xrefId;
  }

  public DicoStorageSystem getDicoQuery(String path) {
    DicoStorageSystemImplem dico = new DicoStorageSystemImplem();
    path = DBMSExecNativeCommand.formatNativePath(path, false, false);
    if (!dico.open(path, DicoStorageSystem.READ_MODE, true))
      return null;
    else
      return dico;
  }

  /**
   * Convert the parameter DicoTerm into String regarding the current dico. This
   * method never return null but empty string if term is null.
   * 
   * @param term
   * 
   * @return
   */
  public String getData(DicoTerm term) {
    if (this == GENE_ONTOLOGY) {
      return DicoUtils.getSimpleGoString(term);
    } else if (this == CDD) {
      return CddParser.getDescription(term);
    } else if (term != null) {
      return term.getDataField();
    }
    return "";
  }

  /**
   * return the id well labelled for a biological user (useful for CDD : instead
   * of display the id (227539), display the accession (COG5214) stored in the
   * term.data
   * 
   * @param term
   * @return
   */
  public String getLabelledId(DicoTerm term) {
    if (this == CDD) {
      return CddParser.getCddAccession(term);
    } else if (this == NCBI_TAXONOMY) {
      return DicoUtils.getCleanedNCBITaxonId(term);
    } else if (term != null) {
      return term.getId();
    }
    return "";
  }

  /**
   * Return the dico whose readerId or additionalReaderId is the same as the
   * given parameter
   * 
   * @param readerId
   * 
   * @return null if no dico is associated to the readerId
   */
  public static Dicos getAssociatedDico(String readerId) {
    for (Dicos dico : Dicos.values()) {
      if (dico.readerId.equalsIgnoreCase(readerId)
          || ((StringUtils.isNotBlank(dico.additionalReaderId)) && dico.additionalReaderId
              .equalsIgnoreCase(readerId))) {
        return dico;
      }
    }
    return null;
  }

  /**
   * 
   * @param readerId
   * 
   * @return the parser which can parse the dico associated to the given
   *         readerId
   */
  public static DicoParsable getParser(String readerId) {
    if (readerId.equals(DicoUtils.READER_GENE_ONTOLOGY)) {
      return new GeneOntologyOBONodeParser();
    }
    if (readerId.equals(DicoUtils.READER_INTERPRO)) {
      return new InterProNamesParser();
    }
    if (readerId.equals(DicoUtils.READER_PFAM)) {
      return new PfamNamesParser();
    }
    if (readerId.equals(DicoUtils.READER_NCBI_TAXONOMY)) {
      return new NcbiTaxonomyTaxNamesParser();
    }
    if (readerId.equals(DicoUtils.READER_NCBI_TAXONOMY_NODES)) {
      return new NcbiTaxonomyTaxNodesParser();
    }
    if (readerId.equals(DicoUtils.READER_ENZYME)) {
      return new EnzymeDatParser();
    }
    if (readerId.equals(DicoUtils.READER_ENZYME_CLASS)) {
      return new EnzymeClassParser();
    }
    if (readerId.equals(DicoUtils.READER_CDD)) {
      return new CddParser();
    }
    if (readerId.equals(DicoUtils.READER_EGGNOG)) {
      return new EggNogDicoParser();
    }
    return null;
  }

}
