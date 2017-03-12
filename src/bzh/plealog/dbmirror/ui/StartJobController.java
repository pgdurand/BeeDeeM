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
 * This interface defines the behaviour of a KDMS Job Controller.
 * 
 * @author Patrick G. Durand
 */
public interface StartJobController {
  /**
   * Allows KDMS to figure out if it can start a job.
   */
  public boolean canStartAJob();

  /**
   * Returns a message. Usually this method is intended to be used with
   * canStartAJob(): when it return false, getControllerMsg() should return an
   * associated message.
   */
  public String getControllerMsg();
}
