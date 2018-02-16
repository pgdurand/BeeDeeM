package bzh.plealog.dbmirror.main;

import java.util.Map;
import java.util.Properties;
import java.util.Map.Entry;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import com.plealog.genericapp.api.EZEnvironment;

import bzh.plealog.dbmirror.ui.resources.DBMSMessages;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;

public class CmdLineUtils {
  private static final String HELP_KEY = "help";
  private static final String H_KEY    = "h";
  private static final String CONFDIR_KEY = "conf-dir";
  
  /**
   * Prepare an option to deal with configuration path.
   */
  @SuppressWarnings("static-access")
  protected static void setConfDirOption(Options opts){
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
  protected static void setHelpOption(Options opts){
    String msg = DBMSMessages.getString("Tool.Utils.info.msg1" );
    opts.addOption(new Option( HELP_KEY, msg ));
    opts.addOption(new Option( H_KEY, msg ));
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
      if (line.hasOption( CONFDIR_KEY ) ){
        DBMSAbstractConfig.setConfPath(DBMSAbstractConfig.pruneQuotes(line.getOptionValue(CONFDIR_KEY)));
      }
      if ( line.hasOption( HELP_KEY ) ||  line.hasOption( H_KEY ) || 
          ( line.getArgList().isEmpty() && line.getOptions().length==0 ) ){
        // Initialize the member variable
        printUsage(toolName, options);
        line = null;
      }
    }
    return line;
  }
  /**
   * Replace environment variable names by their values.
   * 
   * @param text a file path that may contain env var, e.g. $HOME/my-file.txt
   * 
   * @return an update file path, e.g. /Users/pgdurand/my-file.txt
   */
  protected static String expandEnvVars(String text) {
    Map<String, String> envMap = System.getenv();
    for (Entry<String, String> entry : envMap.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      if (EZEnvironment.getOSType()==EZEnvironment.WINDOWS_OS) {
        text = text.replaceAll("\\%" + key + "\\%", value);
      }
      else {
        text = text.replaceAll("\\$\\{" + key + "\\}", value);
        text = text.replaceAll("\\$" + key + "", value);
      }
    }
    return text;
  }

}
