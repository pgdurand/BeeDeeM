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
package bzh.plealog.dbmirror.util.sequence;

import java.io.File;

import bzh.plealog.dbmirror.util.descriptor.DatabankFormat;

public abstract class SequenceValidatorAbstract implements ISequenceValidator {

  protected DatabankFormat databankFormat;

  public DatabankFormat getDatabankFormat() {
    return databankFormat;
  }

  public void setDatabankFormat(DatabankFormat databankFormat) {
    this.databankFormat = databankFormat;
  }

  public void initialise(File input) {

  }

  public void finish() {

  }
}
