package bzh.plealog.dbmirror.main;

import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import bzh.plealog.dbmirror.fetcher.PFTPLoaderDescriptor;
import bzh.plealog.dbmirror.ui.resources.DBMSMessages;

/**
 * This class handles available options for the command-line installer.
 * 
 * @author Patrick G. Durand
 * */
public class CmdLineInstallerOptions {
  
  public static final String CMD_LINE = "CmdLine";
  
  private static final String DESC_OPT = "desc";
  /** Provide a correspondence between cmdline options and descriptor keys.
   * These keys are defined in bzh.plealog.dbmirror.fetcher.PFTPLoaderDescriptor.*/
  @SuppressWarnings("serial")
  private static final Hashtable<String, String> ARGUMENTS = new Hashtable<String, String>(){{
      put(DESC_OPT, "db.list"); 
      put("task", "db.main.task");
      put("force", "force.delete");
      put("td", "task.delay");
      put("fd","ftp.delay");
      put("fr", "ftp.retry");
      put("host","mail.smtp.host");
      put("port","mail.smtp.port");
      put("sender","mail.smtp.sender.mail");
      put("pswd","mail.smtp.sender.pswd");
      put("recipient","mail.smtp.recipient.mail");
      }};
  private static Options OPTIONS = null;

  /**
   * Create the command-line options.
   */
  @SuppressWarnings("static-access")
  protected static Options createOptions(){
    if (OPTIONS!=null){
      return OPTIONS;
    }
    // Options to handle this configuration:
    // bzh.plealog.dbmirror.fetcher.PFTPLoaderDescriptor
    /*
      db.list=<comma separated list of strings>
      db.main.task=<download | info>
      force.delete=<true | false>
      task.delay=1000
      ftp.delay=5000
      ftp.retry=3
      mail.smtp.host=
      mail.smtp.port=
      mail.smtp.sender.mail=
      mail.smtp.sender.pswd=
      mail.smtp.recipient.mail=
     */
    Option dbList = OptionBuilder
        .withArgName( DBMSMessages.getString("Tool.Install.arg2.lbl") )
        .hasArg()
        .withDescription( DBMSMessages.getString("Tool.Install.arg2.desc") )
        .create( DESC_OPT );

    Option task = OptionBuilder
        .withArgName( DBMSMessages.getString("Tool.Install.arg3.lbl") )
        .hasArg()
        .withDescription(  DBMSMessages.getString("Tool.Install.arg3.desc") )
        .create( "task" );

    Option resume = OptionBuilder
        .withArgName( DBMSMessages.getString("Tool.Install.arg4.lbl") )
        .hasArg()
        .withDescription( DBMSMessages.getString("Tool.Install.arg4.desc") )
        .create( "force" );
    

    Option taskDelay = OptionBuilder
        .withArgName( DBMSMessages.getString("Tool.Install.arg5.lbl") )
        .withLongOpt("task-delay")
        .hasArg()
        .withDescription( DBMSMessages.getString("Tool.Install.arg5.desc") )
        .create( "td" );
    
    Option ftpDelay = OptionBuilder
        .withArgName( DBMSMessages.getString("Tool.Install.arg6.lbl") )
        .withLongOpt("ftp-delay")
        .hasArg()
        .withDescription(  DBMSMessages.getString("Tool.Install.arg6.desc") )
        .create( "fd" );
    
    Option ftpRetry = OptionBuilder
        .withArgName( DBMSMessages.getString("Tool.Install.arg7.lbl") )
        .withLongOpt("ftp-retry")
        .hasArg()
        .withDescription( DBMSMessages.getString("Tool.Install.arg7.desc") )
        .create( "fr" );

    Option msh = OptionBuilder
        .withArgName( DBMSMessages.getString("Tool.Install.arg8.lbl") )
        .withLongOpt("mail-smtp-host")
        .hasArg()
        .withDescription(  DBMSMessages.getString("Tool.Install.arg8.desc") )
        .create( "host" );

    Option msp = OptionBuilder
        .withArgName( DBMSMessages.getString("Tool.Install.arg9.lbl") )
        .withLongOpt("mail-smtp-port")
        .hasArg()
        .withDescription( DBMSMessages.getString("Tool.Install.arg9.desc") )
        .create( "port" );
    
    Option mssm = OptionBuilder
        .withArgName( DBMSMessages.getString("Tool.Install.arg10.lbl") )
        .withLongOpt("sender-mail")
        .hasArg()
        .withDescription( DBMSMessages.getString("Tool.Install.arg10.desc") )
        .create( "sender" );

    Option mssp = OptionBuilder
        .withArgName( DBMSMessages.getString("Tool.Install.arg11.lbl") )
        .withLongOpt("sender-pswd")
        .hasArg()
        .withDescription( DBMSMessages.getString("Tool.Install.arg11.desc") )
        .create( "pswd" );

    Option recipient = OptionBuilder
        .withArgName( DBMSMessages.getString("Tool.Install.arg12.lbl") )
        .withLongOpt("recipient-mail")
        .hasArg()
        .withDescription( DBMSMessages.getString("Tool.Install.arg12.desc") )
        .create( "recipient" );
    
    Option eformat = OptionBuilder
        .withArgName( DBMSMessages.getString("Tool.Install.arg13.lbl") )
        .hasArg()
        .withDescription( DBMSMessages.getString("Tool.Install.arg13.desc") )
        .create( DumpBankList.FT_ARG );

    Options options = new Options();
    options.addOption(dbList);
    options.addOption(task);
    options.addOption(resume);
    options.addOption(taskDelay);
    options.addOption(ftpDelay);
    options.addOption(ftpRetry);
    options.addOption(msh);
    options.addOption(msp);
    options.addOption(mssm);
    options.addOption(mssp);
    options.addOption(recipient);
    options.addOption(eformat);
    CmdLineUtils.setConfDirOption(options);
    CmdLineUtils.setHelpOption(options);
    return options;
  }
  
