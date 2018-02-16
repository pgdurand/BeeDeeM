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
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.StringTokenizer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.io.IOUtils;

import bzh.plealog.dbmirror.indexer.DBEntry;
import bzh.plealog.dbmirror.indexer.LuceneUtils;
import bzh.plealog.dbmirror.reader.DBUtils;
import bzh.plealog.dbmirror.ui.resources.DBMSMessages;

/**
 * A utility class to query a user sequence index.<br><br>
 * 
 * Sample use: CmdLineUserQuery -d <path-to-index> -i seqids<br>
 *             
 * @author Patrick G. Durand
 * */
public class CmdLineUserQuery {
  private static final String INDEX_ARG = "d";
  private static final String SEQIDS_ARG = "i";
  private static final String IDSFILE_ARG = "f";

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

    opts = new Options();
    opts.addOption(index);
    opts.addOption(seqids);
    opts.addOption(idsfile);
    CmdLineUtils.setHelpOption(opts);
    return opts;
  }

  private static boolean dumpEntry(File fEntry) {
    BufferedWriter writer = null;
    BufferedReader reader = null;
    String line, msg;
    boolean bRet = true;
    try {
      reader = new BufferedReader(new InputStreamReader(new FileInputStream(
          fEntry), "UTF-8"));
      writer = new BufferedWriter(new OutputStreamWriter(System.out));
      while ((line = reader.readLine()) != null) {
        writer.write(line);
        writer.write("\n");
      }
      writer.flush();
    } catch (Exception ex) {
      msg = String.format(DBMSMessages.getString("Tool.UserQuery.msg4"), fEntry, ex.toString());
      System.err.println(msg);
      bRet = false;
    } finally {
      IOUtils.closeQuietly(reader);
      IOUtils.closeQuietly(writer);
    }
    return bRet;
  }

  private static boolean dumpSeqIDs(String index, String seqids) {
    StringTokenizer tokenizer;
    String id, msg;
    DBEntry entry;
    File dbFile;
    
    index = CmdLineUtils.expandEnvVars(index);
    tokenizer = new StringTokenizer(seqids, ",");
    while (tokenizer.hasMoreTokens()) {
      id = tokenizer.nextToken().trim();
      entry = LuceneUtils.getEntry(index, id);
      dbFile = DBUtils.readDBEntry(entry.getFName(), entry.getStart(), entry.getStop());
      if (dbFile == null) {
        msg = String.format(DBMSMessages.getString("Tool.UserQuery.msg3"), id);
        System.err.println(msg);
      }
      else {
        dumpEntry(dbFile);
        dbFile.delete();
      }
    }
    return true;
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
    String msg, toolName, index, seqids,idsfile;
    Options options;
    boolean bRet = true;
    
    toolName = DBMSMessages.getString("Tool.UserQuery.name");

    // prepare the Logging system
    StarterUtils.configureApplication(null, toolName, true, false, true, false);
    
    // handle the command-line
    options = getCmdLineOptions();
    cmdLine = CmdLineUtils.handleArguments(args, options, toolName);
    if (cmdLine == null) {
      return false;
    }
    index = cmdLine.getOptionValue(INDEX_ARG);
    seqids = cmdLine.getOptionValue(SEQIDS_ARG);
    idsfile = cmdLine.getOptionValue(IDSFILE_ARG);
    
    // add additional controls on cmdline values
    if (seqids==null && idsfile==null){
      msg = DBMSMessages.getString("Tool.UserQuery.msg1");
      System.err.println(msg);
      return false;
    }

    if (seqids!=null && idsfile!=null){
      msg = DBMSMessages.getString("Tool.UserQuery.msg2");
      System.err.println(msg);
      return false;
    }

    return dumpSeqIDs(index, seqids);
  }

  /**
   * Start application.
   * 
   * @param args command line arguments
   * */
  public static void main(String[] args) {
    if (!doJob(args)){
      // exit code=1 : do this to report error to calling app
      System.exit(1);
    }
  }
}
