/* Copyright (C) 2007-2017 Ludovic Antin
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
package test.unit;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import bzh.plealog.dbmirror.fetcher.DBMSFile;
import bzh.plealog.dbmirror.fetcher.DBServerConfig;
import bzh.plealog.dbmirror.fetcher.NameMatcher;
import bzh.plealog.dbmirror.fetcher.PLocalLoader;
import bzh.plealog.dbmirror.util.Utils;

public class PLocalLoaderTest {

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    UtilsTest.configureApp();
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
  }

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testLoadLocalFiles() {
    TestConfig config = new TestConfig();
    PLocalLoader loader = new PLocalLoader(config);
    List<DBMSFile> result = new ArrayList<DBMSFile>();

    if (!loader.initFilesList(result)) {
      Assert.fail("Unable to get files");
    }

    Assert.assertEquals(6, result.size());
  }

  private class TestConfig extends DBServerConfig {
    @Override
    public String getRemoteLocalFolders() {
      return UtilsTest.getTestFilePath("KLocalLoader", "initFilesList", "work",
          "rel.*", "genomes").replace(File.separatorChar, '|');
    }

    @Override
    public NameMatcher getFileMatcher() {
      String[] incPatterns = Utils.tokenize("file.*txt");
      return new NameMatcher(incPatterns, null);
    }
  }

}
