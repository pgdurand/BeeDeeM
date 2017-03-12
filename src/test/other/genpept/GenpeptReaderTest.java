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
package test.other.genpept;

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
 * This code snippet illustrates how to format a Genpept data file using INSD
 * format.
 * 
 * @author Patrick G. Durand
 */
public class GenpeptReaderTest {

  private static void test5(String file) {
    PSequence seq = DBUtils.readGenpeptEntry(new File(file), 0, 0, false);
    PFormatter formatter = new PFormatter();
    Writer w = new BufferedWriter(new OutputStreamWriter(System.out));
    formatter.dump(w, seq, "insd");
    try {
      w.flush();
      w.close();
    } catch (IOException e) {
    }
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    DBMSAbstractConfig.configureLog4J("GenpeptReader");
    DBMSAbstractConfig.setInstallAppConfPath("./conf");
    test5("./tests/junit/DBXrefManager/NP_006580.gp");
  }

}
