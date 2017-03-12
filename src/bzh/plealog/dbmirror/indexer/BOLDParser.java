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
package bzh.plealog.dbmirror.indexer;

import iubio.bioseq.Bioseq;
import iubio.bioseq.SeqRange;
import iubio.readseq.BasicBioseqDoc;
import iubio.readseq.BioseqDocVals;
import iubio.readseq.BioseqFormats;
import iubio.readseq.BioseqRecord;
import iubio.readseq.BioseqWriterIface;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.lucenedico.DicoTermQuerySystem;
import bzh.plealog.dbmirror.util.Formatters;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.conf.DBMirrorConfig;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptorUtils;
import bzh.plealog.dbmirror.util.log.LoggerCentral;
import bzh.plealog.dbmirror.util.runner.DBMSUniqueSeqIdRedundantException;

/**
 * This class defines a parser for BOLD database files. Mainly, it consists in
 * converting the native BOLD fasta file into a Genbank record.
 * 
 * @author Patrick G. Durand
 */
public class BOLDParser implements DBParsable {

  private int                           _entries;
  @SuppressWarnings("unused")
  private boolean                       _verbose;
  @SuppressWarnings("unused")
  private boolean                       _warnError;
  private ParserMonitor                 _pMonitor;

  public static final String            NONE              = "-";
  public static final String            BOLD_ID_PREFIX    = "BOLD:";
  public static final String            GB_FILE_EXT       = ".gb";

  // constants used to prepare Genbank format
  private static final String           SOURCE            = "source";
  private static final String           GENE              = "gene";
  private static final String           CDS               = "cds";
  private static final String           TRANS             = "translation";
  private static final String           ORG               = "organism";
  private static final String           TAX               = "taxonomy";
  private static final String           DBXREF            = "db_xref";
  // if date cannot be formatted
  private static final String           DEF_DATE          = "01-JAN-2000";
  private static final String           DEF_END1          = " barcode ";
  private static final String           DEF_END2          = " barcode sequence ";
  private static final String           DIVISION          = "BLD";

  // used to convert BOLD date to Genbank format
  private final SimpleDateFormat        DATE_BOLD         = new SimpleDateFormat(
                                                              "dd-MMM-yy",
                                                              Locale.UK);
  private final SimpleDateFormat        DATE_GB           = new SimpleDateFormat(
                                                              "dd-MMM-yyyy",
                                                              Locale.UK);

  // for BOLD, use :
  // http://www.boldsystems.org/index.php/Public_RecordView?processid=XXX with
  // XXX=ABMMC10086-10 (a process id without marker name)
  // or
  // http://www.boldsystems.org/connectivity/specimenlookup.php?processid=SASNB553-09.COI-5P
  // (a process id) (utilisez par Genbank)
  private static final String           BXREF             = "BOLD:";
  private static final String           TXREF             = "taxon:";
  // for Genbank, use : http://www.ncbi.nlm.nih.gov/nuccore/XXX
  private static final String           GBXREF            = "GB:";
  private static final String           ACC               = "accession";
  private static final String           LIN               = "linear";
  private static final String           COMM              = "International Barcode of Life (iBOL) Data processed by Plealog Databank Manager System from BOLD databank file.";
  private static final String           TAXSEP            = ";";
  private static final String           TAXEND            = ".";

