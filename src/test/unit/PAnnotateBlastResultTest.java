/* Copyright (C) 2006-2017 Ludovic Antin
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
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import bzh.plealog.bioinfo.api.data.feature.Feature;
import bzh.plealog.bioinfo.api.data.feature.FeatureTable;
import bzh.plealog.bioinfo.api.data.feature.Qualifier;
import bzh.plealog.bioinfo.api.data.searchresult.SRHit;
import bzh.plealog.bioinfo.api.data.searchresult.SRHsp;
import bzh.plealog.bioinfo.api.data.searchresult.SRHspSequence;
import bzh.plealog.bioinfo.api.data.searchresult.SROutput;
import bzh.plealog.bioinfo.io.searchresult.srnative.NativeBlastLoader;
import bzh.plealog.dbmirror.annotator.SROutputAnnotator;
import bzh.plealog.dbmirror.main.Annotate;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;

/**
 * Unit tests of the Blast results Annotator.
 * 
 * @author Ludovic Antin
 * @author Patrick Durand
 */
public class PAnnotateBlastResultTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		UtilsTest.configureApp();
		UtilsTest.cleanInstalledDatabanks();
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

	private void compareBlastResults(SROutput ref, SROutput check) {
		List<SRHit> refHits = ref.getIteration(0).getHits();
		List<SRHit> checkHits = check.getIteration(0).getHits();
		Assert.assertEquals(refHits.size(), checkHits.size());

		SRHit refHit;
		SRHit checkHit;
		List<SRHsp> refHsps;
		List<SRHsp> checkHsps;
		SRHsp refHsp;
		SRHsp checkHsp;
		SRHspSequence refHitSequence;
		SRHspSequence checkHitSequence;
		FeatureTable refFeatureTable;
		FeatureTable checkFeatureTable;
		Feature refFeature = null;
		Feature checkFeature = null;
		Enumeration<Qualifier> refQualifiers = null;
		Enumeration<Qualifier> checkQualifiers = null;
		Qualifier refQualifier;
		Qualifier checkQualifier;

		// for each hit
		for (int indexHit = 0; indexHit < refHits.size(); indexHit++) {
			refHit = refHits.get(indexHit);
			checkHit = checkHits.get(indexHit);

			// count hsp
			refHsps = refHit.getHsps();
			checkHsps = checkHit.getHsps();
			Assert.assertEquals(refHsps.size(), checkHsps.size());

			// for each hsp
			for (int indexHsp = 0; indexHsp < refHsps.size(); indexHsp++) {
				refHsp = refHsps.get(indexHsp);
				checkHsp = checkHsps.get(indexHsp);

				// get hit sequence
				refHitSequence = refHsp.getHit();
				checkHitSequence = checkHsp.getHit();

				// count nb feature in feature table
				refFeatureTable = refHitSequence.getFeatures();
				checkFeatureTable = checkHitSequence.getFeatures();

				Assert.assertEquals(refFeatureTable.features(), checkFeatureTable.features());

				// get feature "source"
				while (refFeatureTable.enumFeatures().hasMoreElements()) {
					refFeature = refFeatureTable.enumFeatures().nextElement();
					if (refFeature.getKey().equalsIgnoreCase("source")) {
						break;
					}
				}
				Assert.assertNotNull(refFeature);
				while (checkFeatureTable.enumFeatures().hasMoreElements()) {
					checkFeature = checkFeatureTable.enumFeatures().nextElement();
					if (checkFeature.getKey().equalsIgnoreCase("source")) {
						break;
					}
				}
				Assert.assertNotNull(checkFeature);

				// count nb qualifiers
				Assert.assertEquals(refFeature.qualifiers(), checkFeature.qualifiers());
				refQualifiers = refFeature.enumQualifiers();

				// for each qualifier => test exist in ref
				while (refQualifiers.hasMoreElements()) {
					refQualifier = refQualifiers.nextElement();
					boolean found = false;

					checkQualifiers = checkFeature.enumQualifiers();
					while (checkQualifiers.hasMoreElements()) {
						checkQualifier = checkQualifiers.nextElement();
						if ((checkQualifier.getName().trim().equalsIgnoreCase(refQualifier.getName().trim()))
								&& (checkQualifier.getValue().trim().equalsIgnoreCase(refQualifier.getValue().trim()))) {
							found = true;
							break;
						}
					}

					Assert.assertTrue("Qualifier '" + refQualifier.getName() + "' with value '" + refQualifier.getValue() + "' not found.", found);
				}
			}
		}
	}

	@Test
	public void testBco() {
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

		// load the Plast results
		NativeBlastLoader loader = new NativeBlastLoader();
		SROutput bo_ref = loader.load(new File(UtilsTest.getTestFilePath("blast_results", "testBco", "hits_with_bco.zml")));
		Assert.assertTrue(bo_ref != null);

		SROutput bo_empty = loader.load(new File(UtilsTest.getTestFilePath("blast_results", "testBco", "hits_only.zml")));
		Assert.assertTrue(bo_empty != null);
		SROutputAnnotator annotator = new SROutputAnnotator();
		annotator.doClassificationAnnotation(bo_empty);
		Assert.assertTrue(bo_empty != null);

		compareBlastResults(bo_ref, bo_empty);
	}

	@Test
	public void testFull() {
		// delete the db_mirror directory to be sure that only one bank is installed
		File mirrorPath = new File(DBMSAbstractConfig.getLocalMirrorPath());// getConfigurator().getProperty(KDMSConfigurator.MIRROR_PATH));
		if (mirrorPath.exists()) {
			try {
				FileUtils.cleanDirectory(mirrorPath);
				Thread.sleep(2000);
			} catch (Exception e) {
				//e.printStackTrace();
				//Assert.fail("Unable to clean the directory : " + mirrorPath.getAbsolutePath());
			}
		}
		// install the uniprot databank
		DefaultLoaderMonitorTest.completeInstall("uniprot", "sample_Uniprot.dsc", true);

		// load the Plast results
		NativeBlastLoader loader = new NativeBlastLoader();
		SROutput bo_ref = loader.load(new File(UtilsTest.getTestFilePath("blast_results", "testFull", "hits_with_full_annot.zml")));
		Assert.assertTrue(bo_ref != null);

		SROutput bo_empty = loader.load(new File(UtilsTest.getTestFilePath("blast_results", "testFull", "hits_only.zml")));
		Assert.assertTrue(bo_empty != null);
		SROutputAnnotator annotator = new SROutputAnnotator();
		annotator.doFullAnnotation(bo_empty);
		Assert.assertTrue(bo_empty != null);

		compareBlastResults(bo_ref, bo_empty);
	}
	@Test
	public void testCmdLine() {
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
    File fToAnnotate = new File(UtilsTest.getTestFilePath("blast_results", "testFull", "hits_only.xml"));
    File result = null;
    try {
      result = File.createTempFile("bdmAnnotTest", ".zml");
      result.deleteOnExit();
    } catch (IOException e) {
      e.printStackTrace();
      Assert.assertTrue(false);//force test to fail
    }
    String[] args = {
        "-i", fToAnnotate.getAbsolutePath(), 
        "-o", result.getAbsolutePath(),
        "-type", "full",
        "-writer", "zml"};
    Assert.assertTrue(Annotate.doJob(args));
	}
}
