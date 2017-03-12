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
package test.other.pfam;

import bzh.plealog.dbmirror.indexer.ParserMonitor;
import bzh.plealog.dbmirror.lucenedico.DicoStorageSystem;
import bzh.plealog.dbmirror.lucenedico.DicoStorageSystemImplem;
import bzh.plealog.dbmirror.lucenedico.DicoTerm;
import bzh.plealog.dbmirror.lucenedico.pfam.PfamNamesParser;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;

/**
 * This code snippet illustrates how to control the content of an index for the
 * Pfam databank.
 * 
 * @author Patrick G. Durand
 */
public class PfamFullIndexReadTest {

  /**
   * @param args
   *          command-line arguments. Two are expected: [0] is the lucene index
   *          and [1] is the Pfam plain text databank file.
   */
  public static void main(String[] args) {
    DBMSAbstractConfig.configureLog4J("InterProFullIndexReadTest");
    System.out.println("Index: " + args[0]);
    System.out.println("DB   : " + args[1]);
    DicoStorageSystem lss = new DicoStorageSystemImplem();
    lss.open(args[0], DicoStorageSystem.READ_MODE);
    PfamNamesParser spp = new PfamNamesParser();
    Monitor gom = new Monitor(lss);
    spp.setParserMonitor(gom);
    long tim = System.currentTimeMillis();
    spp.parse(args[1], null);
    lss.close();
    System.out.println("Time: " + (System.currentTimeMillis() - tim)
        + " ms, Entries: " + gom.counter);
    System.out.println("Entries not found: " + gom.notFound);
  }

  private static class Monitor implements ParserMonitor {
    private DicoStorageSystem lss;
    private int               counter;
    private int               notFound;
    private long              tim;

    public Monitor(DicoStorageSystem lss) {
      this.lss = lss;
      tim = System.currentTimeMillis();
    }

    public void seqFound(String id, String name, String fName, long start,
        long stop, boolean checkRedundancy) {
      counter++;
      if ((counter % 10000) == 0) {
        long delta = System.currentTimeMillis() - tim;
        System.out.println(counter + " (" + delta + " ms)");
        tim = System.currentTimeMillis();
      }
      DicoTerm entry;
      entry = lss.getTerm(id);
      if (entry == null) {
        notFound++;
        System.out.println("not found: " + id + " (" + name + ")");
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
