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
package bzh.plealog.dbmirror.main;

/**
 * Defines API a BeeDeeM command-line tool must implement.
 * 
 * @author Patrick G. Durand
 */
public interface BdmToolApi {
  /**
   * Execute business logic of a tool.
   * 
   * @param args command-line arguments
   * 
   * @return true whether execution is ok, false otherwise.
   */
  public boolean execute(String[] args);
}
