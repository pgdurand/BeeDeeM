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
package bzh.plealog.dbmirror.util.sequence;

import java.io.File;

import bzh.plealog.dbmirror.util.descriptor.DatabankFormat;

public interface ISequenceValidator {

  /**
   * Method call by SequenceFileManager each time a DatabankFormat.idString is
   * found
   * 
   * @return true if the entry should be analyzed
   */
  public boolean startEntry();

  /**
   * Method call by SequenceFileManager at the end of each entry in a sequence
   * file
   * 
   * @return true if the entry is valid
   */
  public boolean stopEntry();

  /**
   * Method call by SequenceFileManager each time a DatabankFormat.beginSequence
   * is found
   * 
   * @return true if the sequence should be analyzed
   */
  public boolean startSequence();

  /**
   * Method call by SequenceFileManager for each line of the entry
   * 
   * @return true if the entry is still valid
   * */
  public boolean analyseLine(StringBuilder line);

  /**
   * Method call by SequenceFileManager to test the parameters of the validator
   * 
   * @return false if the parameters are wrong (example for cutFile : min>max is
   *         worng)
   */
  public boolean isActive();

  /**
   * 
   * @return the databank format of the given input file
   */
  public DatabankFormat getDatabankFormat();

  /**
   * Set the databank format of the given input file
   * 
   * @param databankFormat
   */
  public void setDatabankFormat(DatabankFormat databankFormat);

  /**
   * 
   * @return the string representing this object for parameters in a indexing
   *         unit task
   */
  public String toParametersForUnitTask();

  /**
   * Method called by the SequenceFileManager to initialise the current
   * validator if necessary
   * 
   * @param input
   */
  public void initialise(File input);

  /**
   * Method called by the SequenceFileManager when the file has been parsed -
   */
  public void finish();

}
