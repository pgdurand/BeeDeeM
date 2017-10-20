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
package bzh.plealog.dbmirror.main;

import java.util.Hashtable;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import bzh.plealog.dbmirror.reader.PQueryMirrorBase;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;

/**
 * This is the class to use to query the databanks managed with BeeDeeM.
 * Command line is as follows:<br>
 * -d   input Blast file to annotate (absolute path)<br>
 * -i   output file containing the annotated Blast result (absolute path)<br>
 * -f   format. One of: txt, fas, html, insd, finsd.<br>
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
 * <br>
 * 
 * @author Patrick G. Durand
 */
public class CmdLineQuery {
  public static final String DATABASE = "d";
  public static final String SEQID = "i";
  public static final String FORMAT = "f";

  /**
   * Setup the valid command-line of the application.
   */
  @SuppressWarnings("static-access")
  private static Options getCmdLineOptions() {
    Options opts;

    Option repo = OptionBuilder
        .withArgName( "repository" )
        .hasArg()
        .isRequired()
        .withDescription("type of repository. One of: nucleotide, protein, dico. Mandatory." )
        .create(DATABASE);
    Option in = OptionBuilder
        .withArgName( "seqID" )
        .hasArg()
        .isRequired()
        .withDescription("a sequence ID. Mandatory." )
        .create(SEQID);
    Option ft = OptionBuilder
        .withArgName( "format" )
        .hasArg()
        .isRequired()
        .withDescription("format. One of: txt, fas, html, insd, finsd. Mandatory." )
        .create(FORMAT);

    opts = new Options();
    opts.addOption(repo);
    opts.addOption(in);
    opts.addOption(ft);
    CmdLineUtils.setConfDirOption(opts);
    return opts;
  }

  public static boolean doJob(String[] args) {
    PQueryMirrorBase qm;
    Hashtable<String, String> values;
    CommandLine cmdLine;
    Options options;
    String toolName = "Query";
    
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

    qm.executeJob(values, System.out, DBMSAbstractConfig.getLocalMirrorConfFile());
    return true;
  }

  public static void main(String[] args) {
    if (!doJob(args)){
      System.exit(1);
    }
  }
}
