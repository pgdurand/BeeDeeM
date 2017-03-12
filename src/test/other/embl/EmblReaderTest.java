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
package test.other.embl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import bzh.plealog.dbmirror.reader.DBUtils;
import bzh.plealog.dbmirror.reader.PFormatter;
import bzh.plealog.dbmirror.reader.PSequence;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;

/**
 * This snippet illustrates how to format an EMBL entry using ISND format.
 * 
 * @author Patrick G. Durand
 */
public class EmblReaderTest {

  private static void format(String file) throws IOException {
    PSequence seq = DBUtils.readEmblEntry(new File(file), 0, 0, false);
    PFormatter formatter = new PFormatter();
    Writer w = new BufferedWriter(new OutputStreamWriter(System.out));
    formatter.dump(w, seq, "insd");
    w.flush();
    w.close();
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    // setup the logger system
    DBMSAbstractConfig.configureLog4J("EmblReader");
    // target the path containing Velocity templates (.vm files)
    DBMSAbstractConfig.setInstallAppConfPath("./conf");
    // reformat a simple EMBL entry
    try {
      format("./tests/junit/DBXrefManager/FK669046.embl");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
