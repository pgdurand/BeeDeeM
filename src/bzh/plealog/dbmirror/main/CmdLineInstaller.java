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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.fetcher.PFTPLoaderDescriptor;
import bzh.plealog.dbmirror.fetcher.PFTPLoaderSystem;
import bzh.plealog.dbmirror.ui.resources.DBMSMessages;
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
 * Starting with release 4.3, software can alternatively be configured using
 * command-line arguments. For more information, see software manual or run it
 * without any arguments. To ensure backward compatibility, this software can
 * still be used as stated in the previous paragraph.
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
public class CmdLineInstaller {

  private File prepareListOfBanks(String bankListFormat){
    File f=null;
    FileOutputStream fos=null;
    
    try {
      f = File.createTempFile("bdm-list-of-banks", ".txt");
      fos = new FileOutputStream(f);
      DumpBankList dbl = new DumpBankList();
      dbl.doJob(fos, "all", bankListFormat, "?");
      fos.flush();
      fos.close();
      if (f.length()==0){
        f.delete();
        f=null;
      }
    } catch (Exception e) {
      //for now, hide this msg, not really bad
    }
    
    return f;
  }
  private void sendTerminationMail(PFTPLoaderDescriptor fDescriptor, String bankListFormat) {
    PMailer mailer;
    String host, port, sender, pswd, recp, val, desc, subject, body, dbList, logFile;
    File bankList=null;
    
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
    dbList = fDescriptor.getProperty(PFTPLoaderDescriptor.DBLIST_KEY);
    logFile=LoggerCentral.getLogAppPath()+LoggerCentral.getLogAppFileName();
    if (LoggerCentral.errorMsgEmitted()) { 
      // ERROR
      subject = new MessageFormat(DBMSMessages.getString("Tool.Install.info.msg3")).format(
          new Object[]{DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY, desc});
      body = new MessageFormat(DBMSMessages.getString("Tool.Install.info.msg4")).format(
          new Object[]{desc, dbList, logFile});
    } else if (LoggerCentral.processAborted()) { 
      // ABORT
      subject = new MessageFormat(DBMSMessages.getString("Tool.Install.info.msg5")).format(
          new Object[]{DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY, desc});
      body = new MessageFormat(DBMSMessages.getString("Tool.Install.info.msg6")).format(
          new Object[]{desc, dbList, logFile});
    } else { 
      //OK
      subject = new MessageFormat(DBMSMessages.getString("Tool.Install.info.msg7")).format(
          new Object[]{DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY, desc});
      body = new MessageFormat(DBMSMessages.getString("Tool.Install.info.msg8")).format(
          new Object[]{desc, dbList});
      bankList = prepareListOfBanks(bankListFormat);
    }
    if (bankList!=null){
      mailer.sendMail(recp,subject,body, bankList.getAbsolutePath());
      bankList.deleteOnExit();
    }
    else{
      mailer.sendMail(recp,subject,body);
    }
  }
  
  private boolean startApplication(
      String descriptorName, PFTPLoaderDescriptor fDescCmd, 
      String bankListFormat, String fof) {
    
    PFTPLoaderSystem lSystem;
    PFTPLoaderDescriptor fDesc;
    String descriptor, gdfile;
    boolean silentMode = LoggerCentral.isSilentMode();
    
    if (!silentMode) {
      Properties props = StarterUtils.getVersionProperties();
      StringBuffer buf = new StringBuffer("\n");
      buf.append(props.getProperty("prg.app.name"));
      buf.append(" ");
      buf.append(props.getProperty("prg.version"));
      buf.append(".\n");
      System.out.println(buf.toString());;
    }

    StarterUtils.configureApplication(null,
        DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY + "-" + descriptorName, true,
        false, true);

    if (!silentMode) {
      String logFile = LoggerCentral.getLogAppFileName();
      if(logFile!=null) {
        String msg = new MessageFormat(DBMSMessages.getString("Tool.Install.info.msg2")).format(
            new Object[]{LoggerCentral.getLogAppPath()+logFile});
        System.out.println(msg);
      }
    }    
    descriptor = descriptorName + DBMSAbstractConfig.FEXT_GD;

    Log LOGGER = LogFactory
        .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
            + ".PMirror");
    
    try {
      LoggerCentral.reset();
      fDesc = new PFTPLoaderDescriptor(descriptor);
      // new in v4.3.0: GD file may not be provided
      gdfile = DBMSAbstractConfig.getConfPath(Configuration.DESCRIPTOR) + descriptor;
      if (new File(gdfile).exists()) {
        fDesc.load(new FileInputStream(gdfile), true);
      }
      // new in v4.3.0: is no GD file provided, we can have cmdline arguments
      // provided in optional fDescCmd. These arguments override those from GD file.
      if (fDescCmd!=null){
        fDesc.update(fDescCmd);
      }
      // start job
      lSystem = new PFTPLoaderSystem(new PFTPLoaderDescriptor[] { fDesc });
      lSystem.setFileOfFiles(fof);
      lSystem.runProcessing();
      // send email to administrator if needed
      sendTerminationMail(fDesc, bankListFormat);
    } catch (FileNotFoundException e) {
      LoggerCentral.error(LOGGER, DBMSMessages.getString("Tool.Install.error.msg2") + descriptor);
    } catch (IOException e) {
      LoggerCentral.error(LOGGER, DBMSMessages.getString("Tool.Install.error.msg3") + descriptor);
    } catch (Exception e) {
      LoggerCentral.error(LOGGER, DBMSMessages.getString("Tool.Install.error.msg4") + e);
    }
    if (LoggerCentral.errorMsgEmitted()){
      if (!silentMode) {
        System.err.println("\n"+DBMSMessages.getString("Tool.Install.error.msg1"));
      }
      return false;
    }
    else{
      if (!silentMode) {
        System.out.println("\n"+DBMSMessages.getString("Tool.Install.info.msg1"));
      }
      return true;
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
    globalDesc = (globalDesc==null?CmdLineInstallerOptions.CMD_LINE:globalDesc);
    
    // do we have options? (second and third cases described above)
    PFTPLoaderDescriptor fDescCmd = CmdLineInstallerOptions.getDescriptorFromOptions(cmd);
    if (fDescCmd != null && globalDesc.equals(CmdLineInstallerOptions.CMD_LINE)) {
      globalDesc = fDescCmd.getDescriptorName();
    }
    // default format for report: txt (this report is only created when
    // email stuffs are used)
    String bankListFormat = "txt";
    if (cmd.hasOption(DumpBankList.FT_ARG)){
      bankListFormat = cmd.getOptionValue(DumpBankList.FT_ARG);
    }
    //when using info task-name, do we have to dump list of files
    //to download into a file 
    String fof = null;
    if (cmd.hasOption(CmdLineInstallerOptions.FOF_OPT)){
      fof = cmd.getOptionValue(CmdLineInstallerOptions.FOF_OPT);
    }
    //go, go, go...
    CmdLineInstaller mirror = new CmdLineInstaller();
    if (!mirror.startApplication(globalDesc, fDescCmd, bankListFormat, fof)) {
      System.exit(1);
    }
  }

}
