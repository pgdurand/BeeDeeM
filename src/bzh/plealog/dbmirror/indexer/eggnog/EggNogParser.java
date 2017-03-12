/* Copyright (C) 2007-2017 Ludovic Antin
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
package bzh.plealog.dbmirror.indexer.eggnog;

import java.io.File;

import bzh.plealog.dbmirror.indexer.DBParserException;
import bzh.plealog.dbmirror.indexer.StorageSystem;
import bzh.plealog.dbmirror.indexer.SwissProtParser;
import bzh.plealog.dbmirror.lucenedico.DicoParserException;

/**
 * Parser to analyse and install a EggNog v3 or v4 databank.
 * 
 * The complete EggNog databank installation will result in the installation of
 * a dictionary and 1 complete or 3 databanks (archea, bacteria, eukaryote)
 * 
 * To install an EggNog databank (complete or not), the original fasta file will
 * be converted in the Uniprot format in order to add a feature table for each
 * sequence
 * 
 * @author Ludovic Antin
 * 
 */
public class EggNogParser extends SwissProtParser {

  public EggNogParser() {
    super();
  }

  @Override
  public void parse(String file, StorageSystem ss) throws DBParserException {

    // check the sequences file
    if (!new File(file).exists()) {
      throw new DicoParserException("Unable to read sequences file '" + file
          + "'");
    }

    super.parse(file, ss);
  }

}
