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
package bzh.plealog.dbmirror.task;

/**
 * For internal use only to notify the system has done all tasks on a databank.
 * 
 * @author Patrick G. Durand
 */
public class PTaskEndProcessing extends PAbstractTask {

  public PTaskEndProcessing() {
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getName() {
    return "Ending processing";
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getUserFriendlyName() {
    return "end processing";
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getErrorMsg() {
    return null;
  }

  /**
   * Implementation of KLTask interface.
   */
  public boolean execute() {
    return true;
  }

  public void setParameters(String params) {
  }

}
