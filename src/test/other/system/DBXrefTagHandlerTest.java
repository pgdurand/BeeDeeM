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
package test.other.system;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import org.apache.log4j.BasicConfigurator;

import bzh.plealog.dbmirror.util.sequence.SeqIOUtils;
import bzh.plealog.dbmirror.util.xref.DBXrefInstancesManager;
import bzh.plealog.dbmirror.util.xref.DBXrefTagHandler;
import bzh.plealog.dbmirror.util.xref.DBXrefTagManager;

public class DBXrefTagHandlerTest {
  @SuppressWarnings("unused")
  private static DBXrefTagHandler getDRSwissProtHandler() {
    DBXrefTagHandler handler = new DBXrefTagHandler("DR", ";");
    // DR GO; GO:0048471; C:perinuclear region of cytoplasm; ISS:AgBase.
    handler.addSplitter("GO", ";", ";", "GO", ":");
    // DR InterPro; IPR000308; 14-3-3.
    handler.addSplitter("InterPro", ";", ";", "InterPro", "$");
    // DR Pfam; PF00244; 14-3-3; 1.
    handler.addSplitter("Pfam", ";", ";", "Pfam", "$");
    // DR BRENDA; 3.2.1.31; 244.
    handler.addSplitter("BRENDA", ";", ";", "EC", "$");
    return handler;
  }

  @SuppressWarnings("unused")
  private static DBXrefTagHandler getOXSwissProtHandler() {
    DBXrefTagHandler handler = new DBXrefTagHandler("OX", "=");
    // OX NCBI_TaxID=9913;
    handler.addSplitter("NCBI_TaxID", "=", ";", "tax", "$");
    return handler;
  }

  @SuppressWarnings("unused")
  private static DBXrefTagHandler getGenbankHandler() {
    DBXrefTagHandler handler = new DBXrefTagHandler("/db_xref=", ":");
    // /db_xref="taxon:6239"
    handler.addSplitter("taxon", ":", "\"", "tax", "$");
    // /db_xref="InterPro:IPR006020"
    // handler.addSplitter("InterPro", ":", "\"", "InterPro", "$");
    return handler;
  }

  private static void doTest(String fName, DBXrefTagManager manager) {
    System.out.println("Analyse: " + fName);
    DBXrefInstancesManager instances = new DBXrefInstancesManager();
    try {
      BufferedReader reader = new BufferedReader(new InputStreamReader(
          new FileInputStream(new File(fName)), "UTF-8"));
      String line, xref;
      while ((line = reader.readLine()) != null) {
        xref = manager.getDbXref(line);
        if (xref != null) {
          // System.out.println(xref);
          instances.addInstance(xref);
        }
      }
      reader.close();
    } catch (Exception e) {
      System.err.println("error: " + e);
    }
    String result = instances.toString();

    System.out.println("Result: " + result);
    System.out.println("  parsed xrefs: "
        + DBXrefInstancesManager.getDbXrefs(result));
    System.out.println("--");
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    DBXrefTagManager manager;
    BasicConfigurator.configure();
    manager = new DBXrefTagManager();
    // manager.initialise(new File("dbxref_def.config"));
    manager.initialise(SeqIOUtils.DEFAULT_CONFIG_XREF_RETRIEVE);
    // manager.addTagHandler(getDRSwissProtHandler());
    // manager.addTagHandler(getOXSwissProtHandler());
    // manager.addTagHandler(getGenbankHandler());
    DBXrefTagHandlerTest.doTest("./tests/junit/DBXrefManager/p12265.dat",
        manager);
    DBXrefTagHandlerTest.doTest("./tests/junit/DBXrefManager/z78540.dat",
        manager);
  }

}
