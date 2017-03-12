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

import java.util.Hashtable;

/**
 * This class is used to handle several DBXrefSplitter that could be associated to 
 * a same db tag. As an example, when considering the DR tag of Uniprot data files, we
 * can have many DBXrefSplitter to retrieve GO, InterPro, etc dbxrefs.
 * 
 * @author Patrick G. Durand
 */
public class DBXrefTagHandler {
	private String                            tag;
	private String                            begin;
	private Hashtable<String, DBXrefSplitter> splitters;

	/*Considering the following example:
	 * DR   GO; GO:0048471; C:perinuclear region of cytoplasm; ISS:AgBase.
	 * 
	 * tag   = DR
	 * begin = ; (this is the same 'begin' as for DBXrefSplitter constructor
	 */
  public DBXrefTagHandler(String tag, String begin) {
    super();
    this.tag = tag;
    this.begin = begin;
    splitters = new Hashtable<String, DBXrefSplitter>();
  }

  public String getTag() {
    return tag;
  }

  public void addSplitter(String key, String begin, String end, String code,
      String codeSplitter) {
    splitters.put(key, new DBXrefSplitter(key, begin, end, code, codeSplitter));
  }

  public String getDbXref(String dataLine) {
    DBXrefSplitter splitter;
    String str, key;
    int idx, idx2, size;

    // remove ending spaces
    str = dataLine.trim();
    // contains 'tag' ?
    if (str.startsWith(tag) == false)
      return null;
    // skip tag as well as non-letter chars to locate the beginning of key
    idx = tag.length();
    size = str.length();
    // may happen in wrongly annotated files: nothing after a tag !
    if (idx >= size)
      return null;
    while (!Character.isLetter(str.charAt(idx))) {
      idx++;
      if (idx == size)
        return null;
    }
    // locate key
    str = str.substring(idx);
    idx2 = str.indexOf(begin);
    if (idx2 == -1)
      return null;
    // get a corresponding splitter
    key = str.substring(0, idx2);
    splitter = splitters.get(key);
    if (splitter == null)
      return null;
    return splitter.getXRef(str);
  }
}
