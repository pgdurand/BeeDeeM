/* Copyright (C) 2006-2017 Ludovic Antin
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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import bzh.plealog.dbmirror.main.CmdLineQuery;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;

/**
 * Unit tests of the CmdLine Query Tool.
 * 
 * @author Patrick Durand
 */
public class CmdLineQueryTest {

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    UtilsTest.configureApp();
    UtilsTest.cleanInstalledDatabanks();
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    UtilsTest.cleanInstalledDatabanks();
  }

  @After
  public void tearDown() {
    try {
      FileUtils.deleteDirectory(new File(DBMSAbstractConfig.getLocalMirrorPath()));
    } catch (IOException e) {
    }
  }

  public boolean compareTwoFiles(File file1, File file2)
      throws IOException {

    BufferedReader br1 = new BufferedReader(new FileReader(file1));
    BufferedReader br2 = new BufferedReader(new FileReader(file2));

    String thisLine = null;
    String thatLine = null;

    List<String> list1 = new ArrayList<String>();
    List<String> list2 = new ArrayList<String>();

    while ((thisLine = br1.readLine()) != null) {
      if (thisLine.isEmpty()==false)
        list1.add(thisLine);
    }
    while ((thatLine = br2.readLine()) != null) {
      if (thatLine.isEmpty()==false)
        list2.add(thatLine);
    }

    br1.close();
    br2.close();

    return list1.equals(list2);
  }
  
  protected PrintStream outputFile(File name) throws FileNotFoundException {
    return new PrintStream(new BufferedOutputStream(new FileOutputStream(name)), true);
  }

  @Test
  public void testCmdLine() {
    // delete the db_mirror directory to be sure that only one bank is installed
    File mirrorPath = new File(DBMSAbstractConfig.getLocalMirrorPath());
    if (mirrorPath.exists()) {
      try {
        FileUtils.cleanDirectory(mirrorPath);
        Thread.sleep(2000);
      } catch (Exception e) {
        e.printStackTrace();
        Assert.fail("Unable to clean the directory : " + mirrorPath.getAbsolutePath());
      }
    }
    // install the uniprot databank
    DefaultLoaderMonitorTest.completeInstall("uniprot", "sample_Uniprot.dsc", true);

    String[] args = {
        "-d", "protein", 
        "-i", "KKCC1_RAT",
        "-f", "txt"};
    
    File refFile = new File(UtilsTest.getTestFilePath("Tools", "query.dat"));

    File result=null;
    try {
      result = File.createTempFile("bdmQueryTest", ".dat");
      result.deleteOnExit();
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }
    try {
      System.setOut(outputFile(result));
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail();
    }

    Assert.assertTrue(CmdLineQuery.doJob(args));
    try {
      Assert.assertTrue(compareTwoFiles(refFile, result));
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }
  }
}
