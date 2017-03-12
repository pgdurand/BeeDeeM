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
package bzh.plealog.dbmirror.util.sequence;

import org.apache.commons.lang.StringUtils;

import bzh.plealog.dbmirror.task.PTask;

public class SequenceValidatorTaxon extends SequenceValidatorAbstract {

  private TaxonMatcherHelper _taxonMatcher;
  private String             _taxInclude;
  private String             _taxExclude;
  private boolean            analyseInProgress = false;
  private boolean[]          values            = new boolean[3];

  /**
   * 
   * @param taxons
   */
  public SequenceValidatorTaxon(String taxInclude, String taxExclude) {
    this._taxExclude = taxExclude;
    this._taxInclude = taxInclude;
    this._taxonMatcher = new TaxonMatcherHelper();
    this._taxonMatcher.setTaxonomyFilter(taxInclude, taxExclude);
    this._taxonMatcher.initTaxonMatcher();
  }

  @Override
  public boolean startEntry() {
    analyseInProgress = true;
    return true;
  }

  @Override
  public boolean stopEntry() {
    return true;
  }

  @Override
  public boolean startSequence() {
    analyseInProgress = false;
    return true;
  }

  @Override
  public boolean analyseLine(StringBuilder line) {
    if (analyseInProgress) {
      this._taxonMatcher.isSeqTaxonvalid(line.toString(), values);
      if (values[0]) {
        // we found taxon : the analyse is stopped here
        analyseInProgress = false;
        return values[1];
      }
    }
    return true;
  }

  @Override
  public boolean isActive() {
    if (!TaxonMatcherHelper.isNCBITaxonomyInstalled()) {
      return false;
    }
    return ((StringUtils.isNotBlank(this._taxExclude)) || (StringUtils
        .isNotBlank(this._taxInclude)));
  }

  @Override
  public String toParametersForUnitTask() {
    StringBuffer result = new StringBuffer();
    if (StringUtils.isNotBlank(this._taxInclude)) {
      result.append(PTask.TAX_INCLUDE);
      result.append("=");
      result.append(this._taxInclude);
    }
    if (StringUtils.isNotBlank(this._taxExclude)) {
      if (result.length() > 0) {
        result.append(";");
      }
      result.append(PTask.TAX_EXCLUDE);
      result.append("=");
      result.append(this._taxExclude);
    }
    return result.toString();
  }

}
