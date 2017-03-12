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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.BasicConfigurator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import bzh.plealog.dbmirror.util.runner.DBMSUniqueSeqIdDetector;

public class DBMSUniqueSeqIdDetectorTest {

  private String                  indexFilePath = "KDMSUniqueSeqIdDetectorTest.ldx";
  private DBMSUniqueSeqIdDetector detector;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    BasicConfigurator.configure();
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
  }

  @Before
  public void setUp() throws Exception {
    detector = new DBMSUniqueSeqIdDetector(indexFilePath);
  }

  @After
  public void tearDown() throws Exception {
    detector.closeIndex();
    FileUtils.deleteQuietly(new File(indexFilePath));
  }

  @Test
  public void testInHashset() {
    DBMSUniqueSeqIdDetector.MAX_SEQID = 10;
    assertTrue(detector.add("1"));
    assertTrue(detector.add("2"));
    assertFalse(detector.add("1"));
  }

  @Test
  public void testInIndex() {
    DBMSUniqueSeqIdDetector.MAX_SEQID = 2;
    assertTrue(detector.add("1"));
    assertTrue(detector.add("2"));
    assertTrue(detector.add("3"));
    assertFalse(detector.add("1"));
  }

  @Test
  public void testInIndex2() {
    DBMSUniqueSeqIdDetector.MAX_SEQID = 2;
    assertTrue(detector.add("1"));
    assertTrue(detector.add("2"));
    assertTrue(detector.add("3"));
    assertTrue(detector.add("4"));
    assertTrue(detector.add("5"));
    assertTrue(detector.add("6"));
    assertFalse(detector.add("2"));
  }

  @Test
  public void testWithFile() {
    String filePath = DefaultLoaderMonitorTest.getTestDatabankFilePath(
        "fasta_multi2", "fasta2.faa");
    DBMSUniqueSeqIdDetector.MAX_SEQID = 1000;
    BufferedReader reader = null;
    String line;
    try {
      reader = new BufferedReader(new InputStreamReader(new FileInputStream(
          new File(filePath)), "UTF-8"));
      while ((line = reader.readLine()) != null) {
        if (line.startsWith(">")) {
          line = line.substring(1).trim();
          int idx = line.indexOf(' ');
          if (idx != -1) {
            line = line.substring(0, idx);
          }
          UtilsTest.start();
          detector.add(line);
          UtilsTest.stop("add");
        }
      }

    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      IOUtils.closeQuietly(reader);
    }
    UtilsTest.displayDurations("");
  }

}