  // BOLD source file fields
  public static final String            PROCESSID         = "processid";
  public static final String            SAMPLEID          = "sampleid";
  public static final String            MUSEUMID          = "museumid";
  public static final String            FIELDID           = "fieldid";
  public static final String            BIN_GUID          = "bin_guid";
  public static final String            BIN_NAME          = "bin_name";
  public static final String            VOUCHERTYPE       = "vouchertype";
  public static final String            INST_REG          = "inst_reg";
  public static final String            PHYLUM_REG        = "phylum_reg";
  public static final String            CLASS_REG         = "class_reg";
  public static final String            ORDER_REG         = "order_reg";
  public static final String            FAMILY_REG        = "family_reg";
  public static final String            SUBFAMILY_REG     = "subfamily_reg";
  public static final String            GENUS_REG         = "genus_reg";
  public static final String            SPECIES_REG       = "species_reg";
  public static final String            TAXONOMIST_REG    = "taxonomist_reg";
  public static final String            COLLECTORS        = "collectors";
  public static final String            COLLECTIONDATE    = "collectiondate";
  public static final String            LIFESTAGE         = "lifestage";
  public static final String            LAT               = "lat";
  public static final String            LON               = "lon";
  public static final String            SITE              = "site";
  public static final String            SECTOR            = "sector";
  public static final String            REGION            = "region";
  public static final String            PROVINCE_REG      = "province_reg";
  public static final String            COUNTRY_REG       = "country_reg";
  public static final String            FUNDINGSRC        = "fundingsrc";
  public static final String            SEQENTRYID        = "seqentryid";
  public static final String            SEQDATAID         = "seqdataid";
  public static final String            MARKER_CODE       = "marker_code";
  public static final String            NUCRAW            = "nucraw";
  public static final String            AMINORAW          = "aminoraw";
  public static final String            SEQ_UPDATE        = "seq_update";
  public static final String            TOTAL_TRACE_COUNT = "total_trace_count";
  public static final String            TRACE_COUNT       = "trace_count";
  public static final String            HIGH_TRACE_COUNT  = "high_trace_count";
  public static final String            ACCESSION         = "accession";

  public static HashSet<String>         VALID_FIELDS;
  public static HashMap<String, String> VALID_DEFINITION;

