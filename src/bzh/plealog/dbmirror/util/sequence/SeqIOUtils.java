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
package bzh.plealog.dbmirror.util.sequence;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Serializable;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.biojava.bio.Annotation;
import org.biojava.bio.seq.DNATools;
import org.biojava.bio.seq.RNATools;
import org.biojava.bio.seq.Sequence;
import org.biojava.bio.seq.SequenceIterator;
import org.biojava.bio.seq.SequenceTools;
import org.biojava.bio.seq.io.FastaFormat;
import org.biojava.bio.seq.io.SeqIOTools;
import org.biojava.bio.symbol.AbstractSymbolList;
import org.biojava.bio.symbol.Alphabet;
import org.biojava.bio.symbol.Symbol;
import org.biojava.bio.symbol.SymbolList;
import org.biojava.bio.symbol.SymbolListViews;
import org.biojava.bio.symbol.TranslationTable;

import bzh.plealog.dbmirror.indexer.DBParserException;
import bzh.plealog.dbmirror.indexer.SwissProtParser;
import bzh.plealog.dbmirror.lucenedico.DicoTermQuerySystem;
import bzh.plealog.dbmirror.lucenedico.Dicos;
import bzh.plealog.dbmirror.reader.DBUtils;
import bzh.plealog.dbmirror.util.Formatters;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.descriptor.DatabankFormat;
import bzh.plealog.dbmirror.util.log.LoggerCentral;
import bzh.plealog.dbmirror.util.runner.DBMSUniqueSeqIdRedundantException;
import bzh.plealog.dbmirror.util.xref.DBXrefInstancesManager;
import bzh.plealog.dbmirror.util.xref.DBXrefTagManager;

@SuppressWarnings("deprecation")
public class SeqIOUtils {
  public static final int        STYPE_NORMAL                 = 0;
  public static final int        STYPE_COMPL                  = 1;
  public static final int        STYPE_REVERSE                = 2;
  public static final int        STYPE_RC                     = 3;

  public static final int        TRANS_NONE                   = -1;
  public static final int        TRANS_PLUS_1                 = 0;
  public static final int        TRANS_PLUS_2                 = 1;
  public static final int        TRANS_PLUS_3                 = 2;
  public static final int        TRANS_PLUS_ALL               = 3;
  public static final int        TRANS_MINUS_1                = 4;
  public static final int        TRANS_MINUS_2                = 5;
  public static final int        TRANS_MINUS_3                = 6;
  public static final int        TRANS_MINUS_ALL              = 7;
  public static final int        TRANS_ALL                    = 8;

  public static final String[]   SEQ_TYPE                     = { "Normal",
      "Complement", "Reverse", "Reverse complement"          };
  public static final int[]      SEQ_TYPE_I                   = { STYPE_NORMAL,
      STYPE_COMPL, STYPE_REVERSE, STYPE_RC                   };
  public static final String[]   TRANS_TYPE                   = { "+1", "+2",
      "+3", "All forward", "-1", "-2", "-3", "All reverse", "All six frames" };
  public static final int[]      TRANS_TYPE_I                 = { TRANS_PLUS_1,
      TRANS_PLUS_2, TRANS_PLUS_3, TRANS_PLUS_ALL, TRANS_MINUS_1, TRANS_MINUS_2,
      TRANS_MINUS_3, TRANS_MINUS_ALL, TRANS_ALL              };

  public static final int        UNKNOWN                      = 0;
  public static final int        SWISSPROT                    = 1;
  public static final int        GENBANK                      = 2;
  public static final int        EMBL                         = 3;
  public static final int        FASTAPROT                    = 4;
  public static final int        FASTADNA                     = 5;
  public static final int        FASTARNA                     = 6;
  public static final int        GENPEPT                      = 7;
  public static final int        FASTQ                        = 8;

  public static final String[]   FILE_TYPES                   = { "Unknown",
      "SwissProt", "Genbank", "EMBL", "Fasta-Prot", "Fasta-DNA", "Fasta-RNA",
      "GenPept"                                              };

  private static final Logger       LOGGER                       = LogManager.getLogger(
      DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY + ".SeqIOUtils");

  // see DBXrefSplitter class for more details of the syntax
  public static final String     DEFAULT_CONFIG_XREF_RETRIEVE = "\"DR\" , \"GO\"         , \";\" , \";\" , \"GO\"       , \":\"\n"
                                                                  + "\"DR\" , \"InterPro\"   , \";\" , \";\" , \"InterPro\" , \"$\"\n"
                                                                  + "\"DR\" , \"Pfam\"       , \";\" , \";\" , \"Pfam\"     , \"$\"\n"
                                                                  +
                                                                  /*
                                                                   * Tests on SW
                                                                   * : Brenda
                                                                   * code is not
                                                                   * always
                                                                   * there! Get
                                                                   * EC from
                                                                   * definition
                                                                   * line!
                                                                   */
                                                                  /*
                                                                   * "\"DR\" , \"BRENDA\"     , \";\" , \";\" , \"EC\"       , \"$\"\n"
                                                                   * +
                                                                   */
                                                                  "\"DE\" , \"EC\"         , \"=\" , \";\" , \"EC\"       , \"$\"\n"
                                                                  + "\"OX\" , \"NCBI_TaxID\" , \"=\" , \"};\" , \"taxon\"    , \"$\"\n"
                                                                  + "\"/db_xref=\",\"taxon\", \":\", \"\"\", \"taxon\", \"$\"\n";

