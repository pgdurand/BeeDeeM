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

import bzh.plealog.bioinfo.api.data.feature.FeatureTable;
import bzh.plealog.bioinfo.data.sequence.IBankSequenceInfo;

/**
 * This class is an implementation of a very basic biological sequence. It is
 * intended to transfer data from BioJava to KoriBlast data models.
 * 
 * @author Patrick G. Durand
 */
public class PSequence {
  private IBankSequenceInfo  seqInfo;
  private FeatureTable       featTable;
  private String             sequence;
  private int                seqType;

  public static final String PROTEIC_TYPE_STR = "proteic";
  public static final String NUCLEIC_TYPE_STR = "nucleic";

  public static int          PROTEIC_TYPE     = 0;
  public static int          NUCLEIC_TYPE     = 1;

  public static final String SEQ_TYPE_STR[]   = { PROTEIC_TYPE_STR,
      NUCLEIC_TYPE_STR                       };

  private static final int   FASTA_COLUMN     = 60;

  public PSequence() {

  }

  public PSequence(String seq, int seqType) {
    sequence = seq;
    this.seqType = seqType;
  }

  public PSequence(IBankSequenceInfo seqInfo, FeatureTable featTable) {
    super();
    this.seqInfo = seqInfo;
    this.featTable = featTable;
  }

  public PSequence(IBankSequenceInfo seqInfo, FeatureTable featTable,
      String seq, int seqType) {
    this(seqInfo, featTable);
    sequence = seq;
    this.seqType = seqType;
  }

  public FeatureTable getFeatTable() {
    return featTable;
  }

  public void setFeatTable(FeatureTable featTable) {
    this.featTable = featTable;
  }

  public IBankSequenceInfo getSeqInfo() {
    return seqInfo;
  }

  public void setSeqInfo(IBankSequenceInfo seqInfo) {
    this.seqInfo = seqInfo;
  }

  public void setSequence(String str) {
    sequence = str;
  }

  public String getSequence() {
    return sequence;
  }

  public int getSeqType() {
    return seqType;
  }

  public void setSeqType(int seqType) {
    this.seqType = seqType;
  }

  public String getFastaSequence() {
    StringBuffer szBuf;
    String name;
    int i, size;

    szBuf = new StringBuffer(">");
    if (seqInfo != null) {
      name = seqInfo.getId();
    } else {
      name = "NoID";
    }
    szBuf.append(name);
    if (seqInfo != null) {
      name = seqInfo.getDescription();
    } else {
      name = "NoDefinition";
    }
    szBuf.append(" " + name);
    szBuf.append("\n");
    if (sequence == null) {
      szBuf.append("null\n");
      return szBuf.toString();
    }
    size = sequence.length();
    for (i = 0; i < size; i++) {
      szBuf.append(sequence.charAt(i));
      if (i != 0 && (i + 1) % FASTA_COLUMN == 0)
        szBuf.append("\n");
    }
    // add a terminal carriage return only if sequence size is not a multiple
    // of FASTA_COLUMN
    if (size % FASTA_COLUMN != 0)
      szBuf.append("\n");
    return szBuf.toString();
  }
}
