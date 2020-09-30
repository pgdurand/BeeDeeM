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
    buf.append("Default log file: ");
    buf.append(DBMSAbstractConfig.getLogAppPath()+DBMSAbstractConfig.getLogAppFileName());
    buf.append("\n  (to redirect Log file, use JRE args: -DKL_WORKING_DIR=/my-path -DKL_LOG_FILE=my-file.log)\n");
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
