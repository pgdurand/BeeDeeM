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
package test.other.swiss;

import bzh.plealog.dbmirror.indexer.LuceneStorageSystem;
import bzh.plealog.dbmirror.indexer.StorageSystem;
import bzh.plealog.dbmirror.indexer.SwissProtParser;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * This code snippet illustrates how to filter a Swissprot data file by taxonomy
 * ID. It requires Taxonomy bank installed on your system.
 * 
 * @author Patrick G. Durand
 */
public class SwissProtIndexerTest {

  /**
   * @param args
   */
  public static void main(String[] args) {
    LoggerCentral.configure();
    System.out.println("Start indexing: " + args[0]);
    long tim = System.currentTimeMillis();
    LuceneStorageSystem lss = new LuceneStorageSystem();
    // path to your local databank starage directory
    DBMSAbstractConfig.setLocalMirrorConfFile(args[1] + "/dbmirror.config");
    SwissProtParser spp = new SwissProtParser();
    spp.setTaxonomyFilter("10239", "");
    spp.setVerbose(true);
    lss.open(args[1], StorageSystem.WRITE_MODE);
    spp.parse(args[0], lss);
    lss.close();
    System.out.println("Running time: " + (System.currentTimeMillis() - tim)
        + " ms.");
  }

}
