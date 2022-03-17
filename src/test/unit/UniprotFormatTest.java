/* Copyright (C) 2019-2022 Patrick G. Durand
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;

import org.biojava.bio.seq.Feature;
import org.biojava.bio.seq.Sequence;
import org.biojava.bio.seq.SequenceIterator;
import org.biojava.bio.seq.io.SeqIOTools;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import bzh.plealog.bioinfo.api.data.feature.FeatureTable;
import bzh.plealog.bioinfo.data.sequence.IBankSequenceInfo;
import bzh.plealog.dbmirror.reader.DBUtils;

@SuppressWarnings("deprecation")
public class UniprotFormatTest {
  // change in Uniprot data format (Feature Table) introduce in release 2019_12
  // see https://www.uniprot.org/news/2019/12/18/release#text%5Fft
  // We use an update release of Biojava legacy 1.9 from
  // https://github.com/pgdurand/biojava-legacy.git

  private static String up_file_new = "019R_FRG3G_nf.dat";
  private static int expect_feat_new = 12;
  
  @SuppressWarnings("unused")
  private static String up_file_old = "019R_FRG3G_of.dat";
  @SuppressWarnings("unused")
  private static int expect_feat_old = 8;
  
  private static String current_file = up_file_new;
  private static int current_feats = expect_feat_new;
  
  private static final boolean TEST_ENTIRE_SWISS = false;
  private static String SWISS_PATH = 
      "/tmp/biobanks/p/Uniprot_SwissProt/current/Uniprot_SwissProt/uniprot_sprot.dat";
  private static final boolean DISPLAY_ID = true;
  
  private static HashSet<String> featBiojavaLocRef;
    
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    UtilsTest.configureApp();
    featBiojavaLocRef = new HashSet<String>();
    featBiojavaLocRef.add("chain-1-129-/note=\"toto\" /evidence=\"titi\"");
    featBiojavaLocRef.add("mod_res-1-1-/IsoformId=\"P68250-2\" /note=\"toto\" /evidence=\"titi\"");
    featBiojavaLocRef.add("chain-2 147 483 647-245-/note=\"toto\" /evidence=\"titi\"");
    featBiojavaLocRef.add("chain-31-137-/note=\"toto\" /evidence=\"titi\"");
    featBiojavaLocRef.add("chain-1-55-/note=\"toto\" /evidence=\"titi\"");
    featBiojavaLocRef.add("transit-1--2 147 483 648-/note=\"toto\" /evidence=\"titi\"");
    featBiojavaLocRef.add("chain-69-255-/note=\"toto\" /evidence=\"titi\"");
    featBiojavaLocRef.add("chain-1-24-/note=\"toto\" /evidence=\"titi\"");
    featBiojavaLocRef.add("signal-1--2 147 483 648-/note=\"toto\" /evidence=\"titi\"");
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
  public static final MessageFormat FEATURE_FORMAT = new MessageFormat("{0}-{1}-{2}-{3}");
  private String featureToString(Feature feat) {
    String f = FEATURE_FORMAT.format(new Object[] { 
        feat.getType().toLowerCase(),
        feat.getLocation().getMin(),
        feat.getLocation().getMax(),
        feat.getAnnotation().getProperty("swissprot.featureattribute").toString().trim()});
    //System.out.println("  " + f);
    return f;
  }
  // Method used to encode BeeDeeM Feature properties in order to compare them with BioJava ones
  private String getDescription(bzh.plealog.bioinfo.api.data.feature.Feature feat) {
    Enumeration<bzh.plealog.bioinfo.api.data.feature.Qualifier> enumQ = feat.enumQualifiers();
    while(enumQ.hasMoreElements()) {
      bzh.plealog.bioinfo.api.data.feature.Qualifier qual = enumQ.nextElement();
      if (qual.getName().equalsIgnoreCase(feat.getKey())) {
        return qual.getValue();
      }
    }
    return "?";
  }
  //Method used to encode BioJava Feature properties in order to compare them with BeeDeeM ones
  private String featureToString(bzh.plealog.bioinfo.api.data.feature.Feature feat) {
    String f = FEATURE_FORMAT.format(new Object[] { 
        feat.getKey(),
        feat.getFrom(),
        feat.getTo(),
        getDescription(feat)});
    //System.out.println("  " + f);
    return f;
  }
  
  @SuppressWarnings("rawtypes" )
  private void testSequence(Sequence seq, int curFeats) {
    IBankSequenceInfo si;
    FeatureTable ft;
    HashSet<String> featBiojava, featBeeDeeM;
    assertNotNull(seq);
    if (DISPLAY_ID) {
      System.out.println("> " + seq.getName());
    }
    Iterator iterF = seq.features();
    featBiojava = new HashSet<String>();
    while (iterF.hasNext()) {
      Feature feat = (Feature) iterF.next();
      if (
          !(feat.getLocation().getMin()<0 ||
          feat.getLocation().getMin()==Integer.MAX_VALUE ||
          feat.getLocation().getMax()<0 ||
          feat.getLocation().getMax()==Integer.MAX_VALUE)
          ) {
        //BeeDeeM does not handle fuzzy locations
        featBiojava.add(featureToString(feat));
      }
    }
    if (curFeats!=-1) {
      assertEquals(featBiojava.size(), curFeats);
    }
    // get sequence info
    si = DBUtils.returnUPSeqInfo(seq, false);
    // get feature table; BeeDeeM always add source and protein features w/t what Biojava does provide
    // (source contains taxon info, protein contains protein description and db_xrefs)
    ft = DBUtils.returnUPFeatureTable(seq, si.getId(), 0, 0, false);
    
    assertEquals(featBiojava.size(), ft.features()-2);
    Enumeration<bzh.plealog.bioinfo.api.data.feature.Feature> enumF = ft.enumFeatures();
    featBeeDeeM = new HashSet<String>();
    while(enumF.hasMoreElements()) {
      bzh.plealog.bioinfo.api.data.feature.Feature feat = enumF.nextElement();
      featBeeDeeM.add(featureToString(feat));
    }
    for(String f : featBeeDeeM) {
      if (featBiojava.contains(f)) {
        featBiojava.remove(f);
      }
    }
    assertEquals(featBiojava.size(), 0);
  }
  
  @Test
  public void testUniprotFormatChange() {
    System.out.println(">> testUniprotFormatChange");
    // Uniprot format manual: https://web.expasy.org/docs/userman.html
    // Test Uniprot data format, i.e. Feature Table encoded as:
    //FT   DOMAIN      128    409       Protein kinase.
    //FT   NP_BIND     134    142       ATP (By similarity).
    // From Uniprot release 76 and above, Features are encoded as:
    //FT   CHAIN           1..851
    //FT                   /note="Putative serine/threonine-protein kinase 019R"
    //FT                   /id="PRO_0000410576"

    String file = UtilsTest.getTestFilePath("databank", "uniprot", current_file);
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      Sequence seq;
      
      
      // read the Uniprot File
      SequenceIterator iter = SeqIOTools.readSwissprot(reader);
      assertTrue(iter.hasNext());
      seq = iter.nextSequence();
      testSequence(seq, current_feats);
    } catch (Exception ex) {
      System.err.println("unable to read UniProt file: " + file + ": " + ex);
    }
  }

  @Test
  public void testLargeFile() {
    System.out.println(">> testLargeFile");

    String file = UtilsTest.getTestFilePath("databank", "uniprot", "uniprot_new.dat");
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      Sequence seq;
      // read the Uniprot File
      SequenceIterator iter = SeqIOTools.readSwissprot(reader);
      while(iter.hasNext()) {
        seq = iter.nextSequence();
        testSequence(seq, -1);
      }
    } catch (Exception ex) {
      System.err.println("unable to read UniProt file: " + file + ": " + ex);
    }
  }

  @Test
  public void testUniqueFile() {
    //specific test to debug entire SwissProt
    //Use this URL to get specifi entry:
    // https://www.uniprot.org/uniprot/C0HLM8.txt
    System.out.println(">> testUniqueFile");
    String file = UtilsTest.getTestFilePath("databank", "uniprot", "single-up-test.dat");
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      Sequence seq;
      // read the Uniprot File
      SequenceIterator iter = SeqIOTools.readSwissprot(reader);
      while(iter.hasNext()) {
        seq = iter.nextSequence();
        testSequence(seq, -1);
      }
    } catch (Exception ex) {
      System.err.println("unable to read UniProt file: " + file + ": " + ex);
    }
  }
  
  @SuppressWarnings("rawtypes")
  @Test
  public void testAllLocationTypes() {
    //specific test to check all types of Uniprot FT locations
    System.out.println(">> testUniqueFile");
    String file = UtilsTest.getTestFilePath("databank", "uniprot", "locations-up-test.dat");
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      HashSet<String> featBiojava;
      Sequence seq;
      // read the Uniprot File
      SequenceIterator iter = SeqIOTools.readSwissprot(reader);
      while(iter.hasNext()) {
        seq = iter.nextSequence();
        /**/

        assertNotNull(seq);
        if (DISPLAY_ID) {
          System.out.println("> " + seq.getName());
        }
        Iterator iterF = seq.features();
        featBiojava = new HashSet<String>();
        while (iterF.hasNext()) {
          Feature feat = (Feature) iterF.next();
            //BeeDeeM does not handle fuzzy locations
            featBiojava.add(featureToString(feat));
        }
        assertEquals(featBiojava.size(), 9);
        for (String str : featBiojavaLocRef) {
          featBiojava.remove(str);
        }
        assertEquals(featBiojava.size(), 0);
      }
    } catch (Exception ex) {
      System.err.println("unable to read UniProt file: " + file + ": " + ex);
    }
  }
  
  @Test
  public void testEntireSwissProt() {
    if (!TEST_ENTIRE_SWISS)
      return;
    System.out.println(">> testEntireSwissProt");

    //not really a bad error
    if (new File(SWISS_PATH).exists() == false) {
      System.err.println("File not found: " + SWISS_PATH);
      return;
    }
    int counter=0;
    try (BufferedReader reader = new BufferedReader(new FileReader(SWISS_PATH))) {
      Sequence seq;
      // read the Uniprot File
      SequenceIterator iter = SeqIOTools.readSwissprot(reader);
      while(iter.hasNext()) {
        seq = iter.nextSequence();
        testSequence(seq, -1);
        counter++;
        if (counter % 10000 == 0) {
          System.out.println("Sequences reviewed: " + counter);
        }
      }
    } catch (Exception ex) {
      System.err.println("unable to read UniProt file: " + SWISS_PATH + ": " + ex);
    }
    System.out.println("Total Sequences: " + counter);
  }
}
