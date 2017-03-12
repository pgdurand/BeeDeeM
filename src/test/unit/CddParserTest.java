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

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import bzh.plealog.dbmirror.fetcher.PFTPLoaderDescriptor;
import bzh.plealog.dbmirror.lucenedico.DicoTerm;
import bzh.plealog.dbmirror.lucenedico.DicoTermQuerySystem;
import bzh.plealog.dbmirror.lucenedico.Dicos;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptorUtils;
import bzh.plealog.dbmirror.util.descriptor.DescriptorEntry;

public class CddParserTest {

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    UtilsTest.configureApp();
    UtilsTest.cleanInstalledDatabanks();
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    DicoTermQuerySystem.closeDicoTermQuerySystem();
  }

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testInstall() {
    List<DescriptorEntry> entries = new ArrayList<DescriptorEntry>();
    DescriptorEntry entry1 = RunningMirrorPanelTest.getEntry("CDD_terms.dsc",
        false, "databank", "cdd");
    entries.add(entry1);
    RunningMirrorPanelTest.installerPanel.startLoadingEntries(entries,
        PFTPLoaderDescriptor.MAINTASK_DOWNLOAD);
    DefaultLoaderMonitorTest.assertNbOk(50415, "cdd", "CDD_terms.dsc");

    DicoTermQuerySystem dicos = DicoTermQuerySystem
        .getDicoTermQuerySystem(DBDescriptorUtils
            .getDBMirrorConfig(DBMSAbstractConfig.getLocalMirrorConfFile()));
    DicoTerm term = dicos.getTerm(Dicos.CDD, "214956");
    Assert.assertEquals(term.getDataField(),
        "smart00986@Uracil DNA glycosylase superfamily.");
    Assert.assertEquals(Dicos.CDD.getData(term),
        "Uracil DNA glycosylase superfamily.");
    Assert.assertEquals(Dicos.CDD.getLabelledId(term), "smart00986");

    // the last in the file
    Assert
        .assertEquals(
            dicos.getTerm(Dicos.CDD, "215026").getDataField(),
            "smart01104@Spt5 C-terminal nonapeptide repeat binding Spt4. The C-terminal domain of the transcription elongation factor protein Spt5 is necessary for binding to Spt4 to form the functional complex that regulates early transcription elongation by RNA polymerase II. The complex may be involved in pre-mRNA processing through its association with mRNA capping enzymes. This CTD domain carries a regular nonapeptide repeat that can be present in up to 18 copies, as in S. pombe. The repeat has a characteristic TPA motif.");
    // the first one
    Assert.assertEquals(dicos.getTerm(Dicos.CDD, "214330").getDataField(),
        "CHL00001@RNA polymerase beta subunit");

    // not a PSSM id
    term = dicos.getTerm(Dicos.CDD, "245235");
    Assert.assertNull(term);
    Assert.assertEquals(Dicos.CDD.getData(term), DicoTerm.EMPTY_DESCRIPTION);
    Assert.assertEquals(Dicos.CDD.getLabelledId(term), "");
  }
}
