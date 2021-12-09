/* Copyright (C) 2007-2021 Patrick G. Durand - Ludovic Antin
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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.indexer.DBEntry;
import bzh.plealog.dbmirror.indexer.LuceneUtils;
import bzh.plealog.dbmirror.lucenedico.DicoTerm;
import bzh.plealog.dbmirror.lucenedico.DicoTermQuerySystem;
import bzh.plealog.dbmirror.lucenedico.Dicos;
import bzh.plealog.dbmirror.lucenedico.go.GeneOntologyTerm;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.conf.DBMirrorConfig;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptor;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptorUtils;
import bzh.plealog.dbmirror.util.descriptor.IdxDescriptor;
import bzh.plealog.dbmirror.util.runner.FormatDBRunner;

/**
 * This class is responsible for querying the database mirror to return an
 * entry. This class is not thread safe. Use several Query classes in the case
 * of a multi-threaded system.
 * 
 * @author Patrick G. Durand
 * @author Ludovic Antin
 */
public class PQueryMirrorBase {

  protected DBMirrorConfig         _dbMirrorConfig;
  protected List<IdxDescriptor>    _descriptors;
  private String                   _errMsg;

  protected static final Log       LOGGER    = LogFactory
                                                 .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                     + ".PQueryMirrorBase"); ;

  // available keys to use for the HTTP/GET line
  public static final String       DBKEY     = "database";
  public static final String       IDKEY     = "id";
  public static final String       STARTKEY  = "start";
  public static final String       STOPKEY   = "stop";
  public static final String       ADJUSTKEY = "adjust";
  public static final String       FOMRATKEY = "format";

  private static final String UNK = "unknown";
  
  protected static HashSet<String> KEYS      = new HashSet<String>();

  static {
    KEYS.add(DBKEY);
    KEYS.add(IDKEY);
    KEYS.add(STARTKEY);
    KEYS.add(STOPKEY);
    KEYS.add(ADJUSTKEY);
    KEYS.add(FOMRATKEY);
  }

  public PQueryMirrorBase() {
  }

  /**
   * Prepares a key/value map by retrieving data from a string formatted like an
   * Http Get method.
   * 
   * @param getVar
   *          a string formatted as key=value&key=value...
   */
  private Map<String, String> getValuesFromString(String getVar) {
    Hashtable<String, String> ht;
    StringTokenizer tokenizer;
    String token, key, value;
    int idx;

    LOGGER.debug("Query is: " + getVar);
    tokenizer = new StringTokenizer(getVar, "&");
    ht = new Hashtable<String, String>();
    while (tokenizer.hasMoreTokens()) {
      token = tokenizer.nextToken();
      idx = token.indexOf('=');
      if (idx < 0) {
        LOGGER.debug("key/value pair badly formatted");
        return null;
      }
      key = token.substring(0, idx);
      if (!KEYS.contains(key)) {
        LOGGER.debug("unknown key: " + key);
        return null;
      }
      value = token.substring(idx + 1);
      try {
        ht.put(key, URLDecoder.decode(value, "UTF-8").trim());
      } catch (UnsupportedEncodingException e) {
        LOGGER.debug("unable to Decode in UTF-8: " + value);
      }
    }
    return ht;

  }

  /**
   * Prepares a key/value map by retrieving data from the environment variable
   * HTTP_METHOD.
   */
  public Map<String, String> getValuesFromHttpMethod() {
    String method, getVar;
    int maxch, nred;

    method = System.getProperty("HTTP_METHOD");
    if ("POST".equals(method)) {
      try {
        InputStreamReader r = new InputStreamReader(System.in);
        maxch = Integer.parseInt(System.getProperty("CONTENT_LEN"));
        nred = 0;
        char[] bf = new char[maxch];
        while (nred < maxch) {
          nred += r.read(bf, nred, maxch - nred);
        }
        getVar = new String(bf);
      } catch (Exception e) {
        LOGGER.debug("unable to get values from HTTP/POST method: " + e);
        return null;
      }
    } else if ("GET".equals(method)) {
      getVar = System.getProperty("QUERY_STRING");
    } else {
      getVar = null;
    }
    if (getVar == null || getVar.length() == 0) {
      LOGGER.debug("unable to get values from HTTP-GET/POST method.");
      return null;
    }
    return getValuesFromString(getVar);
  }

  private String loadEntry(File fEntry) {
    StringBuffer buf;
    BufferedReader reader = null;
    String line;
    buf = new StringBuffer();

    try {
      reader = new BufferedReader(new InputStreamReader(new FileInputStream(
          fEntry), "UTF-8"));
      while ((line = reader.readLine()) != null) {
        buf.append(line);
        buf.append("\n");
      }
      line = buf.toString();
    } catch (Exception ex) {
      LOGGER.debug("unable to read file: " + fEntry);
      line = null;
    } finally {
      IOUtils.closeQuietly(reader);
    }
    return line;
  }

  private void dumpDicoData(Dicos dico, StringTokenizer tokenizer, DicoTermQuerySystem dicoConnector, 
      Writer w) throws IOException {
    DicoTerm        term;
    String           id, taxPath;
    
    if (dico==null) {
      return;
    }
    while (tokenizer.hasMoreTokens()) {
      id = tokenizer.nextToken().trim();
      term = dicoConnector.getTerm(dico, id);
      //not found: try to search by organism name
      if (term==null) {
        if(dico.equals(Dicos.NCBI_TAXONOMY)) {
          List<DicoTerm> terms = dicoConnector.getApprochingTerms(id, 1);
          if (terms.size()!=0) {
            id = terms.get(0).getId();
            term = dicoConnector.getTerm(Dicos.NCBI_TAXONOMY, id);
          }
        }
        else if(dico.equals(Dicos.GENE_ONTOLOGY)) {
          //get GO term given a GO-ID; expect standard id for GO, i.e GO:0000002
          //but this code also accepts GO id without GO: prefix, so add it
          id = Dicos.GENE_ONTOLOGY.xrefId+":"+id;
          term = dicoConnector.getTerm(Dicos.GENE_ONTOLOGY, id);
        }
      }
      w.write(id);
      w.write("\t");
      if (term != null) {
        if(dico.equals(Dicos.NCBI_TAXONOMY)) {
          taxPath = dicoConnector.getTaxPath(id, true, true, true);
          w.write(taxPath!=null ? taxPath : UNK);
        }
        else if(dico.equals(Dicos.GENE_ONTOLOGY)) {
          GeneOntologyTerm goTerm = (GeneOntologyTerm) term.get_dataObject();
          w.write(goTerm.get_node_ontology_code());
          w.write(":");
          w.write(goTerm.get_node_name());
        }
        else{
          w.write(term.getDataField().toString());
        }
      }
      else {
        w.write(UNK);
      }
      w.write("\n");
    }
  }
  
	private void handleDicoIds(Writer w, String dbKey, String mirrorPath, List<String> idxNames, String ids) {
		// connect to Dictionary Lucene Indexes
		DicoTermQuerySystem dicoConnector;
		dicoConnector = DicoTermQuerySystem.getDicoTermQuerySystem(_dbMirrorConfig);
		
		// if "dico", we expect having dico type provided int he form: "dico:type"
		// where type is one of Dicos.XXX.xrefId string
		int i = dbKey.indexOf(':');
    if (i!=-1) {
      // get dico type to query
      dbKey = dbKey.substring(i+1);
    }
    else {
      // default will be NCBI taxonomy
      dbKey = Dicos.NCBI_TAXONOMY.xrefId;
    }
		try {
		  dumpDicoData(Dicos.findByXRefId(dbKey), new StringTokenizer(ids, ","), dicoConnector, w);
		} catch (IOException e) {
			LOGGER.debug("unable to write result: " + e);
		}
	}

  private String findReader(String idxPath) {
    String reader = null;
    String path;

    // idxPath contains the absolute path to a Lucene Index. Use that
    // information
    // to locate the appropriate reader ; indeed an annotated sequence databank
    // is
    // always identified by the absolute path to its Lucence index (check
    // dbmirror.config)
    for (IdxDescriptor desc : _descriptors) {
      if (desc.getCode().startsWith(idxPath))
        return desc.getReader();
    }
    // if not found, it means we have a fasta-based blast databank, that has
    // been indexed.
    // In such a case, we locate the format using the corresponding blast path
    // (xxxM.pal or
    // xxxM.nal) since it is that information we have in the dbmirror.config
    // descriptors
    // (again check dbmirror.config). To avoid searching for both .pal and .nal,
    // we simply
    // remove the file extension.
    path = idxPath.substring(0, idxPath.lastIndexOf('.'));
    for (IdxDescriptor desc : _descriptors) {
      if (desc.getCode().startsWith(path))
        return desc.getReader();
    }
    return reader;
  }

  private PSequence[] handleMultipleID(PFormatter formatter,
      String mirrorPath, List<String> idxNames, String ids,
      String dbName, String dbKey) {
    StringTokenizer tokenizer;
    String id;
    ArrayList<PSequence> results;
    PSequence res;

    results = new ArrayList<PSequence>();
    tokenizer = new StringTokenizer(ids, ",");
    while (tokenizer.hasMoreTokens()) {
      id = tokenizer.nextToken().trim();
      res = handleSingleID(formatter, mirrorPath, idxNames, id,
          dbName, dbKey, 0, 0, false);
      if (res != null)
        results.add(res);
      else
        results.add(new PSequence());
    }
    return results.toArray(new PSequence[0]);
  }

  private void handleMultipleID(PFormatter formatter,
      String mirrorPath, List<String> idxNames, File foIDs, 
      String dbName, String dbKey) {
    try {
      Files.lines(foIDs.toPath())
        .forEach(a -> handleSingleID(formatter, mirrorPath, idxNames, a.trim(),
            dbName, dbKey, 0, 0, false));
    } catch (IOException e) {
      LOGGER.warn("unable to read: " +foIDs+ ": " + e);
    }

  }
  private PSequence handleSingleID(PFormatter formatter,
      String mirrorPath, List<String> idxNames, String id,
      String dbName, String dbKey, int start, int stop, boolean adjust) {
    PSequence seq = null;
    DBEntry entry;
    File dbFile;
    String strEntry, reader;
    boolean bOk = false;

    if (mirrorPath != null)
      entry = LuceneUtils.getEntry(mirrorPath, id);
    else
      entry = LuceneUtils.getEntry(idxNames, id);
    if (entry != null) {
      // read the db entry
      dbFile = DBUtils.readDBEntry(entry.getFName(), entry.getStart(),
          entry.getStop());
      // prepare the output
      if (dbFile == null) {
        formatter.dumpError("Unable to retrieve " + id
            + " from database");
      } else {
        // TXT_FORMAT
        if (PFormatter.FORMAT.HTML_FORMAT.equals(formatter.getFormat())) {
          strEntry = loadEntry(dbFile);
          if (strEntry != null) {
            formatter.dump(strEntry, dbName, id);
            bOk = true;
          } else {
            formatter.dumpError("Unable to retrieve " + id
                + " from database");
          }
        } else if (PFormatter.FORMAT.TXT_FORMAT.equals(formatter.getFormat())) {
          strEntry = loadEntry(dbFile);
          if (strEntry != null) {
            formatter.dump(strEntry, dbName, id);
            bOk = true;
          } else {
            formatter.dumpError("Unable to retrieve " + id
                + " from database");
          }
        } else {
          /*
           * int fileFormat =
           * SeqIOUtils.guessFileFormat(dbFile.getAbsolutePath()); switch
           * (fileFormat){ case SeqIOUtils.GENBANK: seq =
           * DBUtils.readGenbankEntry(dbFile, start, stop, adjust); break; case
           * SeqIOUtils.GENPEPT: seq = DBUtils.readGenpeptEntry(dbFile, start,
           * stop, adjust); break; case SeqIOUtils.EMBL: seq =
           * DBUtils.readEmblEntry(dbFile, start, stop, adjust); break; case
           * SeqIOUtils.SWISSPROT: seq = DBUtils.readUniProtEntry(dbFile, start,
           * stop, adjust); break; default: seq=null; }
           */
          reader = findReader(entry.getIndexPath());
          // INSD_FORMAT and FASTA
          if (reader.equals(DBMirrorConfig.GB_READER)) {
            seq = DBUtils.readGenbankEntry(dbFile, start, stop, adjust);
          } else if (reader.equals(DBMirrorConfig.GP_READER)) {
            seq = DBUtils.readGenpeptEntry(dbFile, start, stop, adjust);
          } else if (reader.equals(DBMirrorConfig.EM_READER)) {
            seq = DBUtils.readEmblEntry(dbFile, start, stop, adjust);
          } else if (reader.equals(DBMirrorConfig.UP_READER)) {
            seq = DBUtils.readUniProtEntry(dbFile, start, stop, adjust);
          } else if (reader.equals(DBMirrorConfig.BLASTP_READER)
              || reader.equals(DBMirrorConfig.BLASTN_READER)) {
            seq = DBUtils.readFastaEntry(dbFile, start, stop, adjust);
          } else {
            seq = null;
          }
          if (seq != null) {
            if (PFormatter.FORMAT.INSD_FORMAT.equals(formatter.getFormat()))
              formatter.dump(seq);
            else
              formatter.dump(seq.getFastaSequence(), dbName, id);
            bOk = true;
          } else {
            _errMsg = "Unable to read sequence data for " + id;
            LOGGER.debug(_errMsg);
            formatter.dumpError(_errMsg);
          }
        }
        if (bOk)
          dbFile.delete();
      }
    } else {
      _errMsg = "entry " + id + " not found in index (" + dbKey + ")";
      LOGGER.debug(_errMsg);
      formatter.dumpError(_errMsg);
    }

    return seq;
  }

  /**
   * Execute the query.
   */
  private PSequence[] executeQuery(PFormatter formatter, Map<String, String> data) {
    PSequence[] result = null;
    PSequence seq;
    String val, dbKey, mirrorPath = null, id, dbName, idxKey, path;
    ArrayList<String> idxNames;
    List<String> idxKeys, idxKeys2;
    Iterator<String> iter;
    File foIDs;
    int start, stop;
    boolean adjust, hasfoIDs;

    // get the db identifier (mandatory)
    dbKey = data.get(DBKEY);
    if (dbKey == null) {
      _errMsg = "database not provided";
      LOGGER.debug(_errMsg);
      formatter.dumpError(_errMsg);
      return result;
    }
    // get the sequence ID (mandatory)
    id = data.get(IDKEY);
    if (id == null) {
      _errMsg = "sequence ID not provided";
      LOGGER.debug(_errMsg);
      formatter.dumpError(_errMsg);
      return result;
    }
    // test used by client to detect this service
    if ("a".equals(dbKey) && "b".equals(id)) {
      return null;
    }
    //id can be a single ID, a list of IDs or a path to a file of IDs
    foIDs = new File(id);
    hasfoIDs = foIDs.exists();
    // given the db key, get the path to the db mirror
    idxKeys = _dbMirrorConfig.getMirrorCodes(dbKey);
    // following line add since Fasta-based databanks (used with Plast) are
    // indexed to be queried by Id
    idxKeys2 = DBDescriptorUtils.getBlastDbKeysList(_dbMirrorConfig,
        DBMirrorConfig.PROTEIC_IDX.equals(dbKey) ? DBDescriptor.TYPE.blastp
            : DBDescriptor.TYPE.blastn);
    if (idxKeys == null && idxKeys2 == null)
      mirrorPath = _dbMirrorConfig.getMirrorPath(dbKey);
    else if (idxKeys != null)
      idxKeys.addAll(idxKeys2);
    else
      idxKeys = idxKeys2;
    if (idxKeys == null && mirrorPath == null) {
      _errMsg = "unknown database mirror path for: " + dbKey;
      LOGGER.debug(_errMsg);
      formatter.dumpError(_errMsg);
      return result;
    }
    if (idxKeys != null) {
      // a mirror refers to a list path pointing directly to Lucene indexes
      idxNames = new ArrayList<String>();
      iter = idxKeys.iterator();
      while (iter.hasNext()) {
        idxKey = iter.next();
        path = _dbMirrorConfig.getMirrorPath(idxKey);
        if (path == null) {
          _errMsg = "unknown database mirror path for: " + idxKey;
          LOGGER.debug(_errMsg);
          formatter.dumpError(_errMsg);
          return result;
        } else {
          // following line added since Fasta-based databanks (used with Plast)
          // are indexed to be queried by Id
          // Blast index is now associated with a Lucene index
          if (path.endsWith("M" + FormatDBRunner.PROTEIN_ALIAS_EXT)
              || path.endsWith("M" + FormatDBRunner.NUCLEIC_ALIAS_EXT)) {
            path = path.substring(0, path.lastIndexOf("M."))
                + LuceneUtils.IDX_OK_FEXT;
          }
          // backward compatibility
          if (new File(path).exists() == false) {
            path = null;
          }
        }
        if (path != null && !idxNames.contains(path))
          idxNames.add(path);
      }
      mirrorPath = null;
    } else {
      // following line added since Fasta-based databanks (used with Plast) are
      // indexed to be queried by Id
      // Blast index is now associated with a Lucene index
      if (mirrorPath.endsWith(FormatDBRunner.PROTEIN_ALIAS_EXT)
          || mirrorPath.endsWith(FormatDBRunner.NUCLEIC_ALIAS_EXT)) {
        mirrorPath = mirrorPath.substring(0, mirrorPath.lastIndexOf('.'))
            + LuceneUtils.IDX_OK_FEXT;
      }
      // a mirror refers to a single root path containing Lucene indexes
      mirrorPath = Utils.terminatePath(mirrorPath);
      idxNames = null;
    }
    // given the db key, get the db name
    dbName = _dbMirrorConfig.getMirrorName(dbKey);
    if (dbName == null)
      dbName = dbKey + " " + DBMirrorConfig.DEF_DB_NAME;
    // given the db key, get the reader
    /*
     * reader = _dbMirrorConfig.getMirrorReader(dbKey); if (reader==null){
     * _errMsg = "unknown database reader for: "+dbKey; LOGGER.debug(_errMsg);
     * dumpSevereError(w, _errMsg); return result; }
     */
    // get the start/top values defining the sequence coordinate range
    // from where to retrieve the features. These values are not used for
    // TXT_FORMAT dump
    // get the start value (sequence coordinate)
    val = data.get(STARTKEY);
    try {
      start = Integer.valueOf(val).intValue();
    } catch (Exception ex) {
      start = 0;
    }
    // get the stop value (sequence coordinate)
    val = data.get(STOPKEY);
    try {
      stop = Integer.valueOf(val).intValue();
    } catch (Exception ex) {
      stop = 0;
    }
    start = Math.min(start, stop);
    stop = Math.max(start, stop);
    // figures out if the sequence coordinates of the retrieved
    // features have to be adjusted
    val = data.get(ADJUSTKEY);
    adjust = "true".equalsIgnoreCase(val);

    if (!dbKey.startsWith((DBMirrorConfig.DICO_IDX))) {
      // nuc/prot index
      formatter.startPrologue();
      // process query
      if (hasfoIDs) {
        handleMultipleID(formatter, mirrorPath, idxNames, foIDs, dbName, dbKey);
      }
      else if (id.indexOf(',') == -1) {
        seq = handleSingleID(formatter, mirrorPath, idxNames, id,
            dbName, dbKey, start, stop, adjust);
        if (seq!=null) {
          result = new PSequence[1];
          result[0] = seq;
        }
      } else {
        result = handleMultipleID(formatter, mirrorPath, idxNames, id,
            dbName, dbKey);
      }
      formatter.startEpilogue();
    } else {// dico
      handleDicoIds(formatter.getOutWriter(), dbKey, mirrorPath, idxNames, id);
    }
    return result;
  }

  /**
   * Access to index system without using a dbmirror configuration file.
   * 
   * @param mirrorPath
   *          home directory of an index.
   * @param dbKey
   *          pass DBMirrorConfig.DICO_IDX when using dictionary to speed up
   *          query. For sequence indexes, pass null.
   * @param id
   *          pass a single sequence id when using sequence indexes. Comma
   *          separated list of ids is only allow when using dictionary index.
   * @param os
   *          where to produce the output. Please note that when using
   *          dictionary (DICO_IDX), this method will return as many lines as
   *          provided ids. On each line, one can find the following data: an
   *          id, a tab char and a data string.
   */
  public PSequence[] executeJob(String mirrorPath, String dbKey, String id,
      OutputStream os) {
    BufferedWriter outWriter = null;
    PFormatter formatter;
    PSequence[] result = null;

    _errMsg = null;

    if (mirrorPath == null) {
      _errMsg = "mirrorPath is not provided";
      LOGGER.debug(_errMsg);
      return result;
    }
    if (id == null) {
      _errMsg = "id is not provided";
      LOGGER.debug(_errMsg);
      return result;
    }
    // prepare basic text output
    try {
      if (os != null)
        outWriter = new BufferedWriter(new OutputStreamWriter(os));
    } catch (Exception e) {
      _errMsg = "unable to open buffered writer: " + e;
      LOGGER.debug(_errMsg);
      return result;
    }
    formatter = new PFormatter(PFormatter.FORMAT.TXT_FORMAT, outWriter, null);
    // process query
    if (dbKey==null) {
      dbKey="";
    }
    if (!dbKey.startsWith(DBMirrorConfig.DICO_IDX)) {
      PSequence seq = handleSingleID(formatter, mirrorPath, null,
          id, "", "", 0, 0, false);
      result = new PSequence[1];
      result[0] = seq;
    } else {// dico
      handleDicoIds(outWriter, dbKey, mirrorPath, null, id);
    }

    formatter.closeWriters();
    return result;
  }

  private String prepareQuery(String dbName, String id, Integer start,
      Integer stop, Boolean adjust, String format) {
    StringBuffer szBuf;
    String getVar;

    _errMsg = null;
    szBuf = new StringBuffer();
    if (dbName != null) {
      szBuf.append(DBKEY);
      szBuf.append("=");
      szBuf.append(dbName);
    }
    if (id != null) {
      szBuf.append("&");
      szBuf.append(IDKEY);
      szBuf.append("=");
      szBuf.append(id);
    }
    if (start != null) {
      szBuf.append("&");
      szBuf.append(STARTKEY);
      szBuf.append("=");
      szBuf.append(start);
    }
    if (stop != null) {
      szBuf.append("&");
      szBuf.append(STOPKEY);
      szBuf.append("=");
      szBuf.append(stop);
    }
    if (adjust != null) {
      szBuf.append("&");
      szBuf.append(ADJUSTKEY);
      szBuf.append("=");
      szBuf.append(adjust);
    }
    if (format != null) {
      szBuf.append("&");
      szBuf.append(FOMRATKEY);
      szBuf.append("=");
      szBuf.append(format);
    }
    getVar = szBuf.toString();
    if (getVar.length() == 0) {
      _errMsg = "unable to get values to handle query.";
      LOGGER.debug(_errMsg);
      return null;
    }
    else {
      return getVar;
    }
  }
  public PSequence[] executeJob(String dbName, String id, Integer start,
      Integer stop, Boolean adjust, String format, OutputStream os,
      String dbMirrorConfFile) {
    String getVar = prepareQuery(dbName, id, start, stop, adjust, format);
    if (getVar==null)
      return null;
    return executeJob(getValuesFromString(getVar), os, null, dbMirrorConfFile);
  }

  public PSequence[] executeJob(String dbName, String id, Integer start,
      Integer stop, Boolean adjust, String format, OutputStream os,
      DBMirrorConfig dbMirrorConf) {
    String getVar = prepareQuery(dbName, id, start, stop, adjust, format);
    if (getVar==null)
      return null;
    return executeJob(getValuesFromString(getVar), os, null, dbMirrorConf);
  }

  /**
   * Get FORMAT from user provided arguments. 
   * 
   * @param values user-provided values
   * 
   * @return if argument values is null or does not contain
   * any format argument, returns PFormatter.FORMAT.TXT_FORMAT.
   */
  private PFormatter.FORMAT getFormat(Map<String, String> values) {
    PFormatter.FORMAT format = null;
    // get the output format
    if(values!=null && values.containsKey(FOMRATKEY)) {
      format = PFormatter.FORMAT.findByType(values.get(FOMRATKEY));
    }
    if (format==null) {
      format = PFormatter.FORMAT.TXT_FORMAT; 
    }
    return format;
  }
  
  private PSequence[] executeJob(
      Map<String, String> values, 
      OutputStream stdout, 
      OutputStream stderr,
      DBMirrorConfig dbMirrorConf) {
    
    PFormatter      formatter;
    PSequence[]     result = null;
    
    formatter = new PFormatter(getFormat(values));
    _errMsg = null;
    try {
      if (stdout != null)
        formatter.setOutWriter(new BufferedWriter(new OutputStreamWriter(stdout)));
    } catch (Exception e1) {
      _errMsg = "Internal error 1";
      LOGGER.debug("unable to create stdout buffered writer: " + e1);
      formatter.dumpError(_errMsg);
      formatter.closeWriters();
      return result;
    }

    try {
      if (stderr != null)
        formatter.setErrWriter(new BufferedWriter(new OutputStreamWriter(stderr)));
    } catch (Exception e1) {
      _errMsg = "Internal error 2";
      LOGGER.debug("unable to create stderr buffered writer: " + e1);
      formatter.dumpError(_errMsg);
      formatter.closeWriters();
      return result;
    }
    _dbMirrorConfig = dbMirrorConf;
    _descriptors = DBDescriptorUtils.prepareIndexDBList(_dbMirrorConfig);
    if (values != null) {
      result = executeQuery(formatter, values);
    } else {
      _errMsg = "Internal server error 2: unable to prepare databank list";
      formatter.dumpError(_errMsg);
    }
    formatter.closeWriters();
    return result;
  }

  /**
   * Execute a query against banks managed by BeeDeeM.
   * 
   * @param values user-provided arguments
   * @param stdout where to dump results
   * @param stderr where to dump error messages
   * @param dbMirrorConf BeeDeeM configuration
   * 
   * @return array of PSequence objects or null if working with Dictionary or
   * user has provided a file of sequence IDs.
   * @throws IOException 
   * */
  public PSequence[] executeJob(
      Map<String, String> values, 
      OutputStream stdout, 
      OutputStream stderr, 
      String dbMirrorConfFile) {
    
    DBMirrorConfig config = null;
    
    // load the mirror configuration
    try (FileInputStream fis = new FileInputStream(dbMirrorConfFile)) {
      config = new DBMirrorConfig();
      if (!config.load(fis)) {
        _errMsg = "Error: unable to load databank configuration";
        return null;
      }
    } 
    catch (FileNotFoundException e) {
      _errMsg = "Error: unable to find: " + dbMirrorConfFile;
      LOGGER.debug(_errMsg);
      return null;
    } catch (IOException e1) {
      _errMsg = "Error: unable to read/close: " + dbMirrorConfFile;
      LOGGER.debug(_errMsg);
      return null;
    }
    return executeJob(values, stdout, stderr, config);
  }


  /**
   * Call this method after a call to a executeQuery method to figure out
   * whether or not an error occurred during the process.
   */
  public boolean terminateWithError() {
    return _errMsg != null;
  }

  /**
   * Returns an error message or null.
   */
  public String getErrorMessage() {
    return _errMsg;
  }
}
