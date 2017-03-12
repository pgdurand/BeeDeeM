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

/**
 * This interface defines an FTP loader monitor. It aims at allowing an object
 * to trace the files loading process.
 * 
 * @author Patrick G. Durand
 */
public interface LoaderMonitor {
  public static final int      STATUS_OK      = 0;
  public static final int      STATUS_FAILURE = 1;
  public static final int      STATUS_ABORTED = 2;

  public static final String[] STATUS_STR     = { "ok", "failure", "aborted" };

  /**
   * A file is going to be downloaded from a FTP server.
   * 
   * @param fName
   *          the file name that is going to be downloaded.
   */
  public void beginLoading(String fName);

  /**
   * A file has been downloaded from a FTP server.
   * 
   * @param fName
   *          the file name that has been downloaded.
   * @param status
   *          one of the STATUS_XXX constants defined here.
   */
  public void doneLoading(String fName, int status);
}
