package bzh.plealog.dbmirror.main;

import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;

public class CmdLineUtils {
  
  /**
   * Prepare an option to deal with configuration path.
   */
  @SuppressWarnings("static-access")
  protected static void setConfDirOption(Options opts){
    Option confDir = OptionBuilder
        .withArgName("directory" )
        .withLongOpt("conf-dir")
        .hasArg()
        .withDescription("absolute path to custom conf directory" )
        .create();
    opts.addOption(confDir);
  }
  /**
   * Prepare an option to deal with help.
   */
  protected static void setHelpOption(Options opts){
    String msg = "print this message";
    opts.addOption(new Option( "help", msg ));
    opts.addOption(new Option( "h", msg ));
  }

  /**
   * Handle the help message.
   */
  protected static void printUsage(String toolName, Options opt) {
    // Get version info
    Properties props = StarterUtils.getVersionProperties();
    StringBuffer buf = new StringBuffer("\n");
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
    
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp(
        props.getProperty("prg.app.name")+" "+toolName,
        "tool [options]", 
        opt, 
        buf.toString());
  }

  /**
   * Convert command-line options to a Apache Commons CLI object.
   * 
   * @param args string array from main program method
   * 
   * @return a command-line object or null. Null is returned in two case:
   * -h or -help is requested, or args parsing failed.
   */
  protected static CommandLine handleArguments(String[] args, Options options, String toolName) {
    GnuParser parser;
    CommandLine line = null;

    try {
      parser = new GnuParser();
      line = parser.parse(options, args, true);
    } catch (Exception exp) {
      System.err.println(exp.getMessage());
      printUsage(toolName, options);
      line = null;
    }
    
    if(line!=null){ 
      //--conf-dir is a shortcut to JVM argument -DKL_CONF_DIR=a-path
      if (line.hasOption( "conf-dir" ) ){
        DBMSAbstractConfig.setConfPath(DBMSAbstractConfig.pruneQuotes(line.getOptionValue("conf-dir")));
      }
      if ( line.hasOption( "help" ) ||  line.hasOption( "h" ) || 
          ( line.getArgList().isEmpty() && line.getOptions().length==0 ) ){
        // Initialize the member variable
        printUsage(toolName, options);
        line = null;
      }
    }
    return line;
  }

}
