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
package bzh.plealog.dbmirror.task;

import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * For internal use only to force bank installation to stop. Used for testing, 
 * debugging, etc. Can be used as unit and global tasks.
 * 
 * @author Patrick G. Durand
 */
public class PTaskForceStop extends PAbstractTask {

  public PTaskForceStop() {
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getName() {
    return "ForceBankInstallation";
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getUserFriendlyName() {
    return "force stop";
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getErrorMsg() {
    return getUserFriendlyName();
  }

  /**
   * Implementation of KLTask interface.
   */
  public boolean execute() {
    // a visible message for log file
    LoggerCentral.info(PTask.LOGGER, "FORCE BANK INSTALLATION TO STOP");
    // always fails
    return false;
  }

  public void setParameters(String params) {
  }

}
