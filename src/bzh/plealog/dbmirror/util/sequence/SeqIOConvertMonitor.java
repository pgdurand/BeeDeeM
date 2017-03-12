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

/**
 * This interface defines a monitor that can be used during file format
 * conversion.
 * 
 * @author Patrick G. Durand
 */
public interface SeqIOConvertMonitor {
  /**
   * Method invoked when a new sequence ID is found in a data file.
   */
  public void seqFound(String seqID);

  /**
   * Indicates that the processor has just started the conversion.
   */
  public void startProcessing();

  /**
   * Indicates that the processor has just stop the conversion.
   * 
   * @param time
   *          the time required to process the conversion.
   */
  public void stopProcessing(long time);

}
