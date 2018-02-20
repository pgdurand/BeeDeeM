/* Copyright (C) 2007-2018 Patrick G. DUrand
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
package bzh.plealog.dbmirror.main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.StringTokenizer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.indexer.DBEntry;
import bzh.plealog.dbmirror.indexer.LuceneUtils;
import bzh.plealog.dbmirror.reader.DBUtils;
import bzh.plealog.dbmirror.ui.resources.DBMSMessages;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * A utility class to query a user sequence index to get sequences. Such an index is 
 * created using tool CmdLineIndexer, or BeeDeeM standard index.<br><br>
 * 
 * It is worth noting that this tool can also be used to get a complement set 
 * of sequence IDs. Use argument '-c' for that purpose. <br><br>
 * 
 * Sample use: CmdLineUserQuery -d <path-to-index> -i seqids<br><br>
 *   CmdLineUserQuery -d tests/junit/databank/fasta_prot/uniprot.faa.ld -i M4K2_HUMAN<br>
 *    -> retrieve sequence M4K2_HUMAN from index<br>
 *   CmdLineUserQuery -d tests/junit/databank/fasta_prot/uniprot.faa.ld -c -i M4K2_HUMAN<br>
 *    -> retrieve complement of sequence M4K2_HUMAN from index, i.e. retrieve ALL 
 *    sequences BUT M4K2_HUMAN<br>
 *   CmdLineUserQuery -d tests/junit/databank/fasta_prot/uniprot.faa.ld -f tests/junit/databank/fasta_prot/fo-seqids.txt<br>
 *    -> retrieve from index sequence(s) identified from IDs contained in file fo-seqids.txt <br><br>
 * Use program without any arguments to get help.<br>
 * Note: environment variables are accepted in file path.<br>
 * 
 * A log file called UserIndexQuery.log is created within ${java.io.tmpdir}. This
 * default log file can be redirected using JRE variables KL_WORKING_DIR and
 * KL_LOG_FILE. E.g. java ... -DKL_WORKING_DIR=/my-path -DKL_LOG_FILE=query.log<br><br>
 * 
 * @author Patrick G. Durand
 * */
public class CmdLineUserQuery {
  private static final String INDEX_ARG   = "d";
  private static final String SEQIDS_ARG  = "i";
  private static final String IDSFILE_ARG = "f";
  private static final String OUTPUT_ARG  = "o";
  private static final String COMPLEMENT_ARG  = "c";

