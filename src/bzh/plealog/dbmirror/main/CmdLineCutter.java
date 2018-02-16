/* Copyright (C) 2007-2018 Patrick G. DUrand
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
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import bzh.plealog.dbmirror.ui.resources.DBMSMessages;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.descriptor.DatabankFormat;
import bzh.plealog.dbmirror.util.sequence.SequenceFileManager;
import bzh.plealog.dbmirror.util.sequence.SequenceValidatorCutFile;

/**
 * A utility class to cut sequence file.<br><br>
 * 
 * Sample use: CmdLineCutter -i tests/junit/databank/fasta_prot/uniprot.faa -f 3<br>
 *             to get 3rd sequence up to the end of input file<br>
 *             Note: environment variables are accepted in file path.<br>
 *             
 * @author Patrick G. Durand
 * */
public class CmdLineCutter {
  // from: start of a slice (one-based)
  // if not provided: start from 1
  private static final String                      FROM_ARG   = "f";
  // to: end of a slice  (one-based)
  // if not provided: stop at file end
  private static final String                      TO_ARG     = "t";
  // part: nb of sequences for a single slice
  // if not provided: must use from/to
  private static final String                      PART_ARG   = "p";
  // input sequence file
  private static final String                      FILE_ARG   = "i";
  // input sequence file format
  private static final String                      FORMAT_ARG = "k";
  // where to create the resulting file?
  // if not provided: place the sliced file next to input sequence file
  private static final String                      DIR_ARG    = "d";
  
  // a convenient mapping to DatabankFormat format names. 
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
    Option res_dir = OptionBuilder
        .withArgName( DBMSMessages.getString("Tool.Cutter.arg6.lbl") )
        .hasArg()
        .withDescription( DBMSMessages.getString("Tool.Cutter.arg6.desc") )
        .create(DIR_ARG);

    opts = new Options();
    opts.addOption(part);
    opts.addOption(from);
    opts.addOption(to);
    opts.addOption(file);
    opts.addOption(format);
    opts.addOption(res_dir);
    CmdLineUtils.setHelpOption(opts);
    return opts;
  }

  private static boolean cutFile(String sequenceFile, String resultDir, DatabankFormat format, int from, int to) {
    boolean bRet = true;
    
    sequenceFile = CmdLineUtils.expandEnvVars(sequenceFile);
    if (new File(sequenceFile).exists() == false) {
      String msg = String.format(DBMSMessages.getString("Tool.Cutter.msg9"), sequenceFile);
      System.err.println(msg);
      return false;
    }
    try{
      // create the sequence manager object
      SequenceFileManager sfm = new SequenceFileManager(sequenceFile, format, null, null);
      if (resultDir!=null) {
        sfm.setTmpFileDirectory(resultDir);
      }
      else {
        String tmpDir = new File(sequenceFile).getParent();
        sfm.setTmpFileDirectory(tmpDir);
      }
      
      // sequence validator is a cutter
      SequenceValidatorCutFile validator = new SequenceValidatorCutFile(from, to);
      sfm.addValidator(validator);
      List<File> filteredFiles = sfm.execute();
      
      // get the single result: a file created by the sequence manager
      File filteredFile = filteredFiles.get(0);
      // we rename it using from-to values
      String sourceFileName = new File(sequenceFile).getName();
      // get file name and extension in separate strings
      int idx = sourceFileName.lastIndexOf('.');
      String fName = sourceFileName;
      String fExt = "";
      if (idx!=-1) {
        fName = sourceFileName.substring(0, idx);
        fExt = "."+sourceFileName.substring(idx+1);
      }
      // do we have a path ?
      String path = filteredFile.getParent();
      // prepare elements to be used to rename file
      String f = from==-1?"1":String.valueOf(from);
      String t = to==-1?"end":String.valueOf(to);
      String resultFile = path!=null?Utils.terminatePath(path):"";
      resultFile+=String.format("%s_%s-%s%s", fName, f, t, fExt);
      // log a little message
      String msg = String.format(DBMSMessages.getString("Tool.Cutter.msg1"), resultFile);
      System.out.println(msg);
      // rename !
      filteredFile.renameTo(new File(resultFile));
    }
    catch(Exception ex){
      String msg = String.format(DBMSMessages.getString("Tool.Cutter.msg2"), ex.toString());
      System.err.println(msg);
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
    String msg, toolName, part, from, to, file, format, resultDir;
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
      return false;
    }

    part = cmdLine.getOptionValue(PART_ARG);
    from = cmdLine.getOptionValue(FROM_ARG);
    to = cmdLine.getOptionValue(TO_ARG);
    file = cmdLine.getOptionValue(FILE_ARG);
    format = cmdLine.getOptionValue(FORMAT_ARG);
    resultDir = cmdLine.getOptionValue(DIR_ARG);
    
    // add additional controls on cmdline values
    if (part!=null && (from!=null||to!=null)){
      msg = DBMSMessages.getString("Tool.Cutter.msg3");
      System.err.println(msg);
      return false;
    }
    // get input file format
    if (format==null){
      dbFormat=DatabankFormat.fasta;//default is Fasta
    }
    else{
      dbFormat = formats.get(format);
      if (dbFormat==null){
        msg = String.format(DBMSMessages.getString("Tool.Cutter.msg4"), 
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
      msg = String.format(DBMSMessages.getString("Tool.Cutter.msg5"), file, ipart);
    }
    else if (ifrom!=-1 && ito!=-1){
      msg = String.format(DBMSMessages.getString("Tool.Cutter.msg6"), ifrom, ito, file);
    }
    else if (ito!=-1){
      msg = String.format(DBMSMessages.getString("Tool.Cutter.msg7"), ito, file);
    }
    else{
      msg = String.format(DBMSMessages.getString("Tool.Cutter.msg7"), ifrom, file);
    }
    System.out.println(msg);
    
    // compute new file
    if (ipart==-1){
      bRet = cutFile(file, resultDir, dbFormat, ifrom, ito);
    }
    else{
      System.err.println("ERROR: 'part' not yet implemented... sorry!");
      bRet = false;
    }
    return bRet;
  }
  public static void main(String[] args) {
    if (!doJob(args)){
      // exit code=1 : do this to report error to calling app
      System.exit(1);
    }
  }
}
