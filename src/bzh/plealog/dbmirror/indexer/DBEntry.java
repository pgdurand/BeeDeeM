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
package bzh.plealog.dbmirror.indexer;

/**
 * This class defines a single database entry stored within a Lucene index.
 * 
 * @author Patrick G. Durand.
 */
public class DBEntry {
  private String _id;
  private String _name;
  private String _fName;
  private long   _start;
  private long   _stop;

  private String _indexPath; // for internal use when querying a mirror

  /**
   * Constructor.
   * 
   * @param id
   *          an entry identifier
   * @param name
   *          an entry name
   * @param fName
   *          the name of the database file containing this entry.
   * @param start
   *          absolute position of the beginning of the entry within the file.
   *          Unit is bytes.
   * @param stop
   *          absolute position of the ending of the entry within the file. Unit
   *          is bytes.
   */
  public DBEntry(String id, String name, String fName, String start, String stop) {
    setId(id);
    setName(name);
    setFName(fName);
    setStart(Long.valueOf(start).longValue());
    setStop(Long.valueOf(stop).longValue());
  }

  public String getId() {
    return _id;
  }

  public void setId(String id) {
    this._id = id;
  }

  public String getName() {
    return _name;
  }

  public void setName(String name) {
    this._name = name;
  }

  public String getFName() {
    return _fName;
  }

  public void setFName(String fName) {
    this._fName = fName;
  }

  public long getStart() {
    return _start;
  }

  public void setStart(long start) {
    this._start = start;
  }

  public long getStop() {
    return _stop;
  }

  public void setStop(long stop) {
    this._stop = stop;
  }

  public String getIndexPath() {
    return _indexPath;
  }

  public void setIndexPath(String indexPath) {
    this._indexPath = indexPath;
  }

  public String toString() {
    return (_id + ":" + _name + "," + _start + "," + _stop + " (" + _fName + ")");
  }
}