  public static DBXrefTagManager XREF_MANAGER                 = new DBXrefTagManager(
                                                                  DEFAULT_CONFIG_XREF_RETRIEVE);

  /**
   * Sets a new configuration for DbXrefTagManager.
   */
  public static void setDbXrefTagManager(String xrefConf) {
    XREF_MANAGER = new DBXrefTagManager(xrefConf);
  }

  public static String getStringData(Object data) {
    if (data instanceof List) {
      List<?> lst = (List<?>) data;
      StringBuffer buf = new StringBuffer();
      int i, size;

      size = lst.size();
      for (i = 0; i < size; i++) {
        buf.append(lst.get(i));
        if ((i + 1) < size)
          buf.append(" ");
      }
      return buf.toString();
    } else {
      return data.toString();
    }
  }

  /**
   * This method analyzes an ID given the NCBI recommendations.
   */
  public static String getId(String id, boolean ncbiIdType) {
    int idx;
    if (ncbiIdType) {
      // official NCBI id: return it!
      if (id.startsWith("gi|") || id.startsWith("lcl|")) {
        return id;
      }
      // get ID located between first and second '|'
      // except if ID starts with gnl. In this case, second string
      // is the DB identifier, and seqID is the third string.
      // see table 1.1 from
      // http://www.ncbi.nlm.nih.gov/staff/tao/URLAPI/formatdb_fastacmd.html
      if (id.startsWith("gnl")) {
        id = id.substring(id.indexOf('|') + 1);
      }
      idx = id.indexOf('|');
      if (idx != -1) {
        id = id.substring(idx + 1);
        idx = id.indexOf('|');
        if (idx != -1) {
          id = id.substring(0, idx);
        }
      }
    }
    return id;
  }

  public static String getId(Sequence seq, boolean ncbiIdType) {
    return getId(seq.getName(), ncbiIdType);
  }

  public static String getDescription(Sequence seq, int seqType,
      boolean ncbiIdType) {
    Annotation annot;
    String desc = null;

    annot = seq.getAnnotation();
    try {
      switch (seqType) {
        case SeqIOUtils.EMBL:
        case SeqIOUtils.SWISSPROT:
          desc = getStringData(annot.getProperty("DE"));
          break;
        case SeqIOUtils.GENPEPT:
        case SeqIOUtils.GENBANK:
          desc = getStringData(annot.getProperty("DEFINITION"));
          break;
        case SeqIOUtils.FASTADNA:
        case SeqIOUtils.FASTARNA:
        case SeqIOUtils.FASTAPROT:
          desc = getStringData(annot.getProperty("description"));
          if (desc == null)
            desc = seq.getName();
          break;
      }
    } catch (NoSuchElementException e) {
      desc = null;
    }
    return desc;
  }

  public static String getIdentifier(Sequence seq, int seqType,
      boolean ncbiIdType) {
    Annotation annot;
    String desc = null;
    String id;
    annot = seq.getAnnotation();
    try {
      switch (seqType) {
        case SeqIOUtils.EMBL:
        case SeqIOUtils.SWISSPROT:
          desc = getStringData(annot.getProperty("DE"));
          break;
        case SeqIOUtils.GENPEPT:
        case SeqIOUtils.GENBANK:
          desc = getStringData(annot.getProperty("DEFINITION"));
          break;
        case SeqIOUtils.FASTADNA:
        case SeqIOUtils.FASTARNA:
        case SeqIOUtils.FASTAPROT:
          desc = getStringData(annot.getProperty("description"));
          if (desc == null)
            desc = seq.getName();
          break;
      }
    } catch (NoSuchElementException e) {
      desc = null;
    }
    id = getId(seq, ncbiIdType);
    /*
     * idx = id.indexOf('|'); if (idx==-1){ id = "lcl|"+id; } else if (idx>3){
     * id = "lcl|"+id; }
     */
    if (desc == null)
      annot.setProperty(FastaFormat.PROPERTY_DESCRIPTIONLINE, id);
    else
      annot.setProperty(FastaFormat.PROPERTY_DESCRIPTIONLINE, id + " " + desc);
    return id;
  }

  public static SequenceIterator getSequenceIterator(BufferedReader br,
      int seqType) {
    SequenceIterator iter = null;
    switch (seqType) {
      case SWISSPROT:
        iter = (SequenceIterator) SeqIOTools.readSwissprot(br);
        break;
      case GENBANK:
        iter = (SequenceIterator) SeqIOTools.readGenbank(br);
        break;
      case EMBL:
        iter = (SequenceIterator) SeqIOTools.readEmbl(br);
        break;
      case FASTAPROT:
        iter = (SequenceIterator) SeqIOTools.readFastaProtein(br);
        break;
      case FASTADNA:
        iter = (SequenceIterator) SeqIOTools.readFastaDNA(br);
        break;
      case FASTARNA:
        iter = (SequenceIterator) SeqIOTools.readFastaRNA(br);
        break;
      case GENPEPT:
        iter = (SequenceIterator) SeqIOTools.readGenpept(br);
        break;
    }
    return iter;
  }

  public static boolean isProt(int seqType) {
    switch (seqType) {
      case GENPEPT:
      case SWISSPROT:
      case FASTAPROT:
        return true;
      case GENBANK:
      case FASTADNA:
      case FASTARNA:
      case FASTQ:
      default:
        return false;
    }
  }

