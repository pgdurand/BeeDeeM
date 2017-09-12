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

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.util.LinkedHashtable;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.Configuration;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.conf.DBMirrorConfig;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptor;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptorUtils;
import bzh.plealog.dbmirror.util.descriptor.DatabankDescriptor;
import bzh.plealog.dbmirror.util.descriptor.IdxDescriptor;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

import com.plealog.genericapp.api.log.EZLogger;

/**
 * This is the class to use to report the list of installed banks. Accepted
 * arguments are: <br>
 * -d type of repository. One of: n, p, b, all. Default is: all.<br>
 * -f format. One of: txt, html, galaxy. Default is: txt. <br>
 * In addition, some parameters can be passed to the JVM for special
 * configuration purposes:<br>
 * -DKL_HOME=an_absolute_path ; the absolute path to the KDMS installation home
 * dir. If not set, use user.dir java property. -DKL_DEBUG=true ; if true, if
 * set, log will be in debug mode<br>
 * -DKL_WORKING_DIR=an_absolute_path ; if not set, log and working directories
 * are set to java.io.tmp<br>
 * -DKL_LOG_FILE=a_file_name ; if set, creates a log file with that name within
 * KL_WORKING_DIR<br>
 * <br>
 * 
 * @author Patrick G. Durand
 */
public class DumpBankList {
  public static final String DB_ARG = "d";
  public static final String FT_ARG = "f";

