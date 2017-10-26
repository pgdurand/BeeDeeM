package bzh.plealog.dbmirror.main;

import java.text.MessageFormat;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import bzh.plealog.dbmirror.ui.resources.DBMSMessages;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.conf.DBMirrorConfig;
import bzh.plealog.dbmirror.util.conf.DeleteBankHandler;
import bzh.plealog.dbmirror.util.conf.DeleteBankUtility;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptorUtils;
import bzh.plealog.dbmirror.util.descriptor.IdxDescriptor;
import bzh.plealog.dbmirror.util.runner.DBMSExecNativeCommand;

/**
 * A utility tool to delete bank from he command-line. Command-line arguments are:<br>
 * 
 * -code <bank-code>: index code of the bank to delete. Such a code can be obtained 
 * using the 'info' tool (use 'code' format).<br>
 * -info: display bank directory to be deleted WITHOUT deleting it! <br>
 * 
 * In addition, some parameters can be passed to the JVM for special
 * configuration purposes:<br>
 * -DKL_HOME=an_absolute_path ; the absolute path to the KDMS installation home
 * dir. If not set, use user.dir java property. -DKL_DEBUG=true ; if true, if
 * set, log will be in debug mode<br>
 * -DKL_WORKING_DIR=an_absolute_path ; if not set, log and working directories
 * are set to java.io.tmp<br>
 * -DKL_CONF_DIR=an_absolute_path ; the absolute path to a home-made conf directory. 
 * If not set, use ${user.dir}/conf.
 * -DKL_LOG_FILE=a_file_name ; if set, creates a log file with that name within
 * KL_WORKING_DIR<br>
 * <br>
 * 
 * @author Patrick G. Durand
 * */
public class DeleteBank {

  private static final String CODE_ARG = "code";
  private static final String INFO_ARG = "info";

  /**
   * Setup the valid command-line of the application.
   */
  @SuppressWarnings("static-access")
  private static Options getCmdLineOptions() {
    Options opts;

    Option idx = OptionBuilder
        .withArgName(DBMSMessages.getString("Tool.DeleteBank.arg1.lbl"))
        .hasArg()
        .isRequired()
        .withDescription(DBMSMessages.getString("Tool.DeleteBank.arg1.desc"))
        .create( CODE_ARG );

    opts = new Options();
    opts.addOption(idx);
    opts.addOption(new Option( 
        INFO_ARG, 
        DBMSMessages.getString("Tool.DeleteBank.arg2.desc")));
    CmdLineUtils.setConfDirOption(opts);
    CmdLineUtils.setHelpOption(opts);
    return opts;

  }

  public static void main(String[] args) {
    CommandLine cmdLine;
    List<IdxDescriptor> descriptors;
    IdxDescriptor desc=null;
    String dbCode;
    
    MessageFormat formatter = new MessageFormat(
        DBMSMessages.getString("Tool.DeleteBank.info.msg1"));
    
    // Configure software
    StarterUtils.configureApplication(
        null, 
        DBMSMessages.getString("Tool.DeleteBank.name"), 
        true, false, true);

    // Handle command-line
    cmdLine = CmdLineUtils.handleArguments(
        args, 
        getCmdLineOptions(), 
        DBMSMessages.getString("Tool.DeleteBank.name"));
    if (cmdLine==null){
      System.exit(1);
    }
    
    // Load the banks list
    String dbMirrorConfFile = DBMSAbstractConfig.getLocalMirrorConfFile();
    DBMirrorConfig conf = DBDescriptorUtils.getDBMirrorConfig(dbMirrorConfFile);
    descriptors = DBDescriptorUtils.prepareIndexDBList(conf);

    // Locate the bank to delete
    dbCode = cmdLine.getOptionValue(CODE_ARG);
    for (IdxDescriptor idx : descriptors){
      if (idx.getKbCode().equals(dbCode)){
        desc = idx;
        break;
      }
    }
    if (desc == null){
      String msg = new MessageFormat(DBMSMessages.getString("Tool.DeleteBank.err.msg1")).format(
          new Object[]{dbCode});
      System.err.println(msg);
      System.exit(1);
    }

    // Do we have to only display information about bank to be deleted ?
    if (cmdLine.hasOption(INFO_ARG)){
      boolean osWin = DBMSExecNativeCommand.getOSType() == DBMSExecNativeCommand.WINDOWS_OS;
      String  path = desc.getCode();
      int     idx = path.indexOf(DBMSAbstractConfig.CURRENT_DIR);
      if (idx != -1) {
        path = path.substring(0, idx);
      }
      String dbList = DeleteBankUtility.getPotentialyDeletedBanks(descriptors, path, osWin);
      System.out.println(formatter.format(new Object[] { path, dbList }));
      System.exit(0);
    }
    
    //Otherwise, do the bank deletion!
    if (!DeleteBankUtility.deleteBank(descriptors, desc, new MyDeleteBankHandler())){
      System.err.println(DBMSMessages.getString("InstalledDescriptorList.msg10"));
      System.exit(1);
    }
    else{
      String msg = new MessageFormat(DBMSMessages.getString("Tool.DeleteBank.info.msg2")).format(
          new Object[]{desc.getName()});
      System.out.println(msg);
    }
  }

  private static class MyDeleteBankHandler implements DeleteBankHandler {
    @Override
    public boolean confirmPersonalBankDeletion() {
      return true;
    }

    @Override
    public boolean confirmBankDeletion(String path, String deletedDbs) {
      return true;
    }

    @Override
    public void cannotDeleteBank() {
      System.err.println(DBMSMessages.getString("InstalledDescriptorList.msg10"));
    }
    
  }

}
