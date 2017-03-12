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

import org.apache.log4j.BasicConfigurator;

import bzh.plealog.dbmirror.indexer.FastaParser;
import bzh.plealog.dbmirror.indexer.LuceneStorageSystem;
import bzh.plealog.dbmirror.indexer.StorageSystem;

/**
 * This snippet illustrates how to create the Lucene index of a fasta file.
 * 
 * @author Patrick G. Durand
 */
public class FastaIndexerTest {

  /**
   * @param args
   */
  public static void main(String[] args) {
    BasicConfigurator.configure();
    System.out.println("Start indexing: " + args[0]);
    long tim = System.currentTimeMillis();
    LuceneStorageSystem lss = new LuceneStorageSystem();
    FastaParser spp = new FastaParser();
    spp.setVerbose(true);
    lss.open(args[1], StorageSystem.WRITE_MODE);
    spp.parse(args[0], lss);
    lss.close();
    System.out.println("Running time: " + (System.currentTimeMillis() - tim)
        + " ms.");
  }

}
