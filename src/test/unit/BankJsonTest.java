/* Copyright (C) 2021 Patrick G. Durand
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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import bzh.plealog.dbmirror.fetcher.DBServerConfig;
import bzh.plealog.dbmirror.util.conf.BankJsonDescriptor;

public class BankJsonTest {
  // https://github.com/FasterXML/jackson-databind
  // https://www.twilio.com/blog/java-json-with-jackson
  
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    UtilsTest.configureApp();
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

    
  @Test
  public void testBankLoader() {
    File f = new File("tests/junit/Json/databank.json");
    BankJsonDescriptor desc = BankJsonDescriptor.read(f);
    assertNotNull(desc);
    assertEquals(desc.getMain().getName(), "Genbank_CoreNucleotide");
    assertEquals(desc.getMain().getSize().getBytes(), 2000310261569l);
    assertEquals(desc.getMain().getSize().getSequences(), 95538504);
    assertEquals(desc.getMain().getProvider().size(), 1);
    assertEquals(desc.getMain().getProvider().get(0), "NCBI");
    assertEquals(desc.getMain().getIndex().size(),2);
    assertNotNull(desc.getMain().getIndex().get(BankJsonDescriptor.BLAST_INDEX));
    assertNotNull(desc.getMain().getIndex().get(BankJsonDescriptor.LUCENE_INDEX));
    assertEquals(desc.getMain().getInstallDate(), "24/04/2021");
    assertEquals(desc.getMain().getRelease(), "242");
    assertEquals(desc.getMain().getOwner(), "BeeDeeM");
    assertEquals(desc.getMain().getType().size(),1);
    assertEquals(desc.getMain().getType().get(0), BankJsonDescriptor.TYPE_NUCL);
    assertEquals(desc.getMain().getOmics().size(),1);
    assertEquals(desc.getMain().getOmics().get(0), BankJsonDescriptor.EDAM_GENOMICS);
  }
  
  @Test
  public void testBankWriter() {
    File f = new File("databank_w.json");
    BankJsonDescriptor desc = new BankJsonDescriptor(
        "Genbank_CoreNucleotide", 
        "Genbank_CoreNucleotide databank",
        "24/04/2021", 
        "242",
        DBServerConfig.NUCLEIC_TYPE, 
        "NCBI", 
        "/path/to/gbcore.nal", 
        "/path/to/gbcore.ldx",
        2000310261569l, 
        95538504);
    
    assertTrue(BankJsonDescriptor.write(f, desc));
    f.delete();
  }
}
