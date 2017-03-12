/* Copyright (C) 2007-2017 Ludovic Antin
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
package bzh.plealog.dbmirror.indexer.eggnog;

import org.apache.commons.lang.StringUtils;

/**
 * This class define an entry stored in the EggNog lucene index.
 * 
 * An entry contains : - an id : a sequence id (equivalent to the protein id) -
 * a nog name (example : meNOG00058) - a protein id : a sequence id in the
 * eggnog fasta file (example : 6085.XP_002154315) - a start position - a end
 * position
 * 
 * Important : the same id (NCBI taxon id) can appear more then one time in the
 * lucene index because this index is done to list, count and retrieve domains
 * in order to create the uniprot sequences file from the eggnog sequences fasta
 * file
 * 
 * @author Ludovic Antin
 * 
 */
public class EggNogEntry implements Comparable<EggNogEntry> {

  // the original line used to create this entry
  private String  original  = null;

  // the splitted data
  private String  taxonId   = null;
  private String  nogName   = null;
  private String  proteinId = null;
  private Integer startPos  = null;
  private Integer endPos    = null;

  /**
   * Creates an entry from a member file line. A member line format is #nog name
   * protein name start position end position
   * 
   * @param entry
   */
  public EggNogEntry(String entry) {
    if (entry != null) {
      this.setOriginal(entry);
      String[] data = StringUtils.split(entry, '\t');
      this.setNogName(data[0]);
      // replace . by _ to avoid pb with '.' in headears @see
      // WWWSeqUtils.getIdAndDb
      this.setProteinId(data[1].replace('.', '_'));
      this.setStartPos(Integer.valueOf(data[2]));
      this.setEndPos(Integer.valueOf(data[3]));

      // and now the taxon id which is the first word of the protein id
      // example : for the protein id '6239.F29D10.4', the taxon is '6239'
      this.setTaxonId(this.proteinId.substring(0, this.proteinId.indexOf('_')));
    }
  }

  public String getNogName() {
    return nogName;
  }

  public void setNogName(String nogName) {
    this.nogName = nogName;
  }

  public String getProteinId() {
    return proteinId;
  }

  public void setProteinId(String proteinId) {
    this.proteinId = proteinId;
  }

  public Integer getStartPos() {
    return startPos;
  }

  public void setStartPos(Integer startPos) {
    this.startPos = startPos;
  }

  public Integer getEndPos() {
    return endPos;
  }

  public void setEndPos(Integer endPos) {
    this.endPos = endPos;
  }

  public String getOriginal() {
    return original;
  }

  public void setOriginal(String original) {
    this.original = original;
  }

  public String getTaxonId() {
    return taxonId;
  }

  public void setTaxonId(String taxonId) {
    this.taxonId = taxonId;
  }

  @Override
  public String toString() {
    return this.original;
  }

  @Override
  public int compareTo(EggNogEntry o) {
    if (o instanceof EggNogEntry) {
      return this.getStartPos().compareTo(o.getStartPos());
    } else {
      return 0;
    }
  }
}
