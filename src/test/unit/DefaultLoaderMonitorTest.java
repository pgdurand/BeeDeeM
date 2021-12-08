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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import bzh.plealog.dbmirror.fetcher.DBMSFile;
import bzh.plealog.dbmirror.fetcher.DBServerConfig;
import bzh.plealog.dbmirror.fetcher.DefaultLoaderMonitor;
import bzh.plealog.dbmirror.fetcher.LoaderMonitor;
import bzh.plealog.dbmirror.fetcher.PFTPLoaderDescriptor;
import bzh.plealog.dbmirror.fetcher.PLocalLoader;
import bzh.plealog.dbmirror.indexer.DBEntry;
import bzh.plealog.dbmirror.indexer.LuceneStorageSystem;
import bzh.plealog.dbmirror.indexer.LuceneUtils;
import bzh.plealog.dbmirror.lucenedico.DicoTermQuerySystem;
import bzh.plealog.dbmirror.lucenedico.Dicos;
import bzh.plealog.dbmirror.reader.DBUtils;
import bzh.plealog.dbmirror.reader.PFormatter;
import bzh.plealog.dbmirror.reader.PSequence;
import bzh.plealog.dbmirror.task.PParserTask;
import bzh.plealog.dbmirror.task.PTask;
import bzh.plealog.dbmirror.task.PTaskBold2Genbank;
import bzh.plealog.dbmirror.task.PTaskEngine;
import bzh.plealog.dbmirror.task.PTaskFastaRenamer;
import bzh.plealog.dbmirror.task.PTaskInstallInProduction;
import bzh.plealog.dbmirror.task.PTaskLuceneFastaIndexer;
import bzh.plealog.dbmirror.task.PTaskLuceneGBIndexer;
import bzh.plealog.dbmirror.task.PTaskLuceneSWIndexer;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.conf.DBMirrorConfig;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptor;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptor.TYPE;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptorUtils;
import bzh.plealog.dbmirror.util.descriptor.DatabankFormat;
import bzh.plealog.dbmirror.util.descriptor.DescriptorEntry;
import bzh.plealog.dbmirror.util.descriptor.IdxDescriptor;
import bzh.plealog.dbmirror.util.runner.DBMSUniqueSeqIdDetector;
import bzh.plealog.dbmirror.util.sequence.TaxonMatcherHelper;
import bzh.plealog.dbmirror.util.xref.DBXrefInstancesManager;

/**
 * Test class for DefaultLoaderMonitor
 * 
 * @author Ludovic Antin
 * 
 */
public class DefaultLoaderMonitorTest {

