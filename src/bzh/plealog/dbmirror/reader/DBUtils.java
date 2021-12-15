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
package bzh.plealog.dbmirror.reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.StringReader;
import java.nio.channels.FileChannel;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.biojava.bio.Annotation;
import org.biojava.bio.seq.Feature;
import org.biojava.bio.seq.Sequence;
import org.biojava.bio.seq.StrandedFeature;
import org.biojava.bio.seq.io.GenbankFileFormer;
import org.biojava.bio.seq.io.ReferenceAnnotation;
import org.biojava.bio.seq.io.SeqIOTools;
import org.biojavax.CrossRef;
import org.biojavax.RichObjectFactory;
import org.biojavax.bio.seq.CompoundRichLocation;
import org.biojavax.bio.seq.EmptyRichLocation;
import org.biojavax.bio.seq.Position;
import org.biojavax.bio.seq.RichLocation;
import org.biojavax.bio.seq.SimpleRichLocation;
import org.biojavax.bio.seq.io.GenbankLocationParser;

import bzh.plealog.bioinfo.api.data.feature.FPosition;
import bzh.plealog.bioinfo.api.data.feature.FRange;
import bzh.plealog.bioinfo.api.data.feature.FeatureLocation;
import bzh.plealog.bioinfo.api.data.feature.FeatureTable;
import bzh.plealog.bioinfo.data.feature.IFeatureTable;
import bzh.plealog.bioinfo.data.sequence.IBankSequenceInfo;
import bzh.plealog.dbmirror.indexer.SwissProtParser;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.sequence.SeqIOUtils;

/**
 * This class contains utility methods to handle database files content.
 * 
 * @author Patrick G. Durand
 */
@SuppressWarnings("deprecation")
public class DBUtils {

  private static final Logger    LOGGER                 = LogManager.getLogger(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                             + ".DBUtils");
  private static final String STD_DATE               = "01-JAN-1970";
  private static final int    STD_DATE_INT           = 19700101;
  private static final String NOT_SPECIFIED          = "not specified";
  private static final String QUAL_LOCATION_KEY      = "Location";
  private static final String QUAL_INTERNAl_DATA     = "internal_data";
  private static final String GB_PATENT_DATA_KEY     = "Patent:";
  private static final String EMBL_PATENT_DATA_KEY   = "Patent number";
  private static final String EMBL_PATENT_DATA_KEY2  = "PN ";
  public static final String  DB_XREF_KEY            = "db_xref";
  public static final String  TAXON_KEY              = "taxon";

  public static final int     SW_DB_FORMAT           = 1;
  public static final int     GB_DB_FORMAT           = 2;
  public static final int     FAS_DB_FORMAT          = 3;
  public static final int     GP_DB_FORMAT           = 4;
  public static final int     EM_DB_FORMAT           = 5;
  // Fasta proteic
  public static final int     FP_DB_FORMAT           = 6;
  // Fasta nucleic
  public static final int     FN_DB_FORMAT           = 7;

  // header formats from 10 to 15 are used by Dicos

  public static final int     NO_HEADER_FORMAT       = 20;
  public static final int     SILVA_HEADER_FORMAT    = 21;
  public static final int     TAXONOMY_HEADER_FORMAT = 22;
  public static final int     BOLD_HEADER_FORMAT     = 23;
  public static final int     CDD_HEADER_FORMAT      = 24;

  private static final SimpleDateFormat getDateChecker() {
    return new SimpleDateFormat("dd-MMM-yyyy", Locale.UK);
  }

  public static final SimpleDateFormat getDateFormatter() {
    return new SimpleDateFormat("yyyyMMdd", Locale.UK);
  }

  /**
   * Reads the content of a sequence database file and returns the corresponding
   * entry stored in a file.
   * 
   * @param from
   *          starting byte to start reading in the file
   * @param to
   *          ending byte to stop reading the file
   * 
   * @return a file storing the entry or null if something wrong occured during
   *         file processing.
   */
  public static File readDBEntry(String file, long from, long to) {
    FileInputStream fis = null;
    FileOutputStream fos = null;
    FileChannel fc = null;
    FileChannel fOut = null;
    File output;

    try {
      output = File.createTempFile("klentry", null,
          new File(DBMSAbstractConfig.getWorkingPath()));
      fis = new FileInputStream(file);
      fos = new FileOutputStream(output);
      fc = fis.getChannel();
      fOut = fos.getChannel();
      fc.transferTo(from, to - from + 1, fOut);
      fos.flush();
    } catch (Exception e) {
      LOGGER.warn("Unable to read DB entry: " + e + "(from=" + from + ", to="
          + to + ", file=" + file + ")");
      output = null;
    } finally {
      IOUtils.closeQuietly(fis);
      IOUtils.closeQuietly(fos);
      try {
        if (fOut != null)
          fOut.close();
      } catch (Exception e) {
      }
      try {
        if (fc != null)
          fc.close();
      } catch (Exception e) {
      }
      // IOUtils.closeQuietly(fOut);
      // IOUtils.closeQuietly(fc);
    }
    return output;
  }

  /**
   * Utility method used by analyseFeatureLocation.
   */
  private static String getPosType(String t) {
    String type;
    if (t != null) {
      if (t.equals(Position.IN_RANGE)) {
        type = FPosition.IN_RANGE;
      } else if (t.equals(Position.BETWEEN_BASES)) {
        type = FPosition.BETWEEN_BASES;
      } else {
        type = FPosition.UNIQUE;
      }
    } else {
      type = FPosition.UNIQUE;
    }
    return type;
  }

  /**
   * Utility method used by analyseFeatureLocation.
   */
  private static int getStrand(RichLocation loc) {
    if (loc.getStrand() != null) {
      if (loc.getStrand().equals(RichLocation.Strand.POSITIVE_STRAND)
          || loc.getStrand().equals(RichLocation.Strand.UNKNOWN_STRAND)) {
        return bzh.plealog.bioinfo.api.data.feature.Feature.PLUS_STRAND;
      } else {
        return bzh.plealog.bioinfo.api.data.feature.Feature.MINUS_STRAND;
      }
    } else {
      return bzh.plealog.bioinfo.api.data.feature.Feature.PLUS_STRAND;
    }
  }

