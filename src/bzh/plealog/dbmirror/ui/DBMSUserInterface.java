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
 * This class can be used to create and interact with the KDMS UserInterface.
 * 
 * @author Patrick G. Durand
 */
public class DBMSUserInterface {
  private static DBMSPanel _panel;

  /**
   * Returns the user interface to KDMS. Note that this interface is a singleton
   * and only one UI can be used within a single application.
   * 
   * @param dscPath
   *          the absolute path to the databank descriptor files. (.dsc files)
   */
  public static DBMSPanel getUserInterface(String dscPath, boolean showBtnText)
      throws RuntimeException {
    if (_panel == null) {
      _panel = new DBMSPanel(dscPath, showBtnText);
      
    }

    return _panel;
  }

  /**
   * This method should be called when a user wants to quit an application. If
   * the method returns false, the application should not exit.
   */
  public static boolean canClose() {
    if (_panel != null) {
      return _panel.canClose();
    }
    return true;
  }

  public void setJobController(StartJobController controller) {
    if (_panel != null)
      _panel.setJobController(controller);
  }

  /**
   * This method can be used to figure out if a job is running.
   */
  public static boolean hasJobRunning() {
    if (_panel != null) {
      return _panel.hasJobRunning();
    }
    return false;
  }

  /**
   * This method can be used to figure out if a job has been scheduled.
   */
  public static boolean hasJobScheduled() {
    if (_panel != null) {
      return _panel.hasJobScheduled();
    }
    return false;
  }

  
}
