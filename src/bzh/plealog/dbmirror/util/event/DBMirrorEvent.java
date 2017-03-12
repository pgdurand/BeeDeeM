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
package bzh.plealog.dbmirror.util.event;

import java.util.EventObject;

import bzh.plealog.dbmirror.util.conf.DBMirrorConfig;

public class DBMirrorEvent extends EventObject {
  private static final long serialVersionUID = 8398358182255089122L;
  private TYPE              _type;

  public enum TYPE {
    dbAdded, dbRemoved, dbChanged
  }

  /**
   * @param src
   *          the source object is a DBMirrorConfig instance
   */
  public DBMirrorEvent(Object src, TYPE type) {
    super(src);
    this._type = type;
  }

  public TYPE getType() {
    return _type;
  }

  public DBMirrorConfig getMirrorConfig() {
    return (DBMirrorConfig) this.getSource();
  }
}
