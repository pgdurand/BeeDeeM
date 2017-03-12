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
package bzh.plealog.dbmirror.reader;

import bzh.plealog.bioinfo.data.feature.IFeatureTable;

/**
 * This class is an implementation of a feature table.
 * 
 * @author Patrick G. Durand
 */
public class PFeatureTable extends IFeatureTable {

  private static final long serialVersionUID = -5156803178592434195L;

  public PFeatureTable() {
    super();
  }

  /**
   * Clone this FeatureTable.
   */
  public Object clone() {
    PFeatureTable ft = new PFeatureTable();
    ft.copy(this);
    return ft;
  }

  /**
   * Adds a new Feature.
   */
  public PFeature addFeature(String key, int from, int to, int strand) {
    PFeature fi;

    fi = new PFeature(key, from, to, strand);
    addFeature(fi);

    return fi;
  }
}
