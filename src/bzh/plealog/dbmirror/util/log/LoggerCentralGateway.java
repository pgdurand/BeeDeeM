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
package bzh.plealog.dbmirror.util.log;

/**
 * This interfaces can be used to use a particular log service within KDMS.
 * 
 * Its use is quite simple: setup an implementation of this service and pass an
 * instance of the implemented class to LoggerCentral.
 * 
 * @author Patrick G. Durand
 */
public interface LoggerCentralGateway {

  /**
   * Emit an error message.
   */
  public void error(String msg);

  /**
   * Emit a warning message.
   */
  public void warn(String msg);

  /**
   * Emit a information message.
   */
  public void info(String msg);

  /**
   * Emit a debug message.
   */
  public void debug(String msg);
}
