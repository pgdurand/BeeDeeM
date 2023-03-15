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
package bzh.plealog.dbmirror.util.runner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;

public class DBStampProperties {

  // File used to store release bank date as provided by the data provider
  public static final String           RELDATE_FNAME       = "release-date.txt";

  // File used to store some bank properties
  public static final String           TIME_STAMP_FNAME    = "time.txt";

  // DB time stamp as provided by data provider
  public static final String           RELEASE_TIME_STAMP  = "release.time.stamp";
  // DB installation time stamp
  public static final String           TIME_STAMP          = "time.stamp";
  // nb of entries in Lucene index
  public static final String           NB_ENTRIES          = "entries";
  // nb sequences in blast bank
  public static final String           NB_SEQUENCES        = "sequences";
  // total size of the bank (all files in all DB dir and its sub-dirs)
  public static final String           DB_SIZE             = "size";
  // Date formatter
  public static final SimpleDateFormat BANK_DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd, HH:mm");
  private static final SimpleDateFormat BANK_DATE_FORMATTER_DIR = new SimpleDateFormat("yyyy-MM-dd_HH_mm");

  private static final Log             LOGGER              = LogFactory
      .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
          + ".DBStampProperties");

  /**
   * Save a bank release date within an appropriate file.
   * 
   * @param dir directory containing a bank
   * @param date time stamp of the bank
   */
  public static boolean writeReleaseDate(String dir, Date date) {
    String fName = Utils.terminatePath(dir) + RELDATE_FNAME;
    try (FileOutputStream writer = new FileOutputStream(fName)) {
      Properties props = new Properties();
      props.put(RELEASE_TIME_STAMP, BANK_DATE_FORMATTER.format(date));
      props.store(writer, "");
      writer.flush();
    } catch (Exception e1) {
      LOGGER.warn(e1);
      return false;
    }
    return true;
  }

  /**
   * Retrieve a bank release date.
   * 
   * @param dir directory containing a bank
   * 
   * @return bank release date. Format is: yyy-MM-dd, HH:mm. Return null if
   * the bank release date cannot be retrieved.
   */
  public static String readReleaseDate(String dir){
    String d = null;
    Properties props = new Properties();
    String fName = Utils.terminatePath(dir) + RELDATE_FNAME;
    try(FileInputStream fis = new FileInputStream(fName)){
      props.load(fis);
      d = props.getProperty(RELEASE_TIME_STAMP);
    }
    catch(Exception ex){
      LOGGER.warn(ex);
    }
    return d;
  }
  
  /**
   * Writes a time stamp that identifies when a mirror has been installed.
   * 
   * @param dbPath
   *          the mirror path (parent of mirror current dir)
   * @param installDate installation date of bank
   * @param releaseDate release date of bank as obtained by provider         
   * @param entries
   *          an array of two integers: nb. of entries (Lucene index) and nb. of
   *          sequences (Blast formatdb)
   * @param bankSize bytes size of bank
   */
  public static boolean writeDBStamp(String dbPath, String installDate, String releaseDate,
      int[] entries, long bankSize) {
    String fName;
    Properties props;
    
    boolean bRet = false;

    fName = Utils.terminatePath(dbPath) + DBStampProperties.TIME_STAMP_FNAME;
    try (FileOutputStream writer = new FileOutputStream(fName)) {
      props = new Properties();
      props.put(DBStampProperties.TIME_STAMP, installDate);
      props.put(DBStampProperties.RELEASE_TIME_STAMP, releaseDate);
      props.put(DBStampProperties.NB_ENTRIES, String.valueOf(entries[0]));
      props.put(DBStampProperties.NB_SEQUENCES, String.valueOf(entries[1]));

      props.put(DBStampProperties.DB_SIZE, String.valueOf(bankSize));
      props.store(writer, "");
      writer.flush();
      bRet = true;
    } catch (Exception e1) {
      LOGGER.warn(e1);
    }
    return bRet;
  }

  /**
   * Reads a time stamp.
   * 
   * @param dbPath
   *          the mirror path (parent of mirror current dir)
   * 
   * @return a Properties instance.
   * */
  public static Properties readDBStamp(String dbPath) {
    Properties props;
    String fName;

    props = new Properties();
    fName = Utils.terminatePath(dbPath) + DBStampProperties.TIME_STAMP_FNAME;
    if (new File(fName).exists()){//do this to avoid an exception when file does not exist
      try (FileInputStream reader = new FileInputStream(fName)) {
        props.load(reader);
      } catch (Exception e1) {
        LOGGER.warn(e1);
      }
    }
    return props;
  }

  /**
   * Utility method aims at providing a time stamp formatted as YYY-MM-dd_HH_mm.
   */
  public static String getDBTimeStampAsDirStr(String dbPath) {
    Properties props = readDBStamp(dbPath);
    String d = props.getProperty(TIME_STAMP);
    String d2 = null;
    try {
      Date dt = BANK_DATE_FORMATTER.parse(d);
      d2 = BANK_DATE_FORMATTER_DIR.format(dt);
    } catch (ParseException e) {
      // not bad, so we hide this exception
    }
    return d2;
  }
}
