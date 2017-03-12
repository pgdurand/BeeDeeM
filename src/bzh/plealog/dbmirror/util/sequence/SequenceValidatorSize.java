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

import bzh.plealog.dbmirror.indexer.SwissProtParser;

/**
 * A sequence validator which validate only sequences whose number is between
 * 'min' and 'max' <br>
 * Set -1 to min if you just want a max value <br>
 * Set -1 to max if you just want a min value
 * 
 * @author Ludovic Antin
 * 
 */
public class SequenceValidatorSize extends SequenceValidatorAbstract {

  public static String PARAMETER_TERM     = "seqsize";
  public static String BOUNDARY_DELIMITER = "to";

  // sequence size filter section
  private long         minSequenceSize    = -1;
  private long         maxSequenceSize    = -1;

  // indicates if the line whould be analysed
  protected boolean    analyseInProgress  = false;

  // the current size of the current sequence
  protected long       currentSize        = -1;

  public SequenceValidatorSize(long minSize, long maxSize) {
    this.setMinSequenceSize(minSize);
    this.setMaxSequenceSize(maxSize);
  }

  /**
   * Initialize a new instance with a string which format is mintomax where
   * 'min' and 'max' must be long values. Examples : 1to2000 or -1to500 or 2to-1
   * 
   * @param args
   */
  public SequenceValidatorSize(String args) throws NumberFormatException {
    int index = args.toLowerCase().indexOf(BOUNDARY_DELIMITER);
    if (index != -1) {
      this.setMinSequenceSize(Long.parseLong(args.substring(0, index).trim()));
      this.setMaxSequenceSize(Long.parseLong(args.substring(
          index + BOUNDARY_DELIMITER.length()).trim()));
    }
  }

  public long getMinSequenceSize() {
    return minSequenceSize;
  }

  public void setMinSequenceSize(long minSequenceSize) {
    this.minSequenceSize = minSequenceSize;
  }

  public long getMaxSequenceSize() {
    return maxSequenceSize;
  }

  public void setMaxSequenceSize(long maxSequenceSize) {
    this.maxSequenceSize = maxSequenceSize;
  }

  @Override
  public boolean startSequence() {
    if (this.currentSize > 0) {
      // we found the sequence size inside the description of the sequence
      return this.testSize(this.currentSize);
    } else {
      analyseInProgress = true;
      currentSize = 0;
      return true;
    }
  }

  @Override
  public boolean analyseLine(StringBuilder line) {
    if (analyseInProgress) // count the number of characters in the sequence
    {
      char c;
      for (int i = 0; i < line.length(); i++) {
        c = line.charAt(i);
        if (Character.isLetter(c)) {
          currentSize++;
        }
      }
      // test the max size
      if ((this.maxSequenceSize != -1) && (currentSize > this.maxSequenceSize)) {
        analyseInProgress = false; // too long
        return false;
      }
      // test the min size
      if ((this.maxSequenceSize == -1) && (currentSize > this.minSequenceSize)) {
        // it's ok : unnecessary to continue
        analyseInProgress = false;
        return true;
      }
    } else if (this.currentSize <= 0) {
      // maybe the sequence size is written in the beginning of the file
      long sizeFound = 0;
      switch (this.getDatabankFormat().getType()) {
        case Genbank:
          if (line.toString()
              .startsWith(this.getDatabankFormat().getIdString())) {
            // example
            // "LOCUS       NM_001080826            4726 bp    mRNA    linear   PRI 19-MAR-2014"
            try {
              sizeFound = getSequenceSize(line.toString(), " bp");
              if (sizeFound != -1) {
                this.currentSize = sizeFound;
                return testSize(this.currentSize);
              }
            } catch (Exception ex) {
              // we tried...
            }
          }
          break;
        case SwissProt:
          if (line.toString()
              .startsWith(this.getDatabankFormat().getIdString())) {
            // example "ID   M4K2_HUMAN              Reviewed;         820 AA."
            try {
              sizeFound = getSequenceSize(line.toString(), " "
                  + SwissProtParser.KEYWORD_NB_LETTERS + ".");
              if (sizeFound != -1) {
                this.currentSize = sizeFound;
                return testSize(this.currentSize);
              }
            } catch (Exception ex) {
              // we tried...
            }
          }
          break;
        default:
          break;
      }
    }
    return true;
  }

  @Override
  public boolean isActive() {
    return ((this.getMinSequenceSize() > 0 || this.getMaxSequenceSize() > 0) && ((this
        .getMinSequenceSize() <= this.getMaxSequenceSize()) || this
        .getMaxSequenceSize() == -1));
  }

  @Override
  public boolean startEntry() {
    currentSize = 0;
    return true;

  }

  @Override
  public boolean stopEntry() {
    analyseInProgress = false;
    return testSize(this.currentSize);
  }

  private boolean testSize(long size) {
    // test the min size
    if ((this.minSequenceSize != -1) && (size > 0)
        && (size < this.minSequenceSize)) {
      return false;
    }
    // test the max size
    if ((this.maxSequenceSize != -1) && (size > 0)
        && (size > this.maxSequenceSize)) {
      return false;
    }

    return true;
  }

  private long getSequenceSize(String line, String suffix) {
    int indexBp = line.indexOf(suffix);
    if (indexBp != -1) {
      // read from right to left
      StringBuffer size = new StringBuffer();
      char c;
      for (int index = indexBp - 1; index >= 0; index--) {
        c = line.charAt(index);
        if (Character.isDigit(c)) {
          size.insert(0, c);
        } else {
          break;
        }
      }
      return new Long(size.toString()).longValue();
    }
    return -1l;
  }

  /**
   * @return Examples : seqsize=1to2000 size=-1to800 size=500to-1
   */
  @Override
  public String toParametersForUnitTask() {
    StringBuffer result = new StringBuffer();
    if (this.isActive()) {
      result.append(PARAMETER_TERM);
      result.append("=");
      result.append(this.getMinSequenceSize());
      result.append(BOUNDARY_DELIMITER);
      result.append(this.getMaxSequenceSize());
    }
    return result.toString();
  }
}
