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
package bzh.plealog.dbmirror.util.sequence;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import bzh.plealog.dbmirror.indexer.DBParserException;
import bzh.plealog.dbmirror.util.descriptor.DatabankFormat;

/**
 * Utility class to cut a file given sequence range.
 * 
 * @author Patrick G. Durand
 */
public class DBFileCutter {

  /**
   * Cut fileIn given sequence range. This method conserves the format of the
   * source.
   * 
   * @param fileIn
   *          source file
   * @param fileOut
   *          target file that will be created
   * @param formatType
   *          fileIn format. See SeqIOUtils.XXX constants
   * @param monitor
   *          a monitor to follow conversion. Can be null.
   * @param from
   *          start of the range. 0 based value.
   * @param to
   *          end of the range. 0 based value.
   */
  public static void cutFile(String fileIn, String fileOut, int formatType,
      SeqIOConvertMonitor monitor, int from, int to) throws DBParserException {
    FileInputStream fis = null;
    FileOutputStream fos = null;

    try {
      fis = new FileInputStream(fileIn);
      fos = new FileOutputStream(fileOut);
      cutStream(fis, fos, formatType, monitor, from, to);
    } catch (IOException e) {
      throw new DBParserException(e.getMessage());
    } finally {
      if (fis != null) {
        try {
          fis.close();
        } catch (IOException e) {
        }
      }
      if (fos != null) {
        try {
          fos.close();
        } catch (IOException e) {
        }
      }

    }
  }

  /**
   * Cut input stream given sequence range. This method conserves the format of
   * the source.
   * 
   * @param is
   *          source stream
   * @param os
   *          target stream that will be filled in with sequence data
   * @param formatType
   *          fileIn format. See SeqIOUtils.XXX constants
   * @param monitor
   *          a monitor to follow conversion. Can be null.
   * @param from
   *          start of the range. 0 based value.
   * @param to
   *          end of the range. 0 based value.
   */
  public static void cutStream(InputStream is, OutputStream os, int formatType,
      SeqIOConvertMonitor monitor, int from, int to) throws DBParserException {
    BufferedWriter writer = null;
    BufferedReader reader = null;
    String line;
    long tim;
    int entries;
    boolean seqOk;

    DatabankFormat format = DatabankFormat.getFormatFromSeqIOUtils(formatType);
    if (format == null) {
      throw new DBParserException("unknown format: " + formatType);
    }

    tim = System.currentTimeMillis();
    try {
      if (monitor != null) {
        monitor.startProcessing();
      }
      reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
      writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
      seqOk = false;
      entries = -1;
      while ((line = reader.readLine()) != null) {
        // find a new sequence by its ID line
        if (line.startsWith(format.getIdString())) {
          entries++;
          if (monitor != null) {
            monitor.seqFound(SeqIOUtils.getID(line, format.getIdString()));
          }
          if (entries < from) {
            continue;
          } else if (entries > to) {
            break;
          } else {
            seqOk = true;
          }
        }
        if (seqOk) {
          writer.write(line);
          writer.write("\n");
        }
      }
      writer.flush();
    } catch (Exception e) {
      throw new DBParserException(e.getMessage());
    } finally {
      if (monitor != null) {
        monitor.stopProcessing(System.currentTimeMillis() - tim);
      }
    }
  }

}
