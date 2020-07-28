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

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import bzh.plealog.dbmirror.lucenedico.DicoTermQuerySystem;
import bzh.plealog.dbmirror.util.descriptor.DatabankFormat;
import bzh.plealog.dbmirror.util.sequence.SequenceFileManager;
import bzh.plealog.dbmirror.util.sequence.SequenceValidatorCutFile;
import bzh.plealog.dbmirror.util.sequence.SequenceValidatorDescription;
import bzh.plealog.dbmirror.util.sequence.SequenceValidatorRenamer;
import bzh.plealog.dbmirror.util.sequence.SequenceValidatorSize;
import bzh.plealog.dbmirror.util.sequence.SequenceValidatorTaxon;
import bzh.plealog.dbmirror.util.sequence.TaxonMatcherHelper;

public class SequenceFileManagerTest {

  private File filteredFile;

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

  @After
  public void tearDown() throws Exception {
  }

  @Before
  public void setUp() {

  }

  private String getTestFilePath(String methodName, String fileName) {
    return UtilsTest.getTestFilePath("SequenceFileManager", methodName,
        fileName);
  }

  public SequenceFileManager initSequenceFileManager(String methodName,
      String fileName, DatabankFormat databankFormat) throws IOException {
    String sequenceFilePath = getTestFilePath(methodName, fileName);
    return new SequenceFileManager(sequenceFilePath, databankFormat, null, null);
  }

  public int getNbIdsInFile(String filePathToRead, String idChar) {
    String line;
    int ids = 0;
    try {
      BufferedReader reader = new BufferedReader(new FileReader(filePathToRead));
      while ((line = reader.readLine()) != null) {
        if (idChar.isEmpty() || line.startsWith(idChar)) {
          ids++;
        }
      }
      reader.close();
    } catch (IOException e) {
      fail("Unable to read the file containing the number of sequences");
    }
    return ids;
  }

