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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import bzh.plealog.dbmirror.fetcher.PFTPLoaderDescriptor;
import bzh.plealog.dbmirror.lucenedico.DicoTermQuerySystem;
import bzh.plealog.dbmirror.ui.RunningMirrorPanel;
import bzh.plealog.dbmirror.util.conf.Configuration;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.descriptor.DescriptorEntry;

public class RunningMirrorPanelTest {

  public static RunningMirrorPanel installerPanel         = new RunningMirrorPanel();

  private static File              enzymeInstallDir       = null;
  private static File              goInstallDir           = null;
  private static File              ncbiTaxonomyInstallDir = null;
  private static File              uniprotInstallDir      = null;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    UtilsTest.configureApp();
    UtilsTest.cleanInstalledDatabanks();
    enzymeInstallDir = new File(DBMSAbstractConfig.getLocalMirrorPath(),
        "d/Enzyme/current/Enzyme");
    goInstallDir = new File(DBMSAbstractConfig.getLocalMirrorPath(),
        "d/GeneOntology_terms/current/GeneOntology_terms");
    ncbiTaxonomyInstallDir = new File(DBMSAbstractConfig.getLocalMirrorPath(),
        "d/NCBI_Taxonomy/current/NCBI_Taxonomy");
    uniprotInstallDir = new File(DBMSAbstractConfig.getLocalMirrorPath(),
        "p/Uniprot_Sample/current/Uniprot_Sample");
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    DicoTermQuerySystem.closeDicoTermQuerySystem();
  }

  @Before
  public void setUp() throws Exception {
    UtilsTest.cleanInstalledDatabanks();
  }

  @After
  public void tearDown() throws Exception {
  }

  public static void installLocalNCBITaxonomy() {
    List<DescriptorEntry> entries = new ArrayList<DescriptorEntry>();
    DescriptorEntry entry1 = getEntry("localTaxonomy.dsc", false,
        "RunningMirrorPanel", "databanks");
    entries.add(entry1);
    installerPanel.startLoadingEntries(entries,
        PFTPLoaderDescriptor.MAINTASK_DOWNLOAD);
  }

  public static DescriptorEntry getEntry(String fileName, boolean copyToConf,
      String dir1, String dir2) {
    DescriptorEntry result = null;
    String dscFilePath = UtilsTest.getTestFilePath(dir1, dir2, fileName);
    try {
      File dscFile = new File(dscFilePath);

      // to be able to run unit tests on different hosts : set the absolute path
      // to data files in .dsc files
      String content = FileUtils.readFileToString(dscFile);
      content = content.replace("${local}",
          dscFile.getParent().replace('\\', '/'));
      File newDscFile = new File(dscFile.getParent(), "_" + dscFile.getName());
      FileUtils.write(newDscFile, content);
      newDscFile.deleteOnExit();

      result = DescriptorEntry.createFrom(newDscFile);
      if (copyToConf) {
        File destFile = new File(DBMSAbstractConfig.getConfPath(Configuration.DESCRIPTOR),
            dscFile.getName());
        FileUtils.copyFile(newDscFile, destFile);
        destFile.deleteOnExit();
      }
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
    return result;
  }

  private static DescriptorEntry getEntry(String fileName, boolean copyToConf) {
    return getEntry(fileName, copyToConf, "RunningMirrorPanel", "depends");
  }

  @Test
  public void testNoDepends() {
    Assert.assertFalse(enzymeInstallDir.exists());
    Assert.assertFalse(goInstallDir.exists());
    Assert.assertFalse(ncbiTaxonomyInstallDir.exists());
    List<DescriptorEntry> entries = new ArrayList<DescriptorEntry>();

    DescriptorEntry entry1 = getEntry("uniprotNoDepends.dsc", false);
    entries.add(entry1);

    DescriptorEntry entry2 = getEntry("fastaNoDepends.dsc", false);
    entries.add(entry2);

    installerPanel.startLoadingEntries(entries,
        PFTPLoaderDescriptor.MAINTASK_DOWNLOAD);
    Assert.assertTrue(new File(entry1.getDescriptor().getLocalProdFolder())
        .exists());
    Assert.assertTrue(new File(entry2.getDescriptor().getLocalProdFolder())
        .exists());
    Assert.assertFalse(enzymeInstallDir.exists());
    Assert.assertFalse(goInstallDir.exists());
    Assert.assertFalse(ncbiTaxonomyInstallDir.exists());
  }

  @Test
  public void testDepends() {
    Assert.assertFalse(enzymeInstallDir.exists());
    Assert.assertFalse(goInstallDir.exists());
    Assert.assertFalse(ncbiTaxonomyInstallDir.exists());
    List<DescriptorEntry> entries = new ArrayList<DescriptorEntry>();

    DescriptorEntry entry1 = getEntry("uniprotWithDepends.dsc", false);
    entries.add(entry1);

    DescriptorEntry entry2 = getEntry("fastaWithDepends.dsc", false);
    entries.add(entry2);

    // just for copy to local conf folder
    getEntry("localEnzyme.dsc", true, "RunningMirrorPanel", "databanks");
    getEntry("localTaxonomy.dsc", true, "RunningMirrorPanel", "databanks");
    getEntry("localGeneOntology.dsc", true, "RunningMirrorPanel", "databanks");

    installerPanel.startLoadingEntries(entries,
        PFTPLoaderDescriptor.MAINTASK_DOWNLOAD);
    Assert.assertTrue(new File(entry1.getDescriptor().getLocalProdFolder())
        .exists());
    Assert.assertTrue(new File(entry2.getDescriptor().getLocalProdFolder())
        .exists());
    Assert.assertTrue(enzymeInstallDir.exists());
    Assert.assertTrue(goInstallDir.exists());
    Assert.assertTrue(ncbiTaxonomyInstallDir.exists());
  }

  @Test
  public void testCircularDepends() {
    Assert.assertFalse(enzymeInstallDir.exists());
    List<DescriptorEntry> entries = new ArrayList<DescriptorEntry>();

    DescriptorEntry entry1 = getEntry("uniprotDependsFasta.dsc", true);
    entries.add(entry1);

    DescriptorEntry entry2 = getEntry("fastaDependsUniprot.dsc", true);
    entries.add(entry2);

    installerPanel.startLoadingEntries(entries,
        PFTPLoaderDescriptor.MAINTASK_DOWNLOAD);
    Assert.assertTrue(new File(entry1.getDescriptor().getLocalProdFolder())
        .exists());
    Assert.assertTrue(new File(entry2.getDescriptor().getLocalProdFolder())
        .exists());
    Assert.assertFalse(enzymeInstallDir.exists());
  }

  @Test
  public void testRecursiveDepends() {
    Assert.assertFalse(enzymeInstallDir.exists());
    List<DescriptorEntry> entries = new ArrayList<DescriptorEntry>();

    DescriptorEntry entry1 = getEntry("genbankDependsFasta.dsc", true);
    entries.add(entry1);

    DescriptorEntry entry2 = getEntry("fastaDependsUniprot.dsc", true);
    entries.add(entry2);

    DescriptorEntry entry3 = getEntry("uniprotDependsFasta.dsc", true);
    entries.add(entry3);

    installerPanel.startLoadingEntries(entries,
        PFTPLoaderDescriptor.MAINTASK_DOWNLOAD);
    Assert.assertTrue(new File(entry1.getDescriptor().getLocalProdFolder())
        .exists());
    Assert.assertTrue(new File(entry2.getDescriptor().getLocalProdFolder())
        .exists());
    Assert.assertTrue(new File(entry3.getDescriptor().getLocalProdFolder())
        .exists());
    Assert.assertFalse(enzymeInstallDir.exists());
  }

  @Test
  public void priorityDepends() {
    Assert.assertFalse(uniprotInstallDir.exists());
    List<DescriptorEntry> entries = new ArrayList<DescriptorEntry>();

    getEntry("uniprotNoDepends.dsc", true);
    DescriptorEntry entry1 = getEntry("priorityDepends.dsc", true);
    entries.add(entry1);

    installerPanel.startLoadingEntries(entries,
        PFTPLoaderDescriptor.MAINTASK_DOWNLOAD);
    Assert.assertTrue(uniprotInstallDir.exists());
    Assert.assertTrue(new File(entry1.getDescriptor().getLocalProdFolder())
        .exists());
  }

}
