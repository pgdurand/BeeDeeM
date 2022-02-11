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
package bzh.plealog.dbmirror.fetcher;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Date;

import org.apache.commons.net.ftp.FTPFile;

import com.plealog.genericapp.api.file.EZFileUtils;

import bzh.plealog.bioinfo.util.CoreUtil;

/**
 * Utility class to handle remote file to install by BeeDeeM.
 * 
 * @author Patrick G. Durand
 */
public class DBMSFile {
  private String  remoteDir;
  private String  fName;
  private long    fSize;
  private long    fTimeStamp;
  
  private transient Date fDateStamp;
  private transient File fRepr;
  
  /**
   * Constructor
   * 
   * @param line must contain four fields separated by a tab. Fields in this order:
   * path to file, file name, size (bytes), time stamp (long value from epoch)
   */
  public DBMSFile(String line) {
    super();
    read(line);
  }

  public DBMSFile(File file) {
    super();
    remoteDir = file.getParent();
    fName = file.getName();
    fSize = file.length();
    fTimeStamp = file.lastModified();
  }

  public DBMSFile(String remoteDir, FTPFile ftpFile) {
    super();
    this.remoteDir = remoteDir;
    fName = ftpFile.getName();
    fSize = ftpFile.getSize();
    fDateStamp = ftpFile.getTimestamp().getTime();
    fTimeStamp = fDateStamp.getTime();
  }

  public String getRemoteDir() {
    return remoteDir;
  }
  
  public String getName() {
    return fName;
  }

  public long getSize() {
    return fSize;
  }
  
  public Date getDateStamp() {
    if (fDateStamp==null) {
      fDateStamp = new Date(fTimeStamp);
    }
    return fDateStamp;
  }
  
  public long getEpochStamp() {
    return fTimeStamp;
  }
  
  public File getRemoteFile() {
    if (fRepr==null) {
      fRepr = new File(EZFileUtils.terminatePath(remoteDir)+fName);
    }
    return fRepr;
  }
  
  public void write(Writer writer) throws IOException {
    writer.write(remoteDir);
    writer.write("\t");
    writer.write(fName);
    writer.write("\t");
    writer.write(String.valueOf(fSize));
    writer.write("\t");
    writer.write(String.valueOf(fTimeStamp));
  }
  
  private void read(String line) {
    String[] elements = CoreUtil.tokenize(line, "\t");
    remoteDir = elements[1];
    fName = elements[2];
    fSize = Long.valueOf(elements[3]);
    fTimeStamp = Long.valueOf(elements[4]);
  }

}
