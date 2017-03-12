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
package test.other.genbank;

import bzh.plealog.dbmirror.indexer.GenbankParser;
import bzh.plealog.dbmirror.indexer.LuceneStorageSystem;
import bzh.plealog.dbmirror.indexer.ParserMonitor;
import bzh.plealog.dbmirror.indexer.StorageSystem;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;

/**
 * This code snippet illustrates how to control the content of an index for the
 * Genbank databank.
 * 
 * @author Patrick G. Durand
 */
public class GenbankIndexReadTest {

  /**
   * @param args
   *          command-line arguments. Two are expected: [0] is the lucene index
   *          and [1] is a Ganbank plain text databank file.
   */
  public static void main(String[] args) {
    DBMSAbstractConfig.configureLog4J("GenbankIndexReadTest");
    System.out.println("Index: " + args[0]);
    System.out.println("DB   : " + args[1]);
    LuceneStorageSystem lss = new LuceneStorageSystem();
    lss.open(args[0], StorageSystem.READ_MODE);
    GenbankParser spp = new GenbankParser();
    // spp.setVerbose(true);
    GBMonitor gbm = new GBMonitor(lss);
    spp.setParserMonitor(gbm);
    long tim = System.currentTimeMillis();
    spp.parse(args[1], null);
    lss.close();
    System.out.println("Time: " + (System.currentTimeMillis() - tim)
        + " ms, Entries: " + gbm.counter);
  }

  private static class GBMonitor implements ParserMonitor {
    private LuceneStorageSystem lss;
    private int                 counter;
    private long                tim;

    public GBMonitor(LuceneStorageSystem lss) {
      this.lss = lss;
      tim = System.currentTimeMillis();
    }

    public void seqFound(String id, String name, String fName, long start,
        long stop, boolean checkRedundancy) {
      counter++;
      if ((counter % 50000) == 0) {
        long delta = System.currentTimeMillis() - tim;
        System.out.println(counter + " (" + delta + " ms)");
        tim = System.currentTimeMillis();
      }
      if (lss.getEntry(id) == null) {
        System.out.println("not found: " + id);
      }
    }

    public void startProcessingFile(String fName, long fSize) {
    }

    public void stopProcessingFile(String file, int entries) {
    }

    public boolean redundantSequenceFound() {
      return false;
    }

  }
}
