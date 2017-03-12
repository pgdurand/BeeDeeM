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
import bzh.plealog.dbmirror.lucenedico.eggnog.EggNogDicoParser;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptorUtils;
import bzh.plealog.dbmirror.util.descriptor.DescriptorEntry;

public class EggNogDicoParserTest {

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
  public void testDico() {
    List<DescriptorEntry> entries = new ArrayList<DescriptorEntry>();
    DescriptorEntry entry1 = RunningMirrorPanelTest.getEntry(
        "EggNog_index.dsc", false, "databank", "eggnog");
    entries.add(entry1);
    RunningMirrorPanelTest.installerPanel.startLoadingEntries(entries,
        PFTPLoaderDescriptor.MAINTASK_DOWNLOAD);

    entries = new ArrayList<DescriptorEntry>();
    entry1 = RunningMirrorPanelTest.getEntry("EggNog_dico.dsc", false,
        "databank", "eggnog");
    entries.add(entry1);
    RunningMirrorPanelTest.installerPanel.startLoadingEntries(entries,
        PFTPLoaderDescriptor.MAINTASK_DOWNLOAD);

    Assert.assertEquals(10, DefaultLoaderMonitorTest.getNbSequencesInIndex(
        "eggnog", "EggNog_dico.dsc"));

    DicoTermQuerySystem dicos = DicoTermQuerySystem
        .getDicoTermQuerySystem(DBDescriptorUtils
            .getDBMirrorConfig(DBMSAbstractConfig.getLocalMirrorConfFile()));
    DicoTerm term = dicos.getTerm(Dicos.EGGNOG, "NOG:acidNOG00020");
    Object[] data = new Object[4];
    data[0] = "O";
    data[1] = "Cleaves peptides in various proteins in a process that requires ATP hydrolysis. Has a chymotrypsin-like activity. Plays a major role in the degradation of misfolded proteins (By similarity)";
    data[2] = 4;
    data[3] = 3;
    Assert.assertEquals(EggNogDicoParser.entryFormat.format(data),
        term.getDataField());

    term = dicos.getTerm(Dicos.EGGNOG, "NOG:acidNOG00026");
    data = new Object[4];
    data[0] = "V";
    data[1] = "ABC transporter";
    data[2] = 5;
    data[3] = 3;
    Assert.assertEquals(EggNogDicoParser.entryFormat.format(data),
        term.getDataField());

    term = dicos.getTerm(Dicos.EGGNOG, "NOG:acoNOG04001");
    data = new Object[4];
    data[0] = "L";
    data[1] = "";
    data[2] = 4;
    data[3] = 3;
    Assert.assertEquals(EggNogDicoParser.entryFormat.format(data),
        term.getDataField());

    term = dicos.getTerm(Dicos.EGGNOG, "NOG:acoNOG04004");
    data = new Object[4];
    data[0] = "S";
    data[1] = "";
    data[2] = 1;
    data[3] = 1;
    Assert.assertEquals(EggNogDicoParser.entryFormat.format(data),
        term.getDataField());

    term = dicos.getTerm(Dicos.EGGNOG, "NOG:COG0740");
    data = new Object[4];
    data[0] = "O";
    data[1] = "Cleaves peptides in various proteins in a process that requires ATP hydrolysis. Has a chymotrypsin-like activity. Plays a major role in the degradation of misfolded proteins (By similarity)";
    data[2] = 4;
    data[3] = 4;
    Assert.assertEquals(EggNogDicoParser.entryFormat.format(data),
        term.getDataField());

    term = dicos.getTerm(Dicos.EGGNOG, "NOG:COG0553");
    data = new Object[4];
    data[0] = "L";
    data[1] = "helicase";
    data[2] = 2;
    data[3] = 2;
    Assert.assertEquals(EggNogDicoParser.entryFormat.format(data),
        term.getDataField());
  }

}
