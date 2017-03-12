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

import org.apache.lucene.search.Query;

/**
 * This interface defines a storage system aims at storing required data to
 * retrieve specific entries from sequence database files.
 * 
 * @author Patrick G. Durand
 */
public interface StorageSystem {
  public static final int READ_MODE  = 1;
  public static final int WRITE_MODE = 2;

  /**
   * Opens a storage system in either read or write mode.
   * 
   * @param loadInRAM
   *          parameter only used in READ_MODE.
   * 
   * @return should return false if the storage system cannot be opened.
   */
  public boolean open(String name, int mode, boolean loadInRAM);

  /**
   * Opens a storage system in either read or write mode. Same as open(name,
   * mode, loadInRAM) with loadInRAM set to false.
   * 
   * @return should return false if the storage system cannot be opened.
   */
  public boolean open(String name, int mode);

  /**
   * Return the number of entries contained in the index.
   */
  public int size();

  /**
   * Closes a storage system.
   * 
   * @return should return false if the storage system cannot be closed.
   */
  public boolean close();

  /**
   * Adds a new entry within the storage system.
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
   * 
   * @throws StorageSystemException
   *           if the new entry cannot be added to the storage system.
   */
  public void addEntry(String id, String name, String fName, long start,
      long stop) throws StorageSystemException;

  /**
   * Returns a DBEntry array given a sequence ID.
   * 
   * @throws StorageSystemException
   *           if the storage system cannot be queried.
   */
  public DBEntry[] getEntry(String id) throws StorageSystemException;

  public DBEntry[] getEntry(String id, Query q) throws StorageSystemException;
}
