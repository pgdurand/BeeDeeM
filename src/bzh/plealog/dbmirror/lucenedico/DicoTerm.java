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

/**
 * This class defines a single term entry stored within a Lucene dictionary
 * index.
 * 
 * @author Patrick G. Durand.
 */
public class DicoTerm {
  private String              _id;
  private String              _dataField        = EMPTY_FIELD;
  private Object              _dataObject;

  private static final String EMPTY_FIELD       = "-";
  public static final String  EMPTY_DESCRIPTION = "no description";
  public static final String SYNONYM = "SYN:";
  
  /**
   * Constructor.
   * 
   * @param id
   *          an entry identifier
   */
  public DicoTerm(String id) {
    setId(id);
  }

  public DicoTerm(String id, String dataField) {
    setId(id);
    setDataField(dataField);
  }

  public DicoTerm(String id, String dataField, Object dataObject) {
    setId(id);
    setDataField(dataField);
    set_dataObject(dataObject);
  }

  public String getId() {
    return _id;
  }

  public void setId(String id) {
    this._id = id;
  }

  public String getDataField() {
    return _dataField;
  }

  public void setDataField(String dataField) {
    this._dataField = dataField;
  }

  public String toString() {
    return (_id + ":" + _dataField);
  }

  public Object get_dataObject() {
    return _dataObject;
  }

  public void set_dataObject(Object _dataObject) {
    this._dataObject = _dataObject;
  }
  
  public boolean isSynonym() {
    return _dataField!=null && _dataField.startsWith(SYNONYM);
  }
}
