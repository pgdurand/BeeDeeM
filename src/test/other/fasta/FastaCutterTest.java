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
package test.other.fasta;

import java.io.FileOutputStream;

import org.apache.log4j.BasicConfigurator;

import bzh.plealog.dbmirror.indexer.FastaCutter;

/**
 * This snippet illustrates how to cut a Fasta file.
 * 
 * @author Patrick G. Durand
 */
@SuppressWarnings("deprecation")
public class FastaCutterTest {

  /**
   * @param args
   */
  public static void main(String[] args) {
    BasicConfigurator.configure();
    System.out.println("Start cutting: " + args[0]);
    long tim = System.currentTimeMillis();
    // see also bzh.plealog.dbmirror.util.DBFileCutter
    FastaCutter spp = new FastaCutter();
    try {
      FileOutputStream fos = new FileOutputStream(args[1]);
      spp.parse(args[0], fos, Integer.valueOf(args[2]),
          Integer.valueOf(args[3]));
      fos.flush();
      fos.close();
    } catch (Exception e) {
      System.err.println("Error: " + e.toString());
    }
    System.out.println("Running time: " + (System.currentTimeMillis() - tim)
        + " ms.");
  }

}