  static {
    VALID_FIELDS = new HashSet<String>();
    VALID_FIELDS.add(PROCESSID);
    VALID_FIELDS.add(SAMPLEID);
    VALID_FIELDS.add(MUSEUMID);
    VALID_FIELDS.add(FIELDID);
    VALID_FIELDS.add(BIN_GUID);
    VALID_FIELDS.add(BIN_NAME);
    VALID_FIELDS.add(VOUCHERTYPE);
    VALID_FIELDS.add(INST_REG);
    VALID_FIELDS.add(PHYLUM_REG);
    VALID_FIELDS.add(CLASS_REG);
    VALID_FIELDS.add(ORDER_REG);
    VALID_FIELDS.add(FAMILY_REG);
    VALID_FIELDS.add(SUBFAMILY_REG);
    VALID_FIELDS.add(GENUS_REG);
    VALID_FIELDS.add(SPECIES_REG);
    VALID_FIELDS.add(TAXONOMIST_REG);
    VALID_FIELDS.add(COLLECTORS);
    VALID_FIELDS.add(COLLECTIONDATE);
    VALID_FIELDS.add(LIFESTAGE);
    VALID_FIELDS.add(LAT);
    VALID_FIELDS.add(LON);
    VALID_FIELDS.add(SITE);
    VALID_FIELDS.add(SECTOR);
    VALID_FIELDS.add(REGION);
    VALID_FIELDS.add(PROVINCE_REG);
    VALID_FIELDS.add(COUNTRY_REG);
    VALID_FIELDS.add(FUNDINGSRC);
    VALID_FIELDS.add(SEQENTRYID);
    VALID_FIELDS.add(SEQDATAID);
    VALID_FIELDS.add(MARKER_CODE);
    VALID_FIELDS.add(NUCRAW);
    VALID_FIELDS.add(AMINORAW);
    VALID_FIELDS.add(SEQ_UPDATE);
    VALID_FIELDS.add(TOTAL_TRACE_COUNT);
    VALID_FIELDS.add(TRACE_COUNT);
    VALID_FIELDS.add(HIGH_TRACE_COUNT);
    VALID_FIELDS.add(ACCESSION);

    VALID_DEFINITION = new HashMap<String, String>();
    VALID_DEFINITION.put("COI-5P",
        "Cytochrome Oxidase Subunit 1 gene, partial cds");
    VALID_DEFINITION.put("rbcL",
        "Ribulose-bisphosphate carboxylase gene, partial cds");
    VALID_DEFINITION.put("matK", "Mature K gene, partail cds");
    VALID_DEFINITION.put("ITS", "Internal Transcribed Spacer");
  }
  // logger
  private static final Log              LOGGER            = LogFactory
                                                              .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                                  + ".BOLDParser");

  public BOLDParser() {
  }

  /**
   * Implementation of DBParsable interface.
   */
  public void setVerbose(boolean verbose) {
    _verbose = verbose;
  }

  /**
   * When a BOLD entry is not valid one can choose to notify this parser to
   * avoid raising an error. For that purpose, pass true. Default is false, i.e.
   * the parser will raise an exception if the BOLD file is not valid.
   */
  public void setWarnError(boolean warn) {
    _warnError = warn;
  }

  /**
   * Implementation of DBParsable interface.
   */
  public void setParserMonitor(ParserMonitor pm) {
    _pMonitor = pm;
  }

  private String[] getBoldHeader(String line) {
    String[] data;

    data = line.split("\t");
    for (int i = 0; i < data.length; i++) {
      if (data[i].length() == 0) {
        data[i] = NONE;
      }
    }
    return data;
  }

  // for internal use only to generate Java constants
  /*
   * private static void generateJavaHeaders(String[] headerKeys){
   * 
   * for(String str:headerKeys){
   * System.out.println("public static final String "
   * +str.toUpperCase()+" = \""+str+"\";"); } for(String str:headerKeys){
   * System.out.println("VALID_FIELDS.add("+str.toUpperCase()+");"); } }
   */
  private Map<String, String> prepareStructuredData(String[] headerKeys,
      String[] headerValues) {
    HashMap<String, String> data;
    String str;
    int i, size, idx;

    data = new HashMap<String, String>();
    size = headerKeys.length;
    for (i = 0; i < size; i++) {
      // Species : removing the BOLD:XXX ID stuff
      if (headerKeys[i].equals(SPECIES_REG)
          && (headerValues[i].equals(NONE) == false)) {
        str = headerValues[i];
        idx = str.indexOf(BOLD_ID_PREFIX);
        if (idx != -1) {
          headerValues[i] = str.substring(0, idx - 1);
        }
      }
      // Analyzing data by hand shows that last column (accession) may be
      // missing !
      if (i < headerValues.length)
        data.put(headerKeys[i], headerValues[i]);
      else
        data.put(headerKeys[i], "-");
    }
    return data;
  }

  private String getSeqId(String str) {
    int idx = str.indexOf('.');
    if (idx == -1) {
      return str;
    } else {
      // "." being used as the "version" separator in Genbank seqID, we have to
      // modify them
      return Formatters.replaceAll(str, ".", "_");
    }
  }

  private String getTaxonomy(Map<String, String> data) {
    StringBuffer buf;
    String str;

    str = data.get(PHYLUM_REG);
    if (str.equals(NONE))
      return null;
    buf = new StringBuffer();
    // phylum
    buf.append(str);
    // class
    str = data.get(CLASS_REG);
    if (str.equals(NONE) == false) {
      buf.append(TAXSEP);
      buf.append(str);
      // order
      str = data.get(ORDER_REG);
      if (str.equals(NONE) == false) {
        buf.append(TAXSEP);
        buf.append(str);
        // family
        str = data.get(FAMILY_REG);
        if (str.equals(NONE) == false) {
          buf.append(TAXSEP);
          buf.append(str);
          // subfamily
          str = data.get(SUBFAMILY_REG);
          if (str.equals(NONE) == false) {
            buf.append(TAXSEP);
            buf.append(str);
            // genus
            str = data.get(GENUS_REG);
            if (str.equals(NONE) == false) {
              buf.append(TAXSEP);
              buf.append(str);
              // species
              str = data.get(SPECIES_REG);
              if (str.equals(NONE) == false) {
                buf.append(TAXSEP);
                buf.append(str);
              }
            }
          }
        }
      }
    }
    buf.append(TAXEND);
    return buf.toString();
  }

  private String getOrganism(Map<String, String> data) {
    // try species
    String str = data.get(SPECIES_REG);
    if (str != null && str.equals(NONE) == false) {
      // remove ending "sp." if any ; may confuse Taxonomy ID retrieval
      int idx = str.indexOf(" sp.");
      if (idx != -1) {
        return str.substring(0, idx);
      } else {
        return str;
      }
    }
    // then, genus
    str = data.get(GENUS_REG);
    if (str != null && str.equals(NONE) == false) {
      return str;
    }
    // otherwise, not found
    return NONE;
  }

  private void addSourceNote(BasicBioseqDoc seqdoc, Map<String, String> data,
      String field) {
    String value;

    value = data.get(field);
    if (value == null || NONE.equals(value)) {
      return;
    }
    seqdoc.addFeatureNote(field, value);
  }

  // writing a genbank-like file is inspired from:
  // http://www.ncbi.nlm.nih.gov/nuccore/HM416780.1
  private void dumpSeqRecord(BioseqWriterIface writer,
      Map<String, String> data, DicoTermQuerySystem dico) throws Exception {
    Bioseq seq;
    BasicBioseqDoc seqdoc;
    BioseqRecord seqrec;
    SeqRange range;
    String seqId, nucSequence, aaSequence, acc, tax, org, def, defDate, taxId;

    // get sequences
    nucSequence = data.get(NUCRAW);
    aaSequence = data.get(AMINORAW);
    range = new SeqRange("1.." + nucSequence.length());
    seqId = getSeqId(data.get(PROCESSID));
    // prepare Bioseq record
    seq = new Bioseq(nucSequence);
    seqdoc = new BasicBioseqDoc(seqId);

    // definition
    def = VALID_DEFINITION.get(data.get(MARKER_CODE));
    if (def != null)
      seqdoc.addDocField(BioseqDocVals.kDescription, BXREF + data.get(SAMPLEID)
          + DEF_END1 + def);
    else
      seqdoc.addDocField(BioseqDocVals.kDescription, BXREF + data.get(SAMPLEID)
          + DEF_END2);
    // Accession
    seqdoc.addDocField(BioseqDocVals.kAccession, seqId);
    // Topology
    seqdoc.addDocField(BioseqDocVals.kSeqcircle, LIN);
    // Division
    seqdoc.addDocField(BioseqDocVals.kDivision, DIVISION);
    // Pub date
    try {
      Date d = DATE_BOLD.parse(data.get(SEQ_UPDATE).toUpperCase());
      defDate = DATE_GB.format(d).toUpperCase();
    } catch (Exception e) {
      defDate = DEF_DATE;
    }
    seqdoc.addDocField(BioseqDocVals.kDate, defDate);
    // Source, i.e. organism
    org = getOrganism(data);
    if (org != null && NONE.equals(org) == false)
      seqdoc.addDocField(BioseqDocVals.kSource, org);
    // Taxonomy
    tax = getTaxonomy(data);
    seqdoc.addDocField(BioseqDocVals.kTaxonomy, tax);
    // Comments
    seqdoc.addComment(COMM);
    // sequence statistics
    seqdoc.addSequenceStats(seq);

    // Feature Table

    // source
    seqdoc.addFeature(SOURCE, range);
    if (org != null && NONE.equals(org) == false)
      seqdoc.addFeatureNote(ORG, org);
    seqdoc.addFeatureNote(TAX, tax);
    taxId = dico.getTaxID(org);
    if (taxId != null) {
      // start substring at index 1 to remove the starting "n"
      // (see KDMS Taxonomy API for more information)
      seqdoc.addFeatureNote(DBXREF, TXREF + taxId.substring(1));
    }
    seqdoc.addFeatureNote(DBXREF, BXREF + data.get(PROCESSID));
    acc = data.get(ACCESSION);
    if (acc != null && NONE.equals(acc) == false)
      seqdoc.addFeatureNote(ACC, GBXREF + acc);

    addSourceNote(seqdoc, data, SAMPLEID);
    addSourceNote(seqdoc, data, MUSEUMID);
    addSourceNote(seqdoc, data, FIELDID);
    addSourceNote(seqdoc, data, BIN_GUID);
    addSourceNote(seqdoc, data, BIN_NAME);
    addSourceNote(seqdoc, data, VOUCHERTYPE);
    addSourceNote(seqdoc, data, INST_REG);
    addSourceNote(seqdoc, data, TAXONOMIST_REG);
    addSourceNote(seqdoc, data, COLLECTORS);
    addSourceNote(seqdoc, data, COLLECTIONDATE);
    addSourceNote(seqdoc, data, LIFESTAGE);
    addSourceNote(seqdoc, data, LAT);
    addSourceNote(seqdoc, data, LON);
    addSourceNote(seqdoc, data, SITE);
    addSourceNote(seqdoc, data, SECTOR);
    addSourceNote(seqdoc, data, REGION);
    addSourceNote(seqdoc, data, PROVINCE_REG);
    addSourceNote(seqdoc, data, COUNTRY_REG);
    addSourceNote(seqdoc, data, FUNDINGSRC);
    addSourceNote(seqdoc, data, SEQENTRYID);
    addSourceNote(seqdoc, data, SEQDATAID);
    addSourceNote(seqdoc, data, TOTAL_TRACE_COUNT);
    addSourceNote(seqdoc, data, TRACE_COUNT);
    addSourceNote(seqdoc, data, HIGH_TRACE_COUNT);

    // gene
    seqdoc.addFeature(GENE, range);
    seqdoc.addFeatureNote(GENE, data.get(MARKER_CODE));

    // CDS, if any is available
    if (aaSequence != null && NONE.equals(aaSequence) == false) {
      seqdoc.addFeature(CDS, range);
      seqdoc.addFeatureNote(TRANS, aaSequence);
    }

    // now write sequence record
    seqrec = new BioseqRecord(seq, seqdoc);
    if (writer.setSeq(seqrec))
      writer.writeSeqRecord();
    else
      throw new RuntimeException("Failed to write: " + seqrec);
  }

  private boolean invalidSeq(String seq) {
    // we found string "#NAME?" is some entries... discard that !!!!
    // we cannot really test the entire sequence (iBOLD is quite huge)
    int i, max = Math.min(10, seq.length());
    for (i = 0; i < max; i++) {
      if (Character.isLetter(seq.charAt(i)) == false) {
        return true;
      }
    }
    return false;
  }

  /**
   * Implementation of DBParsable interface.
   */
  public void setCheckSeqIdRedundancy(boolean checkNR) {
    // nothing to do
  }

  /**
   * Implementation of DBParsable interface.
   */
  public void parse(String file, StorageSystem ss) throws DBParserException {
    BufferedReader reader = null;
    BioseqWriterIface writer = null;
    String line, id, fName, seq;
    String[] headerKeys, headerValues;
    Map<String, String> data;
    long curPos = 0, start = 0;
    int endOfLineSize;
    BufferedWriter bWriter = null;
    DBMirrorConfig mirrorConfig;
    DicoTermQuerySystem dico = null;
    try {
      mirrorConfig = DBDescriptorUtils.getDBMirrorConfig(DBMSAbstractConfig
          .getLocalMirrorConfFile());
      dico = DicoTermQuerySystem.getDicoTermQuerySystem(mirrorConfig);
      endOfLineSize = Utils.getLineTerminatorSize(file);
      _entries = 0;
      reader = new BufferedReader(new InputStreamReader(new FileInputStream(
          file), "UTF-8"));
      fName = file;
      id = null;
      if (_pMonitor != null) {
        _pMonitor.startProcessingFile(fName, new File(file).length());
      }
      // read first line: the BOLD file header
      line = reader.readLine();
      if (line != null) {
        headerKeys = getBoldHeader(line);
      } else {
        headerKeys = new String[0];
      }
      // generateJavaHeaders(headerKeys);

      // check data files
      for (String str : headerKeys) {
        if (VALID_FIELDS.contains(str) == false) {
          throw new RuntimeException("Invalid data field: " + str);
        }
      }
      // prepare Bioseq writer
      writer = BioseqFormats.newWriter(BioseqFormats.formatFromName("gb"));
      bWriter = new BufferedWriter(new FileWriter(file + GB_FILE_EXT));
      writer.setOutput(bWriter);
      writer.writeHeader();

      while ((line = reader.readLine()) != null) {
        start = curPos;

        headerValues = getBoldHeader(line);

        data = prepareStructuredData(headerKeys, headerValues);

        curPos += (long) (line.length() + endOfLineSize);
        id = getSeqId(data.get(PROCESSID));

        seq = data.get(NUCRAW);
        if (seq == null || seq.length() == 0 || invalidSeq(seq)) {
          LoggerCentral.error(LOGGER, "discard entry: " + id
              + ": no nucleotide sequence [" + seq + "]");
          // BOLD may contain sequence without any nucleotide sequence !
          continue;
        }
        if (_pMonitor != null) {
          try {
            _pMonitor.seqFound(id, id, fName, start, curPos, true);
          } catch (DBMSUniqueSeqIdRedundantException e) {
            LoggerCentral.warn(LOGGER, "discard redundant entry: " + id);
            // we can skip redundant seqId; after some file analysis, redundants
            // seqIds really
            // target same entries, but provided in different files !
            continue;
          }
        }

        dumpSeqRecord(writer, data, dico);
        _entries++;
      }
      writer.writeTrailer();
      bWriter.flush();
    } catch (Exception e) {
      String msg = "Error while parsing BOLD entry no. " + (_entries + 1);
      LOGGER.warn(msg + ": " + e.getMessage());
      throw new DBParserException(msg + ": " + e.getMessage());
    } finally {
      IOUtils.closeQuietly(reader);
      IOUtils.closeQuietly(bWriter);
      try {
        if (writer != null) {
          writer.close();
        }
      } catch (Exception e) {/* should not happen, so we hide this error */
      }
      DicoTermQuerySystem.closeDicoTermQuerySystem();
      if (_pMonitor != null) {
        _pMonitor.stopProcessingFile(file, _entries);
      }
    }
    LoggerCentral.info(LOGGER, new File(file).getName() + " content: "
        + _entries + " entries");
  }

  /**
   * Implementation of DBParsable interface.
   */
  public int getEntries() {
    return _entries;
  }

  /**
   * Utility method aims at exploring a zipped BOLD file in order to detect the
   * appropriate sequence file. It has been added since BOLD zip file name and
   * its content may not match.
   * 
   * @return a file name without it path. File detection relies on the presence
   *         of a .tsv file or because a zip file entry starts with iBOL. Return
   *         null if the method fails to detect a plain BOLD sequence file.
   */
  public static String extractBoldFileName(File f) {

    ZipEntry e;
    String name;
    Enumeration<? extends ZipEntry> entries;

    // JRE 1.7 syntax : automatically call zf.close
    try (ZipFile zf = new ZipFile(f)) {
      entries = zf.entries();
      while (entries.hasMoreElements()) {
        e = entries.nextElement();
        name = e.getName();
        if (name.endsWith(".tsv")) {
          return name;
        } else if (name.startsWith("iBOL")) {
          return name;
        }
      }
      return null;

    } catch (Exception e1) {
      return null;
    }
  }
}
