package bzh.plealog.dbmirror.main;

import java.io.File;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import com.plealog.genericapp.api.file.EZFileExt;
import com.plealog.genericapp.api.file.EZFileFilter;
import com.plealog.genericapp.api.file.EZFileManager;
import com.plealog.genericapp.api.file.EZFileUtils;

import bzh.plealog.dbmirror.ui.resources.DBMSMessages;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.descriptor.DatabankFormat;
import bzh.plealog.dbmirror.util.sequence.SequenceFileManager;
import bzh.plealog.dbmirror.util.sequence.SequenceValidatorCutFile;

public class CmdLineCutter {
  private static final String FROM_ARG = "f";
  private static final String TO_ARG = "t";
  private static final String PART_ARG = "p";
  private static final String FILE_ARG = "i";
  private static final String FORMAT_ARG = "k";
  private static Hashtable<String, DatabankFormat> formats;
  
  static {
    formats = new Hashtable<>();
    formats.put("fa", DatabankFormat.fasta);
    formats.put("fq", DatabankFormat.fastQ);
    formats.put("gb", DatabankFormat.genbank);
    formats.put("em", DatabankFormat.swissProt);
  }
  
  /**
   * Setup the valid command-line of the application.
   */
  @SuppressWarnings("static-access")
  private static Options getCmdLineOptions() {
    Options opts;
   
    Option part = OptionBuilder
        .withArgName( DBMSMessages.getString("Tool.Cutter.arg1.lbl") )
        .hasArg()
        .withDescription( DBMSMessages.getString("Tool.Cutter.arg1.desc") )
        .create(PART_ARG);
    Option from = OptionBuilder
        .withArgName( DBMSMessages.getString("Tool.Cutter.arg2.lbl") )
        .hasArg()
        .withDescription( DBMSMessages.getString("Tool.Cutter.arg2.desc") )
        .create(FROM_ARG);
    Option to = OptionBuilder
        .withArgName( DBMSMessages.getString("Tool.Cutter.arg3.lbl") )
        .hasArg()
        .withDescription( DBMSMessages.getString("Tool.Cutter.arg3.desc") )
        .create(TO_ARG);
    Option file = OptionBuilder
        .withArgName( DBMSMessages.getString("Tool.Cutter.arg4.lbl") )
        .isRequired()
        .hasArg()
        .withDescription( DBMSMessages.getString("Tool.Cutter.arg4.desc") )
        .create(FILE_ARG);
    Option format = OptionBuilder
        .withArgName( DBMSMessages.getString("Tool.Cutter.arg5.lbl") )
        .hasArg()
        .withDescription( DBMSMessages.getString("Tool.Cutter.arg5.desc") )
        .create(FORMAT_ARG);

    opts = new Options();
    opts.addOption(part);
    opts.addOption(from);
    opts.addOption(to);
    opts.addOption(file);
    opts.addOption(format);
    //CmdLineUtils.setConfDirOption(opts);
    return opts;
  }

  private static boolean cutFile(String sequenceFile, DatabankFormat format, int from, int to) {
    boolean bRet = true;
    try{
      SequenceFileManager sfm = new SequenceFileManager(sequenceFile, format, null, null); 
      SequenceValidatorCutFile validator = new SequenceValidatorCutFile(from, to);
      sfm.addValidator(validator);
      List<File> filteredFiles = sfm.execute();
      File filteredFile = filteredFiles.get(0);
      File sourceFile = new File(sequenceFile);
      String fName = sourceFile.getName();
      String path = sourceFile.getParent();
      String f = from==-1?"1":String.valueOf(from);
      String t = to==-1?"end":String.valueOf(to);
      String resultFile = Utils.terminatePath(path);
      resultFile+=String.format("%s_%s-%s", fName, f, t);
      System.out.println("Created file: "+resultFile);
      filteredFile.renameTo(new File(resultFile));
    }
    catch(Exception ex){
      System.err.println("ERROR: unable to cut file: "+ex.toString());
      bRet = false;
    }
    return bRet;
  }

  private static int getValue(String val){
    if (val==null){
      return -1;
    }
    else{
      return Integer.valueOf(val);
    }
  }

  public static boolean doJob(String[] args){
    CommandLine cmdLine;
    String msg, toolName, part, from, to, file, format;
    int ipart, ifrom, ito;
    Options options;
    DatabankFormat dbFormat;
    boolean bRet = true;
    
    toolName = DBMSMessages.getString("Tool.Cutter.name");

    // prepare the Logging system
    StarterUtils.configureApplication(null, toolName, true, false, true, false);
    
    // handle the command-line
    options = getCmdLineOptions();
    cmdLine = CmdLineUtils.handleArguments(args, options, toolName);
    if (cmdLine == null) {
      System.exit(1);
    }

    part = cmdLine.getOptionValue(PART_ARG);
    from = cmdLine.getOptionValue(FROM_ARG);
    to = cmdLine.getOptionValue(TO_ARG);
    file = cmdLine.getOptionValue(FILE_ARG);
    format = cmdLine.getOptionValue(FORMAT_ARG);
    
    // add additional controls on cmdline values
    if (part!=null && (from!=null||to!=null)){
      System.err.println("ERROR: 'part' cannot be used with 'from/to'");
      return false;
    }
    // get input file format
    if (format==null){
      dbFormat=DatabankFormat.fasta;//default is Fasta
    }
    else{
      dbFormat = formats.get(format);
      if (dbFormat==null){
        msg = String.format("ERROR: format %s is unknown. Use one of: %s", 
            format,
            formats.keySet().toString());
        System.err.println(msg);
        return false;
      }
    }
    
    // convert Str to int
    ipart = getValue(part);
    ifrom = getValue(from);
    ito = getValue(to);

    // prepare a message for the user
    if (ipart!=-1){
      msg = String.format("Cut %s into %d parts", file, ipart);
    }
    else if (ifrom!=-1 && ito!=-1){
      msg = String.format("Get sequences [%d..%d] from %s", ifrom, ito, file);
    }
    else if (ito!=-1){
      msg = String.format("Get sequences [1..%d] from %s", ito, file);
    }
    else{
      msg = String.format("Get sequences [%d..end] from %s", ifrom, file);
    }
    System.out.println(msg);
    
    // compute new file
    if (ipart==-1){
      bRet = cutFile(file, dbFormat, ifrom, ito);
    }
    else{
      System.err.println("ERROR: 'part' not yet implemented... sorry!");
      bRet = false;
    }
    return bRet;
  }
  public static void main(String[] args) {
    if (!doJob(args)){
      System.exit(1);// exit code=1 : do this to report error to calling app
    }
  }
}
