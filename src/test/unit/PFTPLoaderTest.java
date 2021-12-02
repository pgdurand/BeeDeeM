/* Copyright (C) 2007-2021 Ludovic Antin, Patrick Durand
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

import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;

import org.apache.commons.net.ftp.FTPFile;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import bzh.plealog.dbmirror.fetcher.DBServerConfig;
import bzh.plealog.dbmirror.fetcher.DBMSFile;
import bzh.plealog.dbmirror.fetcher.PFTPLoaderEngine;

public class PFTPLoaderTest {

  private DBServerConfig _dbConf;

  @Before
  public void setUp() throws Exception {
    UtilsTest.configureApp();

    _dbConf = new DBServerConfig();
  }

  private DBMSFile createFtpFileToDownload(String filename, String remoteDir, long size) {
    FTPFile ftpFile = new FTPFile();
    ftpFile.setName(filename);
    ftpFile.setTimestamp(Calendar.getInstance());
    ftpFile.setSize(size);
    return new DBMSFile(remoteDir, ftpFile);
  }

  @Test
  public void testDownload() {
    // get the description filepath
    String descriptorFilePath = UtilsTest.getTestFilePath("databank",
        "uniprot_multi", "sample_multiple_uniprot.dsc");

    try {
      _dbConf.load(descriptorFilePath);
    } catch (Exception ex) {
      fail("Unable to load the db descriptor");
    }

    File first = new File(_dbConf.getLocalTmpFolder(), "uniprot-aquaporine.dat");
    File second = new File(_dbConf.getLocalTmpFolder(), "uniprot-glucuronidase.dat");
    File third = new File(_dbConf.getLocalTmpFolder(), "uniprot-kinase.dat");

    first.delete();
    second.delete();
    third.delete();

    ArrayList<DBMSFile> validNames = new ArrayList<DBMSFile>();
    validNames.add(createFtpFileToDownload(first.getName(), _dbConf.getRemoteFolders(), 4478l));
    validNames.add(createFtpFileToDownload(second.getName(), _dbConf.getRemoteFolders(), 421780l));
    validNames.add(createFtpFileToDownload(third.getName(), _dbConf.getRemoteFolders(), 85948l));
    PFTPLoaderEngine loaderEngine = new PFTPLoaderEngine(_dbConf, null,
        validNames);
    ((PFTPLoaderEngine) loaderEngine).setScheduleTime(1000);
    ((PFTPLoaderEngine) loaderEngine).setRetry(3);
    loaderEngine.start();
    try {
      loaderEngine.join();
    } catch (InterruptedException e) {
      Assert.fail(e.getMessage());
    }

    Assert.assertTrue(first.exists());
    Assert.assertTrue(second.exists());
    Assert.assertTrue(third.exists());

    Assert.assertEquals(4478, first.length());
    Assert.assertEquals(421780, second.length());
    Assert.assertEquals(85948, third.length());
  }

}
