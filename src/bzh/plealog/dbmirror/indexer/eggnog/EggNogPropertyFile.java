/* Copyright (C) 2007-2017 Ludovic Antin
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
package bzh.plealog.dbmirror.indexer.eggnog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import bzh.plealog.dbmirror.lucenedico.DicoParserException;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * This class load a property file from eggnog in memory like funcat and
 * description files. All lines are written like : [nog name] [value]
 * 
 * @author Ludovic Antin
 * 
 */
public class EggNogPropertyFile extends HashMap<String, String> {
  private static final long serialVersionUID      = -2647205510984082862L;

  private static final Logger  LOGGER                = LogManager.getLogger(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                          + ".EggNogPropertyFile");

  private File              propertyFile          = null;
  private static final char keyValueSeparatorChar = '\t';

  public EggNogPropertyFile(File propertyFile) {
    this.propertyFile = propertyFile;
  }

  /**
   * Loads the property file
   */
  public void load() {
    BufferedReader reader = null;
    String line = null;
    String[] keyValue = null;

    try {
      reader = new BufferedReader(new InputStreamReader(new FileInputStream(
          this.propertyFile), "UTF-8"));
      while ((line = reader.readLine()) != null) {
        // create a new entry
        try {
          keyValue = StringUtils.split(line, keyValueSeparatorChar);
          this.put(keyValue[0], keyValue.length > 1 ? keyValue[1] : "");
        } catch (Exception ex) {
          if (!line.startsWith("#")) { // do not log for the first line
            LoggerCentral.info(LOGGER, "Unable to read entry for '" + line
                + "' : " + ex.getMessage());
          }
        }

      }
    } catch (Exception ex) {
      throw new DicoParserException(ex.getMessage());
    } finally {
      IOUtils.closeQuietly(reader);
    }
  }
}
