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
package bzh.plealog.dbmirror.ui.resources;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class DBMSMessages {
  private static final String         BUNDLE_NAME     = DBMSMessages.class
                                                          .getPackage()
                                                          .getName()
                                                          + ".messages";

  private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
                                                          .getBundle(BUNDLE_NAME);

  private DBMSMessages() {
  }

  public static String getString(String key) {
    try {
      return RESOURCE_BUNDLE.getString(key);
    } catch (MissingResourceException e) {
      return '!' + key + '!';
    }
  }
}