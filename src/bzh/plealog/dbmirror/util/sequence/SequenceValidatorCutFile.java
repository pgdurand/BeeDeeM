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

/**
 * A sequence validator which validate only sequences whose number is between
 * 'from' and 'to' <br>
 * Set -1 to 'from' if you just want a 'to' value <br>
 * Set -1 to 'to' if you just want a 'from' value
 * 
 * @author Ludovic Antin
 * 
 */
public class SequenceValidatorCutFile extends SequenceValidatorAbstract {

  public static String PARAMETER_TERM     = "cut";
  public static String BOUNDARY_DELIMITER = "to";

  // cut file parameters
  private int          cutFrom            = -1;
  private int          cutTo              = -1;

  private int          nbSequences        = 0;

  public SequenceValidatorCutFile(int from, int to) {
    this.setCutFrom(from);
    this.setCutTo(to);
  }

  /**
   * Initialize a new instance with a string which format is FROMtoTO where
   * 'FROM' and 'TO' must be int values. Examples : 1to2000 or -1to500 or 2to-1
   * 
   * @param args
   */
  public SequenceValidatorCutFile(String args) throws NumberFormatException {
    int index = args.toLowerCase().indexOf(BOUNDARY_DELIMITER);
    if (index != -1) {
      this.setCutFrom(Integer.parseInt(args.substring(0, index).trim()));
      this.setCutTo(Integer.parseInt(args.substring(
          index + BOUNDARY_DELIMITER.length()).trim()));
    }
  }

  public int getCutFrom() {
    return this.cutFrom;
  }

  public void setCutFrom(int cutFrom) {
    this.cutFrom = cutFrom;
  }

  public int getCutTo() {
    return cutTo;
  }

  public void setCutTo(int cutTo) {
    this.cutTo = cutTo;
  }

  @Override
  public boolean startSequence() {
    return true;
  }

  @Override
  public boolean analyseLine(StringBuilder line) {
    return true;
  }

  @Override
  public boolean isActive() {
    return ((this.getCutFrom() > 0 || this.getCutTo() > 0) && ((this
        .getCutFrom() <= this.getCutTo()) || this.getCutTo() == -1));

  }

  @Override
  public boolean startEntry() {
    // new entry
    this.nbSequences++;
    // check from
    if ((this.cutFrom != -1) && (this.nbSequences != 0)
        && (this.nbSequences < this.cutFrom)) {
      return false;
    }
    // check to
    if ((this.cutTo != -1) && (this.nbSequences != 0)
        && (this.nbSequences > this.cutTo)) {
      return false;
    }
    return true;
  }

  @Override
  public boolean stopEntry() {
    return true;
  }

  @Override
  /**
   * @ return Examples : cut=1to2000 cut=-1to800 cut=500to-1
   */
  public String toParametersForUnitTask() {
    StringBuffer result = new StringBuffer();
    if (this.isActive()) {
      result.append(PARAMETER_TERM);
      result.append("=");
      result.append(this.getCutFrom());
      result.append(BOUNDARY_DELIMITER);
      result.append(this.getCutTo());
    }
    return result.toString();
  }

}
