/* Copyright (C) 2006-2017 Patrick G. Durand
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
package bzh.plealog.dbmirror.annotator;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.bioinfo.api.data.searchresult.SROutput;
import bzh.plealog.bioinfo.io.searchresult.ncbi.BlastLoader;
import bzh.plealog.bioinfo.io.searchresult.srnative.NativeBlastWriter;
import bzh.plealog.dbmirror.main.StarterUtils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;

/**
 * This class is used to introduce sequence information within Blast results.
 * 
 * @author Patrick G. Durand
 */
public class PAnnotateBlastResult {
  public static final String    annot_type                = "type";
  public static final String    annot_type_bio_class_only = "bco";
  public static final String    annot_type_full           = "full";
  public static final String    annot_type_none           = "none";

  public static final String    input_file                = "i";
  public static final String    output_file               = "o";

  public static final String    writer_type               = "writer";
  private static final String   writer_type_xml           = "xml";

  private static final String[] mandatory_args            = { input_file,
      output_file, annot_type, writer_type               };
  protected static final Log    LOGGER                    = LogFactory
                                                              .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                                  + ".PAnnotateBlastResult");

  private void printUsage() {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("KLAnnotateBlastResult", getCmdLineOptions());
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
    opts.addOption(annot_type, true,
        "type of annotation to retrieve. Options: bco or full.");
    opts.addOption(input_file, true, "input Blast file to annotate");
    opts.addOption(output_file, true,
        "output file containing the annotated Blast result");
    opts.addOption(writer_type, true, "Type of writer. Options: xml or zml");

    // other options are passed in to the JVM (-D):
    // -DKL_DEBUG=true (whathever the value, if set, log will be in debug mode)
    // -DKL_WORKING_DIR=an_absolute_path ; if not set, log and working dir are
    // set to java.io.tmp
    // -DKL_LOG_FILE=a_file_name ; if set, creates a a log file with that name
    // within KL_WORKING_DIR
    // (these options are defined within class KDMSAbstractConfig)
    return opts;
  }

  public boolean annotate(String input, String output, String writer,
      String type) {
    SROutputAnnotator annotator = null;
    SROutput bo;
    BlastLoader loader;
    NativeBlastWriter kbWriter;
    boolean bRet = true;

    try {
      // prepare the application system
      StarterUtils.configureApplication(null, "SROutputAnnotator", true, false,
          true);

      LOGGER.debug("--> annotate");
      LOGGER.debug("input : " + input);
      LOGGER.debug("output: " + output);
      LOGGER.debug("writer: " + writer);
      LOGGER.debug("type  : " + type);

      // get an NCBI blast data loader
      LOGGER.debug("loading blast result file");
      loader = new BlastLoader();
      bo = loader.load(new File(input));
      if (bo == null)
        return true;// simply nothing to do with no data!

      LOGGER.debug("annotating data");
      // start the Job
      annotator = new SROutputAnnotator();
      if (annot_type_full.equals(type))
        annotator.doFullAnnotation(bo);
      else
        annotator.doClassificationAnnotation(bo);

      LOGGER.debug("writing results");
      // write the result
      if (writer_type_xml.equals(writer)) {
        loader.write(new File(output), bo);
      } else {
        kbWriter = new NativeBlastWriter();
        kbWriter.write(new File(output), bo);
      }
    } catch (Exception e) {
      LOGGER.warn("unable to annotate data file: " + input + ": " + e);
      bRet = false;
    } finally {
      try {
        if (annotator != null) {
          annotator.close();
        }
      } catch (Exception e) {
        LOGGER.warn("unable to close annotator : " + e.getMessage());
      }
    }
    LOGGER.debug("<-- annotate");
    return bRet;
  }

}
