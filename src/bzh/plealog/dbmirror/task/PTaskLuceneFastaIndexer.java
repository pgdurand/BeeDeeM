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
package bzh.plealog.dbmirror.task;

import java.io.File;

import bzh.plealog.dbmirror.indexer.DBParsable;
import bzh.plealog.dbmirror.indexer.FastaParser;
import bzh.plealog.dbmirror.util.descriptor.DatabankFormat;

/**
 * A task capable of indexing a Fasta database file.
 * 
 * @author Patrick G. Durand
 */
public class PTaskLuceneFastaIndexer extends PIndexerTask {

  private FastaParser _parser;

  public PTaskLuceneFastaIndexer(String srcFile) {
    super(srcFile);
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getName() {
    return "LuceneFastaIndexer";
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getUserFriendlyName() {
    return "indexing Fasta file";
  }

  public void forceCheckRedundantIds() {
    _checkNR = true;
  }

  @Override
  public DBParsable getParser() {
    if (this._parser == null) {
      _parser = new FastaParser();
      // because of a SILVA file that contains an empty sequence
      // ...
      // GCGGGCU
      // >HQ399845.1.3026
      // Bacteria;Proteobacteria;Gammaproteobacteria;Aeromonadales;Succinivibrionaceae;uncultured;uncultured
      // rumen bacterium
      //
      // >HQ400490.1.919
      // Archaea;Euryarchaeota;Halobacteria;Halobacteriales;Halobacteriaceae;Candidatus
      // Halobonum;uncultured archaeon
      // UUCCGGUUGAUCCUGCCGGAGGCCAUUGCUAUCGGGGUCCGAUUUAGCCAUGCUAGUCGCACGGGUUCAGACCCGUGGCA
      // ...
      _parser.setWarnError(true);
    }
    return this._parser;
  }

  @Override
  public void parsingDone() {
    if (this.getParser().getEntries() > 0) {
      PTaskFormatDB.addNbSequences(new File(this.getSrc()).getParent(), this
          .getParser().getEntries());
    }
  }

  @Override
  public DatabankFormat getDatabankFormat() {
    return DatabankFormat.fasta;
  }

}