  /**
   * Utility method used by analyseFeatureLocation.
   */
  private static String getDBxRef(CrossRef ref) {
    StringBuffer buf;

    if (ref == null)
      return null;
    buf = new StringBuffer();
    buf.append(ref.getAccession());
    buf.append(".");
    buf.append(ref.getVersion());
    return buf.toString();
  }

  /**
   * Utility method used by analyseFeatureLocation.
   */
  private static FRange getFRange(SimpleRichLocation loc, int from) {
    FPosition fPos1, fPos2;
    Position pos;

    pos = loc.getMinPosition();
    fPos1 = new FPosition(pos.getFuzzyStart(), pos.getFuzzyEnd(),
        pos.getStart() + from, pos.getEnd() + from, getPosType(pos.getType()));
    pos = loc.getMaxPosition();
    fPos2 = new FPosition(pos.getFuzzyStart(), pos.getFuzzyEnd(),
        pos.getStart() + from, pos.getEnd() + from, getPosType(pos.getType()));
    return new FRange(fPos1, fPos2, getDBxRef(loc.getCrossRef()));
  }

  /**
   * Utility method used by analyseFeatureLocation.
   */
  private static FeatureLocation handleCompoundRichLocation(
      CompoundRichLocation loc, int from) {
    FeatureLocation fLoc;
    FRange range;
    Iterator<?> iter;
    Object obj;
    ArrayList<FRange> lst1, lst2;
    int i, size;

    iter = loc.blockIterator();
    fLoc = new FeatureLocation();
    fLoc.setStrand(getStrand(loc));
    if (loc.getTerm().equals(CompoundRichLocation.getJoinTerm()))
      fLoc.setType(FeatureLocation.JOIN);
    else if (loc.getTerm().equals(CompoundRichLocation.getOrderTerm()))
      fLoc.setType(FeatureLocation.ORDER);
    else
      fLoc.setType(FeatureLocation.OTHER);
    lst1 = new ArrayList<>();
    while (iter.hasNext()) {
      obj = iter.next();
      // as for now, we do not allow nested CompoundRichLocation
      if (obj instanceof EmptyRichLocation
          || obj instanceof CompoundRichLocation)
        return null;
      range = getFRange((SimpleRichLocation) obj, from);
      if (range == null)
        return null;
      lst1.add(range);
    }
    // Note: with complement strand, BioJava reverse the order of the elements.
    // We do not want that, since we want to keep the order as provided in the
    // INSDseq file.
    if (fLoc.getStrand() == bzh.plealog.bioinfo.api.data.feature.Feature.MINUS_STRAND) {
      lst2 = new ArrayList<>();
      size = lst1.size() - 1;
      for (i = size; i >= 0; i--) {
        lst2.add(lst1.get(i));
      }
    } else {
      lst2 = lst1;
    }
    size = lst2.size();
    for (i = 0; i < size; i++) {
      fLoc.addRange((FRange) lst2.get(i));
    }
    return fLoc;
  }

  /**
   * Utility method used by analyseFeatureLocation.
   */
  private static FeatureLocation handleSimpleRichLocation(
      SimpleRichLocation loc, int from) {
    FeatureLocation fLoc;
    FRange range;

    range = getFRange(loc, from);
    if (range == null)
      return null;
    fLoc = new FeatureLocation();
    fLoc.setStrand(getStrand(loc));
    fLoc.setType(FeatureLocation.OTHER);
    fLoc.addRange(range);
    return fLoc;
  }

  /**
   * Utility method used by analyseFeatureLocation. This method manages
   * exceptions to the official NCBI/EBI/DDBJ Feature Table manual!
   */
  private static String manageFTException(String loc) {
    String str;

    if (loc.startsWith("bond")) {
      str = FeatureLocation.ORDER + loc.substring(loc.indexOf('('));
    } else {
      str = loc;
    }
    return str;
  }

  /**
   * This method analyzes a BioJava location to return a FeatureLocation object.
   */
  public static FeatureLocation analyseFeatureLocation(String sloc,
      String seqId, int from) {
    String locStr, tidyLocStr;
    FeatureLocation fLoc = null;
    RichLocation loc;

    try {
      locStr = manageFTException(sloc);
      tidyLocStr = locStr.replaceAll("\\s+", "");
      loc = GenbankLocationParser.parseLocation(
          RichObjectFactory.getDefaultNamespace(), seqId, tidyLocStr);
      // as for now, we only handle SimpleRichLocation and CompoundRichLocation
      if ((loc instanceof EmptyRichLocation) == false) {
        if (loc instanceof CompoundRichLocation)
          fLoc = handleCompoundRichLocation((CompoundRichLocation) loc, from);
        else if (loc instanceof SimpleRichLocation)
          fLoc = handleSimpleRichLocation((SimpleRichLocation) loc, from);
      }
    } catch (Exception e) {
      fLoc = null;
    }
    return fLoc;
  }

  /**
   * Returns a date string given a data string. Checks for a format like
   * dd-MMM-yyyy.
   * 
   * @throws ParseException
   *           if date is not formatted like dd-MMM-yyyy.
   */
  private static String getDate(String data) throws ParseException {
    String date;
    int idx;

    idx = data.indexOf('(');
    if (idx != -1) {
      date = data.substring(0, idx - 1);
      getDateChecker().parse(date);
      return date;
    }
    idx = data.indexOf(',');
    if (idx > 10) {
      date = data.substring(idx - 11, idx);
      getDateChecker().parse(date);
      return date;
    } else {
      return STD_DATE;
    }
  }

  /**
   * Sets the dates given a list of date strings.
   */
  private static void setDates(IBankSequenceInfo si, List<?> data)
      throws Exception {
    String value; // cDate, uDate, value;
    long v, min = Long.MAX_VALUE, max = 0;
    int i, size;

    size = data.size();
    for (i = 0; i < size; i++) {
      value = getDate(data.get(i).toString());
      v = getDateChecker().parse(value).getTime();
      min = Math.min(v, min);
      max = Math.max(v, max);
    }
    // cDate = DATE_CHECKER.format(new Date(min));
    // uDate = DATE_CHECKER.format(new Date(max));
    si.setCreationDate(Integer
        .valueOf(getDateFormatter().format(new Date(min))));
    si.setUpdateDate(Integer.valueOf(getDateFormatter().format(new Date(max))));
  }