  public static boolean isFileType(String fName, int typeToCheck) {
    boolean ok = false;
    switch (typeToCheck) {
      case FASTADNA:
      case FASTARNA:
        ok = isFastaNuc(fName, null);
        break;
      case FASTQ:
        ok = isFastaQ(fName);
        break;
      case FASTAPROT:
        ok = isFastaProt(fName);
        break;
      case GENBANK:
        ok = isGenbank(fName);
        break;
      case GENPEPT:
        ok = isGenpept(fName);
        break;
      case SWISSPROT:
        ok = isUniProt(fName);
        break;
      case EMBL:
        ok = isEMBL(fName);
        break;
    }
    return ok;
  }

  public static int guessFileFormat(String fname) {
    int fileType = UNKNOWN;
    StringBuffer sequenceChecked = new StringBuffer();
    if (isFastaQ(fname)) {
      fileType = FASTQ;
    } else if (isFastaNuc(fname, sequenceChecked)) {
      if (isFastaDNA(sequenceChecked.toString())) {
        fileType = FASTADNA;
      } else {
        fileType = FASTARNA;
      }
    } else if (isFastaProt(fname)) {
      fileType = FASTAPROT;
    } else if (isGenbank(fname)) {
      fileType = GENBANK;
    } else if (isGenpept(fname)) {
      fileType = GENPEPT;
    } else if (isUniProt(fname)) {
      fileType = SWISSPROT;
    } else if (isEMBL(fname)) {
      fileType = EMBL;
    }

    return fileType;
  }

  public static void fillDescription(String line, String id, String idDesc,
      StringBuilder buf) {
    buf.append(line.substring(idDesc.length()).replace(id, "").trim());
  }

  public static String cleanDescription(StringBuilder sbDesc) {
    String desc = sbDesc.toString();
    if (desc.length() != 0) {
      // NCBI mutli-header may contain multiple >
      desc = Formatters.replaceAll(desc, ">", "|");

      // NCBI data may contain null char
      int idx = desc.indexOf(0x01);
      if (idx != -1) {
        return desc.substring(0, idx).trim();
      } else {
        return desc.trim();
      }
    }
    return desc;
  }

  /**
   * Analyzes a data line and return the primary ID.
   */
  public static String getID(String line, String idKey) {
    int i, j, max;

    // starting after the key that identifies the ID line, check
    // to get the ID. All but Fasta should have some space chars:
    // skip them.
    i = idKey.length();
    line = line.substring(i).trim();
    max = line.length();
    int indexSpace = line.indexOf(' ');
    if (indexSpace < 0) {
      indexSpace = max + 1;
    }
    int indexDot = line.indexOf(';');
    if (indexDot < 0) {
      indexDot = max + 1;
    }
    j = Math.min(Math.min(max, indexSpace), indexDot);
    return line.substring(0, j);
  }

  /**
   * Return an accession ID given a sequence ID. Usually, the latter contains
   * several strings separated by a pipe character. This method returns the last
   * such string. Example: return XP_021350314.1 from gi|1207922646|ref|XP_021350314.1|
   */
  public static String getAccessionFromId(String id) {
    String[] tokens = Utils.tokenize(id, "|");
    if (tokens.length!=0) {
      return(tokens[tokens.length-1]);
    }
    else {
      return(id);
    }
  }
  
  /**
   * Converts a sequence data file into a Fasta one. Please note that this
   * method does not check the fileIn type. In the worst case the method could
   * read the entire fileIn and does nothing on it. So you may use the various
   * isXXX() methods (where XXX is Genbank, Genpept, EMBL, Uniprot or Fasta)
   * before calling this method and pass in the correct value for formatType.
   * 
   * @param fileIn
   *          input file. Accepted formats: Genbank, Genpept, Uniprot, Embl,
   *          Fasta.
   * @param fileOut
   *          output file. Will be a fasta file.
   * @param formatType
   *          the input file format.
   * @param monitor
   *          a monitor to follow sequence conversion. Can be null.
   * 
   * @return an array of two integers where lower index reports the total number
   *         of sequences found in fileIn and upper index reports the total
   *         number of letters read in fileIn.
   * 
   * @throws DBParserException
   *           if the conversion fails.
   */
  public static int[] convertToFasta(String fileIn, String fileOut,
      int formatType, SeqIOConvertMonitor monitor) {
    return SeqIOUtils.convertToFasta(fileIn, fileOut, formatType, monitor,
        null, null, DBUtils.NO_HEADER_FORMAT);
  }

  /**
   * Converts a sequence data file into a Fasta one. Please note that this
   * method does not check the fileIn type. In the worst case the method could
   * read the entire fileIn and does nothing on it. So you may use the various
   * isXXX() methods (where XXX is Genbank, Genpept, EMBL, Uniprot or Fasta)
   * before calling this method and pass in the correct value for formatType.
   * 
   * @param fis
   *          input stream. Accepted formats: Genbank, Genpept, Uniprot, Embl,
   *          Fasta, fastq.
   * @param fos
   *          output stream. Will be a fasta stream.
   * @param formatType
   *          the input file format.
   * @param monitor
   *          a monitor to follow sequence conversion. Can be null.
   * 
   * @return an array of two integers where lower index reports the total number
   *         of sequences found in fileIn and upper index reports the total
   *         number of letters read in fileIn.
   * 
   * @throws DBParserException
   *           if the conversion fails.
   */
  public static int[] convertToFasta(InputStream fis, OutputStream fos,
      int formatType, SeqIOConvertMonitor monitor) {
    int[] rets = null;

    try {
      rets = SeqIOUtils.convertToFasta(fis, fos, formatType, monitor, null,
          null, DBUtils.NO_HEADER_FORMAT);
    } catch (Exception e) {
      throw new DBParserException(e.getMessage());
    }
    return rets;
  }

