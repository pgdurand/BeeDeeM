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
package bzh.plealog.dbmirror.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Hashtable;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import bzh.plealog.dbmirror.reader.PQueryMirrorBase;
import bzh.plealog.dbmirror.ui.resources.DBMSMessages;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;

/**
 * This is the class to use to query the databanks managed with BeeDeeM.
 * Command line is as follows:<br>
 * -d   databank, one of: protein, nucleotide or dico<br>
 * -i   comma-separated list of sequence IDs, of path to a file of seqIDs (one per line)<br>
 * -f   format. One of: txt, fas, html, insd, finsd.<br>
 * -o   output. If not set, default to stdout.<br>
 * <br>
 * In addition, some parameters can be passed to the JVM for special
 * configuration purposes:<br>
 * -DKL_HOME=an_absolute_path ; the absolute path to the KDMS installation home
 * dir. If not set, use user.dir java property. -DKL_DEBUG=true ; if true, if
 * set, log will be in debug mode<br>
 * -DKL_WORKING_DIR=an_absolute_path ; if not set, log and working dirertories
 * are set to java.io.tmp<br>
 * -DKL_CONF_DIR=an_absolute_path ; the absolute path to a home-made conf directory. 
 * If not set, use ${user.dir}/conf.
 * -DKL_LOG_FILE=a_file_name ; if set, creates a log file with that name within
 * KL_WORKING_DIR<br>
 * -DKL_LOG_TYPE=none|console|file(default)<br><br>
 * <br>
 * 
 * @author Patrick G. Durand
 */
public class CmdLineQuery {
  private static final String DATABASE = "d";
  private static final String SEQID    = "i";
  private static final String FORMAT   = "f";
  private static final String OUTPUT   = "o";

  /**
   * Setup the valid command-line of the application.
   */
  @SuppressWarnings("static-access")
  private static Options getCmdLineOptions() {
    Options opts;

    Option repo = OptionBuilder
        .withArgName( DBMSMessages.getString("Tool.Query.arg1.lbl") )
        .hasArg()
        .isRequired()
        .withDescription( DBMSMessages.getString("Tool.Query.arg1.desc") )
        .create(DATABASE);
    Option in = OptionBuilder
        .withArgName( DBMSMessages.getString("Tool.Query.arg2.lbl") )
        .hasArg()
        .isRequired()
        .withDescription( DBMSMessages.getString("Tool.Query.arg2.desc") )
        .create(SEQID);
    Option ft = OptionBuilder
        .withArgName( DBMSMessages.getString("Tool.Query.arg3.lbl") )
        .hasArg()
        .isRequired()
        .withDescription( DBMSMessages.getString("Tool.Query.arg3.desc") )
        .create(FORMAT);
    Option out = OptionBuilder
        .withArgName( DBMSMessages.getString("Tool.Query.arg4.lbl") )
        .hasArg()
        .withDescription( DBMSMessages.getString("Tool.Query.arg4.desc") )
        .create(OUTPUT);

    opts = new Options();
    opts.addOption(repo);
    opts.addOption(in);
    opts.addOption(ft);
    opts.addOption(out);
    CmdLineUtils.setConfDirOption(opts);
    return opts;
  }

  public static boolean doJob(String[] args) {
    PQueryMirrorBase qm;
    Hashtable<String, String> values;
    CommandLine cmdLine;
    Options options;
    String toolName = DBMSMessages.getString("Tool.Query.name");
    OutputStream os = System.out;
    
    // prepare the Logging system
    StarterUtils.configureApplication(null, toolName, true, false, true);
    options = getCmdLineOptions();
    cmdLine = CmdLineUtils.handleArguments(args, options, toolName);
    if (cmdLine == null) {
      return false;
    }
    // start the Job
    qm = new PQueryMirrorBase();

    // get the key/value data and process the query
    values = new Hashtable<>();
    values.put("database", cmdLine.getOptionValue(DATABASE));
    values.put("id", cmdLine.getOptionValue(SEQID));
    values.put("format", cmdLine.getOptionValue(FORMAT));

    if (cmdLine.hasOption(OUTPUT)) {
      try {
        os = new FileOutputStream(new File(cmdLine.getOptionValue(OUTPUT)));
      } catch (FileNotFoundException e) {
        System.err.println(e);
        return false;
      }
    }
    
    qm.executeJob(values, os, System.err, DBMSAbstractConfig.getLocalMirrorConfFile());
    
    if (qm.terminateWithError()) {
      System.err.println(qm.getErrorMessage());
      return false;
    }
    else {
      return true;
    }
  }

  public static void main(String[] args) {
    if (!doJob(args)){
      System.exit(1);
    }
  }
}
