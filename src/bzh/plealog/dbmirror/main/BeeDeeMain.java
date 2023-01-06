/* Copyright (C) 2022 Patrick G. Durand
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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import bzh.plealog.dbmirror.ui.resources.DBMSMessages;
import eu.infomas.annotation.AnnotationDetector;
import eu.infomas.annotation.AnnotationDetector.TypeReporter;

/**
 * This class starts BeeDeeM for all commands.
 * <br><br>
 * In addition, some parameters can be passed to the JVM for special configuration purposes:<br>
 * -DKL_HOME=an_absolute_path ; the absolute path to the DBMS installation home dir. If not set, use user.dir java property.
 * -DKL_DEBUG=true ; if true, if set, log will be in debug mode<br>
 * -DKL_WORKING_DIR=an_absolute_path ; if not set, log and working directories are set to java.io.tmp<br>
 * -DKL_CONF_DIR=an_absolute_path ; the absolute path to a home-made conf directory. If not set, use ${user.dir}/conf.
 * -DKL_LOG_FILE=a_file_name ; if set, creates a log file with that name within KL_WORKING_DIR<br>
 * -DKL_LOG_TYPE=none|console|file(default)<br><br>
 * 
 * @author Patrick G. Durand
 */
public class BeeDeeMain {

  
  /** Code from: https://github.com/rmuller/infomas-asl
   *  Adapted in package eu.informas.annotation available in BeeDeeM; see README.txt, there.
   **/
  public static Set<Class<?>> findAllClassesUsingClassLoader(){
    HashSet<Class<?>> classes = new HashSet<>();
    final TypeReporter reporter = new TypeReporter() {
      @SuppressWarnings("unchecked")
      @Override
      public Class<? extends Annotation>[] annotations() {
          return new Class[]{BdmTool.class};
      }
      @Override
      public void reportTypeAnnotation(Class<? extends Annotation> annotation,
          String className) {
        try {
          classes.add(Class.forName(className));
        } catch (ClassNotFoundException e) {
          System.err.println(DBMSMessages.getString("Tool.Master.err4.cmd")+" "+e.getMessage());
          System.exit(1);//serious error, should not happen!
        }
      }
      
  };
  final AnnotationDetector cf = new AnnotationDetector(reporter);
  try {
    cf.detect();
  } catch (IOException e) {
    System.err.println(DBMSMessages.getString("Tool.Master.err3.cmd")+" "+e.getMessage());
    System.exit(1);//serious error, should not happen!
  }
  return classes;
  }
  /** */
   
  private static void dumpHelp() {
    Properties props = StarterUtils.getVersionProperties();
    System.out.print(props.getProperty("prg.app.name"));
    System.out.print(" ");
    System.out.println(DBMSMessages.getString("Tool.Master.intro"));
    Hashtable<String, String> tools = new Hashtable<String, String>();
    Set<Class<?>> clazz = findAllClassesUsingClassLoader();
    for(Class<?> c : clazz) {
      BdmTool bdmT = c.getAnnotation(BdmTool.class); 
      if (bdmT != null) {
        tools.put(bdmT.command(), bdmT.description());
      }
    }
    List<String> sortedTools = Collections.list(tools.keys());
    Collections.sort(sortedTools);
    for(String tName : sortedTools) {
      System.out.print("  bmd ");
      System.out.print(tName);
      if (tName.equals("ui")) {
        System.out.print(": ");
      }
      else {
        System.out.print(" [options]: ");
      }
      System.out.println(tools.get(tName));
    }
    
    StringBuffer buf = new StringBuffer();
    System.out.println(DBMSMessages.getString("Tool.Master.more"));
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
    System.out.println(buf.toString());
  }
  
  public static void main(String[] args) {
    /* This code handles strings as "bdm xxx"
     * "bdm" or "bdm.bat" is the caller script
     * "xxx" can be either:
     *   - nothing, -h or --help: display help message
     *   - "ui": start UI Manager
     *   - everything else: try to run an existing command
     */
    String cmd = "help";
    
    if (args.length!=0) {
      cmd = args[0];
      args = Arrays.copyOfRange(args, 1, args.length);
    }
    
    if (cmd.equalsIgnoreCase("help") || 
        cmd.equalsIgnoreCase("-h") || 
        cmd.equalsIgnoreCase("--help")) {
      dumpHelp();
    }
    else {
      // Get all classes from current package
      // to locate ones being annotated as BdmTool classes
      boolean cmdOk = false;
      Set<Class<?>> clazz = findAllClassesUsingClassLoader();
      for(Class<?> c : clazz) {
        BdmTool bdmT = c.getAnnotation(BdmTool.class); 
        String bdmTCmd = bdmT != null ? bdmT.command() : "-" ;
        if (bdmT != null && cmd.equalsIgnoreCase(bdmTCmd)) {
          cmdOk = true;
          try {
            Method method = c.getDeclaredMethod("execute", String[].class);
            Object bRet = method.invoke(c.newInstance(), new Object[] {args});
            if ( ! bRet.toString().equalsIgnoreCase("true") ) {
              System.exit(1);
            }
          } catch (Exception e) {
            System.err.print(DBMSMessages.getString("Tool.Master.err2.cmd"));
            System.err.println(cmd);
            System.err.println(e.getMessage());
            System.exit(1);
          }
        }
      }
      // unknown command
      if (!cmdOk) {
        System.err.print(DBMSMessages.getString("Tool.Master.err.cmd"));
        System.err.print(": ");
        System.err.println(cmd);
        System.exit(1);
      }
    }
  }
}
