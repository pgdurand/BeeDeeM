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
 * A utility class to query a user sequence index.<br><br>
 * 
 * Sample use: CmdLineUserQuery -d <path-to-index> -i seqids<br>
 * Use program without any arguments to get help.<br><br>
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

    opts = new Options();
    opts.addOption(index);
    opts.addOption(seqids);
    opts.addOption(idsfile);
    opts.addOption(outputFile);
    CmdLineUtils.setHelpOption(opts);
    return opts;
  }

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

  private static boolean dumpSeqIDs(String index, String seqids, Writer w) {
    StringTokenizer tokenizer;
    String id, msg;
    DBEntry entry;
    File dbFile;
    
    index = CmdLineUtils.expandEnvVars(index);
    tokenizer = new StringTokenizer(seqids, ",");
    while (tokenizer.hasMoreTokens()) {
      id = tokenizer.nextToken().trim();
      _idProvidedCounter++;
      entry = LuceneUtils.getEntry(index, id);
      if (entry==null) {
        msg = String.format(DBMSMessages.getString("Tool.UserQuery.msg5"), id);
        LoggerCentral.error(LOGGER, msg);
        continue;
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
    return true;
  }
  
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
  /**
   * Run cutting job.
   * 
   * @param args command line arguments
   * 
   * @return true if cutting is ok, false otherwise.
   * */
  public static boolean doJob(String[] args){
    CommandLine cmdLine;
    String msg, toolName, index, seqids, idsfile, outputFile;
    Options options;
    boolean bRet = true;
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

    // get sequences
    if (seqids!=null) {
      bRet = dumpSeqIDs(index, seqids, writer);
    }
    else {
      bRet = dumpSeqIDs(index, new File(idsfile), writer);
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
