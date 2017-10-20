package bzh.plealog.dbmirror.main;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import bzh.plealog.dbmirror.fetcher.PFTPLoaderDescriptor;

/**
 * This class handles available options for the command-line installer.
 * 
 * @author Patrick G. Durand
 * */
public class CmdLineInstallerOptions {
  /** Provide a correspondence between cmdline options and descriptor keys.
   * These keys are defined in bzh.plealog.dbmirror.fetcher.PFTPLoaderDescriptor.*/
  private static final String[] ARGUMENTS = {
      "desc", "db.list", 
      "task", "db.main.task",
      "resume", "resume.date",
      "td", "task.delay",
      "fd","ftp.delay",
      "fr", "ftp.retry",
      "host","mail.smtp.host",
      "port","mail.smtp.port",
      "sender","mail.smtp.sender.mail",
      "pswd","mail.smtp.sender.pswd",
      "recipient","mail.smtp.recipient.mail"
      };
  private static final String TOOL_NAME = "Command-line installer" ;
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
      resume.date=<none | YYYYMMdd>
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
        .withArgName( "descriptor" )
        .hasArg()
        .withDescription(  "comma separated list of descriptor (.dsc) names" )
        .create( "desc" );
    Option task = OptionBuilder
        .withArgName( "task-name" )
        .hasArg()
        .withDescription(  "one of: download, info" )
        .create( "task" );
    Option resume = OptionBuilder
        .withArgName( "date" )
        .hasArg()
        .withDescription(  "one of: none, YYYYMMdd" )
        .create( "resume" );
    Option taskDelay = OptionBuilder
        .withArgName( "delay" )
        .withLongOpt("task-delay")
        .hasArg()
        .withDescription(  "time value in milliseconds between consecutive task execution" )
        .create( "td" );
    Option ftpDelay = OptionBuilder
        .withArgName( "delay" )
        .withLongOpt("ftp-delay")
        .hasArg()
        .withDescription(  "time value in milliseconds between consecutive FTP file retrieval" )
        .create( "fd" );
    Option ftpRetry = OptionBuilder
        .withArgName( "retry" )
        .withLongOpt("ftp-retry")
        .hasArg()
        .withDescription(  "number of times to retry FTP file retrieval" )
        .create( "fr" );
    
    Option msh = OptionBuilder
        .withArgName( "host" )
        .withLongOpt("mail-smtp-host")
        .hasArg()
        .withDescription(  "smtp server host name or IP" )
        .create( "host" );
    Option msp = OptionBuilder
        .withArgName( "port" )
        .withLongOpt("mail-smtp-port")
        .hasArg()
        .withDescription(  "smtp server port" )
        .create( "port" );
    Option mssm = OptionBuilder
        .withArgName( "email" )
        .withLongOpt("sender-mail")
        .hasArg()
        .withDescription(  "email of the sender" )
        .create( "sender" );
    Option mssp = OptionBuilder
        .withArgName( "pswd" )
        .withLongOpt("sender-pswd")
        .hasArg()
        .withDescription(  "password of the sender" )
        .create( "pswd" );
    Option recipient = OptionBuilder
        .withArgName( "email" )
        .withLongOpt("recipient-mail")
        .hasArg()
        .withDescription(  "recipient of the email" )
        .create( "recipient" );
    
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
    options.addOption(CmdLineUtils.getConfDirOption());
    String msg = "print this message";
    options.addOption(new Option( "help", msg ));
    options.addOption(new Option( "h", msg ));
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
    PFTPLoaderDescriptor descriptor;
    String               argName, keyName;
    
    descriptor = new PFTPLoaderDescriptor("CmdLine");
    
    List<String> argList = Arrays.asList(ARGUMENTS);
    Iterator<String> argIter = argList.iterator();
    while(argIter.hasNext()){
      argName = argIter.next();
      if (cmdline.hasOption(argName)){
        keyName = argIter.next();
        descriptor.setProperty(keyName, cmdline.getOptionValue(argName));
      }
    }
    if (descriptor.getProperties().isEmpty()){
      descriptor = null;
    }
    return descriptor;
  }
  
  public static CommandLine handleArguments(String[] args){
    return CmdLineUtils.handleArguments(args, null, createOptions(), TOOL_NAME);
  }
  
}
