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
 * This class describes the properties of a database available for BLAST.
 * 
 * @author Patrick G. Durand
 */
public class DBDescriptor {
  private String             _name;
  private String             _description;
  private String             _code;
  private String             _kbCode;         // used by KB to save the DB
                                              // descriptor in the config file
  private TYPE               _type;
  private String             _users;
  private long               _sequences;
  private long               _diskSize;
  private long               _blastSize;

  public static final String ALL_USERS = "*";

  public static enum TYPE {
    nucleic, proteic, dico, blastp, blastn
  };

  public DBDescriptor() {
    _users = ALL_USERS;
  }

  public DBDescriptor(String name, String description, String code,
      String kbCode, TYPE type, long sequences, long diskSize, long blastSize) {
    this();
    this._name = name;
    this._description = description;
    this._code = code;
    this._kbCode = kbCode;
    this._type = type;
    this._sequences = sequences;
    this._diskSize = diskSize;
    this._blastSize = blastSize;
  }

  public void copy(DBDescriptor src) {
    this._name = src._name;
    this._description = src._description;
    this._code = src._code;
    this._kbCode = src._kbCode;
    this._type = src._type;
    this._users = src._users;
    this._sequences = src._sequences;
    this._diskSize = src._diskSize;
    this._blastSize = src._blastSize;
  }

  public String getCode() {
    return _code;
  }

  public void setCode(String code) {
    this._code = code;
  }

  public String getDescription() {
    return _description;
  }

  public void setDescription(String description) {
    this._description = description;
  }

  public String getName() {
    return _name;
  }

  public void setName(String name) {
    this._name = name;
  }

  public String getKbCode() {
    return _kbCode;
  }

  public void setKbCode(String code) {
    this._kbCode = code;
  }

  public TYPE getType() {
    return _type;
  }

  public void setType(TYPE type) {
    this._type = type;
  }

  public String getAuthorizedUsers() {
    return _users;
  }

  public void setAuthorizedUsers(String users) {
    this._users = users;
  }

  public long getSequences() {
    return _sequences;
  }

  public void setSequences(long sequences) {
    this._sequences = sequences;
  }

  public long getDiskSize() {
    return _diskSize;
  }

  public void setDiskSize(long diskSize) {
    this._diskSize = diskSize;
  }

  public long getBlastSize() {
    return _blastSize;
  }

  public void setBlastSize(long blastSize) {
    this._blastSize = blastSize;
  }

  public String toString() {
    return _name;
  }
}
