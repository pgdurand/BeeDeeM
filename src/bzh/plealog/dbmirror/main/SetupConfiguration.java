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
package bzh.plealog.dbmirror.main;

import java.io.File;
import java.io.IOException;

import bzh.plealog.dbmirror.util.Utils;

/**
 * This class is used by the installer of DBMS to update Windows config files.
 * 
 * @author Patrick G. Durand
 */
public class SetupConfiguration {

  /**
   * @param args
   */
  public static void main(String[] args) {
    try {
      Utils.replaceInFile(new File(args[0]), new File(args[1]), args[2],
          args[3]);
    } catch (IOException e) {
      System.err.println("unable to replace string in file: " + e);
    }
  }

}
