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
package bzh.plealog.dbmirror.ui;

/**
 * This class is used to store the information about the files downloaded from a
 * FTP server.
 * 
 * @author Patrick G. Durand
 */
public class FileInfo {
  // databank descriptor name
  private String descName;
  // number of files to retrieve
  private int    nFiles;
  // number of bytes to download
  private long   bytes;
  // databank size after installation
  private long   dbSize;

  public FileInfo(String descName, int nFiles, long bytes, long dbSize) {
    super();
    this.descName = descName;
    this.nFiles = nFiles;
    this.bytes = bytes;
    this.dbSize = dbSize;
  }

  public int getnFiles() {
    return nFiles;
  }

  public long getBytes() {
    return bytes;
  }

  public void setnFiles(int nFiles) {
    this.nFiles = nFiles;
  }

  public void setBytes(long bytes) {
    this.bytes = bytes;
  }

  public long getDbSize() {
    return dbSize;
  }

  public void setDbSize(long dbSize) {
    this.dbSize = dbSize;
  }

  public String getDescName() {
    return descName;
  }

  public void setDescName(String descName) {
    this.descName = descName;
  }

}