  /**
   * Sets dates given a data object.
   */
  private static void setDate(IBankSequenceInfo si, Object data)
      throws Exception {
    String value;
    long v;
    int d;

    if (data instanceof List) {
      setDates(si, (List<?>) data);
    } else {
      value = getDate(data.toString());
      v = getDateChecker().parse(value).getTime();
      d = Integer.valueOf(getDateFormatter().format(new Date(v)));
      si.setCreationDate(d);
      si.setUpdateDate(d);
    }
  }

  /**
   * Returns the organism given a data string.
   */
  private static String getOrganism(String data) {
    String org;
    int idx;

    idx = data.indexOf('.');
    if (idx >= 0) {
      org = data.substring(0, idx);
    } else {
      org = data;
    }

    return org;
  }

  private static String getPatentId(String dataLine, String key) {
    StringBuffer buf;
    char ch;
    boolean codeOk;
    int idx, i, size, n;

    // from entry A10086 (Genbank) :
    // JOURNAL Patent: EP 0325066-A 1 26-JUL-1989;
    // from entry DL233279 (EMBL) :
    // RL Patent number JP2003199446-A/5, 15-JUL-2003.
    idx = dataLine.indexOf(key);
    if (idx == -1) {
      return null;
    }
    buf = new StringBuffer("pat:");
    size = dataLine.length();
    i = idx + key.length();
    n = 0;
    // skip some space chars
    while (i < size) {
      if (dataLine.charAt(i) != ' ')
        break;
      i++;
    }
    while (i < size) {
      ch = dataLine.charAt(i);
      i++;
      n++;
      codeOk = Character.isLetterOrDigit(ch);
      // example : with EP 0325066-A,
      // we have to retrieve EP0325066
      if (!codeOk && n > 4)
        break;
      if (codeOk)
        buf.append(ch);
    }
    return buf.toString();
  }

  private static String getGenericPatent(Object data, String key) {
    String str = null;

    if (data == null)
      return null;

    if (data instanceof List) {
      List<?> lst = (List<?>) data;
      int i, size;

      size = lst.size();
      for (i = 0; i < size; i++) {
        str = getPatentId(lst.get(i).toString(), key);
        if (str != null)
          break;
      }
    } else {
      str = getPatentId(data.toString(), key);
    }
    return str;
  }

  private static String getEMBLPatent(Object data, String key) {
    if (data instanceof ReferenceAnnotation) {
      return getGenericPatent(((ReferenceAnnotation) data).getProperty("RL"),
          key);
    } else if (data instanceof List) {
      List<?> lst = (List<?>) data;
      String str;
      int i, size;

      size = lst.size();
      for (i = 0; i < size; i++) {
        if (!(lst.get(i) instanceof ReferenceAnnotation)) {
          continue;
        }
        str = getGenericPatent(
            ((ReferenceAnnotation) lst.get(i)).getProperty("RL"), key);
        if (str != null)
          return str;
      }
    }
    return null;
  }

  private static String getGBPatent(Object data) {
    return getGenericPatent(data, GB_PATENT_DATA_KEY);
  }

