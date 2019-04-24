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
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import bzh.plealog.dbmirror.util.sequence.SeqIOUtils;

public class SeqIOUtilsTest {

  @After
  public void tearDown() throws Exception {
  }

  @Before
  public void setUp() {
    // UtilsTest.configureApp();
  }
  @Test
  public void test_accession_1() {
    String id="lcl|XP_021350314.1";
    String acc = SeqIOUtils.getAccessionFromId(id);
    Assert.assertEquals(acc, "XP_021350314.1");
  }
  @Test
  public void test_accession_2() {
    String id="gi|1207922646|ref|XP_021350314.1|";
    String acc = SeqIOUtils.getAccessionFromId(id);
    Assert.assertEquals(acc, "XP_021350314.1");
  }
  @Test
  public void test_accession_3() {
    String id="XP_021350314";
    String acc = SeqIOUtils.getAccessionFromId(id);
    Assert.assertEquals(acc, "XP_021350314");
  }

  @Test
  public void testFileFormat() {
    File tmpFile = null;
    try {
      tmpFile = File.createTempFile("kdmsTest_", null);
      FileUtils.write(tmpFile, ">id\ngggugaggua");
      Assert.assertEquals(SeqIOUtils.FASTARNA,
          SeqIOUtils.guessFileFormat(tmpFile.getAbsolutePath()));
      FileUtils.write(tmpFile, ">id\nacgtagctagc");
      Assert.assertEquals(SeqIOUtils.FASTADNA,
          SeqIOUtils.guessFileFormat(tmpFile.getAbsolutePath()));
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    } finally {
      FileUtils.deleteQuietly(tmpFile);
    }
  }

}
