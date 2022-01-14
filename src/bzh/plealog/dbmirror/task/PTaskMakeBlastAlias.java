/* Copyright (C) 2007-2017 Patrick G. Durand
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
package bzh.plealog.dbmirror.task;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * A task capable of preparing a Blast alias files.
 * 
 * @author Patrick G. Durand
 */
public class PTaskMakeBlastAlias extends PAbstractTask {

  private String              _dbPathName;
  private String              _errMsg;
  private boolean             _isNucleic;
  private static boolean      _useFullPath = false;

  public static final String         BLAST_ALIAS_TAG          = "M";
  public static final String         NUCLEIC_IDX              = "nin";
  public static final String         PROTEIN_IDX              = "pin";
  public static final String         NUCLEIC_ALIAS            = "nal";
  public static final String         PROTEIN_ALIAS            = "pal";

  public static final String         NUCLEIC_IDX_EXT          = "." + NUCLEIC_IDX;
  public static final String         PROTEIN_IDX_EXT          = "." + PROTEIN_IDX;
  public static final String         NUCLEIC_ALIAS_EXT        = "." + NUCLEIC_ALIAS;
  public static final String         PROTEIN_ALIAS_EXT        = "." + PROTEIN_ALIAS;

  // this tag is used to avoid this error message from Blast:
  // [NULL_Caption] WARNING: Recursive situation detected with xxx
  // where xxx is the dbName
  private static final String USE_FULL_PATH = "fullPath";

  private static final Log    LOGGER        = LogFactory
                                                .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                    + ".PTaskEngine");

  public PTaskMakeBlastAlias(String dbPathName, boolean isNucleic) {
    _isNucleic = isNucleic;
    _dbPathName = dbPathName;
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getName() {
    return "Make Blast DB alias";
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getUserFriendlyName() {
    return "preparing Blast databank alias";
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getErrorMsg() {
    return _errMsg;
  }

  /**
   * Implementation of KLTask interface.
   */
  public boolean execute() {

    LoggerCentral.info(LOGGER,
        getName() + " for " + new File(_dbPathName).getName());

    return prepareAliasFile(
        new File(_dbPathName).getParent(), 
        new File(_dbPathName).getName(),
        _isNucleic);
  }

  public void setParameters(String params) {
    Map<String, String> args;
    String value;

    if (params == null)
      return;

    args = Utils.getTaskArguments(params);

    // make alias task accepts an optional argument that can force the write of
    // full path name
    // within the alias file
    value = args.get(USE_FULL_PATH);
    if (value != null)
      _useFullPath = Boolean.TRUE.toString().equals(value);
  }

  /**
   * This method locates an NCBI-based BLAST bank alias file name and get its content
   * given particular conditions. 
   * 
   * @param path absolute path to a directory to search for alias file names
   * @param aExt either .nal or .pal
   * 
   * @return null or BLAST alias file content as List of strings. Such a content is ONLY
   * returned when path contains a unique aExt file, otherwise null is returned. List
   * of strings contains all but TITLE and DBLIST data lines of BLAST bank alias file.
   */
  public static List<String> getDataFromNativeAliasFile(String path, String aExt){
    List<String> data=null;
    File[] files;
    
    files = new File(path).listFiles(new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
          return name.endsWith(aExt);
        }
      });
    if (files==null || files.length!=1) {
      return null;
    }
    data = new ArrayList<>();
    try (Stream<String> stream = Files.lines(Paths.get(files[0].toURI()))) {
      data = stream
          .filter(line -> ! (line.startsWith("#") || line.startsWith("TITLE") || line.startsWith("DBLIST")))
          .collect(Collectors.toList());

    } catch (IOException e) {
      LoggerCentral.error(LOGGER, "unable to read: "+files[0].getAbsolutePath()+": "+e.toString());
      return null;
    }
    if (data.isEmpty()) {
      data=null;
    }
    return data;
  }

  /**
   * This method overrides the standard alias file created by makeblastdb since it
   * seems it does strange stuff with several Fasta files.
   * 
   * @param path absolute path to bank installation (the one containing all files)
   * @param dbName bank name
   * @param isNucleic true or false
   */
  public static boolean prepareAliasFile(String path, String dbName, boolean isNucleic) {
    String fExt1, fExt2, fExt3;
    File[] files;
    File f;
    PrintWriter writer = null;
    String fName;//, parentDir;
    List<String> lines;
    boolean bRet = false, bWrite = false;
    int i, pos;

    fExt1 = (isNucleic ? PTaskMakeBlastAlias.NUCLEIC_IDX_EXT : PTaskMakeBlastAlias.PROTEIN_IDX_EXT);
    fExt2 = (isNucleic ? PTaskMakeBlastAlias.NUCLEIC_ALIAS_EXT : PTaskMakeBlastAlias.PROTEIN_ALIAS_EXT);
    fExt3 = ".msk";// Blast DBs relying on other Blast DBs

    try {
      fName = Utils.terminatePath(path) + dbName + PTaskMakeBlastAlias.BLAST_ALIAS_TAG + fExt2;
      //delete old alias before creating it
      f = new File(fName);
      if (f.exists()) {
        f.delete();
      }
      //get content of NCBI-based BLAST alias file if any found
      //(this may happen when installing native NCBI BLAST bank)
      //this has to be done BEFORE creating new alias file!!!
      //parentDir = new File(path).getParent();
      lines = getDataFromNativeAliasFile(path, fExt2);

      writer = new PrintWriter(fName);
      writer.print("TITLE ");
      writer.println(dbName);
      writer.print("DBLIST ");
      files = new File(path).listFiles();
      Arrays.sort(files);
      for (i = 0; i < files.length; i++) {
        f = files[i];
        if (!f.isFile())
          continue;
        if (_useFullPath) {
          fName = f.getAbsolutePath();
          fName = fName.replace(DBMSAbstractConfig.DOWNLOADING_DIR, "current");
        } else {
          fName = f.getName();
        }
        pos = fName.indexOf(fExt1);// standard blast file
        if (pos >= 0) {
          writer.print(fName.substring(0, pos) + " ");
          bWrite = true;
        } else {// special handling of ".msk" files
          pos = fName.indexOf(fExt3);
          if (pos >= 0) {
            writer.print(fName.substring(0, pos) + " ");
            bWrite = true;
          }
        }
      }
      writer.println();
      //write additional content of native BLAST alias file if any
      if (lines!=null) {
        for (String str : lines) {
          writer.println(str);
        }
      }
      writer.flush();
      if (!bWrite) {
        throw new Exception("unable to find " + fExt1
            + " or .msk files to prepare Blast DB alias");
      }
      bRet = true;
    } catch (Exception e) {
      LoggerCentral.error(LOGGER, "unable to create alias file: " + e);
    } finally {
      IOUtils.closeQuietly(writer);
    }
    return bRet;
  }

  /**
   * Prepare standard blast alias filename.
   */
  public static String getBlastAliasFilePath(String path, String dbName, boolean isProteic) {
    String fExt2;
    String fName;
    fExt2 = (!isProteic ? PTaskMakeBlastAlias.NUCLEIC_ALIAS_EXT : PTaskMakeBlastAlias.PROTEIN_ALIAS_EXT);
    fName = Utils.terminatePath(path) + dbName + PTaskMakeBlastAlias.BLAST_ALIAS_TAG + fExt2;
    return fName;
  }
  
  public static void removeOldAlias(String path, String dbName, boolean isProteic) {
    File f = new File(getBlastAliasFilePath(path, dbName, isProteic));
    if (f.exists())
      f.delete();
  }

}
