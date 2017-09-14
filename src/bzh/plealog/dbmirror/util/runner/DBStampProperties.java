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

public interface DBStampProperties {
  // DB time stamp as provided by data provider
  public static final String RELEASE_TIME_STAMP   = "release.time.stamp";
  // DB installation time stamp
  public static final String TIME_STAMP   = "time.stamp";
  // nb of entries in Lucene index
  public static final String NB_ENTRIES   = "entries";
  // nb sequences in blast bank
  public static final String NB_SEQUENCES = "sequences";
  // total size of the bank (all files in all DB dir and its sub-dirs)
  public static final String DB_SIZE      = "size";
}
