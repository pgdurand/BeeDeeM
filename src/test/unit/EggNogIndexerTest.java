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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import bzh.plealog.dbmirror.fetcher.PFTPLoaderDescriptor;
import bzh.plealog.dbmirror.indexer.DBEntry;
import bzh.plealog.dbmirror.indexer.LuceneUtils;
import bzh.plealog.dbmirror.indexer.eggnog.EggNogPreparator;
import bzh.plealog.dbmirror.indexer.eggnog.EggNogSuperKingdoms;
import bzh.plealog.dbmirror.lucenedico.DicoTermQuerySystem;
import bzh.plealog.dbmirror.reader.DBUtils;
import bzh.plealog.dbmirror.util.descriptor.DatabankFormat;
import bzh.plealog.dbmirror.util.descriptor.DescriptorEntry;
import bzh.plealog.dbmirror.util.sequence.SequenceFileManager;
import bzh.plealog.dbmirror.util.sequence.SequenceValidatorCount;
import bzh.plealog.dbmirror.util.sequence.TaxonMatcherHelper;
import bzh.plealog.dbmirror.util.sequence.SequenceValidatorCount.Range;

public class EggNogIndexerTest {

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    UtilsTest.configureApp();
    UtilsTest.cleanInstalledDatabanks("d");

    if (!TaxonMatcherHelper.isNCBITaxonomyInstalled()) {
      RunningMirrorPanelTest.installLocalNCBITaxonomy();
    }
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

  private long getNbSequencesInFile(File file) throws IOException {
    SequenceFileManager sfm = new SequenceFileManager(file.getAbsolutePath(),
        DatabankFormat.swissProt, null, null);
    sfm.setMustCreateAFilteredFile(false);

    Range range = new Range("all", 0, Long.MAX_VALUE);
    sfm.addValidator(new SequenceValidatorCount(Arrays
        .asList(new Range[] { range })));
    sfm.execute();

    return range.getCount();
  }

  @Test
  public void testArchea() {
    List<DescriptorEntry> entries = new ArrayList<DescriptorEntry>();
    DescriptorEntry entry1 = RunningMirrorPanelTest.getEntry(
        "EggNog_index.dsc", false, "databank", "eggnog");
    entries.add(entry1);
    RunningMirrorPanelTest.installerPanel.startLoadingEntries(entries,
        PFTPLoaderDescriptor.MAINTASK_DOWNLOAD);

    entries = new ArrayList<DescriptorEntry>();
    entry1 = RunningMirrorPanelTest.getEntry("EggNog_archea.dsc", false,
        "databank", "eggnog");
    entries.add(entry1);
    RunningMirrorPanelTest.installerPanel.startLoadingEntries(entries,
        PFTPLoaderDescriptor.MAINTASK_DOWNLOAD);

    String[] ids = new String[] { "240015_ACP_3433", "401053_AciPR4_0982",
        "682795_AciX8_3206", "940615_AciX9_1419", "5833_PF13_0308",
        "5850_PKH_111350", "224325_AF0466", "589924_Ferp_1133",
        "572546_Arcpr_1334", "589924_Ferp_1104", "589924_Ferp_2149",
        "693661_Arcve_0816", "224325_AF0018", "589924_Ferp_1036",
        "401053_AciPR4_2165", "240015_ACP_0113", "682795_AciX8_1307",
        "682795_AciX8_1341", "5850_PKH_101550", "5850_PKH_070340",
        "5833_PF11_0254" };
    for (String id : ids) {
      // D:\biobase_tests\p\EggNog_archea\current\EggNog_archea\EggNog_archea.ldx
      DBEntry entry = LuceneUtils.getEntry(
          new File(entry1.getDescriptor().getLocalProdFolder(), entry1
              .getDescriptor().getName() + ".ldx").getAbsolutePath(), id);

      // test read the entry
      File entryFile = DBUtils.readDBEntry(entry.getFName(), entry.getStart(),
          entry.getStop());
      BufferedReader reader = null;
      try {
        reader = new BufferedReader(new FileReader(entryFile));
        DBUtils.readUniProtEntry(reader, 0, 0, false);
      } catch (Exception ex) {
        Assert.fail(ex.getMessage());
      } finally {
        IOUtils.closeQuietly(reader);
      }
    }
  }

