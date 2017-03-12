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

public class SequenceValidatorCount extends SequenceValidatorSize {

  // ranges
  private List<Range> ranges;

  // min and max sequences size found
  private long        maxSequenceSizeFound = 0;
  private long        minSequenceSizeFound = Long.MAX_VALUE;

  public SequenceValidatorCount(List<Range> ranges) {
    super(-1, Long.MAX_VALUE);
    this.ranges = ranges;
  }

  @Override
  public boolean stopEntry() {
    analyseInProgress = false;

    if ((this.currentSize != -1)) {
      // check min
      if (this.currentSize < this.minSequenceSizeFound) {
        this.minSequenceSizeFound = this.currentSize;
      }

      // check max
      if (this.currentSize > this.maxSequenceSizeFound) {
        this.maxSequenceSizeFound = this.currentSize;
      }

      // check range
      for (Range range : this.ranges) {
        if (range.isInRange(this.currentSize))
          break;
      }
    }

    return false;
  }

  public List<Range> getRanges() {
    return this.ranges;
  }

  public long getMinSequenceSizeFound() {
    return this.minSequenceSizeFound;
  }

  public long getMaxSequenceSizeFound() {
    return this.maxSequenceSizeFound;
  }

  public static class Range {
    private long   _min, _max, _count;
    private String _name;

    public Range(String name, long min, long max) {
      _name = name;
      _min = min;
      _max = max;
    }

    /**
     * @return the _min
     */
    public long getMin() {
      return _min;
    }

    /**
     * @return the _max
     */
    public long getMax() {
      return _max;
    }

    /**
     * @return the _count
     */
    public long getCount() {
      return _count;
    }

    public boolean isInRange(long value) {
      boolean inRange = (value >= _min && value <= _max);

      if (inRange)
        _count++;

      return inRange;
    }

    /**
     * @return the _name
     */
    public String getName() {
      return _name;
    }

  }

}
