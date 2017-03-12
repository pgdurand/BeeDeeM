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
 * A super class to merge the shared method between the FTP and the Local
 * loaderEngine.
 * 
 * @author Patrick G. Durand
 * 
 */
public class LoaderEngine extends Thread {

  protected DBServerConfig _dbsc;
  protected LoaderMonitor  _monitor;
  protected boolean        _ok;

  public LoaderEngine(DBServerConfig dbsc, LoaderMonitor monitor) {
    _dbsc = dbsc;
    _monitor = monitor;
    _ok = false;
  }

  /**
   * Return true if the thread has got something to do.
   * 
   * @return true if the thread return something to do
   */
  protected boolean listingOk() {
    return _ok;
  }

  /**
   * Change the status of the thread
   * 
   * @param _ok
   */
  protected void set_ok(boolean _ok) {
    this._ok = _ok;
  }

  /**
   * Return the DBServerConfig object which contains the descriptor of the
   * databank
   * 
   * @return the descriptor of the databank
   */
  protected DBServerConfig get_dbsc() {
    return _dbsc;
  }

  /**
   * Set the object which describe the descriptor of the databank
   * 
   * @param _dbsc
   *          the descriptor of the databank
   */
  protected void set_dbsc(DBServerConfig _dbsc) {
    this._dbsc = _dbsc;
  }

  /**
   * Return the task monitor of the thread used to do install databank
   * 
   * @return the task monitor
   */
  protected LoaderMonitor get_monitor() {
    return _monitor;
  }

  /**
   * Set the task monitor used to do install databank
   * 
   * @param _monitor
   *          the task monitor
   */
  protected void set_monitor(LoaderMonitor _monitor) {
    this._monitor = _monitor;
  }

}
