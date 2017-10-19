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
package bzh.plealog.dbmirror.util.ant;

import java.io.File;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.util.conf.Configuration;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;

/**
 * This class is used to call Ant targets.
 * 
 * @author Patrick G. Durand
 */
public class PAntTasks {
  private static final Log LOGGER = LogFactory
                                      .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                          + ".PAntTasks");

  public static boolean setProxy(String host, String port) {
    PAntRunner at = new PAntRunner();
    HashMap<String, String> m = new HashMap<String, String>();
    boolean bRet = false;

    try {
      // init
      at.init(DBMSAbstractConfig.getConfPath(Configuration.SYSTEM) + "tasks.xml",
          DBMSAbstractConfig.getConfPath(Configuration.SYSTEM));
      // properties
      m.put("host", host);
      m.put("port", port);
      at.setProperties(m, false);
      // run
      at.runTarget("setproxy");
      bRet = true;
    } catch (Exception e) {
      LOGGER.warn("Unable to set proxy: " + e.getMessage());
    }

    return bRet;
  }

  public static boolean gunzip(String srcFile, String destDir) {
    return unzipGunzip("gunzipper", srcFile, destDir);
  }

  public static boolean unzip(String srcFile, String destDir) {
    return unzipGunzip("unzipper", srcFile, destDir);
  }

  private static boolean unzipGunzip(String taskName, String srcFile,
      String destDir) {
    PAntRunner at = new PAntRunner();
    HashMap<String, String> m = new HashMap<String, String>();
    boolean bRet = false;
    try {
      // init
      at.init(DBMSAbstractConfig.getConfPath(Configuration.SYSTEM) + "tasks.xml",
          DBMSAbstractConfig.getConfPath(Configuration.SYSTEM));
      // properties
      m.put("src", srcFile);
      m.put("dest", destDir);
      at.setProperties(m, false);
      // run
      at.runTarget(taskName);
      bRet = true;
    } catch (Exception e) {
      LOGGER.warn("Unable to execute unzip: " + e.getMessage());
    }
    return bRet;
  }

  public static boolean untar(String srcFile, String destDir) {
    PAntRunner at = new PAntRunner();
    HashMap<String, String> m = new HashMap<String, String>();
    boolean bRet = false;

    try {
      // init
      at.init(DBMSAbstractConfig.getConfPath(Configuration.SYSTEM) + "tasks.xml",
          DBMSAbstractConfig.getConfPath(Configuration.SYSTEM));
      // properties
      m.put("src", srcFile);
      m.put("dest", destDir);
      at.setProperties(m, false);
      // run
      at.runTarget("untarrer");
      bRet = true;
    } catch (Exception e) {
      LOGGER.warn("Unable to execute untar: " + e.getMessage());
    }

    return bRet;
  }

  public static boolean untartgz(String srcFile, String destDir) {
    PAntRunner at = new PAntRunner();
    HashMap<String, String> m = new HashMap<String, String>();
    boolean bRet = false;

    try {
      // init
      at.init(DBMSAbstractConfig.getConfPath(Configuration.SYSTEM) + "tasks.xml",
          DBMSAbstractConfig.getConfPath(Configuration.SYSTEM));
      // properties
      m.put("src", srcFile);
      m.put("dest", destDir);
      at.setProperties(m, false);
      // run
      at.runTarget("untarrertgz");
      bRet = true;
    } catch (Exception e) {
      LOGGER.warn("Unable to execute untartgz: " + e.getMessage());
    }

    return bRet;
  }

  public static boolean deleteFiles(String dir, String fileFilter) {
    PAntRunner at = new PAntRunner();
    HashMap<String, String> m = new HashMap<String, String>();
    boolean bRet = false;

    try {
      // init
      at.init(DBMSAbstractConfig.getConfPath(Configuration.SYSTEM) + "tasks.xml",
          DBMSAbstractConfig.getConfPath(Configuration.SYSTEM));
      // properties
      m.put("dir", dir);
      m.put("ffilter", fileFilter);
      at.setProperties(m, false);
      // run
      at.runTarget("delfiles");
      bRet = true;
    } catch (Exception e) {
      LOGGER.warn("Unable to execute delfiles: " + e.getMessage());
    }

    return bRet;
  }

  public static boolean deleteDirectory(String dir) {
    PAntRunner at = new PAntRunner();
    HashMap<String, String> m = new HashMap<String, String>();
    boolean bRet = false;

    try {

      if (!new File(dir).exists()) {
        return true;
      }

      // init
      at.init(DBMSAbstractConfig.getConfPath(Configuration.SYSTEM) + "tasks.xml",
          DBMSAbstractConfig.getConfPath(Configuration.SYSTEM));
      // properties
      m.put("dir", dir);
      at.setProperties(m, false);
      // run
      at.runTarget("deldir");
      bRet = true;
    } catch (Exception e) {
      LOGGER.warn("Unable to execute delfiles: " + e.getMessage());
    }

    return bRet;
  }

  public static boolean movefile(String src, String dest) {
    PAntRunner at = new PAntRunner();
    HashMap<String, String> m = new HashMap<String, String>();
    File f1;
    boolean bRet = false;
    try {
      // init
      at.init(DBMSAbstractConfig.getConfPath(Configuration.SYSTEM) + "tasks.xml",
          DBMSAbstractConfig.getConfPath(Configuration.SYSTEM));
      // properties
      m.put("src", src);
      m.put("dest", dest);
      at.setProperties(m, false);
      f1 = new File(src);
      // run
      if (f1.isDirectory())
        at.runTarget("movedir");
      else
        at.runTarget("movefile");
      bRet = true;
    } catch (Exception e) {
      LOGGER.warn("Unable to execute movefile: " + e.getMessage());
    }
    return bRet;
  }

  public static boolean zipDir(String srcDir, String destZipFile) {
    PAntRunner at = new PAntRunner();
    HashMap<String, String> m = new HashMap<String, String>();
    boolean bRet = false;
    try {
      // init
      at.init(DBMSAbstractConfig.getConfPath(Configuration.SYSTEM) + "tasks.xml",
          DBMSAbstractConfig.getConfPath(Configuration.SYSTEM));
      // properties
      m.put("src", srcDir);
      m.put("dest", destZipFile);
      at.setProperties(m, false);
      // run
      at.runTarget("zipdir");
      bRet = true;
    } catch (Exception e) {
      LOGGER.warn("Unable to execute zipDir: " + e.getMessage());
    }
    return bRet;
  }

  public static boolean targzDir(String srcDir, String destZipFile) {
    PAntRunner at = new PAntRunner();
    HashMap<String, String> m = new HashMap<String, String>();
    boolean bRet = false;
    try {
      // init
      at.init(DBMSAbstractConfig.getConfPath(Configuration.SYSTEM) + "tasks.xml",
          DBMSAbstractConfig.getConfPath(Configuration.SYSTEM));
      // properties
      m.put("src", srcDir);
      m.put("dest", destZipFile);
      at.setProperties(m, false);
      // run
      at.runTarget("targzdir");
      bRet = true;
    } catch (Exception e) {
      LOGGER.warn("Unable to execute zipDir: " + e.getMessage());
    }
    return bRet;
  }
  /*
   * Keep for history
   * 
   * <!-- - Call the Blast/formatdb application (NCBI). This task is intended -
   * to be used with FASTA files using defLine conforming NCBI conventions. -
   * See http://www.ncbi.nlm.nih.gov/staff/tao/URLAPI/formatdb_fastacmd.html -->
   * <target name="formatdb" > <exec executable="${path}formatdb${ext}"
   * failonerror="true"> <arg line='-n "${name}" -p ${isProt} -i ${src} -o T -l
   * "${logDir}/formatdb.log"'/> </exec> </target>
   */
  /*
   * public static boolean formatdb(String dbName, String files, String path,
   * String ext, boolean isProt){ KLAntRunner at = new KLAntRunner();
   * HashMap<String,String> m = new HashMap<String,String>(); boolean bRet =
   * false; try { //init
   * at.init(KDMSAbstractConfig.getInstallAppConfPath()+"tasks.xml" ,
   * KDMSAbstractConfig.getInstallAppConfPath()); //properties m.put("src",
   * files); m.put("path", path); m.put("ext", ext); m.put("name", dbName);
   * m.put("isProt", isProt ? "T":"F"); m.put("logDir",
   * KDMSAbstractConfig.getLogAppPath()); at.setProperties(m, false); //run
   * at.runTarget("formatdb"); bRet = true; } catch (Exception e) {
   * LOGGER.warn("Unable to execute formatdb: "+e.getMessage()); } return bRet;
   * }
   */
}
