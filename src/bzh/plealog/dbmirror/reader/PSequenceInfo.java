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

import bzh.plealog.bioinfo.data.sequence.IBankSequenceInfo;

/**
 * This class stores sequence information.
 * 
 * @author Patrick G. Durand
 */
public class PSequenceInfo extends IBankSequenceInfo {
  private static final long serialVersionUID = -6474130731808638145L;
  // !!!!! When adding a field, do not forget to update the clone method
  private String            id;
  private String            desc;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getDescription() {
    return desc;
  }

  public void setDescription(String desc) {
    this.desc = desc;
  }

  public Object clone() {
    PSequenceInfo isi = new PSequenceInfo();
    isi.setId(this.getId());
    isi.setDescription(this.getDescription());
    isi.copy(this);
    return isi;
  }
}
