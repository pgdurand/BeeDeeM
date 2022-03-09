/* Copyright (C) 2007-2022 Patrick G. Durand
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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.plealog.genericapp.api.file.EZFileUtils;

import bzh.plealog.dbmirror.fetcher.PFTPLoaderDescriptor;
import bzh.plealog.dbmirror.fetcher.PFTPLoaderSystem;
import bzh.plealog.dbmirror.ui.resources.DBMSMessages;
import bzh.plealog.dbmirror.util.conf.Configuration;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * This is the class to use to check whether or not descriptor files
 * are still ok.<br>
 * <br>
 * The program may be configured using an environment variable: KDMS_CONF_DIR.
 * This variable should declare the absolute path to the configuration directory
 * of KDMS. If that variable is not declared, then the application locates the
 * configuration directory as a sub-directory (i.e. conf) of the directory
 * obtained from Java Preference user.dir variable.<br>
 * <br>
 * In addition, some parameters can be passed to the JVM for special
 * configuration purposes:<br>
 * -DKL_HOME=an_absolute_path ; the absolute path to the KDMS installation home
 * dir. If not set, use user.dir java property. -DKL_DEBUG=true ; if true, if
 * set, log will be in debug mode<br>
 * -DKL_WORKING_DIR=an_absolute_path ; if not set, log and working directories
 * are set to java.io.tmp<br>
 * -DKL_CONF_DIR=an_absolute_path ; the absolute path to a custom conf directory. 
 * If not set, use ${user.dir}/conf.
 * -DKL_LOG_FILE=a_file_name ; if set, creates a log file with that name within
 * KL_WORKING_DIR<br>
 * <br>
 * 
 * @author Patrick G. Durand
 */
public class AutoCheckDescriptors {
  private static final Log LOGGER = LogFactory.getLog(
      DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY + ".DSCChecker");
  private static final String CODE_ARG = "dsc";
  /**
   * Setup the valid command-line of the application.
   */
  @SuppressWarnings("static-access")
  private static Options getCmdLineOptions() {
    Options opts;

    Option dsc = OptionBuilder
        .withArgName(DBMSMessages.getString("Tool.AutoChecker.arg1.lbl"))
        .hasArg()
        .withDescription(DBMSMessages.getString("Tool.AutoChecker.arg1.desc"))
        .create( CODE_ARG );

    opts = new Options();
    opts.addOption(dsc);
    CmdLineUtils.setConfDirOption(opts);
    CmdLineUtils.setHelpOption(opts);
    return opts;
  }
  /**
   * Dump short message on System-out at startup.
   */
  private static void dumpStarterMessage(){
    Properties props = StarterUtils.getVersionProperties();
    StringBuffer buf = new StringBuffer("\n");
    buf.append(props.getProperty("prg.app.name"));
    buf.append(" ");
    buf.append(props.getProperty("prg.version"));
    buf.append(".\n");
    System.out.println(buf.toString());
    
    String msg = new MessageFormat(DBMSMessages.getString("Tool.Install.info.msg2")).format(
        new Object[]{LoggerCentral.getLogAppPath()+LoggerCentral.getLogAppFileName()});
    System.out.println(msg);
  }
  /**
   * Check a descriptor.
   */
  private boolean checkDescriptor(PFTPLoaderDescriptor fDescCmd) {
    PFTPLoaderSystem lSystem;
    PFTPLoaderDescriptor desc;
    
    try {
      LoggerCentral.reset();
      desc = new PFTPLoaderDescriptor(fDescCmd.getDescriptorName());
      desc.update(fDescCmd);
      lSystem = new PFTPLoaderSystem(new PFTPLoaderDescriptor[] { desc });
      lSystem.runProcessing();
    } catch (Exception e) {
      LoggerCentral.error(LOGGER, DBMSMessages.getString("Tool.Install.error.msg4") + e);
    }
    if (LoggerCentral.errorMsgEmitted()){
      return false;
    }
    else{
      return true;
    }
  }

  /**
   * Start the program. 
   */
  public static void main(String[] args) {
    // Handle command-line
    CommandLine cmdLine = CmdLineUtils.handleArguments(
        args, 
        getCmdLineOptions(), 
        DBMSMessages.getString("Tool.AutoChecker.name"));
    if (cmdLine==null){
      System.exit(1);
    }
    // Configure app
    StarterUtils.configureApplication(
        null, DBMSMessages.getString("Tool.AutoChecker.name"), 
        true, false, true);
    
    // Get list of DSC files
    String cPath = DBMSAbstractConfig.getConfPath(Configuration.DESCRIPTOR);
    
    String descriptors = cmdLine.getOptionValue(CODE_ARG);
    
    Collection<File> dscList;
    
    if ("all".equalsIgnoreCase(descriptors)){
      dscList = FileUtils.listFiles(
          new File(cPath), 
          new String[] { DBMSAbstractConfig.FEXT_DD.substring(1) }, 
          false);
    }
    else{
      StringTokenizer tokenizer = new StringTokenizer(descriptors, ",");
      dscList = new ArrayList<>();
      while(tokenizer.hasMoreTokens()){
        String token = tokenizer.nextToken();
        dscList.add(new File(cPath+token+DBMSAbstractConfig.FEXT_DD));
      }
    }
    
    //Check all DSC files located in a conf directory
    PFTPLoaderDescriptor descriptor;
    AutoCheckDescriptors mirror = new AutoCheckDescriptors();
    dumpStarterMessage();
    String msg = new MessageFormat(DBMSMessages.getString("Tool.AutoChecker.info.msg1")).format(
        new Object[]{cPath});
    System.out.println();
    System.out.println(msg);
    System.out.println();
    for(File f : dscList){
      if (f.isDirectory())
        continue;
      String fName = EZFileUtils.getFileName(f);
      descriptor = PFTPLoaderDescriptor.create("false", PFTPLoaderDescriptor.MAINTASK_INFO);
      descriptor.setProperty(PFTPLoaderDescriptor.DBLIST_KEY, fName);
      msg = new MessageFormat(DBMSMessages.getString("Tool.AutoChecker.info.msg2")).format(
          new Object[]{fName});
      System.out.println(msg);
      if (mirror.checkDescriptor(descriptor)){
        System.out.println("          OK");
      }
      else{
        System.out.println("          FAILED");
      }
    }
    System.out.println("\nJob done");
  }

}
