/* Copyright (C) 2019 Patrick G. Durand
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

import java.io.File;
import java.text.MessageFormat;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.aspera.AsperaCmd;
import bzh.plealog.dbmirror.util.aspera.AsperaUtils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.conf.DBMSConfigurator;
import bzh.plealog.dbmirror.util.runner.CommandArgument;

public class AsperaCmdTest {

  private static String bin_path;
  private static String key_path;
  private static String remote_url_ncbi = "anonftp@ftp.ncbi.nlm.nih.gov";
  private static String remote_url_ebi = "era-fasp@fasp.sra.ebi.ac.uk";
  private static String target_dir = Utils.terminatePath(System.getProperty("java.io.tmpdir"));
  private static String ERR_1=" is not defined in dbms config file";
  private static String MSG_1="File is here : {0}{1}";
  
  //convenient way to bypass download tests (use only during debugging)
  private static boolean doDownload = true;
  private static boolean doDownloadNCBI = true;
  private static boolean doDownloadEBI = true;
  
  private static final Log           LOGGER                   = LogFactory
			.getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
					+ ".AsperaCmdTest");

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    UtilsTest.configureApp();
    UtilsTest.cleanInstalledDatabanks();
    bin_path = AsperaUtils.getASCPCmdPath();
    if (bin_path==null) {
    	throw new RuntimeException(DBMSConfigurator.ASPERA_BIN+ERR_1);
    }
    key_path = AsperaUtils.getASCPKeyPath();
    if (key_path==null) {
    	throw new RuntimeException(DBMSConfigurator.ASPERA_KEY+ERR_1);
    }
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

  private void download(AsperaCmd cmd, String f){
    if (!doDownload)
    	return;
	assertTrue(cmd.getRemoteFile(f));
    String name = new File(f).getName();
    LOGGER.info(MessageFormat.format(MSG_1, target_dir ,name));
  }
  private void downloadNCBI(String f){
	  if (!doDownloadNCBI)
	    	return;
	  //we use default Aspera connector (i.e. default arguments setup to access NCBI aspera server)
	  AsperaCmd cmd = new AsperaCmd(bin_path, key_path, remote_url_ncbi, target_dir);
	  download(cmd, f);
  }
  private void downloadEBI(String f){
	  if (!doDownloadEBI)
	    	return;
	  //we use default Aspera connector (i;e. default arguments)
	  AsperaCmd cmd = new AsperaCmd(bin_path, key_path, remote_url_ebi, target_dir);
	  //and add specific port to access EBI aspera server
	  cmd.addArgument("-P", new CommandArgument("33001", false));
	  download(cmd, f);
  }
  
  @Test
  public void testCmdLineConvertor() {
	  String cmdline = "-k 1 -T -l 640M -P 33001 -i /foo/bar/asperaweb_id_dsa.openssh -Q --overwrite=always";
	  String[] keyControl = {"-k", "-Q", "-i", "-T", "-l", "-P", "--overwrite=always"};
	  String[] valueControl = {"1", "", "/foo/bar/asperaweb_id_dsa.openssh", "", "640M", "33001", ""};
	  Map<String, CommandArgument> args = AsperaUtils.handleArguments(cmdline);
	  LOGGER.info("Converted cmdline is: ");
	  LOGGER.info(args.toString());
	  assertTrue(args.size()==keyControl.length);
	  for(int i=0; i<keyControl.length;i++) {
		  LOGGER.info("  argument: "+keyControl[i]);
		  CommandArgument ca = args.get(keyControl[i]); 
		  assertNotNull(ca);
		  LOGGER.info("     value: '"+ca.getArgument()+"' vs. '"+valueControl[i]+"'");
		  assertEquals(ca.getArgument(), valueControl[i]);
	  }
	  
  }
  
  @Test
  public void testFileNCBI1() {
    downloadNCBI("/refseq/H_sapiens/mRNA_Prot/human.1.rna.gbff.gz");
  }

  @Test
  public void testFileNCBI2() {
    downloadNCBI("/refseq/H_sapiens/mRNA_Prot/human.1.protein.faa.gz");
  }
  @Test
  public void testFileEBI1() {
    downloadEBI("/vol1/fastq/ERR164/ERR164409/ERR164409.fastq.gz");
  }
  @Test
  public void testFileEBI2() {
	  if (!doDownloadEBI)
	    	return;
	  Map<String, CommandArgument> args = AsperaUtils.handleArguments(
			  "-k 1 -T -l 640M -P 33001 --overwrite=always");
	  AsperaCmd cmd = new AsperaCmd(bin_path, key_path, remote_url_ebi, target_dir, args);
	  download(cmd, "/vol1/fastq/ERR164/ERR164409/ERR164409.fastq.gz");
  }
}