  private static final Log    LOGGER      = LogFactory
      .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY + ".CmdLineUserQuery");

  private static int _idProvidedCounter = 0;
  private static int _idRetrievedCounter = 0;
  
  /**
   * Setup the valid command-line of the application.
   */
  @SuppressWarnings("static-access")
  private static Options getCmdLineOptions() {
    Options opts;
   
    Option index = OptionBuilder
        .withArgName( DBMSMessages.getString("Tool.UserQuery.arg1.lbl") )
        .hasArg()
        .isRequired()
        .withDescription( DBMSMessages.getString("Tool.UserQuery.arg1.desc") )
        .create(INDEX_ARG);
    Option seqids = OptionBuilder
        .withArgName( DBMSMessages.getString("Tool.UserQuery.arg2.lbl") )
        .hasArg()
        .withDescription( DBMSMessages.getString("Tool.UserQuery.arg2.desc") )
        .create(SEQIDS_ARG);
    Option idsfile = OptionBuilder
        .withArgName( DBMSMessages.getString("Tool.UserQuery.arg3.lbl") )
        .hasArg()
        .withDescription( DBMSMessages.getString("Tool.UserQuery.arg3.desc") )
        .create(IDSFILE_ARG);
    Option outputFile = OptionBuilder
        .withArgName( DBMSMessages.getString("Tool.UserQuery.arg4.lbl") )
        .hasArg()
        .withDescription( DBMSMessages.getString("Tool.UserQuery.arg4.desc") )
        .create(OUTPUT_ARG);
    Option complement = OptionBuilder
        .withArgName( DBMSMessages.getString("Tool.UserQuery.arg5.lbl") )
        .withDescription( DBMSMessages.getString("Tool.UserQuery.arg5.desc") )
        .create(COMPLEMENT_ARG);

    opts = new Options();
    opts.addOption(index);
    opts.addOption(seqids);
    opts.addOption(idsfile);
    opts.addOption(outputFile);
    opts.addOption(complement);
    CmdLineUtils.setHelpOption(opts);
    return opts;
  }

  /**
   * Dump a sequence.
   * 
   * @param fEntry path to tmp file containing a retrieved sequence
   * @param w final destination of the sequence
   * 
   * @return true if success, false if an error occurred. Error is reported in log file.
   * */
  private static boolean dumpEntry(File fEntry, Writer w) {
    BufferedWriter writer = null;
    BufferedReader reader = null;
    String line, msg;
    boolean bRet = true;
    try {
      reader = new BufferedReader(new InputStreamReader(new FileInputStream(
          fEntry), "UTF-8"));
      writer = new BufferedWriter(w);
      while ((line = reader.readLine()) != null) {
        writer.write(line);
        writer.write("\n");
      }
      _idRetrievedCounter++;
      writer.flush();
    } catch (Exception ex) {
      msg = String.format(DBMSMessages.getString("Tool.UserQuery.msg4"), fEntry, ex.toString());
      LoggerCentral.error(LOGGER, msg);
      bRet = false;
    } finally {
      IOUtils.closeQuietly(reader);
    }
    return bRet;
  }

  /**
   * Dump the content of an entry.
   * 
   * @param index path to Lucene index to query. Such an index is created using
   * CmdLineIndexer tool.
   * @param id the ide to find in the index
   * @param w final destination of the sequence
   * */
  private static void dumpEntry(String index, String id, Writer w) {
    String msg;
    DBEntry entry;
    File dbFile;

    entry = LuceneUtils.getEntry(index, id);
    if (entry==null) {
      msg = String.format(DBMSMessages.getString("Tool.UserQuery.msg5"), id);
      LoggerCentral.error(LOGGER, msg);
      return;
    }
    dbFile = DBUtils.readDBEntry(entry.getFName(), entry.getStart(), entry.getStop());
    if (dbFile == null) {
      msg = String.format(DBMSMessages.getString("Tool.UserQuery.msg3"), id);
      LoggerCentral.error(LOGGER, msg);
    }
    else {
      dumpEntry(dbFile, w);
      dbFile.delete();
    }
  }

  /**
   * Collect sequence IDs.
   * 
   * @param ids_map map populated in this method with seq IDs
   * @param seqids source sequence IDs
   * */
  private static void collectSeqIDs(HashSet<String> ids_map, String seqids) {
    StringTokenizer tokenizer;
    String id;
    tokenizer = new StringTokenizer(seqids, ",");
    while (tokenizer.hasMoreTokens()) {
      id = tokenizer.nextToken().trim();
      ids_map.add(id);
      _idProvidedCounter++;
    }
  }
  /**
   * Collect sequence IDs.
   * 
   * @param ids_map map populated in this method with seq IDs
   * @param fofPath path to a file containing sequence IDs
   * */
  private static boolean collectSeqIDs(HashSet<String> ids_map, File fofPath) {
    LineIterator it = null;
    boolean bRet = true;
    try {
      it = FileUtils.lineIterator(fofPath, "UTF-8");
        while (it.hasNext()) {
          collectSeqIDs(ids_map, it.nextLine());
        }
    } 
    catch(Exception ex) {
      String msg = String.format(DBMSMessages.getString("Tool.UserQuery.msg6"), ex.toString());
      LoggerCentral.error(LOGGER, msg);
      bRet = false;
    }
    finally {
      LineIterator.closeQuietly(it);
    }
    return bRet;
  }
  
  /**
   * Locate a sequence ID in a map of sequence IDs.
   * 
   * @param ids_map map of sequence IDs
   * @param id the sequence ID to locate in the map
   * 
   * @return true if found, false otherwise.
   * */
  private static boolean findIdInMap(HashSet<String> ids_map, String id) {
    // take into account that a sequence ID may be formatted using
    // NBCI rules, e.g. sp|P97756|KKCC1_RAT
    StringTokenizer tokenizer = new StringTokenizer(id, "|");
    String token;
    boolean isPdb = false;
    //code adapted from LuceneUtils.getQuery(String id);
    while (tokenizer.hasMoreTokens()) {
      token = tokenizer.nextToken();
      //do we have to stkip bank name ?
      if (LuceneUtils.DB_TOKENS.contains(token)) {
        if ("GNL".equals(token)) {
          // before skipping, check if it is possible to do so
          if (tokenizer.hasMoreTokens())
            tokenizer.nextToken();
        } else if ("PDB".equals(token)) {
          // when PDB is detected, read next token : the entry ID
          // after reading that, we quit, since last token is the chain ID
          isPdb = true;
        }
        continue;
      }
      // id found ?
      if (ids_map.contains(token)) {
        return true;
      }
      if (isPdb)
        break;
    }
    return false;
  }

  /**
   * Slice a sequence ID. When a sequence ID is provided as NCBI ID, e.g.
   * sp|P47809|MP2K4_MOUSE, this method returns the first valid single ID,
   * e.g. P47809.
   * 
   * @param id the sequence ID to slice
   * 
   * @return true if found, false otherwise.
   * */
  private static String sliceId(String id) {
    // take into account that a sequence ID may be formatted using
    // NBCI rules, e.g. sp|P97756|KKCC1_RAT
    StringTokenizer tokenizer = new StringTokenizer(id, "|");
    String token;
    //code adapted from LuceneUtils.getQuery(String id);
    while (tokenizer.hasMoreTokens()) {
      token = tokenizer.nextToken();
      //do we have to stkip bank name ?
      if (LuceneUtils.DB_TOKENS.contains(token)) {
        if ("GNL".equals(token)) {
          // before skipping, check if it is possible to do so
          if (tokenizer.hasMoreTokens())
            tokenizer.nextToken();
        }
        continue;
      }
      return token;
    }
    return id;
    
  }
  /**
   * Dump sequences given complement of IDs.
   * 
   * @param index path to Lucene index to query. Such an index is created using
   * CmdLineIndexer tool.
   * @param ids_map a map of sequence IDs. These IDs are used to locate their complement 
   * in index the index. 
   * @param w a writer
   * 
   * @return true if success, false if an error occurred. Error is reported in log file.
   */
  private static boolean dumpComplementSeqIDs(String index, HashSet<String> ids_map, Writer w) {
    boolean bRet = true;
    Enumeration<DBEntry> entries;
    DBEntry entry;
    String id;
    
    entries = LuceneUtils.entries(index);
    while(entries.hasMoreElements()) {
      entry = entries.nextElement();
      id = entry.getId().toUpperCase();
      if (!findIdInMap(ids_map, id)) {
        id = sliceId(id);
        dumpEntry(index, id, w);
      }
    }
    return bRet;
  }
  
  /**
   * Dump sequences given complement of IDs.
   * 
   * @param index path to Lucene index to query. Such an index is created using
   * CmdLineIndexer tool.
   * @param seqids sequence IDs 
   * @param w a writer
   * 
   * @return true if success, false if an error occurred. Error is reported in log file.
   */
  private static boolean dumpComplementSeqIDs(String index, String seqids, Writer w) {
    HashSet<String> ids_map;
    
    ids_map = new HashSet<>();
    collectSeqIDs(ids_map, seqids);
    dumpComplementSeqIDs(index, ids_map, w);
    return true;
  }

  /**
   * Dump sequences given complement of IDs.
   * 
   * @param index path to Lucene index to query. Such an index is created using
   * CmdLineIndexer tool.
   * @param fofPath fofPath path to a file of IDs.
   * @param w a writer
   * 
   * @return true if success, false if an error occurred. Error is reported in log file.
   */
  private static boolean dumpComplementSeqIDs(String index, File fofPath, Writer w) {
    HashSet<String> ids_map;
    
    ids_map = new HashSet<>();
    collectSeqIDs(ids_map, fofPath);
    dumpComplementSeqIDs(index, ids_map, w);
    return true;
  }

  /**
   * Dump sequences given IDs.
   * 
   * @param index path to Lucene index to query. Such an index is created using
   * CmdLineIndexer tool.
   * @param seqids comma separated list of sequence IDs
   * @param w a writer
   * 
   * @return true if success, false if an error occurred. Error is reported in log file.
   */
  private static boolean dumpSeqIDs(String index, String seqids, Writer w) {
    StringTokenizer tokenizer;
    String id;
    
    tokenizer = new StringTokenizer(seqids, ",");
    while (tokenizer.hasMoreTokens()) {
      id = tokenizer.nextToken().trim();
      _idProvidedCounter++;
      dumpEntry(index, id, w);
    }
    return true;
  }
  
  /**
   * Dump sequences given IDs.
   * 
   * @param index path to Lucene index to query. Such an index is created using
   * CmdLineIndexer tool.
   * @param fofPath path to a file of IDs. Such a file contains lines of sequence IDs,
   * one per line or several (comma separated) ones per line.
   * @param w a writer
   * 
   * @return true if success, false if an error occurred. Error is reported in log file.
   */
  private static boolean dumpSeqIDs(String index, File fofPath, Writer w) {
    LineIterator it = null;
    boolean bRet = true;
    try {
      it = FileUtils.lineIterator(fofPath, "UTF-8");
        while (it.hasNext()) {
          dumpSeqIDs(index, it.nextLine(), w);
        }
    } 
    catch(Exception ex) {
      String msg = String.format(DBMSMessages.getString("Tool.UserQuery.msg6"), ex.toString());
      LoggerCentral.error(LOGGER, msg);
      bRet = false;
    }
    finally {
      LineIterator.closeQuietly(it);
    }
    return bRet;
  }
  
  private static boolean dumpSeqIDs(String index, String seqids, Writer w, boolean complement) {
    if (!complement) {
      return dumpSeqIDs(index, seqids, w);
    }
    else {
      return dumpComplementSeqIDs(index, seqids, w);
    }
  }
  
  private static boolean dumpSeqIDs(String index, File fofPath, Writer w, boolean complement) {
    if (!complement) {
      return dumpSeqIDs(index, fofPath, w);
    }
    else {
      return dumpComplementSeqIDs(index, fofPath, w);
    }
  }

  /**
   * Run query job.
   * 
   * @param args command line arguments
   * 
   * @return true if cutting is ok, false otherwise.
   * */
  public static boolean doJob(String[] args){
    CommandLine cmdLine;
    String msg, toolName, index, seqids, idsfile, outputFile;
    Options options;
    boolean bRet = true, complement;
    Writer writer;
    
    toolName = DBMSMessages.getString("Tool.UserQuery.name");

    // prepare the Logging system
    StarterUtils.configureApplication(null, toolName, true, false, true, false);
    LoggerCentral.info(LOGGER, "*** Starting "+toolName);
    
    // handle the command-line
    options = getCmdLineOptions();
    cmdLine = CmdLineUtils.handleArguments(args, options, toolName);
    if (cmdLine == null) {
      return false;
    }
    index = cmdLine.getOptionValue(INDEX_ARG);
    seqids = cmdLine.getOptionValue(SEQIDS_ARG);
    idsfile = cmdLine.getOptionValue(IDSFILE_ARG);
    outputFile = cmdLine.getOptionValue(OUTPUT_ARG);
    complement = cmdLine.hasOption(COMPLEMENT_ARG);
    
    // add additional controls on cmdline values
    if (seqids==null && idsfile==null){
      msg = DBMSMessages.getString("Tool.UserQuery.msg1");
      LoggerCentral.error(LOGGER, msg);
      return false;
    }
    if (seqids!=null && idsfile!=null){
      msg = DBMSMessages.getString("Tool.UserQuery.msg2");
      LoggerCentral.error(LOGGER, msg);
      return false;
    }

    // open the writer to dump sequences
    try {
      if (outputFile!=null) {
        outputFile = CmdLineUtils.expandEnvVars(outputFile);
        writer = new FileWriter(outputFile);
      }
      else {
        writer = new OutputStreamWriter(System.out);
      }
    }
    catch(Exception ex) {
      msg = String.format(DBMSMessages.getString("Tool.UserQuery.msg7"), ex.toString());
      return false;
    }

    index = CmdLineUtils.expandEnvVars(index);
    // get sequences
    if (seqids!=null) {
      bRet = dumpSeqIDs(index, seqids, writer, complement);
    }
    else {
      idsfile = CmdLineUtils.expandEnvVars(idsfile);
      bRet = dumpSeqIDs(index, new File(idsfile), writer, complement);
    }
    
    // provide some stats to the user (log file only)
    msg = String.format(DBMSMessages.getString("Tool.UserQuery.msg8a"), _idProvidedCounter);
    LoggerCentral.info(LOGGER, msg);
    msg = String.format(DBMSMessages.getString("Tool.UserQuery.msg8b"), _idRetrievedCounter);
    LoggerCentral.info(LOGGER, msg);
    
    // carefully close I/O channels
    LuceneUtils.closeStorages();
    IOUtils.closeQuietly(writer);
    
    return bRet; 
  }

  /**
   * Start application.
   * 
   * @param args command line arguments
   * */
  public static void main(String[] args) {
    CmdLineUtils.informForErrorMsg(!doJob(args)); 
  }
}