  /**
   * Get the descriptor name to use to install databanks.
   * 
   * @param cmdline a command-line object
   * 
   * @return a descriptor name or null is nothing passed in the cmdline
   * */
  public static String getDescriptorName(CommandLine cmdline){
    // In addition to cmdline options, CmdLineInstaller accepts an optional
    // single argument: name of global descriptor, without .gd extension.
    if (cmdline.getArgList().isEmpty()==false){
      return cmdline.getArgList().get(0).toString();
    }
    else{
      return null;
    }
  }
  
  /**
   * Convert cmdline options into a PFTPLoaderDescriptor instance.
   * 
   * @param cmdline a command-line object
   * 
   * @return a PFTPLoaderDescriptor instance or null if nothing set.
   * */
  public static PFTPLoaderDescriptor getDescriptorFromOptions(CommandLine cmdline){
    PFTPLoaderDescriptor descriptor= new PFTPLoaderDescriptor(CMD_LINE);
    String               argName, keyName;
    boolean              userProvidedArgs=false;
    

    Enumeration<String> argIter = ARGUMENTS.keys();
    while(argIter.hasMoreElements()){
      argName = argIter.nextElement();
      if (cmdline.hasOption(argName)){
        userProvidedArgs=true;
        keyName = ARGUMENTS.get(argName);
        String value = cmdline.getOptionValue(argName);
        descriptor.setProperty(keyName, value);
        if (argName.equals(DESC_OPT)) {
          descriptor.setDescriptorName(value);
        }
      }
    }

    return userProvidedArgs ? descriptor : null;
  }
  
  public static CommandLine handleArguments(String[] args){
    return CmdLineUtils.handleArguments(args, createOptions(), DBMSMessages.getString("Tool.Install.name"));
  }
  
}
