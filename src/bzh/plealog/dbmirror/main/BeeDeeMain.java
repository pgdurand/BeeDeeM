/* Copyright (C) 2022 Patrick G. Durand
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

import java.util.Arrays;
import java.util.Properties;

import bzh.plealog.dbmirror.ui.resources.DBMSMessages;

/**
 * This class starts BeeDeeM for all commands.
 * <br><br>
 * In addition, some parameters can be passed to the JVM for special configuration purposes:<br>
 * -DKL_HOME=an_absolute_path ; the absolute path to the DBMS installation home dir. If not set, use user.dir java property.
 * -DKL_DEBUG=true ; if true, if set, log will be in debug mode<br>
 * -DKL_WORKING_DIR=an_absolute_path ; if not set, log and working directories are set to java.io.tmp<br>
 * -DKL_CONF_DIR=an_absolute_path ; the absolute path to a home-made conf directory. If not set, use ${user.dir}/conf.
 * -DKL_LOG_FILE=a_file_name ; if set, creates a log file with that name within KL_WORKING_DIR<br>
 * -DKL_LOG_TYPE=none|console|file(default)<br><br>
 * 
 * @author Patrick G. Durand
 */
public class BeeDeeMain {
  
  private static final String[] TOOL_LIST= {
      "Annotate",
      "DeleteBank",
      "Dump",
      "Install",
      "Query",
      "UiInstall"};
  
  private static void dumpHelp() {
    Properties props = StarterUtils.getVersionProperties();
    System.out.print(props.getProperty("prg.app.name"));
    System.out.print(" ");
    System.out.println(DBMSMessages.getString("Tool.Master.intro"));
    for (String tName : TOOL_LIST) {
      System.out.print("  bmd ");
      System.out.print(DBMSMessages.getString("Tool."+tName+".cmd"));
      if (tName.equals("UiInstall")) {
        System.out.print(": ");
      }
      else {
        System.out.print(" [options]: ");
      }
      System.out.println(DBMSMessages.getString("Tool."+tName+".desc"));
    }
    StringBuffer buf = new StringBuffer();
    System.out.println(DBMSMessages.getString("Tool.Master.more"));
    buf.append("--\n");
    buf.append(props.getProperty("prg.app.name"));
    buf.append(" ");
    buf.append(props.getProperty("prg.version"));
    buf.append(" - ");
    buf.append(props.getProperty("prg.copyright"));
    buf.append("\n");
    buf.append(props.getProperty("prg.license.short"));
    buf.append("\n");
    buf.append(props.getProperty("prg.app.name"));
    buf.append(" manual: ");
    buf.append(props.getProperty("prg.man.url"));
    System.out.println(buf.toString());
  }
  
  public static void main(String[] args) {
    /* This code handles strings as "bdm xxx"
     * "bdm" or "bdm.bat" is the caller script
     * "xxx" can be either:
     *   - nothing, -h or --help: display help message
     *   - "ui": start UI Manager
     *   - everything else: try to run an existing command
     */
    String cmd = "help";
    
    if (args.length!=0) {
      cmd = args[0];
      args = Arrays.copyOfRange(args, 1, args.length);
    }
    
    switch(cmd) {
      case "annotate":
        Annotate.main(args);
        break;
      case "delete":
        DeleteBank.main(args);
        break;
      case "help":
        dumpHelp();
        break;
      case "info":
        DumpBankList.main(args);
        break;
      case "install":
        CmdLineInstaller.main(args);
        break;
      case "query":
        CmdLineQuery.main(args);
        break;
      case "ui":
        UiInstaller.main(args);
        break;
      default:
        System.err.print(DBMSMessages.getString("Tool.Master.err.cmd"));
        System.err.print(": ");
        System.err.println(cmd);
        System.exit(1);
    }
  }
}
