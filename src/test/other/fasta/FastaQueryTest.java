/* Copyright (C) 2007-2018 Patrick G. Durand
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

import bzh.plealog.dbmirror.indexer.DBEntry;
import bzh.plealog.dbmirror.indexer.LuceneStorageSystem;
import bzh.plealog.dbmirror.indexer.StorageSystem;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * This snippet illustrates how to query the Lucene index of a sequence file.
 * 
 * @author Patrick G. Durand
 */
public class FastaQueryTest {

  /**
   * @param args
   */
  public static void main(String[] args) {
    LoggerCentral.configure();
    System.out.println("Query index with ID: " + args[1]);
    long tim = System.currentTimeMillis();
    LuceneStorageSystem lss = new LuceneStorageSystem();
    lss.open(args[0], StorageSystem.READ_MODE);
    DBEntry[] data = lss.getEntry(args[1]);
    if (data!=null) {
      System.out.println("  entry found!");
    }
    else {
      System.out.println("  entry not found!");
    }
    lss.close();
    System.out.println("Running time: " + (System.currentTimeMillis() - tim)
        + " ms.");
  }

}
