/* Copyright (C) 2022 Patrick G. Durand
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
package bzh.plealog.dbmirror.util.conf;

/**
 * Utility class for DBMSAbstractConfigConstants.
 * 
 * @author Patrick G. Durand
 */
public abstract class DBMSAbstractConfigConstants {

  public static final String USER_DIR_PROP_KEY            = "user.dir";
  public static final String CONF_PATH_NAME               = "conf";
  public static final String EXT_PATH_NAME                = "external";
  public static final String BIN_PATH_NAME                = "bin";
  public static final String APP_KEY_PREFIX               = "KL_";
  public static final String APP_HOME_PROP_KEY            = APP_KEY_PREFIX+"HOME";
  public static final String APP_WORKING_DIR_PROP_KEY     = APP_KEY_PREFIX+"WORKING_DIR";
  public static final String APP_CONF_DIR_PROP_KEY        = APP_KEY_PREFIX+"CONF_DIR";
  public static final String APP_DEBUG_MODE_PROP_KEY      = APP_KEY_PREFIX+"DEBUG";

  /**
   * Remove terminal single quotes of a string if any.
   */
  public static String pruneQuotes(String str) {
    if (str == null)
      return str;
    str = str.trim();
    if (str.charAt(0) == '"' || str.charAt(0) == '\'') {
      str = str.substring(1);
    }
    int lastPos = str.length() - 1;
    if (str.charAt(lastPos) == '"' || str.charAt(lastPos) == '\'') {
      str = str.substring(0, lastPos);
    }
    return str;
  }

}
