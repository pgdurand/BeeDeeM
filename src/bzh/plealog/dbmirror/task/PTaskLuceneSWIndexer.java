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

import bzh.plealog.dbmirror.indexer.DBParsable;
import bzh.plealog.dbmirror.indexer.SwissProtParser;
import bzh.plealog.dbmirror.util.descriptor.DatabankFormat;

/**
 * A task capable of indexing a database file coming from Uniprot or SwissProt.
 * 
 * @author Patrick G. Durand
 */
public class PTaskLuceneSWIndexer extends PIndexerTask {

  private String          _taxInclude;
  private String          _taxExclude;
  private SwissProtParser _parser;

  public PTaskLuceneSWIndexer(String srcFile) {
    super(srcFile);
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getName() {
    return "LuceneSWIndexer";
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getUserFriendlyName() {
    return "indexing EMBL/UniProt/Swissprot file";
  }

  @Override
  public void setParameters(String params) {
    super.setParameters(params);
    String value = _args.get(PTask.TAX_INCLUDE);
    if (value != null)
      _taxInclude = value;
    value = _args.get(PTask.TAX_EXCLUDE);
    if (value != null)
      _taxExclude = value;
  }

  @Override
  public DBParsable getParser() {
    if (this._parser == null) {
      _parser = new SwissProtParser();
      _parser.setTaxonomyFilter(_taxInclude, _taxExclude);
    }
    return this._parser;
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
