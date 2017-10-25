/* Copyright (C) 2007-2017 Patrick G. Durand
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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import bzh.plealog.dbmirror.fetcher.PFTPLoaderDescriptor;
import bzh.plealog.dbmirror.main.CmdLineInstallerOptions;

public class CmdLineOptionsTest {

  private static final String[] TEST_1_ANSWERS = {
    // -BEGIN
    // while a global descriptor MUST NOT use .dsc extension, we MUST
    // use it here to enable appropriate testing. Reason: see implementation
    // of PFTPLoaderDescriptor.load() method
    "db.list","SwissProt_human.dsc",
    // -END
    "db.main.task","download",
    "force.delete","false",
    "task.delay","1000",
    "ftp.delay","5000",
    "ftp.retry","3",
    "mail.smtp.host","",
    "mail.smtp.port","",
    "mail.smtp.sender.mail","",
    "mail.smtp.sender.pswd","",
    "mail.smtp.recipient.mail",""
    };

  private static final String[] TEST_2_ANSWERS = {
    "db.list","PDB",
    "db.main.task","null",
    "force.delete","null",
    "task.delay","null",
    "ftp.delay","null",
    "ftp.retry","null",
    "mail.smtp.host","null",
    "mail.smtp.port","null",
    "mail.smtp.sender.mail","null",
    "mail.smtp.sender.pswd","null",
    "mail.smtp.recipient.mail","null"
    };

  private static final String[] TEST_3_ANSWERS = {
    "db.list","PDB",
    "db.main.task","info",
    "force.delete","true",
    "task.delay","1000",
    "ftp.delay","5000",
    "ftp.retry","3",
    "mail.smtp.host","null",
    "mail.smtp.port","null",
    "mail.smtp.sender.mail","null",
    "mail.smtp.sender.pswd","null",
    "mail.smtp.recipient.mail","null"
    };
  
  private static final String[] TEST_4_ANSWERS = {
      // -BEGIN
      // while a global descriptor MUST NOT use .dsc extension, we MUST
      // use it here to enable appropriate testing. Reason: see implementation
      // of PFTPLoaderDescriptor.load() method
      "db.list","PDB.dsc",
      // -END
      "db.main.task","info",
      "force.delete","false",
      "task.delay","500",
      "ftp.delay","1000",
      "ftp.retry","5",
      "mail.smtp.host","",
      "mail.smtp.port","",
      "mail.smtp.sender.mail","",
      "mail.smtp.sender.pswd","",
      "mail.smtp.recipient.mail",""
      };

  private static final String[] TEST_5_ANSWERS = {
      // -BEGIN
      // while a global descriptor MUST NOT use .dsc extension, we MUST
      // use it here to enable appropriate testing. Reason: see implementation
      // of PFTPLoaderDescriptor.load() method
      "db.list","PDB.dsc",
      // -END
      "db.main.task","info",
      "force.delete","false",
      "task.delay","null",
      "ftp.delay","null",
      "ftp.retry","null",
      "mail.smtp.host","",
      "mail.smtp.port","",
      "mail.smtp.sender.mail","",
      "mail.smtp.sender.pswd","",
      "mail.smtp.recipient.mail",""
      };

  private static final String[] TEST_6_ANSWERS = {
      // -BEGIN
      // while a global descriptor MUST NOT use .dsc extension, we MUST
      // use it here to enable appropriate testing. Reason: see implementation
      // of PFTPLoaderDescriptor.load() method
      "db.list","SwissProt_human.dsc",
      // -END
      "db.main.task","download",
      "force.delete","false",
      "task.delay","1000",
      "ftp.delay","5000",
      "ftp.retry","3",
      "mail.smtp.host","ssl0.gmail.com",
      "mail.smtp.port","535",
      "mail.smtp.sender.mail","patrick@plealog.com",
      "mail.smtp.sender.pswd","xxxx",
      "mail.smtp.recipient.mail","patrick.durand@inria.fr"
      };
  
  private static final String[] TEST_7_ANSWERS = {
      // -BEGIN
      // while a global descriptor MUST NOT use .dsc extension, we MUST
      // use it here to enable appropriate testing. Reason: see implementation
      // of PFTPLoaderDescriptor.load() method
      "db.list","SwissProt_human.dsc",
      // -END
      "db.main.task","download",
      "force.delete","true",
      "task.delay","1000",
      "ftp.delay","5000",
      "ftp.retry","3",
      "mail.smtp.host","",
      "mail.smtp.port","",
      "mail.smtp.sender.mail","",
      "mail.smtp.sender.pswd","",
      "mail.smtp.recipient.mail",""
      };

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

  private void assertValues(PFTPLoaderDescriptor fDesc, String[] controls){
    List<String> argList = Arrays.asList(controls);
    Iterator<String> argIter = argList.iterator();
    String keyName, value;
    while(argIter.hasNext()){
      keyName = argIter.next();
      value = argIter.next();
      if (value.equals("null")){
        value=fDesc.getProperty(keyName);
        assertTrue(value==null||value.isEmpty());
      }
      else{
        assertEquals(fDesc.getProperty(keyName), value);
      }
    }
  }
  
  private PFTPLoaderDescriptor getDescriptor(String globalDesc, String gdPath){
    PFTPLoaderDescriptor fDesc = new PFTPLoaderDescriptor(globalDesc);
    try {
      fDesc.load(new FileInputStream(gdPath), false);
    } catch (IOException e) {
      e.printStackTrace();
      //force test failure
      assertTrue(false);
    }
    return fDesc;
  }
  
  @Test
  public void testGlobalDescriptor() {
    String globalDesc = "swiss";
    String gdPath = UtilsTest.getTestFilePath("databank", "cmdLineOptions", globalDesc+".gd");

    PFTPLoaderDescriptor fDesc = getDescriptor(globalDesc, gdPath);
    assertValues(fDesc, TEST_1_ANSWERS);
  }
  
  @Test
  public void testOptions1(){
    // test a basic cmdline option
    String[]    CMDLINE = {"-desc","PDB"};
    CommandLine cmd = CmdLineInstallerOptions.handleArguments(CMDLINE);

    assertNotNull(cmd);
    PFTPLoaderDescriptor fDesc = CmdLineInstallerOptions.getDescriptorFromOptions(cmd);
    assertValues(fDesc, TEST_2_ANSWERS);
  }

  @Test
  public void testOptions2(){
    //test all but email options
    String[]    CMDLINE = {"-desc","PDB","-task","info","-force", "true","-td","1000","-fd","5000","-fr","3"};
    CommandLine cmd = CmdLineInstallerOptions.handleArguments(CMDLINE);

    assertNotNull(cmd);
    PFTPLoaderDescriptor fDesc = CmdLineInstallerOptions.getDescriptorFromOptions(cmd);
    assertValues(fDesc, TEST_3_ANSWERS);
  }

  @Test
  public void testOptions2bis(){
    //same as previous but with long option names
    String[]    CMDLINE = {"-desc","PDB","-task","info","-force", "true","--task-delay","1000","--ftp-delay","5000","--ftp-retry","3"};
    CommandLine cmd = CmdLineInstallerOptions.handleArguments(CMDLINE);

    assertNotNull(cmd);
    PFTPLoaderDescriptor fDesc = CmdLineInstallerOptions.getDescriptorFromOptions(cmd);
    assertValues(fDesc, TEST_3_ANSWERS);
  }

  @Test
  public void testOptions3(){
    // same as test2 but we pass Genbank global descriptor as argument
    String      GB = "Genbank";
    String[]    CMDLINE = {"-desc","PDB","-task","info","-force", "true","-td","1000","-fd","5000","-fr","3", GB};
    CommandLine cmd = CmdLineInstallerOptions.handleArguments(CMDLINE);

    assertNotNull(cmd);
    PFTPLoaderDescriptor fDesc = CmdLineInstallerOptions.getDescriptorFromOptions(cmd);
    assertValues(fDesc, TEST_3_ANSWERS);
    assertEquals(CmdLineInstallerOptions.getDescriptorName(cmd), GB);
  }

  @Test
  public void testOptions4(){
    // test to override values of a given global descriptor (last argument)
    // by new ones passed in as cmdline options.
    // This is THE test that simulates new behavior of CmdLineInstaller Tool
    // expected to solve issue: https://github.com/pgdurand/BeeDeeM/issues/4
    String[]             CMDLINE = {"-desc","PDB","-task","info","-force", "false","-td","500","-fd","1000","-fr","5", "swiss"};
    String               globalDesc;
    
    CommandLine          cmd = CmdLineInstallerOptions.handleArguments(CMDLINE);
    assertNotNull(cmd);
    
    globalDesc = CmdLineInstallerOptions.getDescriptorName(cmd);
    assertNotNull(globalDesc);
    
    String               gdPath = UtilsTest.getTestFilePath("databank", "cmdLineOptions", globalDesc+".gd");
    PFTPLoaderDescriptor fDesc = getDescriptor(globalDesc, gdPath);

    
    //cmdline arguments will override descriptor ones
    PFTPLoaderDescriptor fDescCmd = CmdLineInstallerOptions.getDescriptorFromOptions(cmd);

    
    
    fDesc.update(fDescCmd);
    assertValues(fDesc, TEST_4_ANSWERS);
  }

  @Test
  public void testOptions5(){
    // same as test4 but we reset some values
    String[]             CMDLINE = {"-desc","PDB","-task","info","-force", "false","-td","-","-fd","-","-fr","-"};
    String               globalDesc = "swiss";
    String               gdPath = UtilsTest.getTestFilePath("databank", "cmdLineOptions", globalDesc+".gd");
    PFTPLoaderDescriptor fDesc = getDescriptor(globalDesc, gdPath);
    CommandLine          cmd = CmdLineInstallerOptions.handleArguments(CMDLINE);
    
    assertNotNull(cmd);
    //cmdline arguments will override descriptor ones
    PFTPLoaderDescriptor fDescCmd = CmdLineInstallerOptions.getDescriptorFromOptions(cmd);
    fDesc.update(fDescCmd);
    assertValues(fDesc, TEST_5_ANSWERS);
  }

  @Test
  public void testOptions6(){
    // default swiss global descriptor does not provide email options: we set
    // that from the cmdline options
    String[]             CMDLINE = {"-host","ssl0.gmail.com","-port","535","-sender","patrick@plealog.com",
                                    "-pswd", "xxxx", "-recipient","patrick.durand@inria.fr"};
    String               globalDesc = "swiss";
    String               gdPath = UtilsTest.getTestFilePath("databank", "cmdLineOptions", globalDesc+".gd");
    PFTPLoaderDescriptor fDesc = getDescriptor(globalDesc, gdPath);
    CommandLine          cmd = CmdLineInstallerOptions.handleArguments(CMDLINE);
    
    assertNotNull(cmd);
    //cmdline arguments will override descriptor ones
    PFTPLoaderDescriptor fDescCmd = CmdLineInstallerOptions.getDescriptorFromOptions(cmd);
    fDesc.update(fDescCmd);
    assertValues(fDesc, TEST_6_ANSWERS);
  }

  @Test
  public void testOptions7(){
    // reverse of test6
    String[]             CMDLINE = {"-host","-","-port","-","-sender","-",
                                    "-pswd", "-", "-recipient","-", "swiss2"};
    String               globalDesc;
    
    CommandLine          cmd = CmdLineInstallerOptions.handleArguments(CMDLINE);
    assertNotNull(cmd);

    globalDesc = CmdLineInstallerOptions.getDescriptorName(cmd);
    assertNotNull(globalDesc);
    
    String               gdPath = UtilsTest.getTestFilePath("databank", "cmdLineOptions", globalDesc+".gd");
    PFTPLoaderDescriptor fDesc = getDescriptor(globalDesc, gdPath);

    //cmdline arguments will override descriptor ones
    PFTPLoaderDescriptor fDescCmd = CmdLineInstallerOptions.getDescriptorFromOptions(cmd);
    fDesc.update(fDescCmd);
    assertValues(fDesc, TEST_7_ANSWERS);
  }

}