  private static PTaskEngine         _taskEngine;
  private static DBServerConfig       _dbConf = new DBServerConfig();
  private static DefaultLoaderMonitor _loaderMonitor;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    UtilsTest.configureApp();
    UtilsTest.cleanInstalledDatabanks("d");
    DBMSUniqueSeqIdDetector.MAX_SEQID = 5000000;
    if (!TaxonMatcherHelper.isNCBITaxonomyInstalled()) {
      RunningMirrorPanelTest.installLocalNCBITaxonomy();
    }
    DicoTermQuerySystem.getDicoTermQuerySystem(DBDescriptorUtils
        .getDBMirrorConfig(DBMSAbstractConfig.getLocalMirrorConfFile()));
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    DicoTermQuerySystem.closeDicoTermQuerySystem();
  }

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    UtilsTest.cleanInstalledDatabanks("d");
    _dbConf = new DBServerConfig();
  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
    UtilsTest.cleanInstalledDatabanks("d");
  }

  /**
   * Each test file needed for a test method must be stored in this repository
   * [main]\DefaultLoaderMonitor\[method name]\[file name]
   * 
   * @param methodName
   * @param fileName
   * @return the concatenate file path
   */
  private String getTestFilePath(String methodName, String fileName) {
    return UtilsTest.getTestFilePath("DefaultLoaderMonitor", methodName,
        fileName);
  }

  /**
   * Each test file needed for a complete installation is in this repository
   * [main]\databank\[directory]\[filename]
   * 
   * @param directory
   * @param fileName
   * @return the concatenate file path
   */
  public static String getTestDatabankFilePath(String directory, String fileName) {
    return UtilsTest.getTestFilePath("databank", directory, fileName);
  }

  /**
   * Simulate a complete installation of a databank
   * 
   * @param repositoryName
   * @param descriptionFileName
   */
  public static String completeInstall(String repositoryName,
      String descriptionFileName, boolean installInProduction) {
    // get the description filepath
    String descriptorFilePath = getTestDatabankFilePath(repositoryName,
        descriptionFileName);

    try {
      _dbConf = new DBServerConfig();
      _dbConf.load(descriptorFilePath);
    } catch (Exception ex) {
      fail("Unable to load the db descriptor");
    }

    // load data
    _taskEngine = new PTaskEngine(false);
    // databank files
    ArrayList<DBMSFile> databankFiles = new ArrayList<DBMSFile>();
    if (_dbConf.getIncludeFileList().contains("*")) {
      File srcDir = new File(getTestDatabankFilePath(repositoryName, ""));
      String foundFiles[] = srcDir.list(new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
          return name.matches(_dbConf.getIncludeFileList());
        }
      });
      for (String fileName : foundFiles) {
        databankFiles.add(new DBMSFile(new File(fileName)));
      }
    } else {

      new PLocalLoader(_dbConf).initFilesList(databankFiles);
    }
    assertTrue(databankFiles.size() > 0);

    _loaderMonitor = new DefaultLoaderMonitor(_taskEngine, _dbConf,
        databankFiles.size());

    for (int iFile = 0; iFile < databankFiles.size(); iFile++) {
      _loaderMonitor.beginLoading(databankFiles.get(iFile).getName());

      // no ftp during the test, just a copy
      try {
        File srcFile = new File(getTestDatabankFilePath(repositoryName,
            databankFiles.get(iFile).getName()));
        File destDir = new File(_dbConf.getLocalTmpFolder());
        File destFile = new File(destDir, databankFiles.get(iFile).getName());

        // In case of multiple files, clean directory only before
        // the first file
        try {
          if (destDir.exists() && (iFile == 0))
            FileUtils.cleanDirectory(destDir);
        } catch (Exception e) {
          e.printStackTrace();
        }

        if (!destFile.exists()) {
          if (!destDir.exists()) {
            destDir.mkdirs();
          }
          // Files.copy(srcFile.toPath(), destFile.toPath(),
          // StandardCopyOption.REPLACE_EXISTING);
          Utils.copyBinFile(srcFile, destFile);
        }
      } catch (IOException e) {
        assertTrue("Copy nok : " + e.getMessage(), false);
      }

      _loaderMonitor.doneLoading(databankFiles.get(iFile).getName(),
          LoaderMonitor.STATUS_OK);
    }

    // check tasks
    Object[] tasks = _taskEngine.getTasks().toArray();
    assertNotNull(tasks);

    for (int i = 0; i < tasks.length; i++) {
      PTask task = (PTask) tasks[i];
      if (task instanceof PParserTask) {
        ((PParserTask) task).setParserMonitor(_taskEngine.new MyParserMonitor(
            task.getDbConfName(), null));
      }
      assertTrue(task.getName() + " : " + task.getErrorMsg(), task.execute());
    }

    if ((installInProduction) && (_dbConf.mustBeInstallInProduction())) {
      ArrayList<DBServerConfig> configs = new ArrayList<DBServerConfig>();
      configs.add(_dbConf);
      PTaskInstallInProduction installTask = new PTaskInstallInProduction(
          configs);
      assertTrue(installTask.execute());
    }

    return _dbConf.getName();
  }

  private static int getNbSequences(String repositoryName,
      String descriptionFileName) {
    String descriptorFilePath = getTestDatabankFilePath(repositoryName,
        descriptionFileName);

    try {
      _dbConf.load(descriptorFilePath);
    } catch (Exception ex) {
      fail("Unable to load the db descriptor");
    }

    String ext = "_fdb.num";
    if (_dbConf.getTypeCode().equals("d")) {
      ext = ".num";
    }

    String fastaVolumePath = FilenameUtils.concat(_dbConf.getLocalTmpFolder(),
        _dbConf.getName() + ext);
    if (!new File(fastaVolumePath).exists()) {
      // try in prod
      fastaVolumePath = FilenameUtils.concat(_dbConf.getLocalProdFolder(),
          _dbConf.getName() + ext);
    }
    String line;
    try {
      line = Utils.readFirstLine(new File(fastaVolumePath));
      return new Integer(line).intValue();
    } catch (Exception e) {
      fail("Unable to read the file containing the number of sequences : "
          + e.getMessage());
    }

    return -1;
  }

  private int getSequenceSizeInIndex(String repositoryName,
      String descriptionFileName, int index) {
    String descriptorFilePath = getTestDatabankFilePath(repositoryName,
        descriptionFileName);

    try {
      _dbConf.load(descriptorFilePath);
    } catch (Exception ex) {
      fail("Unable to load the db descriptor");
    }

    // test in index lucene
    IndexReader reader = null;
    DBEntry entry = null;
    String luceneIndexDirectory = FilenameUtils.concat(
        _dbConf.getLocalTmpFolder(), _dbConf.getName() + ".ldx");
    try {
      reader = IndexReader.open(
    		  LuceneUtils.getDirectory(new File(luceneIndexDirectory)), true);
      assertNotNull(reader.document(index));
      entry = LuceneUtils.getEntry(luceneIndexDirectory, reader.document(index)
          .get(LuceneStorageSystem.ID_FIELD));
      assertNotNull(entry);
      return (int) (entry.getStop() - entry.getStart());
    } catch (Exception e) {
      fail("Unable to read the index containing the number of sequences : "
          + e.getMessage());
      e.printStackTrace();
    } finally {
      try {
        reader.close();
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }

    return -1;
  }

  private int getSequenceSizeInFile(String filePath, int index) {
    String idChar = ">";
    String line;
    int currentIndex = -1;
    int nbChars = 0;
    try {
      BufferedReader reader = new BufferedReader(new FileReader(filePath));
      while ((line = reader.readLine()) != null) {
        if (idChar.isEmpty() || line.startsWith(idChar)) {
          currentIndex++;
          continue;
        }
        if (currentIndex == index) {
          for (int indexChar = 0; indexChar < line.length(); indexChar++) {
            if (Character.isLetter(line.charAt(indexChar))) {
              nbChars++;
            }
          }
        }
      }
      reader.close();
    } catch (IOException e) {
      fail("Unable to read the file containing the number of sequences");
    }
    return nbChars;
  }

  private int getSequenceSizeInVolumeFile(String repositoryName,
      String descriptionFileName, int index) {
    String descriptorFilePath = getTestDatabankFilePath(repositoryName,
        descriptionFileName);

    try {
      _dbConf.load(descriptorFilePath);
    } catch (Exception ex) {
      fail("Unable to load the db descriptor");
    }
    File volume1 = new File(Utils.getFileVolumes(_dbConf.getLocalTmpFolder(),
        _dbConf.getName()).get(0));
    return this.getSequenceSizeInFile(volume1.getAbsolutePath(), index);
  }

  private int getSequenceSizeInOriginalFile(String repositoryName,
      String descriptionFileName, int index) {
    String descriptorFilePath = getTestDatabankFilePath(repositoryName,
        descriptionFileName);

    try {
      _dbConf.load(descriptorFilePath);
    } catch (Exception ex) {
      fail("Unable to load the db descriptor");
    }

    String filePathToRead = FilenameUtils.concat(_dbConf.getLocalTmpFolder(),
        _dbConf.getIncludeFileList());
    return this.getSequenceSizeInFile(filePathToRead, index);
  }

  public static int getNbSequencesInIndex(String repositoryName,
      String descriptionFileName) {
    String descriptorFilePath = getTestDatabankFilePath(repositoryName,
        descriptionFileName);

    try {
      _dbConf.load(descriptorFilePath);
    } catch (Exception ex) {
      fail("Unable to load the db descriptor");
    }

    // test in index lucene
    IndexReader reader = null;
    String luceneIndexDirectory = FilenameUtils.concat(
        _dbConf.getLocalTmpFolder(), _dbConf.getName() + ".ldx");
    if (!new File(luceneIndexDirectory).exists()) {
      // try in prod
      luceneIndexDirectory = FilenameUtils.concat(_dbConf.getLocalProdFolder(),
          _dbConf.getName() + ".ldx");
    }
    try {
      reader = IndexReader.open(
    		  LuceneUtils.getDirectory(new File(luceneIndexDirectory)), true);
      assertNotNull(reader.document(0));
      String data = reader.document(0).get(LuceneStorageSystem.ID_FIELD);
      if (!_dbConf.getTypeCode().equals("d")) {
        assertNotNull(LuceneUtils.getEntry(luceneIndexDirectory, data));
      } else {
        assertTrue(StringUtils.isNotBlank(data));
      }
      assertNotNull(reader.document(reader.maxDoc() - 1));
      data = reader.document(reader.maxDoc() - 1).get(
          LuceneStorageSystem.ID_FIELD);
      if (!_dbConf.getTypeCode().equals("d")) {
        assertNotNull(LuceneUtils.getEntry(luceneIndexDirectory, data));
      } else {
        assertTrue(StringUtils.isNotBlank(data));
      }
      return reader.maxDoc();
    } catch (CorruptIndexException e) {
      fail("Unable to read the index containing the number of sequences : "
          + e.getMessage());
      e.printStackTrace();
    } catch (IOException e) {
      fail("Unable to read the index containing the number of sequences : "
          + e.getMessage());
      e.printStackTrace();
    } finally {
      try {
        reader.close();
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }

    return -1;
  }

  private int getNbIdsInDownloadedFile(String repositoryName,
      String descriptionFileName, String idChar, String filenameToRead) {
    String descriptorFilePath = getTestDatabankFilePath(repositoryName,
        descriptionFileName);

    try {
      _dbConf.load(descriptorFilePath);
    } catch (Exception ex) {
      fail("Unable to load the db descriptor");
    }

    String filePathToRead = "";
    if (filenameToRead.isEmpty()) {
      filePathToRead = FilenameUtils.concat(_dbConf.getLocalTmpFolder(),
          _dbConf.getIncludeFileList());
    } else {
      filePathToRead = FilenameUtils.concat(_dbConf.getLocalTmpFolder(),
          filenameToRead);
    }
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

  private static List<String> getFastaSequenceHeaders(String repositoryName,
      String descriptionFileName, boolean onlyForCount) {
    List<String> result = new ArrayList<String>();
    String descriptorFilePath = getTestDatabankFilePath(repositoryName,
        descriptionFileName);

    try {
      _dbConf.load(descriptorFilePath);
    } catch (Exception ex) {
      fail("Unable to load the db descriptor");
    }

    String fastaVolumePath = FilenameUtils.concat(_dbConf.getLocalTmpFolder(),
        _dbConf.getName() + "00");
    if (!new File(fastaVolumePath).exists()) {
      // try in prod
      fastaVolumePath = FilenameUtils.concat(_dbConf.getLocalProdFolder(),
          _dbConf.getName() + "00");
    }
    String line;
    try {
      // BufferedReader reader =
      // Files.newBufferedReader(Paths.get(fastaVolumePath),
      // Charset.defaultCharset());
      BufferedReader reader = new BufferedReader(
          new FileReader(fastaVolumePath));
      while ((line = reader.readLine()) != null) {
        if (line.startsWith(DatabankFormat.fasta.getIdString())) {
          if (onlyForCount) {
            result.add(DatabankFormat.fasta.getIdString());
          } else {
            result.add(line);
          }
        }
      }
      reader.close();
    } catch (IOException e) {
      fail("Unable to read the file containing the number of sequences");
    }
    return result;
  }

  private static int getNbIdsInVolume(String repositoryName,
      String descriptionFileName) {
    return getFastaSequenceHeaders(repositoryName, descriptionFileName, true)
        .size();
  }

  public static void assertNbOk(int expected, String repositoryName,
      String descriptionFileName) {
    assertEquals(expected, getNbSequences(repositoryName, descriptionFileName));
    assertEquals(expected,
        getNbSequencesInIndex(repositoryName, descriptionFileName));
    if (!_dbConf.getTypeCode().equals("d")) {
      assertEquals(expected,
          getNbIdsInVolume(repositoryName, descriptionFileName));
    }
  }

  /**
   * main goal : test the new method DefaultLoaderMonitor.getTaskFilepath and
   * the updated one handleUnitTask using a unique fasta file in the databank
   */
  @Test
  public void testOneFastaFile() {

    String descriptorFilePath = getTestFilePath("testOneFastaFile",
        "SW_human_local.dsc");
    try {
      _dbConf.load(descriptorFilePath);
    } catch (Exception ex) {
      fail("Unable to load the db descriptor");
    }

    _taskEngine = new PTaskEngine(true);
    _loaderMonitor = new DefaultLoaderMonitor(_taskEngine, _dbConf, 1);
    _loaderMonitor.beginLoading("SwissProt_human.fas");
    _loaderMonitor.doneLoading("SwissProt_human.fas", LoaderMonitor.STATUS_OK);

    // check tasks
    Object[] tasks = _taskEngine.getTasks().toArray();
    assertNotNull(tasks);
    assertEquals(6, tasks.length);
    // check task name
    PTaskLuceneFastaIndexer task = (PTaskLuceneFastaIndexer) tasks[1];
    assertTrue(task.getSrc().endsWith("SwissProt_human.fas"));
    // check the existed renamer task
    PTaskFastaRenamer taskRenamer = (PTaskFastaRenamer) tasks[2];
    assertTrue(taskRenamer.getNewFilepath().endsWith(_dbConf.getName() + "00"));
    // check files input for formatDb
    assertEquals(1, _loaderMonitor.getFormatDBFiles().size());
    assertEquals(task.getSrc(), _loaderMonitor.getFormatDBFiles().get(0));

  }

  /**
   * main goal : test the new method DefaultLoaderMonitor.getTaskFilepath and
   * the updated one handleUnitTask using a non fasta format file
   */
  @Test
  public void testSwissProt() {
    String descriptorFilePath = getTestFilePath("testSwissProt",
        "SwissProt_human_local.dsc");

    try {
      _dbConf.load(descriptorFilePath);
    } catch (Exception ex) {
      fail("Unable to load the db descriptor");
    }

    // load data
    _taskEngine = new PTaskEngine(false);
    _loaderMonitor = new DefaultLoaderMonitor(_taskEngine, _dbConf, 1);
    _loaderMonitor.beginLoading("uniprot_sprot_human.dat.gz");
    _loaderMonitor.doneLoading("uniprot_sprot_human.dat.gz",
        LoaderMonitor.STATUS_OK);

    // check tasks
    Object[] tasks = _taskEngine.getTasks().toArray();
    assertNotNull(tasks);
    assertEquals(9, tasks.length);
    PTaskLuceneSWIndexer task = (PTaskLuceneSWIndexer) tasks[2];
    // check task name
    assertTrue(task.getSrc().endsWith("uniprot_sprot_human.dat"));
    // check files input for formatDb
    assertEquals(1, _loaderMonitor.getFormatDBFiles().size());
    assertEquals(task.getSrc(), _loaderMonitor.getFormatDBFiles().get(0));
  }

  /**
   * main goal : test the new method DefaultLoaderMonitor.getTaskFilepath and
   * the updated one handleUnitTask using a bold format file
   */
  @Test
  public void testBold() {
    String descriptorFilePath = getTestFilePath("testBold", "sample_BOLD.dsc");

    try {
      _dbConf.load(descriptorFilePath);
    } catch (Exception ex) {
      fail("Unable to load the db descriptor");
    }

    // load data
    _taskEngine = new PTaskEngine(false);
    _loaderMonitor = new DefaultLoaderMonitor(_taskEngine, _dbConf, 1);
    _loaderMonitor.beginLoading("iBOL.zip");
    _loaderMonitor.doneLoading("iBOL.zip", LoaderMonitor.STATUS_OK);

    // check tasks
    Object[] tasks = _taskEngine.getTasks().toArray();
    assertNotNull(tasks);
    assertEquals(7, tasks.length);
    PTaskBold2Genbank task = (PTaskBold2Genbank) tasks[2];
    PTaskLuceneGBIndexer taskIndexer = (PTaskLuceneGBIndexer) tasks[3];
    // check task name
    assertTrue(task.getSrc().contains(
        _dbConf.getName() + File.separator + "iBOL"));
    // check files input for formatDb
    assertEquals(1, _loaderMonitor.getFormatDBFiles().size());
    // the file will be converted in genbank format
    // so the filename to format db is the same as the gbIndexer
    assertEquals(taskIndexer.getSrc(), _loaderMonitor.getFormatDBFiles().get(0));
  }

  @Test
  public void testCompleteBold() {
    completeInstall("bold", "sample_BOLD.dsc", false);
  }

  @Test
  public void testCompleteEmbl() {
    completeInstall("embl", "sample_EMBL.dsc", false);
  }

  @Test
  public void testCompleteFasta_nuc() {
    completeInstall("fasta_nuc", "sample_fasta_nuc.dsc", false);
    File volume1 = new File(Utils.getFileVolumes(_dbConf.getLocalTmpFolder(),
        _dbConf.getName()).get(0));
    try {
      String volumeContent = FileUtils.readFileToString(volume1).trim();
      Assert.assertTrue(volumeContent.startsWith(">"));
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testCompleteFasta_prot() {
    completeInstall("fasta_prot", "sample_fasta_prot.dsc", false);
    File volume1 = new File(Utils.getFileVolumes(_dbConf.getLocalTmpFolder(),
        _dbConf.getName()).get(0));
    try {
      String volumeContent = FileUtils.readFileToString(volume1).trim();
      Assert.assertTrue(volumeContent.startsWith(">"));
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testCompleteGenbank() {
    completeInstall("genbank", "sample_Genbank.dsc", false);
  }

  @Test
  public void testCompleteSilva() {
    completeInstall("silva", "sample_silva.dsc", false);
    File volume1 = new File(Utils.getFileVolumes(_dbConf.getLocalTmpFolder(),
        _dbConf.getName()).get(0));
    try {
      String volumeContent = FileUtils.readFileToString(volume1).trim();
      Assert.assertTrue(volumeContent.startsWith(">"));
      Assert.assertTrue(volumeContent.contains("[[taxon:198834]]"));
    } catch (IOException e) {
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void testCompleteSilvaEmptySequence() {
    String repository = "silva_empty_sequence";
    String dsc = "sample_silva.dsc";
    completeInstall(repository, dsc, false);
    assertNbOk(9, repository, dsc);

    // assert that the offset is ok in the index even if an empty sequence is
    // here
    assertEquals(2549, getSequenceSizeInIndex(repository, dsc, 1));

    // assert that all sequences are the same between the original file and the
    // volume file
    assertEquals(getSequenceSizeInOriginalFile(repository, dsc, 0),
        getSequenceSizeInVolumeFile(repository, dsc, 0));
    // empty sequence at index 1 in original file
    assertEquals(getSequenceSizeInOriginalFile(repository, dsc, 2),
        getSequenceSizeInVolumeFile(repository, dsc, 1));
    assertEquals(getSequenceSizeInOriginalFile(repository, dsc, 10),
        getSequenceSizeInVolumeFile(repository, dsc, 9));
  }

  @Test
  public void testCompleteUniprot() {
    completeInstall("uniprot", "sample_Uniprot.dsc", false);
  }

  @Test
  public void testCompleteTremblWithBadTaxonId() {
    completeInstall("trembl", "sample_trembl.dsc", false);
  }

  @Test
  public void testCompleteUniprotMulti() {
    completeInstall("uniprot_multi", "sample_multiple_uniprot.dsc", false);
    assertNbOk(62, "uniprot_multi", "sample_multiple_uniprot.dsc");

  }

  @Test
  public void testCompleteFastaMulti() {
    completeInstall("fasta_multi", "sample_multiple_fasta.dsc", false);
    assertNbOk(237, "fasta_multi", "sample_multiple_fasta.dsc");
  }

  @Test
  public void testCompleteFastaMultiWithRedundant() {
    DBMSUniqueSeqIdDetector.MAX_SEQID = 4; // to test multiple commits before
                                           // redundant
    DBMSUniqueSeqIdDetector.CHECK_MEMORY_STEP = 1;
    completeInstall("fasta_multi_redundant", "sample_multiple_fasta.dsc", false);
    assertNbOk(17, "fasta_multi_redundant", "sample_multiple_fasta.dsc");
  }

  // @Test
  // public void testCompleteFastaMulti2() {
  // completeInstall("fasta_multi2", "sample_multiple_fasta.dsc", false);
  // assertEquals(492463, getNbSequences("fasta_multi2",
  // "sample_multiple_fasta.dsc"));
  // assertEquals(492463, getNbSequences("fasta_multi2",
  // "sample_multiple_fasta.dsc"));
  // }

  @Test
  public void testCompleteFastaWithoutIndex() {
    completeInstall("fasta_without_idx", "fasta_without_idx.dsc", false);
  }

  @Test
  public void testCompleteFastaWithRedundantSequences() {
    completeInstall("redundant_sequences", "fasta.dsc", false);
    assertEquals(13,
        getNbIdsInDownloadedFile("redundant_sequences", "fasta.dsc", ">", ""));
    assertNbOk(10, "redundant_sequences", "fasta.dsc");
  }

  @Test
  public void testCompleteFastaWithRedundantSequences2() {
    completeInstall("redundant_sequences", "fasta_noidx.dsc", false);
  }

  @Test
  public void testCompleteSwissProtWithRedundantSequences() {
    completeInstall("redundant_sequences", "sw.dsc", false);
    assertEquals(11,
        getNbIdsInDownloadedFile("redundant_sequences", "sw.dsc", "ID ", ""));
    assertNbOk(9, "redundant_sequences", "sw.dsc");
  }

  @Test
  public void testCompleteGenbankWithRedundantSequences() {
    completeInstall("redundant_sequences", "gb.dsc", false);
    assertEquals(11,
        getNbIdsInDownloadedFile("redundant_sequences", "gb.dsc", "LOCUS ", ""));
    assertNbOk(10, "redundant_sequences", "gb.dsc");
  }

  @Test
  public void testCompleteBoldWithRedundantSequences() {
    // KDMSUniqueSeqIdDetector.MAX_SEQID = 4;
    completeInstall("redundant_sequences", "bold.dsc", false);
    // count all lines and substract the first one which contains column titles
    assertEquals(
        16,
        getNbIdsInDownloadedFile("redundant_sequences", "bold.dsc", "",
            "iBOL_phase_4.50_Plants.tsv") - 1);
    assertNbOk(9, "redundant_sequences", "bold.dsc");
  }

  @Test
  public void testCompleteBoldWithRedundantSequencesSpecial() {
    // a special case if MAX_SEQID = 4 because seq4 & 5 are redundants
    DBMSUniqueSeqIdDetector.MAX_SEQID = 4;
    completeInstall("redundant_sequences", "bold.dsc", false);
    // count all lines and substract the first one which contains column titles
    assertEquals(
        16,
        getNbIdsInDownloadedFile("redundant_sequences", "bold.dsc", "",
            "iBOL_phase_4.50_Plants.tsv") - 1);
    assertNbOk(9, "redundant_sequences", "bold.dsc");
  }

  @Test
  public void testVelocity() {

    PSequence seq = DBUtils.readGenbankEntry(new File(
        "./tests/junit/DBXrefManager/z78540b.dat"), 0, 0, false);
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    Writer w = new BufferedWriter(new OutputStreamWriter(output));
    PFormatter formatter = new PFormatter(PFormatter.FORMAT.INSD_FORMAT, w, null);
    formatter.dump(seq);
    try {
      w.flush();
      w.close();
    } catch (IOException e) {
      e.printStackTrace();
    }

    String result = output.toString();

    assertTrue(result.contains("<INSDSeq_locus>Z78540</INSDSeq_locus>"));
    assertTrue(result.contains("<INSDSeq_moltype>DNA</INSDSeq_moltype>"));
    assertTrue(result
        .contains("<INSDSeq_taxonomy>Eukaryota;Metazoa;Nematoda;Chromadorea;Rhabditida;Rhabditoidea;Rhabditidae;Peloderinae;Caenorhabditis</INSDSeq_taxonomy>"));
    assertTrue(StringUtils.countMatches(result, "<INSDFeature>") == 2);
    assertTrue(StringUtils.countMatches(result, "<INSDQualifier>") == 8);
    try {
      output.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testSeqValidationSize() {
    completeInstall("fasta_seq_filter_size", "databank.dsc", false);
    assertNbOk(3, "fasta_seq_filter_size", "databank.dsc");
  }

  @Test
  public void testSeqValidationDescription() {
    completeInstall("genbank_desc_filter", "databank.dsc", false);
    assertNbOk(2, "genbank_desc_filter", "databank.dsc");
  }

  @Test
  public void testSeqValidationDescriptionApprox() {
    completeInstall("genbank_desc_filter", "databank_approx.dsc", false);
    assertNbOk(3, "genbank_desc_filter", "databank_approx.dsc");
  }

  @Test
  public void testRenameFasta() {
    completeInstall("fasta_rename", "Yersinia_Angola_nuc.dsc", false);
    assertNbOk(4045, "fasta_rename", "Yersinia_Angola_nuc.dsc");
  }

  @Test
  public void testRenameGenbank() {
    completeInstall("genbank_rename", "genbank_rename.dsc", false);
    assertNbOk(30, "genbank_rename", "genbank_rename.dsc");
  }

  @Test
  public void testBugFasta() {
    completeInstall("bugWithFasta", "Listeria_proteins.dsc", true);
    // discovered that trove3.x HashSet crashes jvm
  }

  @Test
  public void testDeleted() {
    List<String> deletedCodes = new ArrayList<String>();
    // install
    String dbName = completeInstall("uniprot", "sample_Uniprot.dsc", true);
    DBMirrorConfig currentConfig = DBDescriptorUtils
        .getDBMirrorConfig(DBMSAbstractConfig.getLocalMirrorConfFile());
    String kbCode = currentConfig.getDbKey(dbName);
    List<String> deleted = currentConfig
        .getMirrorCodes(DBMirrorConfig.DELETED_IDX);
    if (deleted == null) {
      deleted = new ArrayList<String>();
    }
    // assert everything is ok
    Assert.assertTrue(!deleted.contains(kbCode));
    // save config
    DBDescriptorUtils.saveDBMirrorConfig(
        DBMSAbstractConfig.getLocalMirrorConfFile(), currentConfig);
    currentConfig = DBDescriptorUtils.getDBMirrorConfig(DBMSAbstractConfig
        .getLocalMirrorConfFile());

    // reload config
    ArrayList<IdxDescriptor> data = new ArrayList<IdxDescriptor>();
    List<IdxDescriptor> all = DBDescriptorUtils
        .prepareIndexDBList(currentConfig);
    // remove the previous installed one
    for (IdxDescriptor idx : all) {
      if (idx.getName().equalsIgnoreCase(dbName)) {
        deletedCodes.add(idx.getKbCode());
      } else {
        data.add(idx);
      }
    }
    // save the config with the deleted one
    DBMirrorConfig newConfig = DBDescriptorUtils.getMirrorConfig(data, null);
    newConfig.removeMirrorCode(deletedCodes);
    DBDescriptorUtils.saveDBMirrorConfig(
        DBMSAbstractConfig.getLocalMirrorConfFile(), newConfig);

    // reload config
    currentConfig = DBDescriptorUtils.getDBMirrorConfig(DBMSAbstractConfig
        .getLocalMirrorConfFile());
    deleted = currentConfig.getMirrorCodes(DBMirrorConfig.DELETED_IDX);
    if (deleted == null) {
      deleted = new ArrayList<String>();
    }
    // assert deleted list is OK
    Assert.assertTrue(deleted.contains(kbCode));
    Assert.assertNull(currentConfig.getDbKey(dbName));

    // install once again
    dbName = completeInstall("uniprot", "sample_Uniprot.dsc", true);
    kbCode = currentConfig.getDbKey(dbName);
    // kb code is not reused
    Assert.assertTrue(!deleted.contains(kbCode));
  }

  @Test
  public void testNoIIPAndPrepaDir() {
    UtilsTest.cleanInstalledDatabanks("d");
    File n = new File(DBMSAbstractConfig.getLocalMirrorPrepaPath(), "n");
    FileUtils.deleteQuietly(n);
    if (!n.exists()) {
      Assert.assertTrue("Unable to create " + n.getAbsolutePath(), n.mkdirs());
    }
    Assert.assertFalse(DBMSAbstractConfig.getLocalMirrorPath()
        .equalsIgnoreCase(DBMSAbstractConfig.getLocalMirrorPrepaPath()));
    String dbName = completeInstall("testNoIIPAndPrepaDir",
        "sample_fasta_nuc.dsc", true);
    Assert.assertTrue(Paths
        .get(DBMSAbstractConfig.getLocalMirrorPrepaPath(), "n", dbName,
            "download", dbName, "d1").toFile().exists());
    Assert.assertFalse(Paths
        .get(DBMSAbstractConfig.getLocalMirrorPath(), "n", dbName, "current")
        .toFile().exists());
  }

  @Test
  public void testFastaWithTaxon() {
    completeInstall("fasta_with_taxon", "fasta_with_taxon.dsc", true);

    DBMirrorConfig currentConfig = DBDescriptorUtils.getLocalDBMirrorConfig();

    DBDescriptor myDesc = null;
    for (DBDescriptor desc : DBDescriptorUtils.getDBList(currentConfig,
        TYPE.blastp, false)) {
      if (StringUtils.equalsIgnoreCase(desc.getName(), "fasta_with_taxon")) {
        myDesc = desc;
        break;
      }
    }
    Assert.assertNotNull(myDesc);

    myDesc = null;
    for (DBDescriptor desc : DBDescriptorUtils.getDBList(currentConfig,
        TYPE.proteic, false)) {
      if (StringUtils.equalsIgnoreCase(desc.getName(), "fasta_with_taxon")) {
        myDesc = desc;
        break;
      }
    }
    Assert.assertNotNull(myDesc);

  }

  @Test
  public void testCDD() {
    List<DescriptorEntry> entries = new ArrayList<DescriptorEntry>();
    entries.add(RunningMirrorPanelTest.getEntry("CDD_terms.dsc", false,
        "databank", "cdd"));
    RunningMirrorPanelTest.installerPanel.startLoadingEntries(entries,
        PFTPLoaderDescriptor.MAINTASK_DOWNLOAD);

    entries = new ArrayList<DescriptorEntry>();
    entries.add(RunningMirrorPanelTest.getEntry("CDD.dsc", false, "databank",
        "cdd"));
    RunningMirrorPanelTest.installerPanel.startLoadingEntries(entries,
        PFTPLoaderDescriptor.MAINTASK_DOWNLOAD);

    assertNbOk(8, "cdd", "CDD.dsc");

    for (String header : getFastaSequenceHeaders("cdd", "CDD.dsc", false)) {
      Assert.assertTrue("inspect " + header,
          header.contains(DBXrefInstancesManager.HIT_DEF_LINE_START));
      Assert.assertTrue("inspect " + header,
          header.contains(DBXrefInstancesManager.HIT_DEF_LINE_STOP));
      Assert.assertTrue("inspect " + header, header.contains(Dicos.CDD.xrefId));
    }

    DBMirrorConfig currentConfig = DBDescriptorUtils.getLocalDBMirrorConfig();

    DBDescriptor myDesc = null;
    for (DBDescriptor desc : DBDescriptorUtils.getDBList(currentConfig,
        TYPE.blastp, false)) {
      if (StringUtils.equalsIgnoreCase(desc.getName(), "CDD")) {
        myDesc = desc;
        break;
      }
    }
    Assert.assertNotNull(myDesc);

    myDesc = null;
    for (DBDescriptor desc : DBDescriptorUtils.getDBList(currentConfig,
        TYPE.proteic, false)) {
      if (StringUtils.equalsIgnoreCase(desc.getName(), "CDD")) {
        myDesc = desc;
        break;
      }
    }
    Assert.assertNotNull(myDesc);
  }
}
