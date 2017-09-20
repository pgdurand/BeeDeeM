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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import bzh.plealog.dbmirror.util.Utils;

public interface DBStampProperties {

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
    }
    return d;
  }
}
