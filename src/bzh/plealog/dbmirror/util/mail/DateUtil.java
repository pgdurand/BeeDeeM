/* Copyright (C) 2007-2017 Ludovic Antin
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
package bzh.plealog.dbmirror.util.mail;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Class to handle Date data.
 * 
 * @author Ludovic Antin
 */
public class DateUtil {

  private static final String           FMT_FROM_YEAR_TO_SECONDS     = "yyyy/MM/dd HH:mm:ss";
  private static final SimpleDateFormat SDF_UTC_FROM_YEAR_TO_SECONDS = new SimpleDateFormat(
                                                                         FMT_FROM_YEAR_TO_SECONDS);

  static {
    // Convert Local Time to UTC (Works Fine)
    SDF_UTC_FROM_YEAR_TO_SECONDS.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  public static String convertToUTC(Date date) {
    return SDF_UTC_FROM_YEAR_TO_SECONDS.format(date);
  }

}
