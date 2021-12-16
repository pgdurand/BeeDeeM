/* Copyright (C) 2007-2021 Patrick G. Durand
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
package bzh.plealog.dbmirror.lucenedico;

import java.util.List;

/**
 * This interface defines a storage system aims at storing required data to
 * retrieve specific terms from dictionary files.
 * 
 * @author Patrick G. Durand
 */
public interface DicoStorageSystem {
  public static final int READ_MODE  = 1;
  public static final int WRITE_MODE = 2;

  public String getIndexName();

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
   * Closes a storage system.
   * 
   * @return should return false if the storage system cannot be closed.
   */
  public boolean close();

  /**
   * Adds a new term within the storage system.
   * 
   * @param id
   *          a term identifier
   * @param dataField
   *          some data
   * 
   * @throws DicoStorageSystemException
   *           if the new entry cannot be added to the storage system.
   */
  public void addEntry(String id, String dataField)
      throws DicoStorageSystemException;

  /**
   * Adds a new term within the storage system.
   * 
   * @param id
   *          a term identifier
   * @param dataField
   *          some data
   * 
   * @throws DicoStorageSystemException
   *           if the new entry cannot be added to the storage system.
   */
  public void addBinaryEntry(String id, Object dataField)
      throws DicoStorageSystemException;

  /**
   * Returns a DicoTerm given a sequence ID. Returns null if not found.
   * 
   * @throws DicoStorageSystemException
   *           if the storage system cannot be queried.
   */
  public DicoTerm getTerm(String id) throws DicoStorageSystemException;

  /**
   * Returns a DicoTerm array given an array of IDs. If an ID cannot be found in
   * the queried dictionary, then the corresponding entry in the returned array
   * will be set to null.
   * 
   * @throws DicoStorageSystemException
   *           if the storage system cannot be queried.
   */
  public DicoTerm[] getTerms(String[] ids) throws DicoStorageSystemException;

  /**
   * Returns a DicoTerm array representing a path. This method is intended to be
   * used with tree-based dictionary structure. Please note that the returned
   * array contains ids from idTo to idFrom. Returns null if idFrom, idTo or any
   * internal ids between the two previous ones are not found.
   * 
   * @throws DicoStorageSystemException
   *           if the storage system cannot be queried.
   */
  public DicoTerm[] getTerms(String idFrom, String idTo)
      throws DicoStorageSystemException;

  /**
   * Returns a id given a sequence name. Returns null if not found.
   * 
   * @throws DicoStorageSystemException
   *           if the storage system cannot be queried.
   */
  public String getID(String term_name) throws DicoStorageSystemException;

  /**
   * Returns a list of DicoTerms. All the terms are found using an approximate
   * lucene search (http://lucene.apache.org/core/2_9_4/queryparsersyntax.html)
   * of the input string parameter
   * 
   * Each DicoTerm result contains : - id : the taxonomic id - data : the
   * taxonomic name - object : the found score (to allow sort by bests results).
   * Fuzzy default value is set to 0.5
   */
  public List<DicoTerm> getApprochingTerms(String term, int maxTerms)
      throws DicoStorageSystemException;
  
  public List<DicoTerm> getApprochingTerms(String term, String fuzzy, int maxTerms)
      throws DicoStorageSystemException;
}
