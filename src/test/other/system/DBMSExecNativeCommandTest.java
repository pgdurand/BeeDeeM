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
package test.other.system;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;

import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.conf.DBMSConfigurator;
import bzh.plealog.dbmirror.util.log.LoggerCentral;
import bzh.plealog.dbmirror.util.runner.CommandArgument;
import bzh.plealog.dbmirror.util.runner.DBMSExecNativeCommand;

/**
 * This class illustrates the use of the KDMSExecNativeCommand API.
 * 
 * @author Patrick G. Durand
 */
public class DBMSExecNativeCommandTest {

  private static String getConfPath() {
    String path;

    path = System.getProperty("KDMS_CONF_DIR");
    if (path != null)
      path = Utils.terminatePath(path);
    else
      path = Utils.terminatePath(System.getProperty("user.dir")) + "conf"
          + File.separator;
    return path;
  }

  // method adapted from FormatDBRunner
  private static Map<String, CommandArgument> prepareParams(boolean isProt,
      String dbName, List<String> dbFileNames) {
    Hashtable<String, CommandArgument> params;
    StringBuffer buf;
    String p;
    int i, size;

    params = new Hashtable<String, CommandArgument>();
    // isProteic ?
    params.put("-p", new CommandArgument((isProt ? "T" : "F"), false));
    // db File to format
    buf = new StringBuffer();
    size = dbFileNames.size();
    i = 0;
    for (String dataFile : dbFileNames) {
      buf.append(dataFile);
      if ((i + 1) < size) {
        buf.append(",");
      }
      i++;
    }
    p = buf.toString();
    params.put("-i", new CommandArgument(p, true, true, true));
    // parse gi (required to use NCBI recommendations)
    params.put("-o", new CommandArgument("T", false));
    // log file
    params.put("-l", new CommandArgument("formatdb.log", false));
    // db name; only when we have multiple input files
    params.put("-n", new CommandArgument(dbName, false));
    return params;
  }

  // method adapted from KLTaskFormatDB
  private static String getFormatDbPath() {
    DBMSConfigurator conf;
    String formatdb, path;

    conf = DBMSAbstractConfig.getConfigurator();
    if (conf == null) {
      return null;
    }

    formatdb = conf.getProperty(DBMSConfigurator.FDB_PRG_NAME);
    path = conf.getProperty(DBMSConfigurator.FDB_PATH_NAME);
    if (formatdb == null || path == null) {
      return null;
    }

    path = DBMSExecNativeCommand.formatNativePath(path, (path
        .indexOf(DBMSExecNativeCommand.APPDIR_VAR_NAME) >= 0) ? true : false,
        false);
    path = Utils.terminatePath(path) + formatdb;
    if (DBMSExecNativeCommand.getOSType() == DBMSExecNativeCommand.WINDOWS_OS) {
      path += ".exe";
    }
    if (new File(path).exists() == false) {
      return null;
    }
    return path;
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    if (args.length < 4) {
      System.err.println("invalid arguments. Usage: ");
      System.err.println("args[0] = db full path");
      System.err
          .println("        example: /Users/pdurand/biobase/databanks/n/Genbank_CoreNucleotide/download/Genbank_CoreNucleotide/");
      System.err
          .println("args[1] = db name (the one that will be created by formatdb)");
      System.err.println("        example: Genbank_CoreNucleotideT");
      System.err.println("args[2] = comma separated list of fasta volumes");
      System.err
          .println("        example: Genbank_CoreNucleotide00,Genbank_CoreNucleotide01");
      System.err.println("args[3] = db type: true|false (true: protein)");
      System.err.println("          example: false");

      System.exit(1);
    }

    BasicConfigurator.configure();
    String confPath = getConfPath();
    DBMSAbstractConfig.configureLog4J("kdmsUI");
    DBMSAbstractConfig.setInstallAppConfPath(confPath);
    DBMSAbstractConfig.initializeConfigurator(confPath
        + DBMSAbstractConfig.MASTER_CONF_FILE);
    LoggerCentral.reset();

    String dbPath = Utils.terminatePath(args[0]);
    ArrayList<String> dbFileNames = new ArrayList<String>();
    for (String str : Utils.tokenize(args[2])) {
      dbFileNames.add(dbPath + str);
    }
    Map<String, CommandArgument> param = prepareParams("true".equals(args[3]),
        args[1], dbFileNames);
    String formatDBCmd = getFormatDbPath();

    DBMSExecNativeCommand runner = new DBMSExecNativeCommand();
    runner.executeAndWait(formatDBCmd, param);

    // String cmd2[] = {
    // "/Users/pdurand/bin/macos/formatdb",
    // "-o", "T", "-n", "Genbank_CoreNucleotideT", "-l", "formatdb.log", "-i",
    // "Genbank_CoreNucleotide00 Genbank_CoreNucleotide01", "-p", "F" };
    //
    // String workDir =
    // "/Users/pdurand/biobase/databanks/n/Genbank_CoreNucleotide/download/Genbank_CoreNucleotide";
    // try {
    // Process proc = Runtime.getRuntime().exec(cmd2, null, new File(workDir));
    //
    // proc.waitFor();
    // } catch (Exception ex) {
    // System.out.println("error : " + ex);
    // }

  }

}
