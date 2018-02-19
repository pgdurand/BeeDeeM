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

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.indexer.DBParsable;
import bzh.plealog.dbmirror.indexer.FastaParser;
import bzh.plealog.dbmirror.indexer.GenbankParser;
import bzh.plealog.dbmirror.indexer.LuceneStorageSystem;
import bzh.plealog.dbmirror.indexer.LuceneUtils;
import bzh.plealog.dbmirror.indexer.StorageSystem;
import bzh.plealog.dbmirror.indexer.SwissProtParser;
import bzh.plealog.dbmirror.ui.resources.DBMSMessages;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.descriptor.DatabankFormat;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * A utility class to index a sequence file.<br><br>
 * 
 * Sample use: CmdLineIndexer -i tests/junit/databank/fasta_prot/uniprot.faa<br>
 *             Supported format: Embl, Genbank, Fasta<br>
 *             Note: environment variables are accepted in file path.<br><br>
 *             
 * For now, indexing must be done in the same directory as the input sequence file.
 * Consider using symbolic link if your sequence file is located within a read-only 
 * directory.
 * 
 * A log file called UserIndexQuery.log is created within ${java.io.tmpdir}. This
 * default log file can be redirected using JRE variables KL_WORKING_DIR and
 * KL_LOG_FILE. E.g. java ... -DKL_WORKING_DIR=/my-path -DKL_LOG_FILE=query.log<br><br>
 * 
 * @author Patrick G. Durand
 * */
public class CmdLineIndexer {
  
  private static final Log    LOGGER      = LogFactory
      .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY + ".CmdLineIndexer");
  
  /**
   * Prepare command-line arguments.
   * 
   * @return command line options
   */
  @SuppressWarnings("static-access")
  private static Options getCmdLineOptions() {
    Options opts;
   
    Option file = OptionBuilder
        .withArgName( DBMSMessages.getString("Tool.Cutter.arg4.lbl") )
        .isRequired()
        .hasArg()
        .withDescription( DBMSMessages.getString("Tool.Cutter.arg4.desc") )
        .create(CmdLineCutter.FILE_ARG);
    Option format = OptionBuilder
        .withArgName( DBMSMessages.getString("Tool.Cutter.arg5.lbl") )
        .hasArg()
        .withDescription( DBMSMessages.getString("Tool.Cutter.arg5.desc") )
        .create(CmdLineCutter.FORMAT_ARG);

    opts = new Options();
    opts.addOption(file);
    opts.addOption(format);
    CmdLineUtils.setHelpOption(opts);
    return opts;
  }

  /**
   * Return a sequence file parser given a bank format type.
   * 
   * @param dbFormat a bank format
   * 
   * @return a sequence file parser
   */
  private static DBParsable getBankParser(DatabankFormat.DatabankFormatTypes dbFormat) {
    DBParsable parser=null;
    
    switch (dbFormat) {
    case Fasta:
      parser = new FastaParser();
      break;
    case FastQ:
      LoggerCentral.error(LOGGER, DBMSMessages.getString("Tool.Indexer.msg1"));
      break;
    case Genbank:
      parser = new GenbankParser();
      break;
    case SwissProt:
      parser = new SwissProtParser();
      break;
    }
    return parser;
  }
  
  /**
   * Index a sequence file.
   * 
   * @param sequenceFile the sequence file for which we have to index entries
   * @param dbFormat the format of the sequence file
   * 
   * @return true if indexing is ok, false otherwise.
   */
  private static boolean indexFile(String sequenceFile, DatabankFormat dbFormat) {
    boolean bRet = true;
    DBParsable parser;
    String idxName, msg;
    LuceneStorageSystem lss;
    
    sequenceFile = CmdLineUtils.expandEnvVars(sequenceFile);

    parser = getBankParser(dbFormat.getType());
    if (parser==null) {
      return false;
    }

    idxName = sequenceFile + LuceneUtils.DIR_OK_FEXT;
    if (new File(idxName).exists()) {
      msg = String.format(DBMSMessages.getString("Tool.Indexer.msg2"), idxName);
      LoggerCentral.error(LOGGER, msg);
      return false;
    }

    lss = new LuceneStorageSystem();
    parser.setCheckSeqIdRedundancy(true);
    lss.open(idxName, StorageSystem.WRITE_MODE);
    parser.parse(sequenceFile, lss);
    lss.close();

    msg = String.format(DBMSMessages.getString("Tool.Indexer.msg3"), parser.getEntries());
    LoggerCentral.info(LOGGER, msg);
    return bRet;
  }
  
  /**
   * Run indexing job.
   * 
   * @param args command line arguments
   * 
   * @return true if indexing is ok, false otherwise.
   * */
  public static boolean doJob(String[] args){
    CommandLine cmdLine;
    String msg, toolName, file, format;
    Options options;
    DatabankFormat dbFormat;
    
    toolName = DBMSMessages.getString("Tool.Indexer.name");

    // prepare the Logging system
    StarterUtils.configureApplication(null, toolName, true, false, true, false);
    LoggerCentral.info(LOGGER, "*** Starting "+toolName);
    
    // handle the command-line
    options = getCmdLineOptions();
    cmdLine = CmdLineUtils.handleArguments(args, options, toolName);
    if (cmdLine == null) {
      return false;
    }

    file = cmdLine.getOptionValue(CmdLineCutter.FILE_ARG);
    format = cmdLine.getOptionValue(CmdLineCutter.FORMAT_ARG);
    
    dbFormat = CmdLineCutter.getDatabankFormat(format);
    if (dbFormat==null) {
      return false;
    }
    
    msg = String.format(DBMSMessages.getString("Tool.Indexer.msg4"), file);
    LoggerCentral.info(LOGGER, msg);
    return indexFile(file, dbFormat);
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
