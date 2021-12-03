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

/**
 * This class is used to analyze sequence database entry lines of data in order to identify
 * a db xref.
 * 
 * @author Patrick G. Durand
 */
public class DBXrefSplitter {
	/*Considering the following SwissProt entry:
	  DR   GO; GO:0048471; C:perinuclear region of cytoplasm; ISS:AgBase.
	  here is the meaning of the following fields :
       key   = GO
	   begin = ;
	   end   = ;
	   code  = GO
       codeSplitter = :
	
	  In addition, the 'key' parameter from the Constructor would be "DR" in this
	  example. Considering those values, the code will extract the string "GO:0048471"
	  from the DR line. Then, CodeSplitter is used to additionally split the identifier
	  to retrieve "0048471". Finally, the value associated to code is added as prefix to
	  the dbxref. As a result, we can retrieve "GO:0048471".
	  
	  Considering this example:
	  DR   BRENDA; 3.2.1.31; 244.
      with
       key   = BRENDA
	   begin = ;
	   end   = ;
       code  = EC
       codeSplitter = $ ($ means : do not consider)
       then, the code will return "EC:3.2.1.31"
       
       For more examples, see DBXrefTagHandlerTest. 
	*/

  private String begin;
  private String end;
  private String code;
  private String codeSplitter;
  // for internal use
  private String keyb;
  private int    csLength;

  // see above doc for more information.
  public DBXrefSplitter(String key, String begin, String end, String code,
      String codeSplitter) {
    super();
    this.begin = begin;
    this.end = end;
    this.code = code + ":";
    if (!codeSplitter.equals("$")) {
      this.codeSplitter = codeSplitter;
      csLength = codeSplitter.length();
    }
    this.keyb = key + this.begin;
  }

  public String getXRef(String dataLine) {
    String str;
    int i, idx, idx2;

    if ((idx = dataLine.indexOf(keyb)) != -1) {
      str = dataLine.substring(idx + keyb.length());
      for (i=0;i<end.length();i++) {
        idx = str.indexOf(end.charAt(i));
        if (idx!=-1) {
          break;
        }
      }
      if (idx == -1) {
        return null;
      } else {
        if (codeSplitter != null) {
          idx2 = str.indexOf(codeSplitter);
          if (idx2 == -1)
            return null;
          idx2 += csLength;
        } else {
          idx2 = 0;
        }
        return code + str.substring(idx2, idx).trim();
      }
    }
    return null;
  }
}
