/* Copyright (C) 2007-2017 Patrick G. Durand - Ludovic Antin
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
package bzh.plealog.dbmirror.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.apache.commons.lang.mutable.MutableInt;

/**
 * Data formatters.
 * 
 * @author Ludovic Antin
 * @author Patrick G. Durand
 */
public class Formatters {
  public static NumberFormat DECIMAL_FORMATTER = NumberFormat
                                                   .getNumberInstance();
  public static NumberFormat PCT_FORMATTER     = NumberFormat
                                                   .getPercentInstance();

  static {
    PCT_FORMATTER.setMaximumFractionDigits(2);
    ((DecimalFormat) PCT_FORMATTER).setMultiplier(1);
    DECIMAL_FORMATTER.setMaximumFractionDigits(2);
  };

  /**
   * Format the text parameter in nbColumns columns. Each line is nbColumns-1
   * char + '\n'.
   * 
   * @param nbColumns
   *          the number of columns
   * @param text
   * @param prefix
   *          the prefix for each line
   * @param nbLetters
   *          is not a real parameter because it will be filled during the
   *          process to get the real number of characters in the formatted text
   *          (all chars but new line ones)
   * 
   * @return the formatted text
   */
  public static String formatInNbColumns(int nbColumns, String text,
      String prefix, MutableInt nbLetters) {
    if (prefix == null) {
      prefix = "";
    }

    // remove end of line chars
    String textToFormat = Formatters.replaceAll(text, "\n", "");
    textToFormat = Formatters.replaceAll(textToFormat, "\r", "");

    // init data
    StringBuilder result = new StringBuilder(prefix);
    int nbCharsInCurrentLine = prefix.length();

    // run through all chars
    int size = textToFormat.length();
    for (int i = 0; i < size; i++) {
      result.append(textToFormat.charAt(i));
      nbLetters.add(1);
      nbCharsInCurrentLine++;
      // in the following line, I use nbColumns-1 since I want to add a carriage
      // return every nbColumns letters and I'm using a zero-based counter.
      if ((nbCharsInCurrentLine != 0)
          && ((nbCharsInCurrentLine + 1) % nbColumns == 0) && (i != size - 1)) {
        result.append("\n");
        result.append(prefix);
        nbCharsInCurrentLine = prefix.length();
      }
    }

    return result.toString();
  }

  /**
   * replace the "sFind" string by the "sReplace" string in the "str" string.
   * This method is more efficient than the String.replace one
   * 
   * @param str
   * @param sFind
   * @param sReplace
   * 
   * @return
   */
  public static String replaceAll(String str, String sFind, String sReplace) {
    boolean bFound;
    int iPos = -1;

    String newStr = "";
    do {
      iPos = str.indexOf(sFind, ++iPos);
      if (iPos > -1) {
        newStr = newStr + str.substring(0, iPos) + sReplace
            + str.substring(iPos + sFind.length(), str.length());
        str = newStr;
        newStr = "";
        iPos += (sReplace.length() - 1);
        bFound = true;
      } else {
        bFound = false;
      }
    } while (bFound);
    return (str);
  }
}
