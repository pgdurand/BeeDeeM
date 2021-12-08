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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import bzh.plealog.dbmirror.indexer.DBEntry;
import bzh.plealog.dbmirror.indexer.LuceneStorageSystem;
import bzh.plealog.dbmirror.indexer.StorageSystem;
import bzh.plealog.dbmirror.reader.DBUtils;
import bzh.plealog.dbmirror.reader.PFormatter;
import bzh.plealog.dbmirror.reader.PSequence;

public class SwissProtReaderTest {
  @SuppressWarnings("unused")
  private static void testLuceneIndexer() {
    DBEntry[] entries;

    LuceneStorageSystem lss = new LuceneStorageSystem();
    lss.open("spindex", StorageSystem.READ_MODE);
    entries = lss.getEntry("CYC_BOVIN");
    if (entries != null) {
      System.out.println("Got " + entries.length + " entries.");
      for (int i = 0; i < entries.length; i++) {
        System.out.println("[" + (i + 1) + "]: " + entries[i]);
      }
    } else {
      System.out.println("Got no entries.");
    }
    lss.close();

  }

  private static void testReadOnly(String file) {
    PSequence seq = DBUtils.readUniProtEntry(new File(file), 0, 0, false);
    Writer w = new BufferedWriter(new OutputStreamWriter(System.out));
    PFormatter formatter = new PFormatter(PFormatter.FORMAT.F_INSD_FORMAT, w, null);
    formatter.dump(seq);
    try {
      w.flush();
      w.close();
    } catch (IOException e) {
    }
  }

  public static void main(String[] args) {
    testReadOnly("./tests/junit/DBXrefManager/B2R6X2.up");
  }

}
