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

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import bzh.plealog.dbmirror.fetcher.PFTPLoaderDescriptor;
import bzh.plealog.dbmirror.util.BlastCmd;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.conf.DBMirrorConfig;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptorUtils;
import bzh.plealog.dbmirror.util.descriptor.DescriptorEntry;
import bzh.plealog.dbmirror.util.descriptor.IdxDescriptor;

public class BlastCmdTest {

  private static final Log     LOGGER                  = LogFactory
      .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
          + ".BlastCmdTest");

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    UtilsTest.configureApp();
    UtilsTest.cleanInstalledDatabanks();
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

  private int getNbSequences(String blastPath){
    BlastCmd bc = new BlastCmd();
    return bc.getNbSequences(blastPath);
  }
  private void countSequences(IdxDescriptor descriptor, int control){
    // check nb sequences
    String blastPath = descriptor.getCode().substring(0, descriptor.getCode().lastIndexOf("."));
    LOGGER.debug("Counting sequences for: " + descriptor.getName());
    assertEquals(getNbSequences(blastPath), control);
  }
  @Test
  public void testInfo() {
    // install sample DBs for which we know nb of sequences
    List<DescriptorEntry> entries = new ArrayList<DescriptorEntry>();
    DescriptorEntry entry = RunningMirrorPanelTest.getEntry("sample_Uniprot.dsc",
        false, "databank", "uniprot");
    entries.add(entry);
    entry = RunningMirrorPanelTest.getEntry("sample_Genbank.dsc",
        false, "databank", "genbank");
    entries.add(entry);
    RunningMirrorPanelTest.installerPanel.startLoadingEntries(entries,
        PFTPLoaderDescriptor.MAINTASK_DOWNLOAD);
    // locate the newly installed bank in the bank repository
    String dbMirrorConfFile = DBMSAbstractConfig.getLocalMirrorConfFile();
    DBMirrorConfig conf = DBDescriptorUtils.getDBMirrorConfig(dbMirrorConfFile);
    for (IdxDescriptor descriptor : DBDescriptorUtils.getBlastDbList(conf, false)){
      if (descriptor.getName().equals("Uniprot_Sample")){
        countSequences(descriptor, 10);
      }
      else if (descriptor.getName().equals("Genbank_Sample")){
        countSequences(descriptor, 10);      }
    }
  }
}
