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

import bzh.plealog.bioinfo.data.feature.IFeature;

/**
 * This class is an implementation of a feature.
 * 
 * @author Patrick G. Durand
 */
public class PFeature extends IFeature {
  private static final long serialVersionUID = -2071928390737751487L;

  public PFeature() {
    super();
  }

  public PFeature(String key, int from, int to, int strand) {
    super(key, from, to, strand);
  }

  /**
   * Clone the feature.
   */
  public IFeature clone() {
    PFeature feat = new PFeature();
    feat.copy(this);
    return feat;
  }

  /**
   * Figures out if a feature spans beyond left limit.
   */
  public boolean spanLeft() {
    return getFrom() < 0;
  }

  /**
   * Figures out if a feature spans beyond rigth limit.
   */
  public boolean spanRight() {
    return getTo() < 0;
  }

  public String getLoc() throws Exception {
    StringBuffer buf;
    buf = new StringBuffer();
    if (this.spanLeft())
      buf.append("<");
    buf.append(this.getFrom());
    buf.append("..");
    if (this.spanRight())
      buf.append(">");
    buf.append(this.getTo());
    return buf.toString();
  }

}