  private static final Log   LOGGER = LogFactory
                                        .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                            + ".DumpBankList");

  /**
   * Print program usage.
   */
  private void printUsage() {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("DumpBankList", getCmdLineOptions());
  }

  /**
   * handle command-line arguments.
   */
  public CommandLine handleArguments(String[] args) {
    Options options;
    GnuParser parser;
    CommandLine line = null;

    options = getCmdLineOptions();
    try {
      parser = new GnuParser();
      line = parser.parse(options, args);
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
    opts.addOption(DB_ARG, true,
        "type of repository. One of: n, p, b, all. Default is: all.");
    opts.addOption(FT_ARG, true, "format. One of: txt, html, galaxy. Default is: txt.");
    return opts;
  }

  /**
   * Get repository for which we have to report bank list. Default is all.
   */
  private static String getDatabaseType(CommandLine cmdLine) {
    String val = cmdLine.getOptionValue(DB_ARG);
    return val == null ? "all" : val;
  }

  /**
   * Get format to use to report bank list. Default is txt.
   */
  private static String getFormatType(CommandLine cmdLine) {
    String val = cmdLine.getOptionValue(FT_ARG);
    return val == null ? "txt" : val;
  }

  /**
   * Prepare the bank list for a given type.
   */
  private static List<DatabankDescriptor> getMirrorDBList(DBDescriptor.TYPE type) {
    String dbMirrorConfFile = DBMSAbstractConfig.getLocalMirrorConfFile();
    DBMirrorConfig conf = DBDescriptorUtils.getDBMirrorConfig(dbMirrorConfFile);
    List<DatabankDescriptor> dbList = new ArrayList<DatabankDescriptor>();

    for (IdxDescriptor descriptor : DBDescriptorUtils.getDBList(conf, type,
        true)) {
      try {
        if (descriptor.isUserAuthorized()){
    	  dbList.add(new DatabankDescriptor(descriptor));
        }
      } catch (Exception ex) {
        EZLogger.warn("Unable to read databank "
            + (descriptor != null ? descriptor.getName() : "") + " : "
            + ex.getMessage());
      }
    }

    return dbList;
  }

  /**
   * Update annotated status of dbList.
   */
  private static void setAnnotatedStatus(List<DatabankDescriptor> dbList, Set<String> annotatedBanks){
    for(DatabankDescriptor dd : dbList){
      dd.setHasAnnotation(annotatedBanks.contains(dd.getName()));
    }
  }
  
  /**
   * Return set of names of annotated banks.
   */
  private Set<String> getAnnotatedBanks(){
    HashSet<String> annotatedBanks = new HashSet<>();
    List<DatabankDescriptor> dbList;
    
    dbList = getMirrorDBList(DBDescriptor.TYPE.proteic);
    for(DatabankDescriptor dd : dbList){
      annotatedBanks.add(dd.getName());
    }
    dbList = getMirrorDBList(DBDescriptor.TYPE.nucleic);
    for(DatabankDescriptor dd : dbList){
      annotatedBanks.add(dd.getName());
    }
    return annotatedBanks;
  }
  /**
   * Compute cumulative bank size for the entire repository.
   */
  private long countSize(long curSize, List<DatabankDescriptor> dbList){
    for(DatabankDescriptor dd : dbList){
      curSize += dd.getDiskSizeL();
    }
    return curSize;
  }
  
  /**
   * Run job.
   */
  private void doJob(String[] args) {
    VelocityEngine ve;
    VelocityContext context;
    Template t;
    StringWriter writer;
    String db, ft;
    CommandLine cmdLine;
    List<DatabankDescriptor> emptyList = new ArrayList<DatabankDescriptor>();
    List<DatabankDescriptor> dbList;
    long dbTotalSize = 0l;
    
    // Configure software
    StarterUtils.configureApplication(null, "DumpBankList", true, false, true);

    // Get version info
    Properties props = StarterUtils.getVersionProperties();

    // Handle command-line
    cmdLine = handleArguments(args);
    db = getDatabaseType(cmdLine);
    ft = getFormatType(cmdLine);

    // Setup Maps to be used with Velocity Template engine
    LinkedHashtable<String, Object> mdserverinfo = new LinkedHashtable<String, Object>();
    LinkedHashtable<String, Object> config = new LinkedHashtable<String, Object>();
    LinkedHashtable<String, Object> dbs2 = new LinkedHashtable<String, Object>();
    Set<String> annotatedBanks = getAnnotatedBanks();
    
    // Report software configuration
    config.put(
        "name",
        props.getProperty("prg.app.name") + " - "
            + props.getProperty("prg.version"));
    config.put("instpath", DBMSAbstractConfig.getInstallAppPath());
    config.put("instconfpath", StarterUtils.getConfPath()
        + DBMSAbstractConfig.MASTER_CONF_FILE);
    config.put("wkpath", DBMSAbstractConfig.getWorkingPath());
    config.put("logpath", DBMSAbstractConfig.getLogAppPath());
    config.put("confpath", DBMSAbstractConfig.getLocalMirrorConfFile());
    config.put("dbpath", DBMSAbstractConfig.getLocalMirrorPath());

    // Report bank list
    if ("all".equals(db)) {
      // all
      dbList = getMirrorDBList(DBDescriptor.TYPE.blastn);
      setAnnotatedStatus(dbList, annotatedBanks);
      dbTotalSize = countSize(dbTotalSize, dbList);
      dbs2.put("mirror_n", dbList);
      dbList = getMirrorDBList(DBDescriptor.TYPE.blastp);
      setAnnotatedStatus(dbList, annotatedBanks);
      dbTotalSize = countSize(dbTotalSize, dbList);
      dbs2.put("mirror_p", dbList);
      dbList = getMirrorDBList(DBDescriptor.TYPE.dico);
      dbTotalSize = countSize(dbTotalSize, dbList);
      dbs2.put("mirror_d", dbList);
    } else if ("n".equals(db)) {
      // only nucleotide
      dbList = getMirrorDBList(DBDescriptor.TYPE.blastn);
      setAnnotatedStatus(dbList, annotatedBanks);
      dbTotalSize = countSize(dbTotalSize, dbList);
      dbs2.put("mirror_n", dbList);
      dbs2.put("mirror_p", emptyList);
      dbs2.put("mirror_d", emptyList);
    } else if ("p".equals(db)) {
      // only protein
      dbList = getMirrorDBList(DBDescriptor.TYPE.blastp);
      setAnnotatedStatus(dbList, annotatedBanks);
      dbTotalSize = countSize(dbTotalSize, dbList);
      dbs2.put("mirror_p", dbList);
      dbs2.put("mirror_n", emptyList);
      dbs2.put("mirror_d", emptyList);
    } else if ("b".equals(db)) {
      // only biological classification
      dbList = getMirrorDBList(DBDescriptor.TYPE.dico);
      dbTotalSize = countSize(dbTotalSize, dbList);
      dbs2.put("mirror_d", dbList);
      dbs2.put("mirror_n", emptyList);
      dbs2.put("mirror_p", emptyList);
    }

    config.put("reposize", Utils.getBytes(dbTotalSize));
    mdserverinfo.put("config", config);
    mdserverinfo.put("databases2", dbs2);

    // Setup Velocity Engine
    ve = new VelocityEngine();
    ve.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS,
        "org.apache.velocity.runtime.log.NullLogSystem");
    ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH,
        DBMSAbstractConfig.getInstallAppConfPath(Configuration.SYSTEM));

    // Run Velocity Engine
    try (BufferedWriter outWriter = new BufferedWriter(new OutputStreamWriter(
        System.out))) {
      ve.init();
      // Velocity template is taken from "conf/system" directory.
      // One can see that Velocity template name is generated using
      // value of cmdline argument "-f"
      t = ve.getTemplate(String.format("dbmsVersion-%s.vm", ft));
      context = new VelocityContext();
      context.put("bdminfo", mdserverinfo);
      writer = new StringWriter();
      t.merge(context, writer);
      outWriter.write(writer.toString());
    } catch (Exception e) {
      LoggerCentral.warn(LOGGER, e.toString());
    }
  }

  public static void main(String[] args) {
    new DumpBankList().doJob(args);
  }
}