  @Test
  public void testIndexer() {
    // tests data are
    // bacteria :
    // 240015 3
    // 401053 4
    // 682795 5
    // 940615 1

    // eukaryota :
    // 5833 3
    // 5850 4

    // archea :
    // 224325 2
    // 589924 4
    // 572546 1
    // 693661 1

    List<DescriptorEntry> entries = new ArrayList<DescriptorEntry>();
    DescriptorEntry entry1 = RunningMirrorPanelTest.getEntry(
        "EggNog_index.dsc", false, "databank", "eggnog");
    entries.add(entry1);
    RunningMirrorPanelTest.installerPanel.startLoadingEntries(entries,
        PFTPLoaderDescriptor.MAINTASK_DOWNLOAD);

    // test index
    int reverseSize = EggNogPreparator.reverseIndex.getSize();
    Assert.assertEquals(28, reverseSize);
    List<String> indexEntries = EggNogPreparator.reverseIndex
        .getById("224325_AF0466");
    Assert.assertEquals(1, indexEntries.size());

    indexEntries = EggNogPreparator.reverseIndex.getById("5850_PKH_111350");
    Assert.assertEquals(2, indexEntries.size());

    indexEntries = EggNogPreparator.reverseIndex.getById("401053_AciPR4_2165");
    Assert.assertEquals(2, indexEntries.size());

    indexEntries = EggNogPreparator.reverseIndex.getById("682795_AciX8_3206");
    Assert.assertEquals(3, indexEntries.size());

    // test size
    Assert.assertEquals(3,
        EggNogPreparator.reverseIndex.getSize("240015_ACP_3433"));
    Assert.assertEquals(4,
        EggNogPreparator.reverseIndex.getSize("401053_AciPR4_2165"));
    Assert.assertEquals(1,
        EggNogPreparator.reverseIndex.getSize("572546_Arcpr_1334"));
    Assert.assertEquals(11,
        EggNogPreparator.reverseIndex.getSize("5850_PKH_111350"));

    // test sequences
    for (EggNogSuperKingdoms superKingdom : EggNogSuperKingdoms.values()) {
      File sequenceFile = new File(EggNogPreparator.sequencesFile.getParent(),
          superKingdom.name() + ".dat");
      Assert.assertTrue(sequenceFile.getAbsolutePath() + " does not exists",
          sequenceFile.exists());
    }

    File all = new File(EggNogPreparator.sequencesFile.getParent(),
        EggNogSuperKingdoms.all.name() + ".dat");
    File bacteria = new File(EggNogPreparator.sequencesFile.getParent(),
        EggNogSuperKingdoms.bacteria.name() + ".dat");
    File eukaryota = new File(EggNogPreparator.sequencesFile.getParent(),
        EggNogSuperKingdoms.eukaryota.name() + ".dat");
    File archea = new File(EggNogPreparator.sequencesFile.getParent(),
        EggNogSuperKingdoms.archea.name() + ".dat");

    try {
      Assert.assertEquals(13, getNbSequencesInFile(bacteria));
      Assert.assertEquals(7, getNbSequencesInFile(eukaryota));
      Assert.assertEquals(8, getNbSequencesInFile(archea));
      Assert.assertEquals(28, getNbSequencesInFile(all));
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }

    String allData = null;
    String eukaryotaData = null;
    try {
      allData = FileUtils.readFileToString(all);
      eukaryotaData = FileUtils.readFileToString(eukaryota);
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }

    Assert.assertTrue(allData
        .contains("ID   224325_AF0018              Reviewed;         915 AA."));
    Assert
        .assertTrue(allData
            .contains("FT   DOMAIN        1     391       db_xref=NOG:arcNOG04042"));
    Assert
        .assertTrue(allData
            .contains("ID   5833_PF13_0308              Reviewed;         1215 AA."));
    Assert.assertTrue(allData
        .contains("FT   DOMAIN        353     791       db_xref=NOG:COG0553"));
    Assert
        .assertTrue(allData
            .contains("FT   DOMAIN        1     906       db_xref=NOG:acoNOG04001"));
    Assert.assertTrue(allData.contains("OS   Plasmodium falciparum."));
    Assert.assertTrue(allData.contains("OX   NCBI_TaxID=5833;"));
    Assert
        .assertTrue(eukaryotaData
            .contains("ID   5833_PF13_0308              Reviewed;         1215 AA."));
    Assert.assertTrue(eukaryotaData
        .contains("FT   DOMAIN        353     791       db_xref=NOG:COG0553"));
    Assert
        .assertTrue(eukaryotaData
            .contains("FT   DOMAIN        1     906       db_xref=NOG:acoNOG04001"));
    Assert.assertTrue(eukaryotaData.contains("OS   Plasmodium falciparum."));
    Assert.assertTrue(eukaryotaData.contains("OX   NCBI_TaxID=5833;"));

  }
}
