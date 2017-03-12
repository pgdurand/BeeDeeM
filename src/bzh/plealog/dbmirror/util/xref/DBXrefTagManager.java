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
package bzh.plealog.dbmirror.util.xref;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;

/**
 * This class is used to handle all tags that have to be anaylsed in order to
 * retrieve dbxrefs from the various parts of a sequence databse entry.
 * 
 * @author Patrick G. Durand
 */
public class DBXrefTagManager {
  private Hashtable<String, DBXrefTagHandler> handlers;

  private static final Log                    LOGGER = LogFactory
                                                         .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                             + ".DBXrefTagManager");

  public DBXrefTagManager() {
    handlers = new Hashtable<String, DBXrefTagHandler>();
  }

  public DBXrefTagManager(String configString) {
    this();
    initialise(configString);
  }

  public DBXrefTagManager(File configFile) {
    this();
    initialise(configFile);
  }

  public void clear() {
    handlers.clear();
  }

  public void addTagHandler(DBXrefTagHandler handler) {
    handlers.put(handler.getTag(), handler);
  }

  private String trim(String line) {
    String str = line.trim();
    if (str.startsWith("\"") == false)
      throw new RuntimeException("token [" + str
          + "] does not start with quote");
    if (str.endsWith("\"") == false)
      throw new RuntimeException("token [" + str + "] does not end with quote");
    return str.substring(1, str.length() - 1);
  }

  private void analyseLine(String line) {
    DBXrefTagHandler handler;
    StringTokenizer tokenizer;
    String tag, key, begin, end, code, codeSplitter;

    tokenizer = new StringTokenizer(line, ",");
    if (tokenizer.countTokens() != 6)
      throw new RuntimeException("Data line does not contain 6 tokens.");
    tag = trim(tokenizer.nextToken());
    key = trim(tokenizer.nextToken());
    begin = trim(tokenizer.nextToken());
    end = trim(tokenizer.nextToken());
    code = trim(tokenizer.nextToken());
    codeSplitter = trim(tokenizer.nextToken());

    handler = handlers.get(tag);
    if (handler == null) {
      handler = new DBXrefTagHandler(tag, begin);
      handlers.put(tag, handler);
    }
    handler.addSplitter(key, begin, end, code, codeSplitter);
  }

  // use example : see SeqIOUtils.XREF_MANAGER variable
  public void initialise(String configString) {
    StringTokenizer tokenizer;
    String token;

    clear();
    tokenizer = new StringTokenizer(configString, "\n");
    while (tokenizer.hasMoreTokens()) {
      token = tokenizer.nextToken();
      if (token.startsWith("#"))
        continue;
      try {
        analyseLine(token);
      } catch (Exception e) {
        LOGGER.warn("Unable to read definition: [" + token + "]: "
            + e.getMessage());
      }
    }
  }

  // use example : see DBXrefTagHandlerTest
  public void initialise(File configFile) {
    BufferedReader reader = null;
    clear();
    try {
      reader = new BufferedReader(new InputStreamReader(new FileInputStream(
          configFile), "UTF-8"));
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (line.length() == 0)
          continue;
        if (!line.startsWith("#")) {
          try {
            analyseLine(line);
          } catch (Exception e) {
            LOGGER.warn("Unable to read definition: [" + line + "]: "
                + e.getMessage());
          }
        }
      }
    } catch (IOException e) {
      LOGGER.warn("Unable to read file: " + configFile.getAbsolutePath() + ": "
          + e);
    } finally {
      IOUtils.closeQuietly(reader);
    }
  }

  public String getDbXref(String dataLine) {
    Enumeration<DBXrefTagHandler> enumH;
    DBXrefTagHandler handler;
    String xref;

    if (dataLine == null)
      return null;
    enumH = handlers.elements();
    while (enumH.hasMoreElements()) {
      handler = enumH.nextElement();
      xref = handler.getDbXref(dataLine);
      if (xref != null) {
        return xref;
      }
    }
    return null;
  }
}
