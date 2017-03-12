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
package bzh.plealog.dbmirror.util.xref;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;

import bzh.plealog.dbmirror.lucenedico.Dicos;

/**
 * This class is used to store all db xrefs retrieve by the DBXref Retrieval
 * system.
 * 
 * @author Patrick G. Durand
 */
public class DBXrefInstancesManager {
  // format for toString() : "[[EC:3.2.1.31;tax:10090;GO:0005783,0005764]]"
  public static final String                HIT_DEF_LINE_START                  = "[[";
  public static final String                HIT_DEF_LINE_STOP                   = "]]";
  public static final String                HIT_DEF_LINE_XREF_CLASS_SEPARATOR   = ";";
  public static final String                HIT_DEF_LINE_XREF_NAME_ID_SEPARATOR = ":";
  public static final String                HIT_DEF_LINE_XREF_ID_SEPARATOR      = ",";

  public static final String                DEFAULT_FEATURE_TYPE_XREF           = "source";
  public static final String                DEFAULT_FEATURE_TYPE_ORG            = "organism";
  public static final String                DEFAULT_QUAL_TYPE_XREF              = "db_xref";

  // the header template for a fasta file which contains a single
  // classifications data
  public static final MessageFormat         SINGLE_CLASSIF_HEADER_TEMPLATE      = new MessageFormat(
                                                                                    " "
                                                                                        + DBXrefInstancesManager.HIT_DEF_LINE_START
                                                                                        + "{0}"
                                                                                        + DBXrefInstancesManager.HIT_DEF_LINE_XREF_NAME_ID_SEPARATOR
                                                                                        + "{1}"
                                                                                        + DBXrefInstancesManager.HIT_DEF_LINE_STOP);

  private Hashtable<String, DBXrefInstance> instances;

  public DBXrefInstancesManager() {
    instances = new Hashtable<String, DBXrefInstance>();
  }

  /**
   * Adds a db identifier. ID format must be A:B (A = db code, B = id ; Example:
   * GO:0002564).
   */
  public void addInstance(String id) {
    if (id == null)
      return;
    int idx = id.indexOf(':');
    if (idx == -1)
      return;
    String code;
    code = id.substring(0, idx);
    DBXrefInstance instance = instances.get(code);
    if (instance == null) {
      instance = new DBXrefInstance(code);
      instances.put(code, instance);
    }
    instance.addId(id.substring(idx + 1));
  }

  public String toString() {
    StringBuffer buf;
    Enumeration<DBXrefInstance> enumI;

    if (instances.isEmpty())
      return "";
    buf = new StringBuffer(HIT_DEF_LINE_START);
    enumI = instances.elements();
    while (enumI.hasMoreElements()) {
      buf.append(enumI.nextElement().toString());
      if (enumI.hasMoreElements()) {
        buf.append(HIT_DEF_LINE_XREF_CLASS_SEPARATOR);
      }
    }

    buf.append(HIT_DEF_LINE_STOP);
    return buf.toString();
  }

  /**
   * Retrieve database cross-references identifiers out of a hit definition
   * line.
   * 
   * @param hitDefLine
   *          accepted format to be parsed is
   *          [[EC:3.2.1.31;tax:10090;GO:0005783,0005764]]
   * 
   * @return a list of string each of them being a db cross-reference id.
   * */
  public static List<String> getDbXrefs(String hitDefLine) {
    ArrayList<String> dbxrefs;
    StringTokenizer tokenizerClass, tokenizerXref;
    String xrefClass, xrefName, xrefId;
    int from, to;

    dbxrefs = new ArrayList<String>();
    from = hitDefLine.lastIndexOf(HIT_DEF_LINE_START);
    to = hitDefLine.lastIndexOf(HIT_DEF_LINE_STOP);
    if (from == -1 || to == -1 || to <= from) {
      return dbxrefs;
    }
    tokenizerClass = new StringTokenizer(hitDefLine.substring(from
        + HIT_DEF_LINE_START.length(), to), HIT_DEF_LINE_XREF_CLASS_SEPARATOR);
    while (tokenizerClass.hasMoreElements()) {
      xrefClass = tokenizerClass.nextToken();
      from = xrefClass.indexOf(HIT_DEF_LINE_XREF_NAME_ID_SEPARATOR);
      if (from == -1)
        continue;
      xrefName = xrefClass.substring(0, from);
      tokenizerXref = new StringTokenizer(xrefClass.substring(from + 1),
          HIT_DEF_LINE_XREF_ID_SEPARATOR);
      while (tokenizerXref.hasMoreElements()) {
        xrefId = tokenizerXref.nextToken();
        if (xrefName.equals(Dicos.GENE_ONTOLOGY.xrefId))
          dbxrefs.add(xrefName + "; " + Dicos.GENE_ONTOLOGY.xrefId + ":"
              + xrefId);
        else
          dbxrefs.add(xrefName + "; " + xrefId);
      }
    }
    return dbxrefs;
  }

}
