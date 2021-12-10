/* Copyright (C) 2007-2020 Patrick G. Durand
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

import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import bzh.plealog.dbmirror.ui.resources.DBMSMessages;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;

public class CmdLineUtils {
  private static final String CONFDIR_KEY = "conf-dir";
  
  /**
   * Prepare an option to deal with configuration path.
   */
  @SuppressWarnings("static-access")
  public static void setConfDirOption(Options opts){
    Option confDir = OptionBuilder
        .withArgName( DBMSMessages.getString("Tool.Utils.arg1.lbl") )
        .withLongOpt(CONFDIR_KEY)
        .hasArg()
        .withDescription( DBMSMessages.getString("Tool.Utils.arg1.desc" ) )
        .create();
    opts.addOption(confDir);
  }
  /**
   * Prepare an option to deal with help.
   */
  public static void setHelpOption(Options opts){
    bzh.plealog.bioinfo.util.CmdLineUtils.setHelpOption(opts);
  }

  private static String getFooter() {
    Properties props = StarterUtils.getVersionProperties();
    StringBuffer buf = new StringBuffer("\n");
    String logFile = DBMSAbstractConfig.getLogAppFileName();
    buf.append("Default log file: ");
    buf.append(logFile==null?"none":DBMSAbstractConfig.getLogAppPath()+logFile);
    buf.append("\n");
    buf.append("--\n");
    buf.append("To control Log, use JRE args:\n");
    buf.append("   -DKL_WORKING_DIR=/my-path\n");
    buf.append("   -DKL_LOG_FILE=my-file.log\n");
    buf.append("   -DKL_LOG_TYPE=none|console|file(default)\n");
    buf.append("--\n");
    buf.append("To control Log level, use JRE args:\n");
    buf.append("   -DKL_DEBUG=true|false(default)\n");
    buf.append("--\n");
    buf.append("To override default configuration ('"+props.getProperty("prg.app.name")+"/conf' directory), use JRE args:\n");
    buf.append("   -DKL_CONF_DIR=/path/to/new/conf_dir . Such a path must target all expected conf sub-directories (system, scripts, descriptors)\n");
    buf.append("--\n");
    buf.append("To override dbms.config values, use JRE args:\n");
    buf.append("   -DKL_<key>=<new_value>, where <key> is a dbms.config key and <new_value> is a value. For key, replace '.' by '__' (double underscore). E.g to override default 'mirror.path' value, use 'KL_mirror__path=/new/path'\n");
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
    return buf.toString();
  }
  /**
   * Convert command-line options to a Apache Commons CLI object.
   * 
   * @param args string array from main program method
   * 
   * @return a command-line object or null. Null is returned in two case:
   * -h or -help is requested, or args parsing failed.
   */
  public static CommandLine handleArguments(String[] args, Options options, String toolName) {
    return bzh.plealog.bioinfo.util.CmdLineUtils.handleArguments(
        args, 
        options, 
        StarterUtils.getVersionProperties().getProperty("prg.app.name")+" "+toolName, 
        getFooter());
   }

}
