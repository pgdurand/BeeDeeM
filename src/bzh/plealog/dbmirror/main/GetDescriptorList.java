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
 */package bzh.plealog.dbmirror.main;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;

import com.plealog.genericapp.api.file.EZFileUtils;

import bzh.plealog.dbmirror.ui.resources.DBMSMessages;
import bzh.plealog.dbmirror.util.conf.Configuration;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;

/**
 * Utility tool aims at listing bank descriptors available in default
 * conf directory of BeeDeeM.
 * 
 * @author Patrick G. Durand
 */
public class GetDescriptorList {
  public static final String LS_ARG = "l";
  public static final String PT_ARG = "p";

  /**
   * Setup the valid command-line of the application.
   */
  @SuppressWarnings("static-access")
  private static Options getCmdLineOptions() {
    Options opts;

    Option list = OptionBuilder
        .withArgName( DBMSMessages.getString("Tool.DscList.arg1.lbl") )
        .withDescription( DBMSMessages.getString("Tool.DscList.arg1.desc") )
        .create( LS_ARG );
    
    Option print = OptionBuilder
        .withArgName( DBMSMessages.getString("Tool.DscList.arg2.lbl") )
        .hasArg()
        .withDescription( DBMSMessages.getString("Tool.DscList.arg2.desc") )
        .create( PT_ARG );

    opts = new Options();
    opts.addOption(list);
    opts.addOption(print);
    CmdLineUtils.setConfDirOption(opts);
    CmdLineUtils.setHelpOption(opts);
    return opts;
  }

  public static void main(String[] args) {
    // Handle command line arguments
    CommandLine cmdLine = CmdLineUtils.handleArguments(
        args, 
        getCmdLineOptions(), 
        DBMSMessages.getString("Tool.DscList.name"));
    if (cmdLine==null){
      System.exit(1);
    }
    // Configure app
    StarterUtils.configureApplication(
        null, DBMSMessages.getString("Tool.DscList.name"), 
        true, false, true);
    
    // Get list of DSC files
    String cPath = DBMSAbstractConfig.getConfPath(Configuration.DESCRIPTOR);

    // List descriptors?
    if (cmdLine.hasOption(LS_ARG)) {
      Collection<File> dscList = FileUtils.listFiles(
          new File(cPath), 
          new String[] { DBMSAbstractConfig.FEXT_DD.substring(1) }, 
          false);
      List<String> listF = new ArrayList<String>();
      for(File f : dscList) {
        listF.add(EZFileUtils.getFileName(f));
      }
      System.out.println(DBMSMessages.getString("Tool.DscList.msg3") + cPath);
      System.out.println(String.valueOf(listF.size()) + " " + DBMSMessages.getString("Tool.DscList.msg4"));
      Collections.sort(listF, String.CASE_INSENSITIVE_ORDER);
      for(String s : listF) {
        System.out.println(s);
      }
    }
    // Print the content of a given descriptor?
    else if (cmdLine.hasOption(PT_ARG)) {
      String dscName = cmdLine.getOptionValue(PT_ARG);
      String sPath = dscName + DBMSAbstractConfig.FEXT_DD;
      System.out.println(DBMSMessages.getString("Tool.DscList.msg3") + cPath);
      //System.out.println(DBMSMessages.getString("Tool.DscList.msg1") + dscName);
      System.out.println(DBMSMessages.getString("Tool.DscList.msg2") + sPath);
      try {
        System.out.println("----");
        System.out.println(EZFileUtils.getFileContent(new File(cPath+sPath)));
        System.out.println("----");
      } catch (IOException e) {
        System.err.println(e.getMessage());
        System.exit(1);
      }
    }
    
  }

}