  public boolean contains(File f, String searchString) {
    Scanner in = null;
    try {
      in = new Scanner(new FileReader(f));
      while (in.hasNextLine()) {
        if (in.nextLine().indexOf(searchString) >= 0) {
          return true;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        in.close();
      } catch (Exception e) { /* ignore */
      }
    }
    return false;
  }

  public boolean containsId(File file, String idChar, String id) {
    String line;

    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {

      while ((line = reader.readLine()) != null) {
        if (idChar.isEmpty() || line.startsWith(idChar)) {
          line = line.replace(idChar, "").trim();
          if (line.startsWith(id)) {
            return true;
          }
        }
      }
      reader.close();
    } catch (IOException e) {
      fail("Unable to read the file containing the number of sequences");
    }
    return false;
  }

  public int getNbLinesContainsDescription(File file, String descriptionChar,
      String description) {
    String line;
    int descriptions = 0;
    try {
      BufferedReader reader = new BufferedReader(new FileReader(file));
      while ((line = reader.readLine()) != null) {
        if (line.startsWith(descriptionChar)) {
          if (line.contains(description)) {
            descriptions++;
          }
        }
      }
      reader.close();
    } catch (IOException e) {
      fail("Unable to read the file containing the number of sequences");
    }
    return descriptions;
  }

  public void checkResults(SequenceFileManager sfm, int nbFound, int nbDiscarded) {
    Assert.assertEquals(
        nbFound,
        getNbIdsInFile(filteredFile.getAbsolutePath(), sfm.getDatabankFormat()
            .getIdString()));
    Assert.assertEquals(nbFound, sfm.getNbSequencesFound());
    Assert.assertEquals(nbDiscarded, sfm.getNbSequencesDiscarded());
  }

  private void testRenameIds(String fileName, DatabankFormat databankFormat,
      int nbEntries) {
    try {
      SequenceFileManager sfm = initSequenceFileManager("testRename", fileName,
          databankFormat);
      SequenceValidatorRenamer validator = new SequenceValidatorRenamer();
      validator.setUpdateIds(true);
      sfm.addValidator(validator);

      // test just id
      filteredFile = sfm.execute().get(0);
      checkResults(sfm, nbEntries, 0);
      for (int i = 1; i <= nbEntries; i++) {
        Assert.assertTrue(containsId(filteredFile,
            databankFormat.getIdString(), String.valueOf(i)));
      }

      // test prefix with spaces
      sfm = initSequenceFileManager("testRename", fileName, databankFormat);
      validator = new SequenceValidatorRenamer();
      validator.setUpdateIds(true);
      validator.setIdPrefix("a nice prefix");
      sfm.addValidator(validator);
      filteredFile = sfm.execute().get(0);
      for (int i = 1; i <= nbEntries; i++) {
        Assert.assertTrue(containsId(filteredFile,
            databankFormat.getIdString(), "a_nice_prefix" + i));
      }

      // test suffix with spaces
      sfm = initSequenceFileManager("testRename", fileName, databankFormat);
      validator = new SequenceValidatorRenamer();
      validator.setUpdateIds(true);
      validator.setIdSuffix(" a very beautiful suffix");
      sfm.addValidator(validator);
      filteredFile = sfm.execute().get(0);
      for (int i = 1; i <= nbEntries; i++) {
        Assert.assertTrue(containsId(filteredFile,
            databankFormat.getIdString(), i + "_a_very_beautiful_suffix"));
      }

      // test boths and tabs
      sfm = initSequenceFileManager("testRename", fileName, databankFormat);
      validator = new SequenceValidatorRenamer();
      validator.setUpdateIds(true);
      validator.setIdPrefix("a\tprefix\tvery long to see if it works ");
      validator.setIdSuffix(" a very very\tvery\tvery long\tsuffix");
      sfm.addValidator(validator);
      filteredFile = sfm.execute().get(0);
      for (int i = 1; i <= nbEntries; i++) {
        Assert.assertTrue(containsId(filteredFile,
            databankFormat.getIdString(),
            "a_prefix_very_long_to_see_if_it_works_" + i
                + "_a_very_very_very_very_long_suffix"));
      }

    } catch (IOException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testRenameIds() {
    testRenameIds("fastaFile.fas", DatabankFormat.fasta, 10);
    testRenameIds("genbank.dat", DatabankFormat.genbank, 10);
    testRenameIds("uniprot.dat", DatabankFormat.swissProt, 10);
  }

  @Test
  public void testRenameDescFasta() {
    try {
      String description = "what a beautiful description";
      SequenceFileManager sfm = initSequenceFileManager("testRename",
          "fastaFile.fas", DatabankFormat.fasta);
      SequenceValidatorRenamer validator = new SequenceValidatorRenamer();
      validator.setUpdateDescriptions(true);
      validator.setNewDescription(description);
      sfm.addValidator(validator);
      filteredFile = sfm.execute().get(0);
      checkResults(sfm, 10, 0);
      Assert.assertEquals(
          10,
          getNbLinesContainsDescription(filteredFile,
              DatabankFormat.fasta.getBeginDescriptionString(), description));
      // id with no desc and with a space char after
      Assert.assertTrue(this.contains(filteredFile,
          ">gi|595582364|ref|NM_001290753.1| " + description));
      // id with no desc and no space char after
      Assert.assertTrue(this.contains(filteredFile,
          ">gi|594551389|gb|KF989488.1| " + description));

      sfm = initSequenceFileManager("testRename", "fastaFile.fas",
          DatabankFormat.fasta);
      validator = new SequenceValidatorRenamer();
      validator.setUpdateDescriptions(true);
      validator.setUsePreviousId(true);
      validator.setUpdateIds(true);
      sfm.addValidator(validator);
      filteredFile = sfm.execute().get(0);
      checkResults(sfm, 10, 0);
      for (int i = 1; i <= 10; i++) {
        Assert.assertTrue(containsId(filteredFile,
            DatabankFormat.fasta.getIdString(), String.valueOf(i)));
      }
      Assert.assertTrue(this.contains(filteredFile,
          " gi|594551389|gb|KF989488.1|"));
      Assert.assertTrue(this.contains(filteredFile,
          " gi|594550953|gb|KF986170.1|"));
      Assert.assertTrue(this.contains(filteredFile,
          " gi|594550943|gb|KF986165.1|"));
      Assert.assertTrue(this.contains(filteredFile,
          " gi|522577967|gb|KC814772.1|"));
      Assert.assertTrue(this.contains(filteredFile,
          " gi|595582407|ref|NM_001080826.2|"));
      Assert.assertTrue(this.contains(filteredFile,
          " gi|595582364|ref|NM_001290753.1|"));
      Assert.assertTrue(this.contains(filteredFile,
          " gi|595582353|ref|NM_001290723.1|"));
      Assert.assertTrue(this.contains(filteredFile,
          " gi|595582112|ref|NM_001290724.1|"));
      Assert.assertTrue(this.contains(filteredFile,
          " gi|595508533|gb|JEMT01000476.1|"));
      Assert.assertTrue(this.contains(filteredFile,
          " gi|594553085|gb|KF528683.1|"));

      sfm = initSequenceFileManager("testRename", "fastaFile.fas",
          DatabankFormat.fasta);
      validator = new SequenceValidatorRenamer();
      validator.setUpdateDescriptions(true);
      sfm.addValidator(validator);
      filteredFile = sfm.execute().get(0);
      checkResults(sfm, 10, 0);
      Assert.assertFalse(this.contains(filteredFile,
          "Physeter catodon creatine"));
      Assert.assertFalse(this.contains(filteredFile,
          "Homo sapiens homolog of rat pragma of Rnd2 (SGK223), mRNA"));
      Assert.assertFalse(this.contains(filteredFile,
          "histidine kinase (cheA) gene, partial cds"));

    } catch (IOException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testRenameDescGenbank() {
    try {
      String description = "what a beautiful description";
      SequenceFileManager sfm = initSequenceFileManager("testRename",
          "genbank.dat", DatabankFormat.genbank);
      SequenceValidatorRenamer validator = new SequenceValidatorRenamer();
      validator.setUpdateDescriptions(true);
      validator.setNewDescription(description);
      sfm.addValidator(validator);
      filteredFile = sfm.execute().get(0);
      checkResults(sfm, 10, 0);
      Assert.assertEquals(
          10,
          getNbLinesContainsDescription(filteredFile,
              DatabankFormat.genbank.getBeginDescriptionString(), description));
      // a second line description must have disappeared
      Assert.assertFalse(this.contains(filteredFile, "(CKMT2), mRNA."));
      // a description was empty
      Assert.assertTrue(this.contains(filteredFile,
          DatabankFormat.genbank.getBeginDescriptionString() + " "
              + description));

      sfm = initSequenceFileManager("testRename", "genbank.dat",
          DatabankFormat.genbank);
      validator = new SequenceValidatorRenamer();
      validator.setUpdateDescriptions(true);
      validator.setUsePreviousId(true);
      validator.setUpdateIds(true);
      sfm.addValidator(validator);
      filteredFile = sfm.execute().get(0);
      checkResults(sfm, 10, 0);
      for (int i = 1; i <= 10; i++) {
        Assert.assertTrue(containsId(filteredFile,
            DatabankFormat.genbank.getIdString(), String.valueOf(i)));
      }
      Assert.assertTrue(this.contains(filteredFile, " NM_001080826"));
      Assert.assertTrue(this.contains(filteredFile, " NM_001290753"));
      Assert.assertTrue(this.contains(filteredFile, " NM_001290723"));
      Assert.assertTrue(this.contains(filteredFile, " NM_001290724"));
      Assert.assertTrue(this.contains(filteredFile, " JEMT01000476"));
      Assert.assertTrue(this.contains(filteredFile, " KF528683"));
      Assert.assertTrue(this.contains(filteredFile, " KF989488"));
      Assert.assertTrue(this.contains(filteredFile, " KF986170"));
      Assert.assertTrue(this.contains(filteredFile, " KF986165"));
      Assert.assertTrue(this.contains(filteredFile, " KC814772"));
      // a second line description must have disappeared
      Assert.assertFalse(this.contains(filteredFile, "(CKMT2), mRNA."));
      // a description was empty
      Assert.assertTrue(this.contains(filteredFile,
          DatabankFormat.genbank.getBeginDescriptionString() + " "
              + "NM_001290724"));

      sfm = initSequenceFileManager("testRename", "genbank.dat",
          DatabankFormat.genbank);
      validator = new SequenceValidatorRenamer();
      validator.setUpdateDescriptions(true);
      sfm.addValidator(validator);
      filteredFile = sfm.execute().get(0);
      checkResults(sfm, 10, 0);
      Assert
          .assertFalse(this
              .contains(filteredFile,
                  "Rhizophagus irregularis DAOM 197198w jcf7180003165684, whole genome"));
      Assert.assertFalse(this.contains(filteredFile, "shotgun sequence."));
      Assert.assertFalse(this.contains(filteredFile,
          "Homo sapiens homolog of rat pragma of Rnd2 (SGK223), mRNA"));
      Assert.assertFalse(this.contains(filteredFile,
          "clone T1071-6 putative CheA signal"));

    } catch (IOException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testRenameDescSwissProt() {
    try {
      String description = "what a beautiful description";
      SequenceFileManager sfm = initSequenceFileManager("testRename",
          "uniprot.dat", DatabankFormat.swissProt);
      SequenceValidatorRenamer validator = new SequenceValidatorRenamer();
      validator.setUpdateDescriptions(true);
      validator.setNewDescription(description);
      sfm.addValidator(validator);
      filteredFile = sfm.execute().get(0);
      checkResults(sfm, 10, 0);
      Assert
          .assertEquals(
              10,
              getNbLinesContainsDescription(filteredFile,
                  DatabankFormat.swissProt.getBeginDescriptionString(),
                  description));
      // a second line description must have disappeared
      Assert.assertFalse(this.contains(filteredFile,
          "Full=B lymphocyte serine/threonine-protein kinase"));
      // a description was empty
      Assert.assertTrue(this.contains(filteredFile,
          DatabankFormat.swissProt.getBeginDescriptionString() + " "
              + description));

      sfm = initSequenceFileManager("testRename", "uniprot.dat",
          DatabankFormat.swissProt);
      validator = new SequenceValidatorRenamer();
      validator.setUpdateDescriptions(true);
      validator.setUsePreviousId(true);
      validator.setUpdateIds(true);
      sfm.addValidator(validator);
      filteredFile = sfm.execute().get(0);
      checkResults(sfm, 10, 0);
      for (int i = 1; i <= 10; i++) {
        Assert.assertTrue(containsId(filteredFile,
            DatabankFormat.swissProt.getIdString(), String.valueOf(i)));
      }
      Assert.assertTrue(this.contains(filteredFile, "MP2K7_HUMAN"));
      Assert.assertTrue(this.contains(filteredFile, "Q967X2_CIOIN"));
      Assert.assertTrue(this.contains(filteredFile, "A7RQU9_NEMVE"));
      Assert.assertTrue(this.contains(filteredFile, "Q9NA00_9CRUS"));
      Assert.assertTrue(this.contains(filteredFile, "Q91356_COTCO"));
      Assert.assertTrue(this.contains(filteredFile, "Q9PU23_TRASC"));
      Assert.assertTrue(this.contains(filteredFile, "Q90WS6_9SAUR"));
      Assert.assertTrue(this.contains(filteredFile, "KKCC1_RAT"));
      Assert.assertTrue(this.contains(filteredFile, "M4K2_HUMAN"));
      Assert.assertTrue(this.contains(filteredFile, "MP2K4_MOUSE"));
      // a second line description must have disappeared
      Assert.assertFalse(this.contains(filteredFile,
          "Full=B lymphocyte serine/threonine-protein kinase"));
      // a description was empty
      Assert.assertTrue(this.contains(filteredFile,
          DatabankFormat.swissProt.getBeginDescriptionString() + " "
              + "MP2K4_MOUSE"));

      sfm = initSequenceFileManager("testRename", "uniprot.dat",
          DatabankFormat.swissProt);
      validator = new SequenceValidatorRenamer();
      validator.setUpdateDescriptions(true);
      sfm.addValidator(validator);
      filteredFile = sfm.execute().get(0);
      checkResults(sfm, 10, 0);
      Assert.assertFalse(this.contains(filteredFile,
          "AltName: Full=CaM-kinase IV kinase;"));
      Assert.assertFalse(this.contains(filteredFile, "EC=2.7.1.40;"));
      Assert.assertFalse(this.contains(filteredFile,
          "RecName: Full=Pyruvate kinase;"));
      Assert.assertFalse(this.contains(filteredFile,
          "SubName: Full=Protein kinase Ck2-beta;"));
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testCutFile() {
    try {
      SequenceFileManager sfm = initSequenceFileManager("testCutFile",
          "fastaFile.fas", DatabankFormat.fasta);
      SequenceValidatorCutFile validator = new SequenceValidatorCutFile(2, 5);
      sfm.addValidator(validator);
      filteredFile = sfm.execute().get(0);

      checkResults(sfm, 4, 6);
      Assert.assertTrue(contains(filteredFile, "gi|595582364"));
      Assert.assertTrue(contains(filteredFile, "gi|595582353"));
      Assert.assertTrue(contains(filteredFile, "gi|595582112"));
      Assert.assertTrue(contains(filteredFile, "gi|595508533"));
      Assert.assertFalse(contains(filteredFile, "gi|594553085"));

    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testCutFileNoMin() {
    try {
      SequenceFileManager sfm = initSequenceFileManager("testCutFile",
          "fastaFile.fas", DatabankFormat.fasta);
      SequenceValidatorCutFile validator = new SequenceValidatorCutFile(-1, 5);
      sfm.addValidator(validator);
      filteredFile = sfm.execute().get(0);

      checkResults(sfm, 5, 5);
      Assert.assertTrue(contains(filteredFile, "gi|595508533"));
      Assert.assertFalse(contains(filteredFile, "gi|594553085"));
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testCutFileNoMax() {
    try {
      SequenceFileManager sfm = initSequenceFileManager("testCutFile",
          "fastaFile.fas", DatabankFormat.fasta);
      SequenceValidatorCutFile validator = new SequenceValidatorCutFile(4, -1);
      sfm.addValidator(validator);
      filteredFile = sfm.execute().get(0);

      checkResults(sfm, 7, 3);
      Assert.assertFalse(contains(filteredFile, "gi|595582364"));
      Assert.assertTrue(contains(filteredFile, "gi|594553085"));
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testSequenceSize() {
    try {
      SequenceFileManager sfm = initSequenceFileManager("testSequenceSize",
          "fastaFile.fas", DatabankFormat.fasta);
      SequenceValidatorSize validator = new SequenceValidatorSize(200, 500);
      sfm.addValidator(validator);
      filteredFile = sfm.execute().get(0);

      checkResults(sfm, 2, 8);
      Assert.assertTrue(contains(filteredFile, "gi|522577967"));
      Assert.assertTrue(contains(filteredFile, "gi|595508533"));
      Assert.assertFalse(contains(filteredFile, "gi|594553085"));
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testSequenceSizeNoMin() {
    try {
      SequenceFileManager sfm = initSequenceFileManager("testSequenceSize",
          "fastaFile.fas", DatabankFormat.fasta);
      SequenceValidatorSize validator = new SequenceValidatorSize(-1, 500);
      sfm.addValidator(validator);
      filteredFile = sfm.execute().get(0);

      checkResults(sfm, 2, 8);
      Assert.assertTrue(contains(filteredFile, "gi|522577967"));
      Assert.assertTrue(contains(filteredFile, "gi|595508533"));
      Assert.assertFalse(contains(filteredFile, "gi|594553085"));
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testSequenceSizeNoMax() {
    try {
      SequenceFileManager sfm = initSequenceFileManager("testSequenceSize",
          "fastaFile.fas", DatabankFormat.fasta);
      SequenceValidatorSize validator = new SequenceValidatorSize(500, -1);
      sfm.addValidator(validator);
      filteredFile = sfm.execute().get(0);

      checkResults(sfm, 8, 2);
      Assert.assertFalse(contains(filteredFile, "gi|522577967"));
      Assert.assertFalse(contains(filteredFile, "gi|595508533"));
      Assert.assertTrue(contains(filteredFile, "gi|594553085"));
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testSequenceSizeGenbank() {
    try {
      SequenceFileManager sfm = initSequenceFileManager("testSequenceSize",
          "genbank.dat", DatabankFormat.genbank);
      SequenceValidatorSize validator = new SequenceValidatorSize(1000, 2000);
      sfm.addValidator(validator);
      filteredFile = sfm.execute().get(0);

      checkResults(sfm, 4, 6);
      Assert.assertTrue(contains(filteredFile, "NM_001290723"));
      Assert.assertTrue(contains(filteredFile, "NM_001290724"));
      Assert.assertTrue(contains(filteredFile, "KF528683"));
      Assert.assertTrue(contains(filteredFile, "KF989488"));
      Assert.assertFalse(contains(filteredFile, "KC814772"));
      Assert.assertFalse(contains(filteredFile, "NM_001080826"));
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testSequenceSizeUniprot() {
    try {
      SequenceFileManager sfm = initSequenceFileManager("testSequenceSize",
          "uniprot.dat", DatabankFormat.swissProt);
      SequenceValidatorSize validator = new SequenceValidatorSize(200, 400);
      sfm.addValidator(validator);
      filteredFile = sfm.execute().get(0);

      checkResults(sfm, 6, 4);
      Assert.assertTrue(contains(filteredFile, "MP2K4_MOUSE"));
      Assert.assertTrue(contains(filteredFile, "Q967X2_CIOIN"));
      Assert.assertTrue(contains(filteredFile, "Q9PU23_TRASC"));
      Assert.assertTrue(contains(filteredFile, "Q90WS6_9SAUR"));
      Assert.assertFalse(contains(filteredFile, "MP2K7_HUMAN"));
      Assert.assertFalse(contains(filteredFile, "KKCC1_RAT"));
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testDescription() {
    try {
      SequenceFileManager sfm = initSequenceFileManager("testDescription",
          "fastaFile.fas", DatabankFormat.fasta);
      SequenceValidatorDescription validator = new SequenceValidatorDescription(
          "kinase"); // kinase
      sfm.addValidator(validator);
      filteredFile = sfm.execute().get(0);

      checkResults(sfm, 5, 5);
      Assert.assertTrue(contains(filteredFile, "gi|595582353"));
      Assert.assertTrue(contains(filteredFile, "gi|595582112"));
      Assert.assertTrue(contains(filteredFile, "gi|594553085"));
      Assert.assertTrue(contains(filteredFile, "gi|594551389"));
      Assert.assertTrue(contains(filteredFile, "gi|522577967"));
      Assert.assertFalse(contains(filteredFile, "gi|594550943"));

    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testDescriptionMultiples() {
    try {
      SequenceFileManager sfm = initSequenceFileManager("testDescription",
          "fastaFile.fas", DatabankFormat.fasta);
      SequenceValidatorDescription validator = new SequenceValidatorDescription(
          "kinase@sapiens"); // kinase
      validator.setExactSearch(true);
      sfm.addValidator(validator);

      filteredFile = sfm.execute().get(0);

      checkResults(sfm, 6, 4);
      Assert.assertTrue(contains(filteredFile, "gi|595582407"));
      Assert.assertTrue(contains(filteredFile, "gi|595582112"));
      Assert.assertFalse(contains(filteredFile, "gi|594550943"));

    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testDescriptionNot() {
    try {
      SequenceFileManager sfm = initSequenceFileManager("testDescription",
          "fastaFile.fas", DatabankFormat.fasta);
      SequenceValidatorDescription validator = new SequenceValidatorDescription(
          "!kinase"); // kinase
      validator.setExactSearch(true);
      sfm.addValidator(validator);

      filteredFile = sfm.execute().get(0);

      checkResults(sfm, 5, 5);
      Assert.assertTrue(contains(filteredFile, "gi|595582407"));
      Assert.assertTrue(contains(filteredFile, "gi|594550943"));
      Assert.assertFalse(contains(filteredFile, "gi|595582112"));

    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testDescriptionBoths() {
    try {
      SequenceFileManager sfm = initSequenceFileManager("testDescription",
          "fastaFile.fas", DatabankFormat.fasta);
      SequenceValidatorDescription validator = new SequenceValidatorDescription(
          "maturase@!kinase"); // kinase
      validator.setExactSearch(true);
      sfm.addValidator(validator);

      filteredFile = sfm.execute().get(0);

      checkResults(sfm, 2, 8);
      Assert.assertTrue(contains(filteredFile, "gi|594550953"));
      Assert.assertTrue(contains(filteredFile, "gi|594550943"));
      Assert.assertFalse(contains(filteredFile, "gi|595582112"));

    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testDescriptionExactSearch() {
    try {
      SequenceFileManager sfm = initSequenceFileManager("testDescription",
          "fastaFile.fas", DatabankFormat.fasta);
      SequenceValidatorDescription validator = new SequenceValidatorDescription(
          "kanaze"); // kinase
      validator.setExactSearch(true);
      sfm.addValidator(validator);
      filteredFile = sfm.execute().get(0);

      checkResults(sfm, 0, 10);

      sfm = initSequenceFileManager("testDescription", "fastaFile.fas",
          DatabankFormat.fasta);
      validator = new SequenceValidatorDescription("kinase"); // kinase
      validator.setExactSearch(false);
      sfm.addValidator(validator);
      filteredFile = sfm.execute().get(0);

      checkResults(sfm, 5, 5);
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testDescriptionNoResult() {
    try {
      SequenceFileManager sfm = initSequenceFileManager("testDescription",
          "fastaFile.fas", DatabankFormat.fasta);
      SequenceValidatorDescription validator = new SequenceValidatorDescription(
          "no result");
      sfm.addValidator(validator);
      filteredFile = sfm.execute().get(0);

      checkResults(sfm, 0, 10);

    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testValidatorBadFormat() {
    try {
      SequenceFileManager sfm = initSequenceFileManager("testDescription",
          "fastaFile.fas", DatabankFormat.genbank);
      SequenceValidatorSize validator = new SequenceValidatorSize(-1, 40000);
      sfm.addValidator(validator);
      filteredFile = sfm.execute().get(0);

      checkResults(sfm, 0, 0);

    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  
  @Test
  public void testTaxonInclude_1() {
    try {
      SequenceFileManager sfm = initSequenceFileManager("testTaxonomy",
          "genbank.dat", DatabankFormat.genbank);
      SequenceValidatorTaxon validator = new SequenceValidatorTaxon("561", "");
      sfm.addValidator(validator);
      filteredFile = sfm.execute().get(0);
      checkResults(sfm, 1, 9);

    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testTaxonInclude_2() {
    try {
      SequenceFileManager sfm = initSequenceFileManager("testTaxonomy", "uniprot.dat",
          DatabankFormat.swissProt);
      SequenceValidatorTaxon validator = new SequenceValidatorTaxon("2759", "");
      sfm.addValidator(validator);
      filteredFile = sfm.execute().get(0);
      checkResults(sfm, 5, 5);
    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testTaxonInclude_3() {
    try {
      SequenceFileManager sfm = initSequenceFileManager("testTaxonomy", "uniprot.dat",
          DatabankFormat.swissProt);
      SequenceValidatorTaxon validator = new SequenceValidatorTaxon("40674,543", "");
      sfm.addValidator(validator);
      filteredFile = sfm.execute().get(0);
      checkResults(sfm, 5, 5);

    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testTaxonExclude() {
    try {
      SequenceFileManager sfm = initSequenceFileManager("testTaxonomy",
          "genbank.dat", DatabankFormat.genbank);
      SequenceValidatorTaxon validator = new SequenceValidatorTaxon("", "1236");
      sfm.addValidator(validator);
      filteredFile = sfm.execute().get(0);
      checkResults(sfm, 7, 3);

      sfm = initSequenceFileManager("testTaxonomy", "genbank.dat",
          DatabankFormat.genbank);
      validator = new SequenceValidatorTaxon("", "2759,2");
      sfm.addValidator(validator);
      filteredFile = sfm.execute().get(0);
      checkResults(sfm, 1, 9);

      sfm = initSequenceFileManager("testTaxonomy", "genbank.dat",
          DatabankFormat.genbank);
      validator = new SequenceValidatorTaxon("", "1236,10239,9443");
      sfm.addValidator(validator);
      filteredFile = sfm.execute().get(0);
      checkResults(sfm, 3, 7);

      sfm = initSequenceFileManager("testTaxonomy", "uniprot.dat",
          DatabankFormat.swissProt);
      validator = new SequenceValidatorTaxon("", "1236");
      sfm.addValidator(validator);
      filteredFile = sfm.execute().get(0);

      checkResults(sfm, 7, 3);

    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testTaxonIncludeExclude() {
    try {
      SequenceFileManager sfm = initSequenceFileManager("testTaxonomy",
          "genbank.dat", DatabankFormat.genbank);
      SequenceValidatorTaxon validator = new SequenceValidatorTaxon("2759",
          "9443");
      sfm.addValidator(validator);
      filteredFile = sfm.execute().get(0);

      checkResults(sfm, 2, 8);

      sfm = initSequenceFileManager("testTaxonomy", "uniprot.dat",
          DatabankFormat.swissProt);
      sfm.addValidator(validator);
      filteredFile = sfm.execute().get(0);

      checkResults(sfm, 2, 8);

    } catch (Exception e) {
      Assert.fail(e.getMessage());
    }
  }
  
/*
  @SuppressWarnings("unchecked")
  @Test
  public void testPaginateFile() {
    SequenceValidatorPaginate validator = null;

    try {
      SequenceFileManager sfm = initSequenceFileManager("testCutFile",
          "fastaFile.fas", DatabankFormat.fasta);
      validator = new SequenceValidatorPaginate(4);
      sfm.addValidator(validator);

      filteredFile = sfm.execute().get(0);

      checkResults(sfm, 10, 0);

      // 3 batches ?
      Assert.assertTrue(validator.getCreatedBatches().size() == 3);

      // batch 1
      Entry<File, Integer> entry = (Entry<File, Integer>) validator
          .getCreatedBatches().entrySet().toArray()[0];
      Assert.assertTrue(entry.getValue() == 4);
      filteredFile = entry.getKey();
      Assert.assertTrue(contains(filteredFile, "gi|595582407"));
      Assert.assertTrue(contains(filteredFile, "gi|595582364"));
      Assert.assertTrue(contains(filteredFile, "gi|595582353"));
      Assert.assertTrue(contains(filteredFile, "gi|595582112"));
      FileUtils.deleteQuietly(filteredFile);

      // batch 2
      entry = (Entry<File, Integer>) validator.getCreatedBatches().entrySet()
          .toArray()[1];
      Assert.assertTrue(entry.getValue() == 4);
      filteredFile = entry.getKey();
      Assert.assertTrue(contains(filteredFile, "gi|595508533"));
      Assert.assertTrue(contains(filteredFile, "gi|594553085"));
      Assert.assertTrue(contains(filteredFile, "gi|594551389"));
      Assert.assertTrue(contains(filteredFile, "gi|594550953"));
      FileUtils.deleteQuietly(filteredFile);

      // batch 3
      entry = (Entry<File, Integer>) validator.getCreatedBatches().entrySet()
          .toArray()[2];
      Assert.assertTrue(entry.getValue() == 2);
      filteredFile = entry.getKey();
      Assert.assertTrue(contains(filteredFile, "gi|594550943"));
      Assert.assertTrue(contains(filteredFile, "gi|594550943"));
      FileUtils.deleteQuietly(filteredFile);

    } catch (Exception e) {
      Assert.fail(e.getMessage());
    } finally {
      // Clean
      if (validator != null && !validator.getCreatedBatches().isEmpty()) {
        for (File file : validator.getCreatedBatches().keySet()) {
          FileUtils.deleteQuietly(file);
        }
      }
    }
  }
  */
  // @Test
  // public void testBigFile()
  // {
  // try
  // {
  // SequenceFileManager sfm = initSequenceFileManager("testBigFile", "nr",
  // DatabankFormat.fasta);
  // // SequenceValidatorCutFile validator2 = new SequenceValidatorCutFile(-1,
  // 500000);
  // // sfm.addValidator(validator2);
  // SequenceValidatorSize validator = new SequenceValidatorSize(50, -1);
  // sfm.addValidator(validator);
  //
  // long start = Calendar.getInstance().getTimeInMillis();
  // filteredFile = sfm.execute().get(0);
  // System.out.println(sfm.getNbSequencesFound() + " found and " +
  // sfm.getNbSequencesDiscarded()
  // + " discarded in " + (Calendar.getInstance().getTimeInMillis() - start) +
  // " ms.");
  //
  // }
  // catch (Exception e)
  // {
  // Assert.fail(e.getMessage());
  // }
  // }
}
