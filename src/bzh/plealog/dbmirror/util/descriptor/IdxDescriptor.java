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
package bzh.plealog.dbmirror.util.descriptor;


/**
 * This class contains the description of a data index.
 * 
 * @author Patrick G. Durand
 */
public class IdxDescriptor extends DBDescriptor {
  private String _reader;

  public IdxDescriptor() {
    super();
  }

  public IdxDescriptor(String name, String description, String code,
      String kbCode, DBDescriptor.TYPE type, long sequences, long diskSize,
      long blastSize) {
    super(name, description, code, kbCode, type, sequences, diskSize, blastSize);
  }

  public IdxDescriptor(String name, String description, String code,
      String kbCode, String reader, DBDescriptor.TYPE type, long sequences,
      long diskSize, long blastSize) {
    this(name, description, code, kbCode, type, sequences, diskSize, blastSize);
    setReader(reader);
  }

  /**
   * Returns the type of database reader to use when querying a data index.
   * Value is one of DBMirrorConfig.XX_READER.
   */
  public String getReader() {
    return _reader;
  }

  /**
   * Sets the type of database reader to use when querying a data index. Value
   * must be one of DBMirrorConfig.XX_READER.
   */
  public void setReader(String reader) {
    this._reader = reader;
  }

  public void copy(DBDescriptor src) {
    super.copy(src);
    this._reader = ((IdxDescriptor) src)._reader;
  }
}
