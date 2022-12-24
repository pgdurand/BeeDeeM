/* Copyright (C) 2006-2021 Ludovic Antin, Patrick Durand
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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import bzh.plealog.dbmirror.lucenedico.Dicos;
import bzh.plealog.dbmirror.main.CmdLineQuery;
import bzh.plealog.dbmirror.reader.PQueryMirrorBase;
import bzh.plealog.dbmirror.reader.PSequence;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;

/**
 * Unit tests of the CmdLine Query Tool and underlying PQueryMirrorBase class.
 * 
 * @author Patrick Durand
 */
public class CmdLineQueryTest {

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    UtilsTest.configureApp();
    UtilsTest.cleanInstalledDatabanks();
    DefaultLoaderMonitorTest.completeInstall("uniprot", "sample_Uniprot.dsc", true);
    DefaultLoaderMonitorTest.completeInstall("taxonomy", "NCBI_Taxonomy.dsc", true);
    DefaultLoaderMonitorTest.completeInstall("enzyme", "enzyme.dsc", true);
    DefaultLoaderMonitorTest.completeInstall("cdd", "CDD_terms.dsc", true);
    DefaultLoaderMonitorTest.completeInstall("interpro", "ipr_terms.dsc", true);
    DefaultLoaderMonitorTest.completeInstall("go", "go_terms.dsc", true);
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    UtilsTest.cleanInstalledDatabanks();
    try {
      FileUtils.deleteDirectory(new File(DBMSAbstractConfig.getLocalMirrorPath()));
    } catch (IOException e) {
    }
  }

  @After
  public void tearDown() {
  }

  public boolean compareTwoFiles(File file1, File file2)
      throws IOException {

    BufferedReader br1 = new BufferedReader(new FileReader(file1));
    BufferedReader br2 = new BufferedReader(new FileReader(file2));

    String thisLine = null;
    String thatLine = null;

    List<String> list1 = new ArrayList<String>();
    List<String> list2 = new ArrayList<String>();

    while ((thisLine = br1.readLine()) != null) {
      if (thisLine.isEmpty()==false)
        list1.add(thisLine);
    }
    while ((thatLine = br2.readLine()) != null) {
      if (thatLine.isEmpty()==false)
        list2.add(thatLine);
    }

    br1.close();
    br2.close();

    return list1.equals(list2);
  }
  
  protected PrintStream outputFile(File name) throws FileNotFoundException {
    return new PrintStream(new BufferedOutputStream(new FileOutputStream(name)), true);
  }

  @Test
  public void testCmdLine() {
    String[] args = {
        "-d", "protein", 
        "-i", "KKCC1_RAT",
        "-f", "txt"};
    
    File refFile = new File(UtilsTest.getTestFilePath("Tools", "query.dat"));

    File result=null;
    try {
      result = File.createTempFile("bdmQueryTest", ".dat");
      result.deleteOnExit();
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }
    try {
      System.setOut(outputFile(result));
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail();
    }

    Assert.assertTrue(new CmdLineQuery().execute(args));
    try {
      Assert.assertTrue(compareTwoFiles(refFile, result));
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }
  }
  
  @Test
  public void testCmdLineFOIDs() {
    File foIDsFile = new File(UtilsTest.getTestFilePath("Tools", "foIDs.txt"));
    
    String[] args = {
        "-d", "protein", 
        "-i", foIDsFile.getAbsolutePath(),
        "-f", "txt"};
    
    File refFile = new File(UtilsTest.getTestFilePath("Tools", "query3.dat"));

    File result=null;
    try {
      result = File.createTempFile("bdmQueryTest", ".dat");
      result.deleteOnExit();
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }
    try {
      System.setOut(outputFile(result));
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail();
    }

    //AssertFalse because file of IDs contains a fake ID, so we expect an error message
    Assert.assertFalse(new CmdLineQuery().execute(args));
    try {
      Assert.assertTrue(compareTwoFiles(refFile, result));
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }
  }
  
  @Test
  public void testQueryMirrorBase1() {
    
    PQueryMirrorBase querySystem = new PQueryMirrorBase();
    Hashtable<String, String> values;
    
    // get the key/value data and process the query
    values = new Hashtable<>();
    values.put("database", "protein");
    values.put("id", "KKCC1_RAT");
    values.put("format", "txt");

    //txt format: only dump entry on stdout
    PSequence[] seqs = querySystem.executeJob(values, System.out, System.err, 
        DBMSAbstractConfig.getLocalMirrorConfFile());
    Assert.assertFalse(querySystem.terminateWithError());
    Assert.assertNull(seqs);
  }
  
  @Test
  public void testQueryMirrorBase2() {
    
    PQueryMirrorBase querySystem = new PQueryMirrorBase();
    Hashtable<String, String> values;
    
    // get the key/value data and process the query
    values = new Hashtable<>();
    values.put("database", "protein");
    values.put("id", "KKCC1_RAT");
    values.put("format", "insd");

    //txt format: only dump entry on stdout
    PSequence[] seqs = querySystem.executeJob(values, System.out, System.err, 
        DBMSAbstractConfig.getLocalMirrorConfFile());
    Assert.assertFalse(querySystem.terminateWithError());
    Assert.assertNotNull(seqs);
    Assert.assertNotNull(seqs[0]);
    Assert.assertNotNull(seqs[0].getFeatTable());
    Assert.assertEquals(seqs[0].getFeatTable().features(), 25);
    Assert.assertNotNull(seqs[0].getSeqInfo());
    Assert.assertEquals(seqs[0].getSeqInfo().getId(), "P97756");
    Assert.assertEquals(seqs[0].getSeqInfo().getOrganism(), "Rattus norvegicus (Rat)");
    Assert.assertEquals(seqs[0].getSequence().length(), 505);
    Assert.assertTrue(seqs[0].getSequence().startsWith("MERSPAVCCQDPRAELVERVAA"));
  }
  
  @Test
  public void testQueryMirrorBase3() {
    
    PQueryMirrorBase querySystem = new PQueryMirrorBase();
    Hashtable<String, String> values;
    
    // get the key/value data and process the query
    values = new Hashtable<>();
    values.put("database", "protein");
    values.put("id", "COCO_CHANEL");
    values.put("format", "insd");

    PSequence[] seqs = querySystem.executeJob(values, System.out, null, 
        DBMSAbstractConfig.getLocalMirrorConfFile());
    Assert.assertTrue(querySystem.terminateWithError());
    Assert.assertNull(seqs);
  }
  
  @Test
  public void testQueryMirrorBase4() {
    PQueryMirrorBase querySystem = new PQueryMirrorBase();
    
    PSequence[] seqs = querySystem.executeJob("protein", "Q91356_COTCO", 0,
        0, false, "fas", System.out, DBMSAbstractConfig.getLocalMirrorConfFile());

    Assert.assertFalse(querySystem.terminateWithError());
    Assert.assertNotNull(seqs);
    
    Assert.assertNotNull(seqs[0]);
    Assert.assertNotNull(seqs[0].getFeatTable());
    Assert.assertEquals(seqs[0].getFeatTable().features(), 3);
    Assert.assertNotNull(seqs[0].getSeqInfo());
    Assert.assertEquals(seqs[0].getSeqInfo().getId(), "Q91356");
    Assert.assertEquals(seqs[0].getSeqInfo().getOrganism(), "Coturnix coturnix (Common quail) (Tetrao coturnix)");
    Assert.assertEquals(seqs[0].getSequence().length(), 367);
    Assert.assertTrue(seqs[0].getSequence().startsWith("WMDLWQSPLTMEDLICYSF"));
    Assert.assertTrue(seqs[0].getSequence().endsWith("PPALHASFFSEQY"));
  }
  
  @Test
  public void testQueryMirrorBase5() {
    PQueryMirrorBase querySystem = new PQueryMirrorBase();
    
    PSequence[] seqs = querySystem.executeJob("protein", "Q91356_COTCO", 15,
        128, false, "fas", System.out, DBMSAbstractConfig.getLocalMirrorConfFile());

    Assert.assertFalse(querySystem.terminateWithError());
    Assert.assertNotNull(seqs);
    
    Assert.assertNotNull(seqs[0]);
    Assert.assertNotNull(seqs[0].getFeatTable());
    Assert.assertEquals(seqs[0].getFeatTable().features(), 2);
    Assert.assertNotNull(seqs[0].getSeqInfo());
    Assert.assertEquals(seqs[0].getSeqInfo().getId(), "Q91356");
    Assert.assertEquals(seqs[0].getSeqInfo().getOrganism(), "Coturnix coturnix (Common quail) (Tetrao coturnix)");
    Assert.assertEquals(seqs[0].getSequence().length(), 114);
    Assert.assertTrue(seqs[0].getSequence().startsWith("ICYSFQ"));
    Assert.assertTrue(seqs[0].getSequence().endsWith("ASPYPGVQINEEFCQRF"));
  }

  @Test
  public void testCmdLineTaxo() {
    
    File refFile = new File(UtilsTest.getTestFilePath("Tools", "queryTaxo.dat"));
    
    File result=null;
    try {
      result = File.createTempFile("bdmQueryTest", ".dat");
      result.deleteOnExit();
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }

    String[] args = {
        "-d", "dico", 
        "-i", "9606,2157,10116,10090,45351,99999",
        "-f", "txt",
        "-o", result.getAbsolutePath()};

    Assert.assertTrue(new CmdLineQuery().execute(args));
    
    try {
      Assert.assertTrue(compareTwoFiles(refFile, result));
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }

  }
  
  @Test
  public void testQueryMirrorBase6() {
    
    PQueryMirrorBase querySystem = new PQueryMirrorBase();
    Hashtable<String, String> values;
    
    // get the key/value data and process the query
    values = new Hashtable<>();
    values.put("database", "taxo"); // Use wrong dico name
    values.put("id", "9606,2157,10116,10090,45351,99999");
    values.put("format", "txt");

    //txt format: only dump entry on stdout
    PSequence[] seqs = querySystem.executeJob(values, System.out, System.err, 
        DBMSAbstractConfig.getLocalMirrorConfFile());
    //wrong index name, so we expect failure
    Assert.assertTrue(querySystem.terminateWithError());
    //wrong index, so return empty PSequences
    Assert.assertNotNull(seqs);
  }
  
  @Test
  public void testQueryMirrorBase7() {
    
    PQueryMirrorBase querySystem = new PQueryMirrorBase();
    Hashtable<String, String> values;
    
    // get the key/value data and process the query
    values = new Hashtable<>();
    values.put("database", "dico"); 
    //AAAAA is not a TaxID, by design the software does not report it as error
    //but as an unknown TaxID 
    values.put("id", "9606,2157,10116,10090,45351,99999,AAAAA");
    values.put("format", "txt");

    //txt format: only dump entry on stdout
    PSequence[] seqs = querySystem.executeJob(values, System.out, System.err, 
        DBMSAbstractConfig.getLocalMirrorConfFile());
    Assert.assertFalse(querySystem.terminateWithError());
    Assert.assertNull(seqs);
  }
  
  @Test
  public void testQueryMirrorBase8() {
    
    File refFile = new File(UtilsTest.getTestFilePath("Tools", "queryTaxo2.dat"));
    
    File result=null;
    try {
      result = File.createTempFile("bdmQueryTest", ".dat");
      result.deleteOnExit();
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }

    try {
      System.setOut(outputFile(result));
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail();
    }
    PQueryMirrorBase querySystem = new PQueryMirrorBase();
    Hashtable<String, String> values;
    
    // get the key/value data and process the query
    values = new Hashtable<>();
    values.put("database", "dico"); 
    //try to locates some organisms... or not!
    //TaxFinder enables some typos, so try it too!
    values.put("id", "Gadus morhua, homo spaiens, COCO_CHANEL, Archaea, Vespa orientalis");
    values.put("format", "txt");

    //txt format: only dump entry on stdout
    querySystem.executeJob(values, System.out, System.err, 
        DBMSAbstractConfig.getLocalMirrorConfFile());
    Assert.assertFalse(querySystem.terminateWithError());
    try {
      Assert.assertTrue(compareTwoFiles(refFile, result));
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }
  }
  
  @Test
  public void testQueryMirrorBase9() {
    
    File refFile = new File(UtilsTest.getTestFilePath("Tools", "queryCDD.dat"));
    
    File result=null;
    try {
      result = File.createTempFile("bdmQueryTest", ".dat");
      result.deleteOnExit();
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }

    try {
      System.setOut(outputFile(result));
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail();
    }
    PQueryMirrorBase querySystem = new PQueryMirrorBase();
    Hashtable<String, String> values;
    
    // get the key/value data and process the query
    values = new Hashtable<>();
    values.put("database", "dico:"+Dicos.CDD.xrefId);//xrefId=CDD  
    //AAAAA is not a TaxID, by design the software does not report it as error
    //but as an unknown TaxID 
    values.put("id", "214330, 176955,198171");
    values.put("format", "txt");

    //txt format: only dump entry on stdout
    querySystem.executeJob(values, System.out, System.err, 
        DBMSAbstractConfig.getLocalMirrorConfFile());
    Assert.assertFalse(querySystem.terminateWithError());
    try {
      Assert.assertTrue(compareTwoFiles(refFile, result));
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }
  }
  
  
  @Test
  public void testQueryMirrorBase10() {
    
    File refFile = new File(UtilsTest.getTestFilePath("Tools", "queryEC.dat"));
    
    File result=null;
    try {
      result = File.createTempFile("bdmQueryTest", ".dat");
      result.deleteOnExit();
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }

    try {
      System.setOut(outputFile(result));
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail();
    }
    PQueryMirrorBase querySystem = new PQueryMirrorBase();
    Hashtable<String, String> values;
    
    // get the key/value data and process the query
    values = new Hashtable<>();
    values.put("database", "dico:"+Dicos.ENZYME.xrefId);//xrefId=EC 
    //AAAAA is not a TaxID, by design the software does not report it as error
    //but as an unknown TaxID 
    values.put("id", "1.1.1.4, 3.2.1.5, 4.5, A.B.C");
    values.put("format", "txt");

    //txt format: only dump entry on stdout
    querySystem.executeJob(values, System.out, System.err, 
        DBMSAbstractConfig.getLocalMirrorConfFile());
    Assert.assertFalse(querySystem.terminateWithError());
    try {
      Assert.assertTrue(compareTwoFiles(refFile, result));
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }
  }
  
  @Test
  public void testQueryMirrorBase11() {
    
    File refFile = new File(UtilsTest.getTestFilePath("Tools", "queryIPR.dat"));
    
    File result=null;
    try {
      result = File.createTempFile("bdmQueryTest", ".dat");
      result.deleteOnExit();
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }

    try {
      System.setOut(outputFile(result));
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail();
    }
    PQueryMirrorBase querySystem = new PQueryMirrorBase();
    Hashtable<String, String> values;
    
    // get the key/value data and process the query
    values = new Hashtable<>();
    values.put("database", "dico:"+Dicos.INTERPRO.xrefId);//xrefId=InterPro 
    //AAAAA is not a TaxID, by design the software does not report it as error
    //but as an unknown TaxID 
    values.put("id", "IPR000001,IPR000013, IPR046327, IPR046335, IPR_HERMES");
    values.put("format", "txt");

    //txt format: only dump entry on stdout
    querySystem.executeJob(values, System.out, System.err, 
        DBMSAbstractConfig.getLocalMirrorConfFile());
    Assert.assertFalse(querySystem.terminateWithError());
    try {
      Assert.assertTrue(compareTwoFiles(refFile, result));
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }
  }
  
  @Test
  public void testQueryMirrorBase12() {
    
    File refFile = new File(UtilsTest.getTestFilePath("Tools", "queryGO.dat"));
    
    File result=null;
    try {
      result = File.createTempFile("bdmQueryTest", ".dat");
      result.deleteOnExit();
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }

    try {
      System.setOut(outputFile(result));
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail();
    }
    PQueryMirrorBase querySystem = new PQueryMirrorBase();
    Hashtable<String, String> values;
    
    // get the key/value data and process the query
    values = new Hashtable<>();
    values.put("database", "dico:"+Dicos.GENE_ONTOLOGY.xrefId);//xrefId=GO
    //AAAAA is not a TaxID, by design the software does not report it as error
    //but as an unknown TaxID 
    values.put("id", "GO:0000002,GO:0018130,GO:2001316,2001315,0000005,GUERLAIN");
    values.put("format", "txt");

    //txt format: only dump entry on stdout
    querySystem.executeJob(values, System.out, System.err, 
        DBMSAbstractConfig.getLocalMirrorConfFile());
    Assert.assertFalse(querySystem.terminateWithError());
    try {
      Assert.assertTrue(compareTwoFiles(refFile, result));
    } catch (IOException e) {
      e.printStackTrace();
      Assert.fail();
    }
  }
}
