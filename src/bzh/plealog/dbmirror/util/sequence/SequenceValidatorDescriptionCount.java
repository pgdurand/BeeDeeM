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

import java.util.List;

import bzh.plealog.dbmirror.util.descriptor.DatabankFormat;
import bzh.plealog.dbmirror.util.sequence.SequenceValidatorCount.Range;

public class SequenceValidatorDescriptionCount extends
    SequenceValidatorAbstract {

  // ranges
  private List<Range> ranges;

  // min and max description size found
  private long        maxDescriptionSizeFound = 0;
  private long        minDescriptionSizeFound = Long.MAX_VALUE;

  // indicates if the line whould be analysed
  protected boolean   analyseInProgress       = false;

  // the current size of the current description
  protected long      currentSize             = -1;

  public SequenceValidatorDescriptionCount(List<Range> ranges) {
    this.ranges = ranges;
  }

  public List<Range> getRanges() {
    return this.ranges;
  }

  public long getMinDescriptionSizeFound() {
    return this.minDescriptionSizeFound;
  }

  public long getMaxDescriptionSizeFound() {
    return this.maxDescriptionSizeFound;
  }

  @Override
  public boolean startEntry() {
    analyseInProgress = true;
    currentSize = 0;
    return true;
  }

  @Override
  public boolean stopEntry() {
    return false;
  }

  @Override
  public boolean startSequence() {
    analyseInProgress = false;
    if ((this.currentSize != -1)) {
      // check min
      if (this.currentSize < this.minDescriptionSizeFound) {
        this.minDescriptionSizeFound = this.currentSize;
      }

      // check max
      if (this.currentSize > this.maxDescriptionSizeFound) {
        this.maxDescriptionSizeFound = this.currentSize;
      }

      // check range
      for (Range range : this.ranges) {
        if (range.isInRange(this.currentSize))
          break;
      }
    }
    return true;
  }

  @Override
  public boolean analyseLine(StringBuilder line) {
    if (analyseInProgress) {

      DatabankFormat format = getDatabankFormat();
      String lineStr = line.toString();

      if (lineStr.startsWith(format.getBeginDescriptionString())
          || lineStr.startsWith(format.getBeginDescriptionString())) {

        switch (format.getType()) {

          case Fasta:
          case FastQ:

            int idx = lineStr.indexOf(" ");
            if (idx != 1) {
              currentSize += lineStr.length() - (idx + 1);
            }

            break;

          case Genbank:
          case SwissProt:

            if (lineStr.startsWith(format.getBeginDescriptionString())) {
              currentSize += lineStr
                  .replaceFirst(format.getBeginDescriptionString(), "").trim()
                  .length();
            } else if (lineStr
                .startsWith(format.getContinueDescriptionString())) {
              currentSize += lineStr
                  .replaceFirst(format.getContinueDescriptionString(), "")
                  .trim().length();
            }

            break;
        }
      }
    }
    return true;
  }

  @Override
  public boolean isActive() {
    // always true; this validator is only used for counting
    return true;
  }

  @Override
  public String toParametersForUnitTask() {
    // nothing to do because this validator is not used by KDMS
    return null;
  }

}
