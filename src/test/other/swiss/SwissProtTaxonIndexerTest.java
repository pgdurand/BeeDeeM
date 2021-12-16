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

import java.io.File;

import bzh.plealog.dbmirror.indexer.LuceneStorageSystem;
import bzh.plealog.dbmirror.indexer.StorageSystem;
import bzh.plealog.dbmirror.indexer.SwissProtParser;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

public class SwissProtTaxonIndexerTest {

  private static String getConfPath() {
    String path;

    path = System.getProperty("KDMS_CONF_DIR");
    if (path != null)
      path = Utils.terminatePath(path);
    else
      path = Utils.terminatePath(System.getProperty("user.dir")) + "conf/kb"
          + File.separator;
    return path;
  }

  public static void main(String[] args) {
    String confPath = getConfPath();
    DBMSAbstractConfig.configureLog4J("kdmsUI");
    DBMSAbstractConfig.setConfPath(confPath);

    DBMSAbstractConfig.initializeConfigurator(confPath
        + DBMSAbstractConfig.MASTER_CONF_FILE);
    LoggerCentral.reset();
    LoggerCentral.configure();
    System.out.println("Start indexing: " + args[0]);
    long tim = System.currentTimeMillis();
    LuceneStorageSystem lss = new LuceneStorageSystem();
    SwissProtParser spp = new SwissProtParser();
    spp.setVerbose(true);
    spp.setTaxonomyFilter("2759", null);
    lss.open(args[1], StorageSystem.WRITE_MODE);
    spp.parse(args[0], lss);
    lss.close();
    System.out.println("Entries matching criteria: " + spp.getEntries());
    System.out.println("Running time: " + (System.currentTimeMillis() - tim)
        + " ms.");
  }

}
