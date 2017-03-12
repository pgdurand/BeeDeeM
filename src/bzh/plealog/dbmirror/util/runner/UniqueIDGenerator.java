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

import java.util.HashSet;
import java.util.Random;

/**
 * This class can be used to generate unique identifiers. Such identifiers are
 * made of two parts: a root string (the prefix of the ID) and an integer (the
 * suffix of the ID). This integer is incremented each time an ID is requested.
 * 
 * @author Patrick G. Durand
 */
public class UniqueIDGenerator {
  private static HashSet<String> _keys    = new HashSet<String>();
  private static int             _counter = 0;
  private static String          _root    = "id";

  /**
   * Enables to register an existing key.
   */
  public static synchronized void registerKey(String key) {
    if (_keys.contains(key) == false)
      _keys.add(key);
  }

  /**
   * Gets a new unique id.
   */
  public static String getUniqueKey() {
    return getUniqueKey(_root);
  }

  /**
   * Gets a new unique id using a particular root string for the id prefix.
   */
  public static synchronized String getUniqueKey(String root) {
    String str;
    while (true) {
      str = root + (++_counter);
      if (_keys.contains(str) == false) {
        _keys.add(str);
        return str;
      }
    }
  }

  /**
   * Return an ID.
   */
  public static synchronized String getRIDKey(String prefix) {
    char[] chars = "ABSDEFGHIJKLMNOPQRSTUVWXYZ1234567890".toCharArray();
    Random r = new Random(System.currentTimeMillis());
    char[] id = new char[11];
    for (int i = 0; i < 11; i++) {
      id[i] = chars[r.nextInt(chars.length)];
    }
    return prefix + String.copyValueOf(id);
  }

  /**
   * Given a name and a number, returns an identifier.
   */
  public static String getKey(String name, int num) {
    return name + "_" + num;
  }
}
