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
package bzh.plealog.dbmirror.util.descriptor;

import bzh.plealog.dbmirror.reader.DBUtils;
import bzh.plealog.dbmirror.util.sequence.SeqIOUtils;

/**
 * This class is a factory which stores all databank format allowed by Plealog
 * softwares. <br>
 * A databank format is defined by a type and some strings mandatory to parse a
 * databank file. <br>
 * To get a databank format just call the static method getFormat(int type)
 * 
 * @author Ludovic Antin
 * 
 */
public class DatabankFormat {

  public static enum DatabankFormatTypes {
    Fasta, FastQ, Genbank, SwissProt
  }

  public static DatabankFormat fasta     = new DatabankFormat(
                                             DatabankFormatTypes.Fasta, ">",
                                             ">", ">", ">");
  public static DatabankFormat fastQ     = new DatabankFormat(
                                             DatabankFormatTypes.FastQ, "@",
                                             "@", "@", "@");
  public static DatabankFormat genbank   = new DatabankFormat(
                                             DatabankFormatTypes.Genbank,
                                             "LOCUS", "DEFINITION",
                                             "          ", "ORIGIN");
  public static DatabankFormat swissProt = new DatabankFormat(
                                             DatabankFormatTypes.SwissProt,
                                             "ID", "DE", "DE", "SQ");

  public static DatabankFormat getFormat(DatabankFormatTypes formatType) {
    if (formatType == null) {
      return null;
    }
    switch (formatType) {
      case Fasta:
        return fasta;
      case FastQ:
        return fastQ;
      case Genbank:
        return genbank;
      case SwissProt:
        return swissProt;
      default:
        return null;
    }
  }

  public static DatabankFormat getFormatFromSeqIOUtils(int formatType) {
    switch (formatType) {
      case SeqIOUtils.FASTADNA:
      case SeqIOUtils.FASTARNA:
      case SeqIOUtils.FASTAPROT:
        return fasta;
      case SeqIOUtils.FASTQ:
        return fastQ;
      case SeqIOUtils.GENBANK:
      case SeqIOUtils.GENPEPT:
        return genbank;
      case SeqIOUtils.SWISSPROT:
      case SeqIOUtils.EMBL:
        return swissProt;
      default:
        return null;
    }
  }

  public static DatabankFormat getFormatTypeFromDBUtils(int formatType) {
    switch (formatType) {
      case DBUtils.FAS_DB_FORMAT:
      case DBUtils.FN_DB_FORMAT:
      case DBUtils.FP_DB_FORMAT:
        return fasta;
      case DBUtils.GB_DB_FORMAT:
      case DBUtils.GP_DB_FORMAT:
        return genbank;
      case DBUtils.SW_DB_FORMAT:
      case DBUtils.EM_DB_FORMAT:
        return swissProt;
      default:
        return null;
    }
  }

  private DatabankFormatTypes type;
  private String              idString;
  private String              beginDescriptionString;
  private String              continueDescriptionString;
  private String              beginSequenceString;

  private DatabankFormat(DatabankFormatTypes type, String idString,
      String beginDescriptionString, String continueDescriptionString,
      String beginSequenceString) {
    this.setType(type);
    this.setIdString(idString);
    this.setBeginDescriptionString(beginDescriptionString);
    this.setContinueDescriptionString(continueDescriptionString);
    this.setBeginSequenceString(beginSequenceString);
  }

  private void setType(DatabankFormatTypes type) {
    this.type = type;
  }

  public DatabankFormatTypes getType() {
    return type;
  }

  private void setIdString(String idString) {
    this.idString = idString;
  }

  public String getIdString() {
    return idString;
  }

  private void setBeginDescriptionString(String beginDescriptionString) {
    this.beginDescriptionString = beginDescriptionString;
  }

  public String getBeginDescriptionString() {
    return beginDescriptionString;
  }

  private void setContinueDescriptionString(String continueDescriptionString) {
    this.continueDescriptionString = continueDescriptionString;
  }

  public String getContinueDescriptionString() {
    return continueDescriptionString;
  }

  private void setBeginSequenceString(String beginSequenceString) {
    this.beginSequenceString = beginSequenceString;
  }

  public String getBeginSequenceString() {
    return beginSequenceString;
  }
}
