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

import java.util.HashSet;

import bzh.plealog.dbmirror.reader.DBUtils;

/**
 * This class is used to store all db identifier belonging to a same db code.
 * 
 * @author Patrick G. Durand
 */
public class DBXrefInstance {
  private HashSet<String> ids;
  private StringBuffer    buf;
  private boolean         cutId = false;

  public DBXrefInstance(String key) {
    ids = new HashSet<String>();
    buf = new StringBuffer();
    this.cutId = key.equalsIgnoreCase(DBUtils.TAXON_KEY);
    buf.append(key);
    buf.append(DBXrefInstancesManager.HIT_DEF_LINE_XREF_NAME_ID_SEPARATOR);
  }

  public void addId(String id) {
    if (cutId) {
      // quickly done for taxon id on Trembl
      // example : NCBI_TaxID=570416 {ECO:0000313|EMBL:AHZ52867.1,
      // ECO:0000313|Proteomes:UP000025242};
      int cutIdx = -1;
      for (int i = 0; i < id.length(); i++) {
        if (!Character.isDigit(id.charAt(i))) {
          cutIdx = i;
          break;
        }
      }
      if (cutIdx > 0) {
        id = id.substring(0, cutIdx);
      }
    }

    if (ids.contains(id))
      return;
    if (!ids.isEmpty()) {
      buf.append(DBXrefInstancesManager.HIT_DEF_LINE_XREF_ID_SEPARATOR);
    }
    ids.add(id);
    buf.append(id);
  }

  public String toString() {
    return buf.toString();
  }
}