  /**
   * Converts a sequence data file into a Fasta one. Please note that this
   * method does not check the fileIn type. In the worst case the method could
   * read the entire fileIn and does nothing on it. So you may use the various
   * isXXX() methods (where XXX is Genbank, Genpept, EMBL, Uniprot or Fasta)
   * before calling this method and pass in the correct value for formatType.
   * 
   * @param fileIn
   *          input file. Accepted formats: Genbank, Genpept, Uniprot, Embl,
   *          Fasta, fastq.
   * @param fileOut
   *          output file. Will be a fasta file.
   * @param formatType
   *          the input file format.
   * @param monitor
   *          a monitor to follow sequence conversion. Can be null.
   * @param taxMatcher
   *          a TaxonMatcherHelper object used to filter out sequences from
   *          fileIn given taxon data. Can be null.
   * @param dico
   *          a DicoTermQuerySystem object requires to access biological
   *          classifications. Can be null. Cannot be null if taxMatcher is not
   *          null.
   * @param headerFormat
   *          the header format. One of DBUtils.XX_HEADER_FORMAT values.
   * 
   * @return an array of two integers where lower index reports the total number
   *         of sequences found in fileIn and upper index reports the total
   *         number of letters read in fileIn.
   * 
   * @throws DBParserException
   *           if the conversion fails.
   */
  public static int[] convertToFasta(String fileIn, String fileOut,
      int formatType, SeqIOConvertMonitor monitor,
      TaxonMatcherHelper taxMatcher, DicoTermQuerySystem dico, int headerFormat)
      throws DBParserException {
    int[] rets = null;
    FileInputStream is = null;
    FileOutputStream os = null;

    try {
      is = new FileInputStream(fileIn);
      os = new FileOutputStream(fileOut);
      rets = convertToFasta(is, os, formatType, monitor, taxMatcher, dico,
          headerFormat);
    } catch (IOException e) {
      throw new DBParserException(e.getMessage());
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
        }
      }
      if (os != null) {
        try {
          os.close();
        } catch (IOException e) {
        }
      }

    }
    return rets;

  }

  /**
   * Converts a sequence data file into a Fasta one. Please note that this
   * method does not check the fileIn type. In the worst case the method could
   * read the entire fileIn and does nothing on it. So you may use the various
   * isXXX() methods (where XXX is Genbank, Genpept, EMBL, Uniprot or Fasta)
   * before calling this method and pass in the correct value for formatType.
   * 
   * @param fis
   *          input stream. Accepted formats: Genbank, Genpept, Uniprot, Embl,
   *          Fasta.
   * @param fos
   *          output stream. Will be a fasta stream.
   * @param formatType
   *          the input file format.
   * @param monitor
   *          a monitor to follow sequence conversion. Can be null.
   * @param taxMatcher
   *          a TaxonMatcherHelper object used to filter out sequences from
   *          fileIn given taxon data. Can be null.
   * @param dico
   *          a DicoTermQuerySystem object requires to access biological
   *          classifications. Can be null. Cannot be null if taxMatcher is not
   *          null.
   * @param headerFormat
   *          the header format. One of DBUtils.XX_HEADER_FORMAT values.
   * 
   * @return an array of two integers where lower index reports the total number
   *         of sequences found in fileIn and upper index reports the total
   *         number of letters read in fileIn.
   * 
   * @throws DBParserException
   *           if the conversion fails.
   */
  public static int[] convertToFasta(InputStream fis, OutputStream fos,
      int formatType, SeqIOConvertMonitor monitor,
      TaxonMatcherHelper taxMatcher, DicoTermQuerySystem dico, int headerFormat)
      throws DBParserException {
    DatabankFormat format = DatabankFormat.getFormatFromSeqIOUtils(formatType);
    if (format == null) {
      throw new DBParserException("unknown format: " + formatType);
    }
    return SeqIOUtils.convertToFasta(fis, fos, format.getIdString(),
        format.getBeginDescriptionString(),
        format.getContinueDescriptionString(), format.getBeginSequenceString(),
        monitor, taxMatcher, dico, headerFormat);
  }

  /**
   * Converts a sequence data file into a Fasta one. Please note that this
   * method does not check the fileIn type. In the worst case the method could
   * read the entire fileIn and does nothing on it. So you may use the various
   * isXXX() methods (where XXX is Genbank, Genpept, EMBL, Uniprot or Fasta)
   * before calling this method.
   * 
   * @param is
   *          input file. Accepted formats: Genbank, Genpept, Uniprot, Embl,
   *          Fasta. Converting a Fasta fileIn to a fasta fileOut can be weird,
   *          but it is used to check the format.
   * @param os
   *          output file. Will be a fasta file.
   * @param idKey
   *          the key that identifies the sequence identifier field in the input
   *          file.
   * @param beginDescKey
   *          the key that identifies the description line (beginning) in the
   *          input file.
   * @param contDescKey
   *          the key that identifies the description line (continued) in the
   *          input file.
   * @param seqKey
   *          the key that identifies the beginning of the sequence in the input
   *          file.
   * @param monitor
   *          a monitor to follow sequence conversion. Can be null.
   * @param taxMatcher
   *          a TaxonMatcherHelper object used to filter out sequences from
   *          fileIn given taxon data. Can be null.
   * @param dico
   *          a DicoTermQuerySystem object requires to access biological
   *          classifications. Can be null. Cannot be null if taxMatcher is not
   *          null.
   * @param headerFormat
   *          the header format. One of DBUtils.XX_HEADER_FORMAT values.
   * 
   * @return an array of two integers where lower index reports the total number
   *         of sequences found in fileIn and upper index reports the total
   *         number of letters read in fileIn.
   * 
   * @throws DBParserException
   *           if the conversion fails.
   */
  private static int[] convertToFasta(InputStream is, OutputStream os,
      String idKey, String beginDescKey, String contDescKey, String seqKey,
      SeqIOConvertMonitor monitor, TaxonMatcherHelper taxMatcher,
      DicoTermQuerySystem dico, int headerFormat) {

    BufferedWriter writer = null;
    BufferedReader reader = null;
    DBXrefInstancesManager instManager;
    String line, id = null, xref = null;
    StringBuilder bufSeq, bufDesc;
    StringBuilder sequenceHeader = null;
    boolean writeSeq = false, readSeq = false, readDesc = false, dumpLetters, handleTaxon = true, seqOk = true, isFastq = false;
    boolean[] values = new boolean[3];
    int i, size, letterCount, entryCount = 0;
    char c;
    long tim;
    boolean displaySilvaLicense = true;

    tim = System.currentTimeMillis();

    try {
      if (monitor != null) {
        monitor.startProcessing();
      }
      isFastq = idKey.equals("@");
      if (taxMatcher != null && taxMatcher.hasTaxonomyAvailable() == false) {
        throw new Exception(TaxonMatcherHelper.ERR1);
      }
      reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
      writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
      bufSeq = new StringBuilder();
      bufDesc = new StringBuilder();
      letterCount = 0;
      instManager = new DBXrefInstancesManager();
      while ((line = reader.readLine()) != null) {
        /*
         * if (LoggerCentral.processAborted()){ throw new
         * KLTaskEngineAbortException(); }
         */
        // Important notice: the following organization of if/then/else
        // structures is designed to handle all formats within a single code.
        // GenBank/Genpept/Embl and SW have indeed ID, Desc and Sequence keys on
        // different lines (Locus/ID, Definition/DE, Origin/SE). Fasta however
        // provide ID, Desc and Seq keys (which is the unique > char) on the
        // same line.

        // find a new sequence by its ID line
        if (line.startsWith(idKey)) {
          id = getID(line, idKey);
          readSeq = false;
          readDesc = false;
          writeSeq = false;
          handleTaxon = true;
          seqOk = true;
        }
        // find the description: begin
        if (line.startsWith(beginDescKey)) {
          if (!readDesc) {
            readDesc = true;
            bufDesc.setLength(0);
          }
          // silva
          if (headerFormat == DBUtils.SILVA_HEADER_FORMAT) {
            if (displaySilvaLicense) {
              // user must respect silva license term
              // LoggerCentral.info(LOGGER,
              // KDMSMessages.getString("CheckSilvaLicence"));
              displaySilvaLicense = false;
            }
            // Ludovic Antin 10/02/2015 : silva taxonomy is not the same as ncbi
            // taxonomy
            // String termId = null;
            //
            // String[] terms = line.split(";");
            // ArrayUtils.reverse(terms);
            //
            // for (String termName : terms) {
            // termId = dico.getTaxID(termName);
            // if (termId != null)
            // break;
            // }
            //
            // if (termId != null) {
            // line += " ";
            // line += DBXrefInstancesManager.HIT_DEF_LINE_START;
            // line += DBXrefInstancesManager.TAX_KEY;
            // line +=
            // DBXrefInstancesManager.HIT_DEF_LINE_XREF_NAME_ID_SEPARATOR;
            // line += termId.substring(1); // del first character ('n')
            // line += DBXrefInstancesManager.HIT_DEF_LINE_STOP;
            // } else {
            // LoggerCentral.info(LOGGER, "No taxonomy id for : " + line);
            // }

          }
          // in case of CDD sequences databank
          else if (headerFormat == DBUtils.CDD_HEADER_FORMAT) {
            // the header is something like
            // >gnl|CDD|214437 MTH00152, ND6, NADH dehydrogenase subunit 6;
            // Provisional
            // the last word in the id is the id indexed in the CDD dico terms

            int index = id.lastIndexOf('|') + 1;
            if (index > 0) {
              String termId = id.substring(index);
              // is this id exists in the CDD dico ?
              if (StringUtils.isNotBlank(termId)) {
                if (dico.getTerm(Dicos.CDD, termId) != null) {
                  line += DBXrefInstancesManager.SINGLE_CLASSIF_HEADER_TEMPLATE
                      .format(new Object[] { Dicos.CDD.xrefId, termId });
                }
              }
            }

          }
          // taxonomy
          else if (headerFormat == DBUtils.TAXONOMY_HEADER_FORMAT) {
            String termId = null;

            int lineBreak = line.indexOf(']') + 1;
            line = line.substring(0, lineBreak);

            int startTerm = line.indexOf('[') + 1;
            int endTerm = line.indexOf(']');

            String termName = line.substring(startTerm, endTerm);

            termId = dico.getTaxID(termName);

            if (termId != null) {
              // del first character ('n')
              line += DBXrefInstancesManager.SINGLE_CLASSIF_HEADER_TEMPLATE
                  .format(new Object[] { Dicos.NCBI_TAXONOMY.xrefId,
                      termId.substring(1) });
            } else {
              LoggerCentral.info(LOGGER, "No taxonomy id for : " + line);
            }

          }

          fillDescription(line, id, beginDescKey, bufDesc);
        }
        // find the description: continue
        else if (line.startsWith(contDescKey) && readDesc) {
          fillDescription(line, id, beginDescKey, bufDesc);
        } else if (readDesc) {
          readDesc = false;
        }
        if (!readSeq) {
          xref = XREF_MANAGER.getDbXref(line);
          instManager.addInstance(xref);
        } else {
          xref = null;
        }
        if (taxMatcher != null && handleTaxon) {
          taxMatcher.isSeqTaxonvalid(values, xref);
          seqOk = values[1];
          if (values[0] == true) {
            handleTaxon = false;
          }
        }
        // find the sequence itself
        if (line.startsWith(seqKey)) {
          if (seqOk) {
            // consider a new sequence when we have it
            // (Genbank/RefSeq: may contains CONTIG instead of sequence).
            try {
              if (monitor != null) {
                monitor.seqFound(getId(id, true));
              }

              // dump Fasta header here

              if (id.startsWith("tr|")) {
                id = id.replaceFirst("tr|", "sp|");
              }
              // lan 01/10/2014 : Because of empty sequence in silva original
              // downloaded file : write header only if sequence is not empty
              sequenceHeader = new StringBuilder();
              sequenceHeader.append('>');
              sequenceHeader.append(id);
              sequenceHeader.append(' ');
              sequenceHeader.append(SeqIOUtils.cleanDescription(bufDesc));
              sequenceHeader.append(' ');
              sequenceHeader.append(instManager.toString());
              sequenceHeader.append('\n');
              writeSeq = true;
            } catch (DBMSUniqueSeqIdRedundantException ex) {
              writeSeq = false;
            }
          }
          instManager = new DBXrefInstancesManager();
          readDesc = false;
          readSeq = true;
        }
        // dump the sequence: skip everything but the letters
        else if (writeSeq) {
          // exception: SNP data files from the NCBI contained comments!!!!!
          // Skip these lines that contain a # character
          bufSeq.setLength(0);
          dumpLetters = false;
          size = line.length();
          for (i = 0; i < size; i++) {
            c = line.charAt(i);
            if (c == '#') {
              dumpLetters = false;
              break;
            }
            if (Character.isLetter(c)) {
              bufSeq.append(c);
            } else if (c == '*' || c == '-') {
              bufSeq.append('X');
            }
            // FastQ: sequence is on a single line, truncate it
            if (isFastq && ((i + 1) % 80) == 0) {
              bufSeq.append("\n");
            }
            letterCount++;
            if (!dumpLetters)
              dumpLetters = true;
          }
          if (dumpLetters) {
            if (bufSeq.length() > 0) {
              // write sequence header the first time
              if ((sequenceHeader != null) && (sequenceHeader.length() > 0)) {
                writer.write(sequenceHeader.toString());
                sequenceHeader.setLength(0);
                entryCount++;
              }
              writer.write(bufSeq.toString());
              writer.write('\n');
            }
          }
          // FastQ: sequence is on a single line. As soon as sequence is
          // written, we stop future writings for
          // the current sequence
          if (isFastq) {
            writeSeq = false;
          }
        }
      }
      // writer.write("--EOF--");
      writer.flush();
    } catch (Exception e) {
      throw new DBParserException(e.getMessage());
    } finally {
      if (monitor != null) {
        monitor.stopProcessing(System.currentTimeMillis() - tim);
      }
    }
    return new int[] { entryCount, letterCount };
  }

  /**
   * Utility method. Read up to 50 line of a fasta file and returns the sequence
   * read.
   */
  private static String readFastaPartial(String fname, char idKey) {
    BufferedReader reader = null;
    StringBuffer buf;
    String line;
    boolean reading = false;
    int readCounter = 0, lineCounter = 0;

    buf = new StringBuffer();
    try {
      reader = new BufferedReader(new FileReader(fname));
      while ((line = reader.readLine()) != null) {
        if (line.length() == 0)
          continue;
        lineCounter++;
        if (lineCounter > 100)// read up to 100 lines max to find a idKey (> for
                              // Fasta, @ for FastQ)
          break;
        if (line.charAt(0) == idKey) {
          if (reading)
            break;
          if (!reading) {
            reading = true;
            continue;
          }
        }
        if (reading) {
          readCounter++;
          buf.append(line);
          // FastQ: sequence is on a single line
          if (idKey == '@' && line.startsWith("+")) {
            break;
          }
          if (readCounter > 10)// read up to 10 lines max to get a sequence
                               // peace
            break;
        }
      }
    } catch (Exception e) {// should not happen
    } finally {
      IOUtils.closeQuietly(reader);
    }

    return buf.toString();
  }

  /**
   * Basic method that checks whether the sequence is a protein. Return true if
   * this sequence contains a letter that differs from ACGTNUX.
   */
  public static boolean isProteic(String sequence) {
    int i, size;
    size = sequence.length();
    sequence = sequence.toUpperCase();
    for (i = 0; i < size; i++) {
      if ("ACGTNUX".indexOf(sequence.charAt(i)) == -1)
        return true;
    }
    return false;
  }

  /**
   * Method to check if the parameter sequence is DNA or RNA
   * 
   * @param sequence
   * 
   * @return true if the sequence contains characters 'T'
   */
  public static boolean isFastaDNA(String sequence) {
    if (StringUtils.isNotBlank(sequence)) {
      return sequence.toUpperCase().contains("T");
    }
    return false;
  }

  /**
   * Figures out if a file is a fasta protein sequence file.
   */
  public static boolean isFastaProt(String fName) {
    String seq;

    seq = readFastaPartial(fName, '>');
    if (seq.length() == 0)
      return false;
    return isProteic(seq);
  }

  /**
   * Figures out if a file is a fasta nucleotide sequence file. And store the
   * checked sequence in the buffer if not null
   */
  public static boolean isFastaNuc(String fName, StringBuffer sequenceBuffer) {
    String seq;

    seq = readFastaPartial(fName, '>');
    if (seq.length() == 0)
      return false;
    if (sequenceBuffer != null) {
      sequenceBuffer.append(seq);
    }
    return !isProteic(seq);
  }

  /**
   * Figures out if a file is a FastQ nucleotide sequence file.
   */
  public static boolean isFastaQ(String fName) {
    String seq;

    seq = readFastaPartial(fName, '@');
    if (seq.length() == 0)
      return false;
    // return !isProteic(seq.toUpperCase());
    // we do not control FastQ letters since we can have extended Nuc UIPAC
    // codes
    return true;
  }

  /**
   * Utility method. Checker if a file is of a given molecular type. Used to
   * check Embl/Genbanl/Genpept file to check if it contains AA or BP key.
   */
  private static boolean checkfile(String file, String idKey, String molType) {
    BufferedReader reader = null;
    String line, id = null;
    int counter = 0;

    try {
      reader = new BufferedReader(new InputStreamReader(new FileInputStream(
          file), "UTF-8"));
      while ((line = reader.readLine()) != null) {
        if (counter > 50)
          break;
        if (line.startsWith(idKey)) {
          id = line.toUpperCase().trim();
          break;
        }
        counter++;
      }

    } catch (Exception e) {
    }
    if (reader != null) {
      try {
        reader.close();
      } catch (IOException e) {
      }
    }
    if (id == null)
      return false;
    return id.indexOf(molType) != -1;
  }

  /**
   * Figures out if a file is from Uniprot.
   */
  public static boolean isUniProt(String file) {
    return checkfile(file, SwissProtParser.KEYWORD_ID,
        SwissProtParser.KEYWORD_NB_LETTERS + ".");
  }

  /**
   * Figures out if a file is from Embl.
   */
  public static boolean isEMBL(String file) {
    return checkfile(file, "ID", " BP.");
  }

  /**
   * Figures out if a file is from Genbank.
   */
  public static boolean isGenbank(String file) {
    return checkfile(file, "LOCUS", " BP ");
  }

  /**
   * Figures out if a file is from Genpept.
   */
  public static boolean isGenpept(String file) {
    return checkfile(file, "LOCUS", " AA ");
  }

  // startFrame, endFrame = [0..2]
  private static boolean translateSequenceForward(OutputStream os,
      Sequence seq, TranslationTable tt, int startFrame, int endFrame) {
    SymbolList dna, rna, prot, syms;
    Sequence trans;
    String desc;
    boolean bRet;

    try {
      // for each frame
      for (int i = startFrame; i <= endFrame; i++) {
        dna = seq.subList(i + 1, seq.length() - (seq.length() - i) % 3);
        if (dna.getAlphabet() == DNATools.getDNA()) {
          rna = DNATools.toRNA(dna);
        } else {
          rna = dna;
        }
        syms = SymbolListViews.windowedSymbolList(rna, 3);
        prot = SymbolListViews.translate(syms, tt);
        desc = SeqIOUtils.getDescription(seq, SeqIOUtils.FASTADNA, true);
        trans = SequenceTools.createSequence(prot, "", seq.getName()
            + (desc != null ? desc : "") + " TranslationFrame: +" + (i + 1),
            Annotation.EMPTY_ANNOTATION);
        SeqIOTools.writeFasta(os, trans);
      }
      bRet = true;
    } catch (Exception ex) {
      LOGGER.warn("Translation failed: " + ex);
      bRet = false;
    }
    return (bRet);
  }

  // startFrame, endFrame = [0..2]
  private static boolean translateSequenceReverse(OutputStream os,
      Sequence seq, TranslationTable tt, int startFrame, int endFrame) {
    SymbolList dna, rna, prot, syms;
    String desc;
    Sequence trans;
    boolean bRet;

    try {
      // for each frame
      for (int i = startFrame; i <= endFrame; i++) {
        dna = seq.subList(i + 1, seq.length() - (seq.length() - i) % 3);
        if (dna.getAlphabet() == DNATools.getDNA()) {
          rna = DNATools.toRNA(dna);
        } else {
          rna = dna;
        }
        rna = RNATools.reverseComplement(rna);
        syms = SymbolListViews.windowedSymbolList(rna, 3);
        prot = SymbolListViews.translate(syms, tt);
        desc = SeqIOUtils.getDescription(seq, SeqIOUtils.FASTADNA, true);
        trans = SequenceTools.createSequence(prot, "", seq.getName()
            + (desc != null ? desc : "") + " TranslationFrame: -" + (i + 1),
            Annotation.EMPTY_ANNOTATION);
        SeqIOTools.writeFasta(os, trans);
      }
      bRet = true;
    } catch (Exception ex) {
      LOGGER.warn("Translation failed: " + ex);
      bRet = false;
    }
    return (bRet);
  }

  /**
   * Translates a set of DNA sequences to their reverse or complement or RC
   * counterpart.
   * 
   * @param sequences
   *          a reader to a Fasta formatted set of sequences.
   * @param gCode
   *          the genetic code to use.
   * @param transType
   *          one of the TRANS_XXX constants defined in this class.
   * 
   * @return a Fasta formatted string or null if conversion failed
   * */
  public static String translateSequences(Reader sequences, String gCode,
      int transType) {
    BufferedReader br = null;
    ByteArrayOutputStream baos = null;
    Sequence seq;
    TranslationTable tt;
    try {
      br = new BufferedReader(/* new StringReader(sequences) */sequences);
      baos = new ByteArrayOutputStream();
      SequenceIterator seqi = (SequenceIterator) SeqIOTools.fileToBiojava(
          "fasta", "dna", br);
      if (gCode == null)
        tt = RNATools.getGeneticCode(TranslationTable.UNIVERSAL);
      else
        tt = RNATools.getGeneticCode(gCode);
      while (seqi.hasNext()) {
        seq = seqi.nextSequence();
        switch (transType) {
          case TRANS_PLUS_1:
            SeqIOUtils.translateSequenceForward(baos, seq, tt, 0, 0);
            break;
          case TRANS_PLUS_2:
            SeqIOUtils.translateSequenceForward(baos, seq, tt, 1, 1);
            break;
          case TRANS_PLUS_3:
            SeqIOUtils.translateSequenceForward(baos, seq, tt, 2, 2);
            break;
          case TRANS_PLUS_ALL:
            SeqIOUtils.translateSequenceForward(baos, seq, tt, 0, 2);
            break;
          case TRANS_MINUS_1:
            SeqIOUtils.translateSequenceReverse(baos, seq, tt, 0, 0);
            break;
          case TRANS_MINUS_2:
            SeqIOUtils.translateSequenceReverse(baos, seq, tt, 1, 1);
            break;
          case TRANS_MINUS_3:
            SeqIOUtils.translateSequenceReverse(baos, seq, tt, 2, 2);
            break;
          case TRANS_MINUS_ALL:
            SeqIOUtils.translateSequenceReverse(baos, seq, tt, 0, 2);
            break;
          case TRANS_ALL:
            SeqIOUtils.translateSequenceForward(baos, seq, tt, 0, 2);
            SeqIOUtils.translateSequenceReverse(baos, seq, tt, 0, 2);
            break;
        }
      }
    } catch (Exception ex) {
      LOGGER.warn("Translation failed: " + ex);
      baos = null;
    } finally {
      IOUtils.closeQuietly(baos);
      IOUtils.closeQuietly(br);
    }
    return (baos != null ? baos.toString() : null);
  }

  /**
   * Converts a set of DNA sequences to their reverse or complement or RC
   * counterpart.
   * 
   * @param sequences
   *          a reader to a Fasta formatted set of sequences.
   * @param convType
   *          one of the STYPE_XXX constants defined in this class.
   * 
   * @return a Fasta formatted string or null if conversion failed
   * */
  public static String convertSequences(Reader sequences, int convType) {
    BufferedReader br = null;
    ByteArrayOutputStream baos = null;
    Sequence seq, nSeq;
    String desc;
    SymbolList dna;

    try {
      br = new BufferedReader(sequences);
      baos = new ByteArrayOutputStream();
      SequenceIterator seqi = (SequenceIterator) SeqIOTools.fileToBiojava(
          "fasta", "dna", br);
      while (seqi.hasNext()) {
        dna = null;
        seq = seqi.nextSequence();
        switch (convType) {
          case STYPE_COMPL:
            dna = DNATools.complement(seq);
            break;
          case STYPE_RC:
            dna = DNATools.reverseComplement(seq);
            break;
          case STYPE_REVERSE:
            dna = new ReverseSymbolList(seq);
            break;
          default:
            dna = seq;
        }
        if (dna != null) {
          // Use always FastaDNA type: ok, this is just to retrieve the
          // description
          // will work whatever the sequence type.
          desc = SeqIOUtils.getDescription(seq, SeqIOUtils.FASTADNA, true);
          nSeq = SequenceTools.createSequence(dna, "", seq.getName()
              + (desc != null ? desc : "") + " (" + SEQ_TYPE[convType] + ")",
              Annotation.EMPTY_ANNOTATION);
          SeqIOTools.writeFasta(baos, nSeq);
        }
      }
    } catch (Exception ex) {
      LOGGER.warn("Conversion failed: " + ex);
      baos = null;
    } finally {
      IOUtils.closeQuietly(baos);
      IOUtils.closeQuietly(br);
    }
    return (baos != null ? baos.toString() : null);
  }

  // put code of ReverseSymbolList here since that class is not exposed in
  // BioJava 1.6 Jar!
  private static class ReverseSymbolList extends AbstractSymbolList implements
      Serializable {
    private static final long serialVersionUID = -8574797630954091691L;
    private final SymbolList  source;

    @SuppressWarnings("unused")
    public SymbolList getSource() {
      return source;
    }

    public ReverseSymbolList(SymbolList source) {
      this.source = source;
    }

    public Alphabet getAlphabet() {
      return source.getAlphabet();
    }

    public int length() {
      return source.length();
    }

    public Symbol symbolAt(int index) throws IndexOutOfBoundsException {
      return source.symbolAt(length() - index + 1);
    }
  }

}
