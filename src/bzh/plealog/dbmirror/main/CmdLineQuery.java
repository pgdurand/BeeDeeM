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
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.reader.PQueryMirrorBase;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;

/**
 * This is the class to use to query the databanks managed with KDMS.
 * Command line is as follows:<br>
 * -d   input Blast file to annotate (absolute path)<br>
 * -i   output file containing the annotated Blast result (absolute path)<br>
 * -f   type of annotation to retrieve. Options: bco or full. Use bco to only retrieve
 * <br>
 * In addition, some parameters can be passed to the JVM for special
 * configuration purposes:<br>
 * -DKL_HOME=an_absolute_path ; the absolute path to the KDMS installation home
 * dir. If not set, use user.dir java property. -DKL_DEBUG=true ; if true, if
 * set, log will be in debug mode<br>
 * -DKL_WORKING_DIR=an_absolute_path ; if not set, log and working dirertories
 * are set to java.io.tmp<br>
 * -DKL_LOG_FILE=a_file_name ; if set, creates a log file with that name within
 * KL_WORKING_DIR<br>
 * <br>
 * 
 * @author Patrick G. Durand
 */
public class CmdLineQuery {
  public static final String database = "d";
  public static final String seqid = "i";
  public static final String format = "f";

  private static final String[] mandatory_args = { database, seqid, format };

  private static final Log LOGGER = LogFactory.getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY + ".Query");

  private void printUsage() {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("Query", getCmdLineOptions());
  }

  public CommandLine handleArguments(String[] args) {
    Options options;
    GnuParser parser;
    CommandLine line = null;

    options = getCmdLineOptions();
    try {
      parser = new GnuParser();
      line = parser.parse(options, args);
      for (String arg : mandatory_args) {
        if (!line.hasOption(arg)) {
          throw new Exception("missing mandatory argument: " + arg);
        }
      }
    } catch (Exception exp) {
      LOGGER.warn("invalid command-line:" + exp);
      printUsage();
      line = null;
    }
    return line;
  }

  /**
   * Setup the valid command-line of the application.
   */
  private static Options getCmdLineOptions() {
    Options opts;

    opts = new Options();
    opts.addOption(database, true, "type of repository. One of: nucleotide, protein, dico.");
    opts.addOption(seqid, true, "a sequence ID");
    opts.addOption(format, true, "format. One of: txt, fas, html, insd, finsd.");
    return opts;
  }

  private void doJob(String[] args) {
    PQueryMirrorBase qm;
    Hashtable<String, String> values;
    CommandLine cmdLine;

    // prepare the Logging system
    StarterUtils.configureApplication(null, "QueryMirror", true, false, true);

    cmdLine = handleArguments(args);
    if (cmdLine == null) {
      System.exit(1);
    }
    // start the Job
    qm = new PQueryMirrorBase();

    // get the key/value data and process the query
    values = new Hashtable<>();
    values.put("database", cmdLine.getOptionValue(database));
    values.put("id", cmdLine.getOptionValue(seqid));
    values.put("format", cmdLine.getOptionValue(format));

    qm.executeJob(values, System.out, DBMSAbstractConfig.getLocalMirrorConfFile());
  }

  public static void main(String[] args) {
    new CmdLineQuery().doJob(args);
  }
}