  private static String getStringData(Object data) {
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
   * Returns the taxonomy given a data string.
   */
  private static String getTaxonomy(String str) {
    StringBuffer buf;
    StringTokenizer tokenizer;
    int i;

    // following while loop is for Genbank Taxonomy: since species is provided
    // ahead taxonomy, one has to remove that. To do so, locate the first ; of
    // the taxonomy data line, then go back to reach the first space char.
    i = str.indexOf(';');
    while (i >= 0) {
      if (str.charAt(i) == ' ') {
        str = str.substring(i + 1);
        break;
      }
      i--;
    }
    // BioJava works as follows:
    // for a single line tax data, a single string is returned;
    // for a multi-line tax data, an array of strings is returned: the
    // following code will start to remove [ ], if any, contained in the
    // string representation of an array.
    i = str.indexOf('[');
    if (i >= 0) {
      str = str.substring(i + 1);
    }
    i = str.indexOf(']');
    if (i >= 0) {
      str = str.substring(0, i);
    }
    buf = new StringBuffer();
    tokenizer = new StringTokenizer(str, ";,.");
    while (tokenizer.hasMoreTokens()) {
      buf.append(tokenizer.nextToken().trim());
      if (tokenizer.hasMoreTokens()) {
        buf.append(";");
      }
    }
    return buf.toString();
  }

  /**
   * Returns a SequenceInfo object from a Genbank sequence.
   */
  private static IBankSequenceInfo returnGBSeqInfo(Sequence seq, boolean isProt) {
    IBankSequenceInfo si;
    String value;
    Annotation annotItem;
    long v;
    int d;

    si = new IBankSequenceInfo();
    annotItem = seq.getAnnotation();
    // ID
    si.setId(seq.getName());
    // description
    try {
      value = getStringData(annotItem.getProperty("DEFINITION"));
    } catch (Exception e) {
      value = NOT_SPECIFIED;
    }
    si.setDescription(value);
    // molecular type
    try {
      value = annotItem.getProperty("TYPE").toString();
    } catch (Exception e) {
      if (isProt)
        value = PSequence.PROTEIC_TYPE_STR;
      else
        value = PSequence.NUCLEIC_TYPE_STR;
    }
    si.setMoltype(value);
    // topology
    try {
      value = annotItem.getProperty("CIRCULAR").toString();
    } catch (Exception e) {
      value = NOT_SPECIFIED;
    }
    si.setTopology(value);
    // division
    try {
      value = annotItem.getProperty("DIVISION").toString();
    } catch (Exception e) {
      value = NOT_SPECIFIED;
    }
    si.setDivision(value);
    // dates
    try {
      value = annotItem.getProperty("MDAT").toString();
      // value = getDate(value);
      v = getDateChecker().parse(value).getTime();
      d = Integer.valueOf(getDateFormatter().format(new Date(v)));
    } catch (Exception e) {
      d = STD_DATE_INT;
    }
    si.setCreationDate(d);
    si.setUpdateDate(d);
    // organism
    try {
      value = annotItem.getProperty("SOURCE").toString();
    } catch (Exception e) {
      value = NOT_SPECIFIED;
    }
    si.setOrganism(value);
    // taxonomy
    try {
      value = getTaxonomy(annotItem.getProperty("ORGANISM").toString());
    } catch (Exception e) {
      value = NOT_SPECIFIED;
    }
    si.setTaxonomy(value);

    // sequence size
    try {
      d = Integer.valueOf(annotItem.getProperty("SIZE").toString());
    } catch (Exception e) {
      d = 0;
    }
    si.setSequenceSize(d);
    return si;
  }

  /*
   * private static void analyseFeatLocation(Location loc, Point p){ Iterator
   * iter; Location l;
   * 
   * iter = loc.blockIterator(); p.x=Integer.MAX_VALUE; p.y=Integer.MIN_VALUE;
   * while(iter.hasNext()){ l = (Location) iter.next(); if (l.getMin()<p.x)
   * p.x=l.getMin(); if (l.getMax()>p.y) p.y=l.getMax(); } return; }
   */
  /**
   * Reads a feature table from a Sequence within a specific range.
   * 
   * @param seq
   *          the sequence from which to retrieve the features.
   * @param start
   *          sequence coordinate (one-based value) defining a range. If equals
   *          to zero, the value is ignored and the entire feature table will be
   *          returned.
   * @param stop
   *          sequence coordinate (one-based value) defining a range. If equals
   *          to zero, the value is ignored and the entire feature table will be
   *          returned.
   * @param remap
   *          if true then all original sequence coordinates will be transformed
   *          by removing &apos;start-1&apos;.
   * 
   * @return a feature table.
   */
  private static FeatureTable returnGBFeatureTable(Sequence seq, String id,
      int start, int stop, boolean remap) {
    FeatureTable ft;
    bzh.plealog.bioinfo.api.data.feature.Feature kFeat;
    StrandedFeature feat;
    Annotation annotItem;
    Iterator<?> iter, qIter;
    Object obj, obj2;
    String key, strLoc, patData;
    MyGenbankFileFormer gff;
    FeatureLocation fLoc;
    int adjust, a, b;

    // special case for Patent DB
    annotItem = seq.getAnnotation();
    try {// Genbank: look for journals
      obj = annotItem.getProperty("JOURNAL");
      patData = getGBPatent(obj);
    } catch (NoSuchElementException e) {
      patData = null;
    }
    if (patData == null) {
      // EMBL: a nightmare!
      try {
        // look for a reference annotation
        obj = annotItem.getProperty(ReferenceAnnotation.class);
        patData = getEMBLPatent(obj, EMBL_PATENT_DATA_KEY);
      } catch (NoSuchElementException e) {
        patData = null;
      }
      // otherwise look for CC part where there are other key value/pairs!
      if (patData == null) {
        try {
          obj = annotItem.getProperty("CC");
          patData = getEMBLPatent(obj, EMBL_PATENT_DATA_KEY2);
        } catch (NoSuchElementException e) {
          patData = null;
        }
      }
    }

    // other features
    gff = new MyGenbankFileFormer();
    ft = new IFeatureTable();
    ft.setDate(getDateFormatter().format(new Date()));
    iter = seq.features();
    a = start;
    b = stop;
    if (remap && start != 0)
      adjust = start - 1;
    else
      adjust = 0;
    start -= adjust;
    stop -= adjust;
    while (iter.hasNext()) {
      obj = iter.next();
      if ((obj instanceof StrandedFeature) == false)
        continue;
      feat = (StrandedFeature) obj;

      strLoc = gff.formatLocation(feat);
      fLoc = analyseFeatureLocation(strLoc, id != null ? id : "seqId", -adjust);
      if (a != 0 && b != 0) {
        fLoc = fLoc.cut(start, stop);
      }
      if (fLoc == null)
        continue;
      try {
        kFeat = ft
            .addFeature(
                feat.getType(),
                fLoc.getStart(),
                fLoc.getEnd(),
                feat.getStrand().getValue() < 0 ? bzh.plealog.bioinfo.api.data.feature.Feature.MINUS_STRAND
                    : bzh.plealog.bioinfo.api.data.feature.Feature.PLUS_STRAND);
        kFeat.setFeatureLocation(fLoc);
        annotItem = feat.getAnnotation();
        qIter = annotItem.keys().iterator();
        kFeat.addQualifier(QUAL_LOCATION_KEY, fLoc.toString());
        while (qIter.hasNext()) {
          key = qIter.next().toString();
          if (!key.equals(QUAL_INTERNAl_DATA)) {
            obj2 = annotItem.getProperty(key);
            if (obj2 instanceof List) {
              for (Object item : (List<?>) obj2) {
                kFeat.addQualifier(key, item.toString());
              }
            } else {
              kFeat.addQualifier(key, obj2.toString());
            }
          }
        }
        if (patData != null && feat.getType().equalsIgnoreCase("source")) {
          kFeat.addQualifier(DB_XREF_KEY, patData);
        }
      } catch (Exception ex) {
      }
    }
    return ft;
  }

  private static void analyseEmblIDLine(String idLine, IBankSequenceInfo si) {
    StringTokenizer tokenizer;
    String token;
    int idx1, idx2;

    // doc:
    // http://www.ebi.ac.uk/embl/Documentation/User_manual/usrman.html#3_4_1
    // get sequence size
    idx1 = idLine.lastIndexOf(";");
    idx2 = idLine.lastIndexOf("BP.");
    if (idx1 != -1 && idx2 != -1) {
      si.setSequenceSize(Integer.valueOf(idLine.substring(idx1 + 1, idx2 - 1)
          .trim()));
    }
    tokenizer = new StringTokenizer(idLine, ";");
    // seqID
    token = tokenizer.nextToken().trim();
    si.setId(token);
    // skip seq version
    tokenizer.nextToken();
    // topology
    token = tokenizer.nextToken().trim();
    si.setDivision(token);
    // molecular type
    token = tokenizer.nextToken().trim();
    si.setMoltype(token);
    // skip data class
    tokenizer.nextToken();
    // divsion
    token = tokenizer.nextToken().trim();
    si.setDivision(token);
  }

  /**
   * Returns a SequenceInfo object from a Uniprot sequence.
   */
  private static IBankSequenceInfo returnUPSeqInfo(Sequence seq, boolean isEmbl) {
    IBankSequenceInfo si;
    String value;
    Annotation annotItem;

    si = new IBankSequenceInfo();
    annotItem = seq.getAnnotation();

    if (!isEmbl) {// UniProt
      si.setId(seq.getName());
      si.setMoltype("aa");
      si.setTopology("linear");
      si.setDivision(NOT_SPECIFIED);
      si.setSequenceSize(seq.length());
    } else {// EMBL
      try {
        analyseEmblIDLine(annotItem.getProperty("ID").toString(), si);
      } catch (Exception e) {
        LOGGER.warn("Invalid ID line.");
      }
    }
    // description
    try {
      value = getStringData(annotItem.getProperty("DE"));
    } catch (Exception e) {
      value = NOT_SPECIFIED;
    }
    si.setDescription(value);

    // dates
    try {
      setDate(si, annotItem.getProperty("DT"));
    } catch (Exception e) {
      si.setCreationDate(STD_DATE_INT);
      si.setUpdateDate(STD_DATE_INT);
    }
    // organism
    try {
      value = getOrganism(annotItem.getProperty(
          SwissProtParser.KEYWORD_ORGANISM).toString());
    } catch (Exception e) {
      value = NOT_SPECIFIED;
    }
    si.setOrganism(value);
    // taxonomy
    try {
      value = getTaxonomy(annotItem.getProperty("OC").toString());
    } catch (Exception e) {
      value = NOT_SPECIFIED;
    }
    si.setTaxonomy(value);

    return si;
  }

  private static void prepareQualifier(
      bzh.plealog.bioinfo.api.data.feature.Feature kFeat, String str,
      boolean explode) {
    String dbkey, value;
    int idx;

    if (explode) {
      idx = str.indexOf(';');
      if (idx >= 0) {
        dbkey = str.substring(0, idx).trim();
        value = str.substring(idx + 1).trim();
      } else {
        dbkey = "db";
        value = str.trim();
      }
    } else {
      dbkey = DB_XREF_KEY;
      value = str;
    }
    kFeat.addQualifier(dbkey, value);
  }

  // in Uniprot/Swissprot, the EC number is now located in the DE field
  // see http://www.expasy.ch/sprot/userman.html#DE_line
  private static void retrieveECNumber(String desc,
      bzh.plealog.bioinfo.api.data.feature.Feature kFeat) {
    String ecNum;
    int idx;

    idx = desc.indexOf("EC=");
    if (idx != -1) {
      ecNum = desc.substring(idx + 3);

      // To manage SwissProt files
      // example : EC=3.4.11.18 {ECO:0000255|HAMAP-Rule:MF_03174};
      idx = ecNum.indexOf('{');
      if (idx < 0) {
        idx = ecNum.indexOf(';');
      }
      if (idx > 0) {
        kFeat.addQualifier(DB_XREF_KEY, "EC; " + ecNum.substring(0, idx)
            + "; -.");
        // check for additional EC numbers
        retrieveECNumber(ecNum.substring(idx + 1), kFeat);
      }
    }
  }

  private static void retrieveUPDBXref(FeatureTable ft, Sequence seq,
      int start, int stop, boolean remap) {
    bzh.plealog.bioinfo.api.data.feature.Feature kFeat, protFeat = null;
    Annotation annotItem;
    Object obj;
    List<?> values;
    String str, deLine = null, taxon = null;
    boolean explode;
    int adjust, from, to, i, size, a, b, idx1, idx2;

    a = start;
    b = stop;
    // adjust coordinates if needed
    if (remap)
      adjust = start - 1;
    else
      adjust = 0;
    start -= adjust;
    stop -= adjust;
    if (a == 0 && b == 0) {
      from = 1;
      to = seq.length();
    } else {
      from = start;
      to = stop;
    }
    annotItem = seq.getAnnotation();
    // check for the OX (Taxonomy) and add it to the source as a db_xref
    try {
      obj = annotItem.getProperty(SwissProtParser.KEYWORD_ORGANISM_ID);
      // sample: OX NCBI_TaxID=10090;
      str = obj.toString();
      idx1 = str.indexOf('=');
      idx2 = -1;

      // To manage TrEMBL files
      // example : NCBI_TaxID=570416 {ECO:0000313|EMBL:AHZ52867.1,
      // ECO:0000313|Proteomes:UP000025242};
      for (int idxTest = idx1 + 1; idxTest < str.length(); idxTest++) {
        if (!Character.isDigit(str.charAt(idxTest))) {
          idx2 = idxTest;
          break;
        }
      }

      if (idx1 != -1 && idx2 != -1) {
        taxon = str.substring(idx1 + 1, idx2);
      }
    } catch (Exception ex) {
    }
    // check for the organism
    try {
      obj = annotItem.getProperty(SwissProtParser.KEYWORD_ORGANISM);
      // creates feture of type db_xref
      kFeat = ft.addFeature("source", from, to,
          bzh.plealog.bioinfo.api.data.feature.Feature.PLUS_STRAND);
      // get full description
      if (obj instanceof List) {
        values = (List<?>) obj;
        size = values.size();
        str = "";
        for (i = 0; i < size; i++) {
          str += values.get(i).toString();
        }
      } else {
        str = obj.toString();
      }
      kFeat.addQualifier("organism", str);
      // set taxon if any
      if (taxon != null) {
        kFeat.addQualifier(DB_XREF_KEY, TAXON_KEY + ":" + taxon);
      }
    } catch (Exception ex) {
    } // check for the name
    try {
      obj = annotItem.getProperty("DE");
      // creates feature of type protein
      kFeat = ft.addFeature("protein", from, to,
          bzh.plealog.bioinfo.api.data.feature.Feature.PLUS_STRAND);
      // get full description
      if (obj instanceof List) {
        values = (List<?>) obj;
        size = values.size();
        str = "";
        for (i = 0; i < size; i++) {
          str += values.get(i).toString();
        }
      } else {
        str = obj.toString();
      }
      deLine = str;
      kFeat.addQualifier("product", str);
      protFeat = kFeat;
    } catch (Exception ex) {
    }
    // check for the DR property
    try {
      obj = annotItem.getProperty("DR");
      // creates feature of type db_xref if no protein
      // previously found
      if (protFeat != null) {
        kFeat = protFeat;
        explode = false;
      } else {
        kFeat = ft.addFeature(DB_XREF_KEY, from, to,
            bzh.plealog.bioinfo.api.data.feature.Feature.PLUS_STRAND);
        explode = true;
      }
      // get db_xrefs
      if (obj instanceof List) {
        values = (List<?>) obj;
        size = values.size();
        for (i = 0; i < size; i++) {
          str = values.get(i).toString();
          prepareQualifier(kFeat, str, explode);
        }
      } else {
        prepareQualifier(kFeat, obj.toString(), explode);
      }
      // adds EC Numbers if any
      if (deLine != null)
        retrieveECNumber(deLine, kFeat);
    } catch (Exception ex) {
    }
  }

  public static void retrieveUPFeatures(FeatureTable ft, Sequence seq,
      String id, int start, int stop, boolean remap) {
    bzh.plealog.bioinfo.api.data.feature.Feature kFeat;
    MyGenbankFileFormer gff;
    FeatureLocation fLoc;
    Feature feat;
    Annotation annotItem;
    Iterator<?> iter;
    String strLoc;
    int adjust, a, b;
    String featureValue = null;
    String featureKey = null;
    int indexSeparator = -1;

    a = start;
    b = stop;
    if (remap && start != 0)
      adjust = start - 1;
    else
      adjust = 0;
    start -= adjust;
    stop -= adjust;
    gff = new MyGenbankFileFormer();
    iter = seq.features();
    while (iter.hasNext()) {
      feat = (Feature) iter.next();
      strLoc = gff.formatLocation(feat);
      fLoc = analyseFeatureLocation(strLoc, id != null ? id : "seqId", -adjust);
      if (fLoc == null)
        continue;
      if (a != 0 && b != 0) {
        fLoc = fLoc.cut(start, stop);
      }
      if (fLoc == null)
        continue;
      try {
        kFeat = ft.addFeature(feat.getType().toLowerCase(), fLoc.getStart(),
            fLoc.getEnd(),
            bzh.plealog.bioinfo.api.data.feature.Feature.PLUS_STRAND);
        kFeat.setFeatureLocation(fLoc);
        annotItem = feat.getAnnotation();
        kFeat.addQualifier(QUAL_LOCATION_KEY, fLoc.toString());
        featureKey = kFeat.getKey();
        featureValue = annotItem.getProperty("swissprot.featureattribute")
            .toString().trim();
        if (featureValue.startsWith(DBUtils.DB_XREF_KEY)) {
          indexSeparator = featureValue.indexOf('=');
          if (indexSeparator > 0) {
            featureKey = DBUtils.DB_XREF_KEY;
            featureValue = featureValue.substring(indexSeparator + 1);
          }
        }
        kFeat.addQualifier(featureKey, featureValue);
      } catch (Exception ex) {
      }
    }

  }

  private static String cutSequence(String seq, int from, int to) {
    if (seq.length() == 0)// contig, assembly have no sequence
      return seq;
    if (from < 0 || to < 0) {
      return seq;
    } else {
      if (to >= seq.length())
        to = seq.length() - 1;
      return seq.substring(from, to + 1);
    }
  }

  /**
   * Reads a feature table from a Sequence within a specific range.
   * 
   * @param seq
   *          the sequence from which to retrieve the features.
   * @param start
   *          sequence coordinate (one-based value) defining a range. If equals
   *          to zero, the value is ignored and the entire feature table will be
   *          returned.
   * @param stop
   *          sequence coordinate (one-based value) defining a range. If equals
   *          to zero, the value is ignored and the entire feature table will be
   *          returned.
   * @param remap
   *          if true then all original sequence coordinates will be transformed
   *          by removing &apos;start-1&apos;.
   * 
   * @return a feature table.
   */
  private static FeatureTable returnUPFeatureTable(Sequence seq, String id,
      int start, int stop, boolean remap) {

    FeatureTable ft;

    ft = new IFeatureTable();
    ft.setDate(getDateFormatter().format(new Date()));
    retrieveUPDBXref(ft, seq, start, stop, remap);
    retrieveUPFeatures(ft, seq, id, start, stop, remap);

    return ft;
  }

  private static PSequence readGenbankLikeEntry(BufferedReader reader,
      int start, int stop, boolean remap, boolean isProt) throws Exception {
    Sequence seq;
    IBankSequenceInfo si;
    FeatureTable ft;

    // read the Genbank/Genpept File
    if (isProt)
      seq = SeqIOTools.readGenpept(reader).nextSequence();
    else
      seq = SeqIOTools.readGenbank(reader).nextSequence();
    // get sequence info
    si = returnGBSeqInfo(seq, isProt);
    // get feature table
    ft = returnGBFeatureTable(seq, si.getId(), start, stop, remap);
    return new PSequence(si, ft, cutSequence(seq.seqString(), start - 1,
        stop - 1), isProt ? PSequence.PROTEIC_TYPE : PSequence.NUCLEIC_TYPE);
  }

  private static PSequence readEmblEntry(BufferedReader reader, int start,
      int stop, boolean remap) throws Exception {
    Sequence seq;
    IBankSequenceInfo si;
    FeatureTable ft;

    // read the Embl File
    seq = SeqIOTools.readEmbl(reader).nextSequence();
    // get sequence info
    si = returnUPSeqInfo(seq, true);
    // get feature table
    ft = returnGBFeatureTable(seq, si.getId(), start, stop, remap);
    return new PSequence(si, ft, cutSequence(seq.seqString(), start - 1,
        stop - 1), PSequence.NUCLEIC_TYPE);
  }

  private static PSequence loadFastaEntry(File file, int fType, int start,
      int stop, boolean remap) throws Exception {
    Sequence seq = null;
    IBankSequenceInfo si;
    PSequence kls;
    boolean isProt = false;
    BufferedReader reader = null;
    String date;
    int iDate;

    try {
      reader = new BufferedReader(new FileReader(file));
      switch (fType) {
        case SeqIOUtils.FASTADNA:
          seq = SeqIOTools.readFastaDNA(reader).nextSequence();
          break;
        case SeqIOUtils.FASTARNA:
          seq = SeqIOTools.readFastaRNA(reader).nextSequence();
          break;
        case SeqIOUtils.FASTAPROT:
          seq = SeqIOTools.readFastaProtein(reader).nextSequence();
          isProt = true;
          break;
      }
    } catch (Exception e1) {
      LOGGER.warn("Unable to open fasta file: " + file.getAbsolutePath() + ": "
          + e1.getMessage());
      return null;
    } finally {
      IOUtils.closeQuietly(reader);
    }
    date = getDateFormatter().format(new Date());
    iDate = Integer.valueOf(date);
    si = new IBankSequenceInfo();
    si.setId(SeqIOUtils.getIdentifier(seq, fType, true));
    si.setDescription(SeqIOUtils.getDescription(seq, fType, false));
    si.setMoltype(fType == SeqIOUtils.FASTAPROT ? PSequence.PROTEIC_TYPE_STR
        : PSequence.NUCLEIC_TYPE_STR);
    // following added to avoid problem when dumping XML data
    si.setTopology("?");
    si.setDivision("?");
    si.setUpdateDate(iDate);
    si.setCreationDate(iDate);
    si.setOrganism("?");
    si.setTaxonomy("?");
    si.setSequenceSize(seq.length());
    kls = new PSequence(cutSequence(seq.seqString(), start - 1, stop - 1),
        isProt ? PSequence.PROTEIC_TYPE : PSequence.NUCLEIC_TYPE);
    kls.setFeatTable(new IFeatureTable());// added to avoid problem when dumping
                                          // XML Data
    kls.setSeqInfo(si);
    return kls;
  }

  public static PSequence readUniProtEntry(BufferedReader reader, int start,
      int stop, boolean remap) throws Exception {
    Sequence seq;
    IBankSequenceInfo si;
    FeatureTable ft;

    // read the Uniprot File
    seq = SeqIOTools.readSwissprot(reader).nextSequence();
    // get sequence info
    si = returnUPSeqInfo(seq, false);
    // get feature table
    ft = returnUPFeatureTable(seq, si.getId(), start, stop, remap);
    return new PSequence(si, ft, cutSequence(seq.seqString(), start - 1,
        stop - 1), PSequence.PROTEIC_TYPE);
  }

  /**
   * Reads a Uniprot entry. Parameters start and stop are sequence coordinates
   * (one- based values) defining the range from where to retrieve the features.
   * When both start and stop equal zero, the entire feature table will be
   * retrieved.
   * 
   * @param file
   *          the absolute path identifying the Uniprot file.
   * @param start
   *          start coordinate. One-based value.
   * @param stop
   *          stop coordinate. One-based value.
   * @param remap
   *          if true then all original sequence coordinates will be transformed
   *          by removing &apos;start-1&apos;.
   */
  public static PSequence readUniProtEntry(File file, int start, int stop,
      boolean remap) {
    PSequence seq;
    BufferedReader reader = null;

    try {
      reader = new BufferedReader(new FileReader(file));
      seq = readUniProtEntry(reader, start, stop, remap);
    } catch (Exception ex) {
      LOGGER.warn("unable to read UniProt file: " + file.getAbsolutePath()
          + ": " + ex);
      seq = null;
    } finally {
      IOUtils.closeQuietly(reader);
    }
    return seq;
  }

  /**
   * Reads a Uniprot entry. Parameters start and stop are sequence coordinates
   * (one- based values) defining the range from where to retrieve the features.
   * When both start and stop equal zero, the entire feature table will be
   * retrieved.
   * 
   * @param data
   *          a string containing a full Uniprot entry.
   * @param start
   *          start coordinate. One-based value.
   * @param stop
   *          stop coordinate. One-based value.
   * @param remap
   *          if true then all original sequence coordinates will be transformed
   *          by removing &apos;start-1&apos;.
   */
  public static PSequence readUniProtEntry(String data, int start, int stop,
      boolean remap) {
    PSequence seq;
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new StringReader(data));
      seq = readUniProtEntry(reader, start, stop, remap);
    } catch (Exception ex) {
      LOGGER.warn("unable to read UniProt data: " + ex);
      seq = null;
    } finally {
      try {
        if (reader != null)
          reader.close();
      } catch (Exception e) {
      }
    }
    return seq;
  }

  /**
   * Reads a Genbank entry. Parameters start and stop are sequence coordinates
   * (one- based values) defining the range from where to retrieve the features.
   * When both start and stop equal zero, the entire feature table will be
   * retrieved.
   * 
   * @param data
   *          a string containing a full Genbank entry.
   * @param start
   *          start coordinate. One-based value.
   * @param stop
   *          stop coordinate. One-based value.
   * @param remap
   *          if true then all original sequence coordinates will be transformed
   *          by removing &apos;start-1&apos;.
   */
  public static PSequence readGenbankEntry(String data, int start, int stop,
      boolean remap) {
    PSequence seq;
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new StringReader(data));
      seq = readGenbankLikeEntry(reader, start, stop, remap, false);
    } catch (Exception ex) {
      LOGGER.warn("unable to read Genbank data: " + ex);
      seq = null;
    } finally {
      try {
        if (reader != null)
          reader.close();
      } catch (Exception e) {
      }
    }
    return seq;
  }

  /**
   * Reads a Genbank entry. Parameters start and stop are sequence coordinates
   * (one- based values) defining the range from where to retrieve the features.
   * When both start and stop equal zero, the entire feature table will be
   * retrieved.
   * 
   * @param file
   *          the absolute path identifying the Genbank file.
   * @param start
   *          start coordinate. One-based value.
   * @param stop
   *          stop coordinate. One-based value.
   * @param remap
   *          if true then all original sequence coordinates will be transformed
   *          by removing &apos;start-1&apos;.
   */
  public static PSequence readGenbankEntry(File file, int start, int stop,
      boolean remap) {
    PSequence seq;
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader(file));
      seq = readGenbankLikeEntry(reader, start, stop, remap, false);
    } catch (Exception ex) {
      LOGGER.warn("unable to read Genbank file: " + file.getAbsolutePath()
          + ": " + ex);
      seq = null;
    } finally {
      IOUtils.closeQuietly(reader);
    }
    return seq;
  }

  /**
   * Reads a Genpept entry. Parameters start and stop are sequence coordinates
   * (one- based values) defining the range from where to retrieve the features.
   * When both start and stop equal zero, the entire feature table will be
   * retrieved.
   * 
   * @param data
   *          a string containing a full Genpept entry.
   * @param start
   *          start coordinate. One-based value.
   * @param stop
   *          stop coordinate. One-based value.
   * @param remap
   *          if true then all original sequence coordinates will be transformed
   *          by removing &apos;start-1&apos;.
   */
  public static PSequence readGenpeptEntry(String data, int start, int stop,
      boolean remap) {
    PSequence seq;
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new StringReader(data));
      seq = readGenbankLikeEntry(reader, start, stop, remap, true);
    } catch (Exception ex) {
      LOGGER.warn("unable to read Genbank data: " + ex);
      seq = null;
    } finally {
      try {
        if (reader != null)
          reader.close();
      } catch (Exception e) {
      }
    }
    return seq;
  }

  /**
   * Reads a Genpept entry. Parameters start and stop are sequence coordinates
   * (one- based values) defining the range from where to retrieve the features.
   * When both start and stop equal zero, the entire feature table will be
   * retrieved.
   * 
   * @param file
   *          the absolute path identifying the Genpept file.
   * @param start
   *          start coordinate. One-based value.
   * @param stop
   *          stop coordinate. One-based value.
   * @param remap
   *          if true then all original sequence coordinates will be transformed
   *          by removing &apos;start-1&apos;.
   */
  public static PSequence readGenpeptEntry(File file, int start, int stop,
      boolean remap) {
    PSequence seq;
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader(file));
      seq = readGenbankLikeEntry(reader, start, stop, remap, true);
    } catch (Exception ex) {
      LOGGER.warn("unable to read Genbank file: " + file.getAbsolutePath()
          + ": " + ex);
      seq = null;
    } finally {
      IOUtils.closeQuietly(reader);
    }
    return seq;
  }

  /**
   * Reads a Embl entry. Parameters start and stop are sequence coordinates
   * (one- based values) defining the range from where to retrieve the features.
   * When both start and stop equal zero, the entire feature table will be
   * retrieved.
   * 
   * @param data
   *          a string containing a full Embl entry.
   * @param start
   *          start coordinate. One-based value.
   * @param stop
   *          stop coordinate. One-based value.
   * @param remap
   *          if true then all original sequence coordinates will be transformed
   *          by removing &apos;start-1&apos;.
   */
  public static PSequence readEmblEntry(String data, int start, int stop,
      boolean remap) {
    PSequence seq;
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new StringReader(data));
      seq = readEmblEntry(reader, start, stop, remap);
    } catch (Exception ex) {
      LOGGER.warn("unable to read Embl data: " + ex);
      seq = null;
    } finally {
      try {
        if (reader != null)
          reader.close();
      } catch (Exception e) {
      }
    }
    return seq;
  }

  /**
   * Reads an Embl entry. Parameters start and stop are sequence coordinates
   * (one- based values) defining the range from where to retrieve the features.
   * When both start and stop equal zero, the entire feature table will be
   * retrieved.
   * 
   * @param file
   *          the absolute path identifying the Embl file.
   * @param start
   *          start coordinate. One-based value.
   * @param stop
   *          stop coordinate. One-based value.
   * @param remap
   *          if true then all original sequence coordinates will be transformed
   *          by removing &apos;start-1&apos;.
   */
  public static PSequence readEmblEntry(File file, int start, int stop,
      boolean remap) {
    PSequence seq;
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new FileReader(file));
      seq = readEmblEntry(reader, start, stop, remap);
    } catch (Exception ex) {
      LOGGER.warn("unable to read Embl file: " + file.getAbsolutePath() + ": "
          + ex);
      seq = null;
    } finally {
      IOUtils.closeQuietly(reader);
    }
    return seq;
  }

  /**
   * Reads a Fasta entry. Parameters start and stop are sequence coordinates
   * (one- based values) defining the range from where to retrieve the features.
   * When both start and stop equal zero, the entire feature table will be
   * retrieved. If the fasta file contains one sequence, only the first one is
   * returned.
   * 
   * @param file
   *          the absolute path identifying the Fasta file.
   * @param start
   *          start coordinate. One-based value.
   * @param stop
   *          stop coordinate. One-based value.
   * @param remap
   *          if true then all original sequence coordinates will be transformed
   *          by removing &apos;start-1&apos;.
   */
  public static PSequence readFastaEntry(File file, int start, int stop,
      boolean remap) {
    PSequence seq;
    int fType;
    try {
      fType = SeqIOUtils.guessFileFormat(file.getAbsolutePath());
      switch (fType) {
        case SeqIOUtils.FASTADNA:
          seq = loadFastaEntry(file, fType, start, stop, remap);
          break;
        case SeqIOUtils.FASTARNA:
          seq = loadFastaEntry(file, fType, start, stop, remap);
          break;
        case SeqIOUtils.FASTAPROT:
          seq = loadFastaEntry(file, fType, start, stop, remap);
          break;
        default:
          throw new Exception("not a Fasta file");
      }
    } catch (Exception ex) {
      LOGGER.warn("unable to read Fasta file: " + file.getAbsolutePath() + ": "
          + ex);
      seq = null;
    }
    return seq;
  }

  /**
   * This class is used to access BioJava protected code.
   */
  private static class MyGenbankFileFormer extends GenbankFileFormer {
    public MyGenbankFileFormer() {
      super();
    }
  }
  // BioJava 1.5 code... not used yet since it does not read tax data from the
  // Entry!!!
  /*
   * public static void readSwissProtEntry(String data){
   * 
   * BufferedReader br = null;
   * 
   * try { //create a buffered reader to read the sequence file specified by
   * args[0] br = new BufferedReader(new StringReader(data)); //a namespace to
   * override that in the file Namespace ns =
   * RichObjectFactory.getDefaultNamespace(); //we are reading DNA sequences
   * RichSequenceIterator seqs = RichSequence.IOTools.readUniProt(br,ns); while
   * (seqs.hasNext()) { RichSequence rs = seqs.nextRichSequence(); Iterator
   * iter; iter = rs.getFeatureSet().iterator(); while(iter.hasNext()){ Feature
   * feat; feat = (Feature) iter.next();
   * //System.out.println(feat.getType()+": "
   * +feat.getAnnotation().getProperty("feature_desc"));
   * System.out.println(feat.getType()+": "+feat.getAnnotation().keys()); } //
   * write it in EMBL format to standard out
   * RichSequence.IOTools.writeINSDseq(System.out, rs, ns); } br.close(); }
   * catch (Exception e) { // TODO Auto-generated catch block
   * e.printStackTrace(); }
   * 
   * }
   */
}
