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
package bzh.plealog.dbmirror.lucenedico;

import bzh.plealog.dbmirror.indexer.ParserMonitor;

/**
 * This inteface defines a parsable dictionary file.
 * 
 * @author Patrick G. Durand
 */
public interface DicoParsable {
  /**
   * Sets to on or off the verbose mode.
   */
  public void setVerbose(boolean verbose);

  /**
   * Sets a DicoParserMonitor.
   */
  public void setParserMonitor(ParserMonitor pm);

  /**
   * Parse a database file and store some data within a Lucene index.
   */
  public void parse(String file, DicoStorageSystem ss)
      throws DicoParserException;

  /**
   * Returned the number of terms discovered by this parser within a dictionary
   * file.
   */
  public int getTerms();
}
