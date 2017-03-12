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

import javax.swing.event.EventListenerList;

public class DBMirrorListenerSupport {
  protected EventListenerList _listenerList;

  public DBMirrorListenerSupport() {
    _listenerList = new EventListenerList();
  }

  public void fireHitChange(DBMirrorEvent mge) {
    // Guaranteed to return a non-null array
    Object[] listeners = _listenerList.getListenerList();
    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == DBMirrorListener.class) {
        ((DBMirrorListener) listeners[i + 1]).mirrorChanged(mge);
      }
    }
  }

  public void addDBMirrorListener(DBMirrorListener listener) {
    if (listener == null)
      return;
    _listenerList.add(DBMirrorListener.class, listener);
  }

  public void removeDBMirrorListener(DBMirrorListener listener) {
    if (listener == null)
      return;
    _listenerList.remove(DBMirrorListener.class, listener);
  }
}
