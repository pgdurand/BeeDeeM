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
package test.unit;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import bzh.plealog.dbmirror.util.AsperaCmd;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.conf.DBMSConfigurator;

public class AsperaCmdTest {

  private static String key_path;
  private static String remote_url = "anonftp@ftp.ncbi.nlm.nih.gov";
  private static String target_dir = Utils.terminatePath(System.getProperty("java.io.tmpdir"));

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    UtilsTest.configureApp();
    UtilsTest.cleanInstalledDatabanks();
    key_path = DBMSAbstractConfig.getConfigurator().getProperty(DBMSConfigurator.ASPERA_KEY);
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

  private void download(String f){
    AsperaCmd cmd = new AsperaCmd(key_path, remote_url, target_dir);
    assertTrue(cmd.getRemoteFile(f));
    String name = new File(f).getName();
    System.out.println(target_dir+name);
    new File(target_dir+name).delete();
  }
  @Test
  public void testFile1() {
    download("/refseq/H_sapiens/mRNA_Prot/human.1.rna.gbff.gz");
  }

  @Test
  public void testFile2() {
    download("/refseq/H_sapiens/mRNA_Prot/human.1.protein.faa.gz");
  }
}
