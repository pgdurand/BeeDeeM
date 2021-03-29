/* Copyright (C) 2006-2017 Patrick G. Durand
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

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Unit tests of the Databank Manager.
 * 
 * @author Patrick G. Durand
 */
@RunWith(Suite.class)
@SuiteClasses({
	/*
	 * DO NOT add AsperaCmdTest to this test suite: will fail on TravisCI
	 * since Aspera software is not installed.
	 */
  CmdLineOptionsTest.class,
  CddParserTest.class, 
  DBXrefInstancesManagerTest.class, 
  DefaultLoaderMonitorTest.class,
  EggNogIndexerTest.class,
  DBMSUniqueSeqIdDetectorTest.class,
  PLocalLoaderTest.class,
  LuceneStorageSystemTest.class,
  NcbiTaxonomyIndexerTest.class,
  RunningMirrorPanelTest.class,
  SeqIOUtilsTest.class,
  SequenceFileManagerTest.class,
  UtilsTest.class,
  PAnnotateBlastResultTest.class,
  CmdLineQueryTest.class,
  PFTPLoaderTest.class
  })
public class AllTests {

}
