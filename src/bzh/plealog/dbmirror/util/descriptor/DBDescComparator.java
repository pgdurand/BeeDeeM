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
package bzh.plealog.dbmirror.util.descriptor;

import java.util.Comparator;

/**
 * This class can be used to sort DBDescriptor objects by ascending order of
 * their names. This comparator is not case sensitive.
 * 
 * @author Patrick G. Durand.
 */
public class DBDescComparator implements Comparator<DBDescriptor> {
  public int compare(DBDescriptor db1, DBDescriptor db2) {
    return db1.getName().toLowerCase().compareTo(db2.getName().toLowerCase());
  }
}