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
package bzh.plealog.dbmirror.util.runner;

public class CommandArgument {
  private String  argument;
  private boolean isPath;
  private boolean keepFileNameOnly;
  private boolean isWorkingPath;

  public CommandArgument(String argument, boolean isPath) {
    super();
    this.argument = argument;
    this.isPath = isPath;
  }

  public CommandArgument(String argument, boolean isPath,
      boolean keepFileNameOnly) {
    super();
    this.argument = argument;
    this.isPath = isPath;
    this.keepFileNameOnly = keepFileNameOnly;
  }

  public CommandArgument(String argument, boolean isPath,
      boolean keepFileNameOnly, boolean isWorkingPath) {
    super();
    this.argument = argument;
    this.isPath = isPath;
    this.keepFileNameOnly = keepFileNameOnly;
    this.isWorkingPath = isWorkingPath;
  }

  public String getArgument() {
    return argument;
  }

  public void setArgument(String argument) {
    this.argument = argument;
  }

  public boolean isPath() {
    return isPath;
  }

  public void setPath(boolean isPath) {
    this.isPath = isPath;
  }

  public boolean isKeepFileNameOnly() {
    return keepFileNameOnly;
  }

  public void setKeepFileNameOnly(boolean keepFileNameOnly) {
    this.keepFileNameOnly = keepFileNameOnly;
  }

  public boolean isWorkingPath() {
    return isWorkingPath;
  }

  public void setWorkingPath(boolean isWorkingPath) {
    this.isWorkingPath = isWorkingPath;
  }

}
