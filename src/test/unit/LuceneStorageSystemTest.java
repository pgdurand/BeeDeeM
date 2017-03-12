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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.util.Version;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import bzh.plealog.dbmirror.indexer.DBEntry;
import bzh.plealog.dbmirror.indexer.FastaParser;
import bzh.plealog.dbmirror.indexer.LuceneStorageSystem;
import bzh.plealog.dbmirror.indexer.ParserMonitor;
import bzh.plealog.dbmirror.indexer.StorageSystem;
import bzh.plealog.dbmirror.util.runner.DBMSUniqueSeqIdDetector;

public class LuceneStorageSystemTest {

  private LuceneStorageSystem lss;

  private String getTestFilePath(String methodName, String fileName) {
    return UtilsTest.getTestFilePath("LuceneStorageSystem", methodName,
        fileName);
  }

  @Before
  public void setUp() throws Exception {
    UtilsTest.configureApp();
    this.lss = new LuceneStorageSystem();
  }

  private TestMonitor parseFastaFile(String methodName, String idxDirName,
      String fastaFilename, String redondantDirName) {
    String idxDirPath = getTestFilePath(methodName, idxDirName);
    String fastaFilePath = getTestFilePath(methodName, fastaFilename);
    String redondantDirPath = getTestFilePath(methodName, redondantDirName);
    TestMonitor monitor = null;
    File idxFile = new File(idxDirPath);
    if (idxFile.exists()) {
      if (!idxFile.delete()) {
        try {
          FileUtils.deleteDirectory(idxFile);
        } catch (IOException e) {
          Assert.fail("Unable to delete " + idxFile.getAbsolutePath() + " : "
              + e.getMessage());
        }
      }
    }
    // delete the 'd1' file too
    File d1File = new File(getTestFilePath(methodName, "d1"));
    if (d1File.exists()) {
      if (!d1File.delete()) {
        Assert.fail("Unable to delete " + d1File.getAbsolutePath());
      }
    }
    // check redondant
    File redondantDir = new File(redondantDirPath);
    if (redondantDir.exists()) {
      if (!redondantDir.delete()) {
        try {
          FileUtils.deleteDirectory(redondantDir);
        } catch (IOException e) {
          Assert.fail("Unable to delete " + redondantDir.getAbsolutePath()
              + " : " + e.getMessage());
        }
      }
    }

    // launch indexation
    try {
      this.lss.open(idxDirPath, StorageSystem.WRITE_MODE);
      monitor = new TestMonitor(this.lss, redondantDirPath);
      FastaParser parser = new FastaParser();
      parser.setParserMonitor(monitor);
      parser.setCheckSeqIdRedundancy(true);
      parser.setVerbose(false);
      parser.parse(fastaFilePath, this.lss);
    } catch (Exception ex) {
      Assert.fail(ex.getMessage());
    } finally {
      this.lss.close();
    }

    return monitor;
  }

  @Test
  public void fastaParser() {
    String idxDirPath = getTestFilePath("FastaParser", "uniprot.ldx");
    TestMonitor monitor = parseFastaFile("FastaParser", "uniprot.ldx",
        "uniprot.faa", "seqIds.ldx");
    DBEntry[] entries;
    String currentId;

    // check indexation
    assertTrue("Unable to open fasta index " + idxDirPath,
        this.lss.open(idxDirPath, StorageSystem.READ_MODE));
    for (int i = 0; i < monitor.getIds().size(); i++) {
      currentId = monitor.getIds().get(i);
      QueryParser queryParser = new QueryParser(Version.LUCENE_29,
          LuceneStorageSystem.IDXABLE_FIELD, new StandardAnalyzer(
              Version.LUCENE_29));

      BooleanQuery query = new BooleanQuery();
      try {
        query.add(queryParser.parse(currentId), Occur.MUST);
      } catch (ParseException e) {
        fail(e.getMessage());
      }
      entries = this.lss.getEntry(currentId, query);
      assertNotNull("Sequence id '" + currentId + "' not found", entries);
      assertEquals(1, entries.length);

      entries = this.lss.getEntry(currentId);
      assertNotNull("Sequence id '" + currentId + "' not found", entries);
      assertTrue(entries.length > 0);
      assertEquals("Sequence id '" + currentId + "' is not the first result",
          currentId, entries[0].getId());

      query = new BooleanQuery();
      try {
        query.add(queryParser.parse(currentId), Occur.MUST);
      } catch (ParseException e) {
        fail(e.getMessage());
      }
      entries = this.lss.getEntry(currentId, query);
      assertNotNull("Sequence id '" + currentId + "' not found", entries);
      assertEquals(1, entries.length);

      entries = this.lss.getEntry(currentId);
      assertNotNull("Sequence id '" + currentId + "' not found", entries);

    }

    // check redundant
    assertEquals(0, monitor.getRedundantIds().size());

    try {
      File dir = new File(idxDirPath);
      dir.delete();
      FileUtils.deleteDirectory(dir);
    } catch (Exception e) {
    }

    try {
      File dir = new File(getTestFilePath("FastaParser", "seqIds.ldx"));
      dir.delete();
      FileUtils.deleteDirectory(dir);
    } catch (Exception e) {
    }
  }

  @Test
  public void redundantSequences() {
    TestMonitor monitor = parseFastaFile("RedundantSequences", "uniprot.ldx",
        "uniprot.faa", "seqIds.ldx");
    assertEquals(3, monitor.getRedundantIds().size());

    try {
      File dir = new File(getTestFilePath("RedundantSequences", "uniprot.ldx"));
      dir.delete();
      FileUtils.deleteDirectory(dir);
    } catch (Exception e) {
    }

    try {
      File dir = new File(getTestFilePath("RedundantSequences", "seqIds.ldx"));
      dir.delete();
      FileUtils.deleteDirectory(dir);
    } catch (Exception e) {
    }
  }

  private static class TestMonitor implements ParserMonitor {
    private ArrayList<String>       ids;
    private ArrayList<String>       names;
    private DBMSUniqueSeqIdDetector seqIdDetector;
    private ArrayList<String>       redundantIds;

    public TestMonitor(LuceneStorageSystem lss, String redondantDirPath) {
      this.ids = new ArrayList<String>();
      this.names = new ArrayList<String>();
      this.redundantIds = new ArrayList<String>();
      this.seqIdDetector = new DBMSUniqueSeqIdDetector(redondantDirPath);
    }

    public ArrayList<String> getIds() {
      return this.ids;
    }

    public ArrayList<String> getRedundantIds() {
      return this.redundantIds;
    }

    public void seqFound(String id, String name, String fName, long start,
        long stop, boolean checkRedundancy) {
      this.ids.add(id);
      this.names.add(name);
      System.out.println("Sequence found : " + id);
      if (!this.seqIdDetector.add(id)) {
        this.redundantIds.add(id);
      }
    }

    public void startProcessingFile(String fName, long fSize) {
    }

    public void stopProcessingFile(String file, int entries) {
      seqIdDetector.closeIndex();
    }

    public boolean redundantSequenceFound() {
      return this.redundantIds.size() > 0;
    }

  }
}
