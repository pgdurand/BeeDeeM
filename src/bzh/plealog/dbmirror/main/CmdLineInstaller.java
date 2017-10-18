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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.fetcher.PFTPLoaderDescriptor;
import bzh.plealog.dbmirror.fetcher.PFTPLoaderSystem;
import bzh.plealog.dbmirror.util.conf.Configuration;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;
import bzh.plealog.dbmirror.util.mail.PMailer;

/**
 * This is the class to use to start databank installation from the command
 * line. The program takes a single argument, a string, being a global
 * descriptor file name without its extension. That file is supposed to be
 * located within the conf directory of the application.<br>
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
 * -DKL_LOG_FILE=a_file_name ; if set, creates a log file with that name within
 * KL_WORKING_DIR<br>
 * <br>
 * 
 * @author Patrick G. Durand
 */
public class CmdLineInstaller {

  public static final String  ERR_DESC_MISSING = "Missing directives file. Processus aborted.";
  private static final String ERR1             = "File not found: ";
  private static final String ERR2             = "Unable to read file: ";
  private static final String ERR3             = "Unexpected error: ";
  private static final Log    LOGGER           = LogFactory
                                                   .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                       + ".PMirror");

  private void sendTerminationMail(PFTPLoaderDescriptor fDescriptor) {
    PMailer mailer;
    String host, port, sender, pswd, recp, val, desc;

    host = fDescriptor.getProperty(PFTPLoaderDescriptor.MAILER_HOST);
    if (host == null || host.length() == 0)
      return;
    sender = fDescriptor.getProperty(PFTPLoaderDescriptor.MAILER_SENDER);
    if (sender == null || sender.length() == 0)
      return;
    recp = fDescriptor.getProperty(PFTPLoaderDescriptor.MAILER_RECP);
    if (recp == null || recp.length() == 0)
      return;
    mailer = new PMailer(host, sender);
    port = fDescriptor.getProperty(PFTPLoaderDescriptor.MAILER_PORT);
    if (port != null && port.length() != 0)
      mailer.setSmtpPort(Integer.valueOf(port));
    pswd = fDescriptor.getProperty(PFTPLoaderDescriptor.MAILER_PSWD);
    if (pswd != null && pswd.length() != 0)
      mailer.setPassword(pswd);
    val = fDescriptor.getProperty(PFTPLoaderDescriptor.MAILER_DEBUG);
    if (val != null && val.equals("true"))
      mailer.setDebug(true);
    desc = fDescriptor.getDescriptorName();
    if (LoggerCentral.errorMsgEmitted()) {
      mailer.sendMail(recp, "[" + DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
          + "] - processing " + desc + ": ERROR", "Processing of " + desc
          + " emitted warnings. Please check KDMS log files.");
    } else if (LoggerCentral.processAborted()) {
      mailer.sendMail(recp, "[" + DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
          + "] - processing " + desc + ": STOP", "Processing of " + desc
          + " has been canceled. Please check KDMS log files.");
    } else {
      mailer.sendMail(recp, "[" + DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
          + "] - processing " + desc + ": OK", "Processing of " + desc
          + " is successful. Databases are now in production.");
    }
  }
  private void dumpStarterMessage(){
    Properties props = StarterUtils.getVersionProperties();
    StringBuffer buf = new StringBuffer("\n");
    buf.append(props.getProperty("prg.app.name"));
    buf.append(" ");
    buf.append(props.getProperty("prg.version"));
    buf.append(".\n");
    System.out.println(buf.toString());
    System.out.println("Log file is: "+DBMSAbstractConfig.getLogAppPath()+DBMSAbstractConfig.getLogAppFileName());
    System.out.println("             check out this file to get processing details...");
  }
  private void startApplication(String descriptorName, PFTPLoaderDescriptor fDescCmd) {
    PFTPLoaderSystem lSystem;
    PFTPLoaderDescriptor fDesc;
    String descriptor;

    StarterUtils.configureApplication(null,
        DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY + "-" + descriptorName, true,
        false, true);
    dumpStarterMessage();
    
    descriptor = descriptorName + DBMSAbstractConfig.FEXT_GD;

    try {
      LoggerCentral.reset();
      fDesc = new PFTPLoaderDescriptor(descriptor);
      fDesc.load(new FileInputStream(DBMSAbstractConfig.getOSDepConfPath(Configuration.DESCRIPTOR)
          + descriptor), true);
      if (fDescCmd!=null){
        fDesc.update(fDescCmd);
      }
      lSystem = new PFTPLoaderSystem(new PFTPLoaderDescriptor[] { fDesc });
      lSystem.runProcessing();
      // send email to administrator if needed
      sendTerminationMail(fDesc);
    } catch (FileNotFoundException e) {
      LoggerCentral.error(LOGGER, ERR1 + descriptor);
    } catch (IOException e) {
      LoggerCentral.error(LOGGER, ERR2 + descriptor);
    } catch (Exception e) {
      LoggerCentral.error(LOGGER, ERR3 + e);
    }
    if (LoggerCentral.errorMsgEmitted()){
      System.out.println("Failed: review log file");
    }
    else{
      System.out.println("SUCCESS");
    }
  }

  /**
   * Expect either a single argument, such an argument and some options or
   * some options only.<br/><br/>
   * 
   * First case: argument is the name of a global descriptor file. Such a file must have 
   * the extension gd (stands for Global Descriptor) and must be located within the 
   * OS-dependent conf path of the application. Pass the file name without its extension.
   * <br/><br/>
   * Second case: argument is same as above. One can also pass in some additional
   * options that will override declarations of the global descriptor.
   * <br/><br/>
   * Third case: use command-line options only to define what have to be installed.
   * <br/><br/>
   * Options are defined in CmdLineInstallerOptions utility class. Use -h or -help
   * option to get software command-line description.
   */
  public static void main(String[] args) {
    // convert the array of strings into an appropriate object
    CommandLine cmd = CmdLineInstallerOptions.handleArguments(args);
    
    // nothing to do, exit!
    if (cmd==null){
      return;
    }
    
    // do we have a global descriptor name? (first and second cases described above)
    String globalDesc = CmdLineInstallerOptions.getDescriptorName(cmd);
    globalDesc = (globalDesc==null?"CmdLine":globalDesc);
    
    // do we have options? (second and third cases described above)
    PFTPLoaderDescriptor fDescCmd = CmdLineInstallerOptions.getDescriptorFromOptions(cmd);
    
    //go, go, go...
    CmdLineInstaller mirror = new CmdLineInstaller();
    mirror.startApplication(globalDesc, fDescCmd);
  }

}
