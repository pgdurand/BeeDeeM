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
package bzh.plealog.dbmirror.fetcher;

/**
 * This class describes a file explorer. It's used to merge the shared methods
 * between FTP and Local fileListLoader.
 * 
 * @author Patrick G. Durand
 * 
 */
public class FileListLoader extends Thread {

  protected DBServerConfig _dbsc;
  protected boolean        _ok;

  public FileListLoader(DBServerConfig dbsc) {
    _dbsc = dbsc;
  }

  protected boolean listingOk() {
    return _ok;
  }

  protected DBServerConfig get_dbsc() {
    return _dbsc;
  }

  protected void set_dbsc(DBServerConfig _dbsc) {
    this._dbsc = _dbsc;
  }

  protected void set_ok(boolean _ok) {
    this._ok = _ok;
  }

}
