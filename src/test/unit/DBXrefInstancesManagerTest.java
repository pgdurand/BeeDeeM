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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import bzh.plealog.dbmirror.util.sequence.SeqIOUtils;
import bzh.plealog.dbmirror.util.xref.DBXrefInstancesManager;
import bzh.plealog.dbmirror.util.xref.DBXrefTagManager;

public class DBXrefInstancesManagerTest {

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

  /**
   * Extract DBXrefs from a file with the parameter manager
   * 
   * @param filePath
   * @param manager
   * @return
   */
  private String getDbXrefs(String filePath, DBXrefTagManager manager) {
    DBXrefInstancesManager instances = new DBXrefInstancesManager();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(
        new FileInputStream(filePath), "UTF-8"))) {
      String line, xref;
      while ((line = reader.readLine()) != null) {
        xref = manager.getDbXref(line);
        if (xref != null) {
          instances.addInstance(xref);
        }
      }
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }

    return instances.toString();
  }

  @Test
  public void testOne() {
    DBXrefTagManager manager = new DBXrefTagManager();
    manager.initialise(SeqIOUtils.DEFAULT_CONFIG_XREF_RETRIEVE);
    String result = getDbXrefs(
        UtilsTest.getTestFilePath("DBXrefManager", "p12265.dat"), manager);
    Assert
        .assertEquals(
            "[[taxon:10090;EC:3.2.1.31;GO:0005783,0005764,0005792,0004566,0043169,0005975;Pfam:PF00703,PF02836,PF02837;InterPro:IPR008979,IPR006101,IPR013812,IPR006104,IPR006102,IPR006103,IPR017853,IPR013781]]",
            result);
    Assert
        .assertEquals(
            "[taxon; 10090, EC; 3.2.1.31, GO; GO:0005783, GO; GO:0005764, GO; GO:0005792, GO; GO:0004566, GO; GO:0043169, GO; GO:0005975, Pfam; PF00703, Pfam; PF02836, Pfam; PF02837, InterPro; IPR008979, InterPro; IPR006101, InterPro; IPR013812, InterPro; IPR006104, InterPro; IPR006102, InterPro; IPR006103, InterPro; IPR017853, InterPro; IPR013781]",
            DBXrefInstancesManager.getDbXrefs(result).toString());
  }

  @Test
  public void testTwo() {
    DBXrefTagManager manager = new DBXrefTagManager();
    manager.initialise(SeqIOUtils.DEFAULT_CONFIG_XREF_RETRIEVE);
    String result = getDbXrefs(
        UtilsTest.getTestFilePath("DBXrefManager", "z78540.dat"), manager);
    Assert.assertEquals("[[taxon:6239]]", result);
    Assert.assertEquals("[taxon; 6239]",
        DBXrefInstancesManager.getDbXrefs(result).toString());
  }

  @Test
  public void testThree() {
    String testFilePath = UtilsTest.getTestFilePath("DBXrefManager",
        "usp26.dat");
    DBXrefTagManager manager = new DBXrefTagManager();
    manager.initialise(SeqIOUtils.DEFAULT_CONFIG_XREF_RETRIEVE);
    String result = getDbXrefs(testFilePath, manager);
    Assert.assertEquals("[[taxon:10090]]", result);
    Assert.assertEquals("[taxon; 10090]",
        DBXrefInstancesManager.getDbXrefs(result).toString());

    String newConfig = SeqIOUtils.DEFAULT_CONFIG_XREF_RETRIEVE
        + "\"/db_xref=\",\"GeneID\", \":\", \"\"\", \"GeneID\", \"$\"\n";
    manager.initialise(newConfig);
    result = getDbXrefs(testFilePath, manager);
    Assert.assertEquals("[[taxon:10090;GeneID:83563]]", result);
    Assert.assertEquals("[taxon; 10090, GeneID; 83563]", DBXrefInstancesManager
        .getDbXrefs(result).toString());
  }
}
