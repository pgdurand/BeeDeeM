/* Copyright (C) 2006-2020 Patrick Durand
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

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import bzh.plealog.bioinfo.api.data.searchresult.SRClassification;
import bzh.plealog.bioinfo.api.data.searchresult.SROutput;
import bzh.plealog.bioinfo.api.data.searchresult.io.SRWriter;
import bzh.plealog.bioinfo.io.searchresult.SerializerSystemFactory;
import bzh.plealog.bioinfo.io.searchresult.srnative.NativeBlastLoader;
import bzh.plealog.dbmirror.annotator.SRAnnotatorUtils;
import bzh.plealog.dbmirror.lucenedico.DicoTermQuerySystem;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptorUtils;

/**
 * Unit tests for SRClassification.
 *
 * @author Patrick G. Durand
 */
public class PAnnotateBlastClassifTest {
  private static DicoTermQuerySystem    dicoSystem;
  private static SRWriter         nativeBlastWriter;
  private static File            tmpFile;
  
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	  UtilsTest.configureApp();
    UtilsTest.cleanInstalledDatabanks();
    
	  // setup a native BOutput writer
    nativeBlastWriter = SerializerSystemFactory.getWriterInstance(SerializerSystemFactory.NATIVE_WRITER);
    // setup a temp file (will be deleted in tearDownAfterClass())
    tmpFile = File.createTempFile("blastTest", ".zml");
    System.out.println("Temp file is: "+ tmpFile.getAbsolutePath());
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	  UtilsTest.cleanInstalledDatabanks();
	}

	@After
	public void tearDown() {
	  try {
      FileUtils.deleteDirectory(new File(DBMSAbstractConfig.getLocalMirrorPath()));
    } catch (IOException e) {
    }
	}

	@Test
	public void testSRClassification() {
    // delete the db_mirror directory to be sure that only one bank is installed
    File mirrorPath = new File(DBMSAbstractConfig.getLocalMirrorPath());// getConfigurator().getProperty(KDMSConfigurator.MIRROR_PATH));
    if (mirrorPath.exists()) {
      try {
        FileUtils.cleanDirectory(mirrorPath);
        Thread.sleep(2000);
      } catch (Exception e) {
        e.printStackTrace();
        Assert.fail("Unable to clean the directory : " + mirrorPath.getAbsolutePath());
      }
    }
    // install the uniprot databank
    DefaultLoaderMonitorTest.completeInstall("uniprot", "sample_Uniprot.dsc", true);


    dicoSystem = DicoTermQuerySystem
        .getDicoTermQuerySystem(DBDescriptorUtils
            .getDBMirrorConfig(DBMSAbstractConfig.getLocalMirrorConfFile()));
    // load the Plast results
    NativeBlastLoader loader = new NativeBlastLoader();
    SROutput bo = loader.load(new File(UtilsTest.getTestFilePath("blast_results", "testFull", "hits_with_full_annot.zml")));
    Assert.assertTrue(bo != null);

    SRClassification classif = SRAnnotatorUtils.prepareClassification(bo, dicoSystem);
    assertNotNull(classif);
    assertEquals(classif.size(), 75);
    bo.setClassification(classif);
    
    // write new File
    nativeBlastWriter.write(tmpFile, bo);
	}
	
}
