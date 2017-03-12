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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import bzh.plealog.dbmirror.fetcher.DBServerConfig;
import bzh.plealog.dbmirror.fetcher.PFTPLoaderDescriptor;
import bzh.plealog.dbmirror.fetcher.PFTPLoaderSystem;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

public class PFTPLoaderSystemTest {

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    UtilsTest.configureApp();
  }

  @Before
  public void setUp() {
    LoggerCentral.setRunning(false);
    LoggerCentral.reset();
  }

  private File install(String dbName) {
    String mainTestsFilePath = UtilsTest
        .getTestFilePath("KFTPLoaderSystemTest");

    DBMSAbstractConfig.setOSDepConfPath(mainTestsFilePath);
    File gdFile = new File(mainTestsFilePath, dbName + ".gd");
    DBServerConfig config = new DBServerConfig();
    try {
      config.load(new File(mainTestsFilePath, dbName + ".dsc")
          .getAbsolutePath());
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    }
    File prodRepository = new File(config.getLocalProdFolder());
    if (prodRepository.exists()) {
      try {
        FileUtils.deleteDirectory(prodRepository);
      } catch (IOException e) {
        Assert.fail(e.getMessage());
      }
      Assert.assertTrue("Prod repository must not exists",
          !prodRepository.exists());
    }

    PFTPLoaderSystem lSystem;
    PFTPLoaderDescriptor fDesc;
    try {
      fDesc = new PFTPLoaderDescriptor(gdFile.getName());
      fDesc.load(new FileInputStream(gdFile.getAbsolutePath()), true);
      lSystem = new PFTPLoaderSystem(new PFTPLoaderDescriptor[] { fDesc });
      lSystem.runProcessing();
    } catch (FileNotFoundException e) {
      Assert.fail(e.getMessage());
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    }

    return prodRepository;
  }

  @Test
  public void installInProduction() {
    File prodRepository = install("uniprot");
    Assert.assertTrue("Prod repository exists", prodRepository.exists());
    Assert
        .assertTrue("d1 file exists", new File(prodRepository, "d1").exists());
    Assert.assertFalse(LoggerCentral.errorMsgEmitted());
  }

  @Test
  public void notInstallInProduction() {
    File prodRepository = install("uniprotNoInProd");
    Assert.assertTrue("Prod repository does not exists",
        !prodRepository.exists());
    Assert.assertTrue("d1 file does not exists",
        !new File(prodRepository, "d1").exists());
    Assert.assertFalse(LoggerCentral.errorMsgEmitted());
  }

  @Test
  public void justInstallInProduction() {
    File prodRepository = install("uniprotJustInProd");
    Assert.assertTrue("Prod repository exists", prodRepository.exists());
    Assert
        .assertTrue("d1 file exists", new File(prodRepository, "d1").exists());
    Assert.assertFalse(LoggerCentral.errorMsgEmitted());
  }

}
