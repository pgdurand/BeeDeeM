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
package bzh.plealog.dbmirror.reader;

import bzh.plealog.dbmirror.lucenedico.Dicos;
import bzh.plealog.dbmirror.util.conf.DBMirrorConfig;

public class DBFormatEntry {

  public static DBFormatEntry fastaProteic          = new DBFormatEntry(
                                                        DBUtils.FP_DB_FORMAT,
                                                        "Fasta Proteic",
                                                        DBMirrorConfig.BLASTP_READER);
  public static DBFormatEntry fastaNucleic          = new DBFormatEntry(
                                                        DBUtils.FN_DB_FORMAT,
                                                        "Fasta Nucleic",
                                                        DBMirrorConfig.BLASTN_READER);
  public static DBFormatEntry uniprot               = new DBFormatEntry(
                                                        DBUtils.SW_DB_FORMAT,
                                                        "Uniprot (TrEMBL, SwissProt)",
                                                        DBMirrorConfig.UP_READER);
  public static DBFormatEntry genpept               = new DBFormatEntry(
                                                        DBUtils.GP_DB_FORMAT,
                                                        "Genpept/Refseq Protein",
                                                        DBMirrorConfig.GP_READER);
  public static DBFormatEntry genbank               = new DBFormatEntry(
                                                        DBUtils.GB_DB_FORMAT,
                                                        "Genbank/Refseq Nucleic",
                                                        DBMirrorConfig.GB_READER);
  public static DBFormatEntry embl                  = new DBFormatEntry(
                                                        DBUtils.EM_DB_FORMAT,
                                                        "EMBL",
                                                        DBMirrorConfig.EM_READER);
  public static DBFormatEntry silva                 = new DBFormatEntry(
                                                        DBUtils.FN_DB_FORMAT,
                                                        "Silva Fasta Nucleic",
                                                        DBMirrorConfig.BLASTN_READER,
                                                        DBUtils.SILVA_HEADER_FORMAT);
  public static DBFormatEntry bold                  = new DBFormatEntry(
                                                        DBUtils.FN_DB_FORMAT,
                                                        "BOLD Barcode DNA",
                                                        DBMirrorConfig.GB_READER,
                                                        DBUtils.BOLD_HEADER_FORMAT);
  public static DBFormatEntry fastaProteicWithTaxon = new DBFormatEntry(
                                                        DBUtils.FN_DB_FORMAT,
                                                        "Fasta Proteic with annotation data",
                                                        DBMirrorConfig.BLASTP_READER,
                                                        DBUtils.TAXONOMY_HEADER_FORMAT);
  public static DBFormatEntry fastaNucleicWithTaxon = new DBFormatEntry(
                                                        DBUtils.FN_DB_FORMAT,
                                                        "Fasta Nucleic with annotation data",
                                                        DBMirrorConfig.BLASTN_READER,
                                                        DBUtils.TAXONOMY_HEADER_FORMAT);
  public static DBFormatEntry geneOntology          = new DBFormatEntry(
                                                        Dicos.GENE_ONTOLOGY);
  public static DBFormatEntry interPro              = new DBFormatEntry(
                                                        Dicos.INTERPRO);
  public static DBFormatEntry pfam                  = new DBFormatEntry(
                                                        Dicos.PFAM);
  public static DBFormatEntry enzyme                = new DBFormatEntry(
                                                        Dicos.ENZYME);
  public static DBFormatEntry taxonomy              = new DBFormatEntry(
                                                        Dicos.NCBI_TAXONOMY);
  public static DBFormatEntry cdd                   = new DBFormatEntry(
                                                        Dicos.CDD);

  private int                 type;
  private String              name;
  private String              reader;
  private int                 headerFormat;

  /**
   * 
   * @param entryName
   * 
   * @return a known entry which name is the string given in parameter
   */
  public static DBFormatEntry getEntry(String entryName) {
    if (DBFormatEntry.fastaProteic.getName().equalsIgnoreCase(entryName))
      return DBFormatEntry.fastaProteic;
    if (DBFormatEntry.fastaNucleic.getName().equalsIgnoreCase(entryName))
      return DBFormatEntry.fastaNucleic;
    if (DBFormatEntry.uniprot.getName().equalsIgnoreCase(entryName))
      return DBFormatEntry.uniprot;
    if (DBFormatEntry.genpept.getName().equalsIgnoreCase(entryName))
      return DBFormatEntry.genpept;
    if (DBFormatEntry.genbank.getName().equalsIgnoreCase(entryName))
      return DBFormatEntry.genbank;
    if (DBFormatEntry.embl.getName().equalsIgnoreCase(entryName))
      return DBFormatEntry.embl;
    if (DBFormatEntry.silva.getName().equalsIgnoreCase(entryName))
      return DBFormatEntry.silva;
    if (DBFormatEntry.bold.getName().equalsIgnoreCase(entryName))
      return DBFormatEntry.bold;
    if (DBFormatEntry.fastaProteicWithTaxon.getName().equalsIgnoreCase(
        entryName))
      return DBFormatEntry.fastaProteicWithTaxon;
    if (DBFormatEntry.fastaNucleicWithTaxon.getName().equalsIgnoreCase(
        entryName))
      return DBFormatEntry.fastaNucleicWithTaxon;
    if (DBFormatEntry.geneOntology.getName().equalsIgnoreCase(entryName))
      return DBFormatEntry.geneOntology;
    if (DBFormatEntry.interPro.getName().equalsIgnoreCase(entryName))
      return DBFormatEntry.interPro;
    if (DBFormatEntry.pfam.getName().equalsIgnoreCase(entryName))
      return DBFormatEntry.pfam;
    if (DBFormatEntry.enzyme.getName().equalsIgnoreCase(entryName))
      return DBFormatEntry.enzyme;
    if (DBFormatEntry.taxonomy.getName().equalsIgnoreCase(entryName))
      return DBFormatEntry.taxonomy;
    if (DBFormatEntry.cdd.getName().equalsIgnoreCase(entryName))
      return DBFormatEntry.cdd;

    return null;
  }

  public DBFormatEntry(int type, String name, String reader) {
    super();
    this.type = type;
    this.name = name;
    this.reader = reader;
  }

  public DBFormatEntry(int type, String name, String reader, int headerFormat) {
    this(type, name, reader);
    this.headerFormat = headerFormat;
  }

  public DBFormatEntry(Dicos dico) {
    this(dico.format, dico.description, dico.readerId);
  }

  public int getType() {
    return type;
  }

  public String getReader() {
    return reader;
  }

  public String getName() {
    return name;
  }

  public String toString() {
    return name;
  }

  public int getHeaderFormat() {
    return headerFormat;
  }

  /**
   * Figures out if this entry is SILVA
   */
  public boolean isSilva() {
    return (this.getHeaderFormat() == DBUtils.SILVA_HEADER_FORMAT);
  }

  /**
   * Figures out if this entry is BOLD
   */
  public boolean isBOLD() {
    return (this.getHeaderFormat() == DBUtils.BOLD_HEADER_FORMAT);
  }

  /**
   * Returns the db format type. Values are one of DBMirrorConfig.XXX_READER
   * values. This is because it is the reader type that is stored in dbmirror
   * config file.
   */
  public String getDBType() {
    return this.getReader();
  }

  /**
   * Figures out if we are installing a Fasta-taxonomic based databank.
   */
  public boolean isTaxonomy() {
    return (this.getHeaderFormat() == DBUtils.TAXONOMY_HEADER_FORMAT);
  }
}
