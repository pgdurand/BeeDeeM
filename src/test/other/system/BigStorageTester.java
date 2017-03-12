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
package test.other.system;

import java.util.Date;

import bzh.plealog.dbmirror.indexer.LuceneStorageSystem;
import bzh.plealog.dbmirror.indexer.StorageSystem;
import bzh.plealog.dbmirror.indexer.StorageSystemException;

public class BigStorageTester {

  /**
   * @param args
   */
  public static void main(String[] args) {
    LuceneStorageSystem lss = new LuceneStorageSystem();
    lss.open("bigindex", StorageSystem.WRITE_MODE);
    int i, id = 10000, end = 1000000;
    long start = 100000, stop = 100000, tim;

    // end = Integer.valueOf(args[0]).intValue();
    System.out.println("Start at: " + new Date());
    System.out.println("Nb items: " + end);
    System.out.println("Total memory: " + Runtime.getRuntime().totalMemory());
    System.out.println("Free memory: " + Runtime.getRuntime().freeMemory());
    tim = System.currentTimeMillis();
    try {
      for (i = 0; i < end; i++) {
        lss.addEntry(String.valueOf(id++), "toto", "aFile", start++, stop++);
        if ((i % 10000) == 0) {
          System.out.println(i);
        }
        if ((i % 100000) == 0) {
          System.out.println("Call GC: " + Runtime.getRuntime().freeMemory());
          System.gc();
          System.out.println("   done: " + Runtime.getRuntime().freeMemory());
        }
      }
    } catch (StorageSystemException e) {
      System.err.println("Error: " + e);
    }
    lss.close();
    System.out.println("End at: " + new Date());
    System.out.println("Total time: "
        + ((System.currentTimeMillis() - tim) / 1000) + " s.");
  }

}
