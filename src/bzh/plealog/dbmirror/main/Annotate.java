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

import org.apache.commons.cli.CommandLine;

import bzh.plealog.dbmirror.annotator.PAnnotateBlastResult;

/**
 * This class can be used to annotate Blast results. Command line is as follows:<br>
 * -i         input Blast file to annotate (absolute path)<br>
 * -o         output file containing the annotated Blast result (absolute path)<br>
 * -type      type of annotation to retrieve. Options: bco or full. Use bco to only retrieve
 * biological classifications information. Use full to retrieve full feature tables.<br>
 * -writer    Type of writer. Options: xml or zml. Use xml to write NCBI XML data file (not
 * suitable to store full feature tables). Use zml to store BlastViewer native data format (suitable
 * to store feature tables data).<br><br>
 * In addition, some parameters can be passed to the JVM for special configuration purposes:<br>
 * -DKL_HOME=an_absolute_path ; the absolute path to the DBMS installation home dir. If not set, use user.dir java property.
 * -DKL_DEBUG=true ; if true, if set, log will be in debug mode<br>
 * -DKL_WORKING_DIR=an_absolute_path ; if not set, log and working directories are set to java.io.tmp<br>
 * -DKL_CONF_DIR=an_absolute_path ; the absolute path to a home-made conf directory. If not set, use ${user.dir}/conf.
 * -DKL_LOG_FILE=a_file_name ; if set, creates a log file with that name within KL_WORKING_DIR<br><br>
 * 
 * @author Patrick G. Durand
 */
public class Annotate {
  public static void main(String[] args) {
    PAnnotateBlastResult annotator;
    CommandLine cmdLine;
    String input, output, writer, type;

    // prepare the Logging system
    StarterUtils.configureApplication(null, "Annotate", true, false, true);

    annotator = new PAnnotateBlastResult();

    cmdLine = annotator.handleArguments(args);
    if (cmdLine == null) {
      System.exit(1);
    }

    input = cmdLine.getOptionValue(PAnnotateBlastResult.input_file);
    output = cmdLine.getOptionValue(PAnnotateBlastResult.output_file);
    writer = cmdLine.getOptionValue(PAnnotateBlastResult.writer_type);
    type = cmdLine.getOptionValue(PAnnotateBlastResult.annot_type);

    if (!annotator.annotate(input, output, writer, type)) {
      System.exit(1);// do this to report error to calling app
    }
  }
}
