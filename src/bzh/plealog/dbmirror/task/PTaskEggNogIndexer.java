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
package bzh.plealog.dbmirror.task;

import bzh.plealog.dbmirror.indexer.DBParsable;
import bzh.plealog.dbmirror.indexer.eggnog.EggNogParser;
import bzh.plealog.dbmirror.util.descriptor.DatabankFormat;

/**
 * A task used to parse uniprot formatted sequences file converted from an
 * eggnog sequences fasta file
 * 
 * @author Ludovic Antin
 * 
 */
public class PTaskEggNogIndexer extends PIndexerTask {
  private EggNogParser parser = null;

  public PTaskEggNogIndexer(String srcFile) {
    super(srcFile);
  }

  @Override
  public String getName() {
    return "KLTaskEggNogIndexer";
  }

  @Override
  public String getUserFriendlyName() {
    return "indexing eggnog sequences file converted into uniprot format";
  }

  @Override
  public DBParsable getParser() {
    if (this.parser == null) {
      this.parser = new EggNogParser();
    }
    return this.parser;
  }

  @Override
  public void parsingDone() {
    // nothing to do
  }

  @Override
  public DatabankFormat getDatabankFormat() {
    return DatabankFormat.swissProt;
  }

}
