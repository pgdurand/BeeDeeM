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
package bzh.plealog.dbmirror.fetcher;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class defines a Calendar matcher. It is used to filter files given a
 * Calendar range.
 * 
 * @author Patrick G. Durand
 */
public class CalendarMatcher {
  private Calendar               _start;
  private Calendar               _end;
  private boolean                _initOk        = false;

  private final SimpleDateFormat _dateFormatter = new SimpleDateFormat(
                                                    "yyyyMMdd");
  private static final Log       LOGGER         = LogFactory
                                                    .getLog(CalendarMatcher.class);

  /**
   * Constructor.
   */
  public CalendarMatcher(Calendar from, Calendar to) {
    _start = from;
    _end = to;
  }

  /**
   * Constructor with two pre-formatted strings. Format to use is yyyymmdd such
   * as 20070924 (September 24th, 2007).
   * 
   * @param from
   *          a string formatted as yyyymmdd.
   * @param to
   *          a string formatted as yyyymmdd.
   */
  public CalendarMatcher(String from, String to) {
    try {
      Calendar c1 = Calendar.getInstance();
      c1.setTime(_dateFormatter.parse(from));
      _start = c1;
    } catch (ParseException e) {
      LOGGER.warn("Invalid starting date: " + from);
      return;
    }
    try {
      Calendar c2 = Calendar.getInstance();
      c2.setTime(_dateFormatter.parse(to));
      _end = c2;
    } catch (ParseException e) {
      LOGGER.warn("Invalid ending date: " + from);
      return;
    }
    _initOk = true;
  }

  /**
   * Figures out if this matcher has been correctly initialized.
   */
  public boolean initialized() {
    return _initOk;
  }

  /**
   * Checks whether parameter d is included within the Calendar range defines in
   * this CalendarMatcher.
   */
  public boolean match(Date d) {
    if (!_initOk)
      return false;
    return (d.compareTo(_start.getTime()) >= 0 && d.compareTo(_end.getTime()) <= 0);
  }
}
