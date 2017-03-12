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

import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;

public class SequenceValidatorRenamer extends SequenceValidatorAbstract {

  private boolean updateIds                      = false;
  private long    firstId                        = 0;
  private String  idPrefix                       = "";
  private String  idSuffix                       = "";

  private boolean updateDescriptions             = false;
  private String  newDescription                 = "";
  private boolean usePreviousId                  = false;

  private boolean analyseInProgress              = false;
  private String  currentEntryId                 = "";
  // set to true if the id has been updated for the current entry
  private boolean currentEntryIdUpdated          = false;
  // idem for the description
  private boolean currentEntryDescriptionUpdated = false;
  // to ignore next description lines
  private boolean ignoreNextDescriptionLines     = false;
  // real id used and incremented during process
  private long    nextId                         = 0;

  public boolean isUpdateIds() {
    return updateIds;
  }

  public void setUpdateIds(boolean updateIds) {
    this.updateIds = updateIds;
  }

  public String getIdPrefix() {
    return idPrefix;
  }

  public void setIdPrefix(String idPrefix) {
    if (idPrefix == null) {
      idPrefix = "";
    }
    idPrefix = idPrefix.replace(' ', '_').replace('\t', '_');
    this.idPrefix = idPrefix;
  }

  public String getIdSuffix() {
    return idSuffix;
  }

  public void setIdSuffix(String idSuffix) {
    if (idSuffix == null) {
      idSuffix = "";
    }
    idSuffix = idSuffix.replace(' ', '_').replace('\t', '_');
    this.idSuffix = idSuffix;
  }

  public boolean isUpdateDescriptions() {
    return updateDescriptions;
  }

  public void setUpdateDescriptions(boolean updateDescriptions) {
    this.updateDescriptions = updateDescriptions;
  }

  public String getNewDescription() {
    return newDescription;
  }

  public void setNewDescription(String newDescription) {
    if (newDescription == null) {
      newDescription = "";
    }
    this.newDescription = newDescription;
  }

  public boolean isUsePreviousId() {
    return usePreviousId;
  }

  public void setUsePreviousId(boolean usePreviousId) {
    this.usePreviousId = usePreviousId;
  }

  public long getFirstId() {
    return firstId;
  }

  public void setFirstId(long firstId) {

    this.firstId = firstId;
    // because of the first increment : substract 1
    this.nextId = this.firstId - 1;
  }

  /**
   * 
   * @return the next id for the next entry
   */
  public long getNextId() {
    // because of the automatic increment in nextEntry() method : add 1
    return this.nextId + 1;
  }

  @Override
  public boolean startEntry() {

    // first id is 1
    this.nextId++;

    this.analyseInProgress = true;
    this.currentEntryIdUpdated = false;
    this.currentEntryDescriptionUpdated = false;
    this.ignoreNextDescriptionLines = false;
    this.currentEntryId = "";

    // allways write entry in the filtered file
    return true;
  }

  @Override
  public boolean stopEntry() {
    // allways write entry in the filtered file
    return true;
  }

  @Override
  public boolean startSequence() {

    // do not need to analyse the sequence
    this.analyseInProgress = false;
    // allways write entry in the filtered file
    return true;
  }

  @Override
  public boolean analyseLine(StringBuilder line) {
    if (this.analyseInProgress) {

      if (StringUtils.isBlank(this.currentEntryId)) {
        // get the id
        this.currentEntryId = SeqIOUtils.getID(line.toString(), this
            .getDatabankFormat().getIdString());
      }
      String newId = this.currentEntryId;

      // should we update the id ?
      if (this.isUpdateIds() && !this.currentEntryIdUpdated) {

        if (StringUtils.isNotBlank(this.currentEntryId)) {
          int index = line.indexOf(this.currentEntryId);
          newId = this.getIdPrefix() + String.valueOf(this.nextId)
              + this.getIdSuffix();
          line.replace(index, index + this.currentEntryId.length(), newId);

          // update the id : done
          this.currentEntryIdUpdated = true;
        }
      }

      // should we update the description ?
      if (this.isUpdateDescriptions()) {
        if (!this.currentEntryDescriptionUpdated
            && line.toString().startsWith(
                this.getDatabankFormat().getBeginDescriptionString())) {
          // the new description
          String newDescription = this.currentEntryId;
          if (!this.isUsePreviousId()) {
            newDescription = this.getNewDescription();
          }

          if (this.getDatabankFormat().getBeginDescriptionString()
              .equalsIgnoreCase(this.getDatabankFormat().getIdString())) { // update
                                                                           // the
                                                                           // line
                                                                           // with
                                                                           // id
                                                                           // and
                                                                           // desc

            line.delete(0, line.length());
            line.append(this.getDatabankFormat().getIdString());
            line.append(newId);
            line.append(" ");
            line.append(newDescription);

          } else {// replace the old description with the new one

            // get the index of the first char
            int firstCharIndex = -1;
            int index = -1;
            for (index = this.getDatabankFormat().getBeginDescriptionString()
                .length(); index < line.length(); index++) {
              if (!Character.isSpaceChar(line.charAt(index))) {
                firstCharIndex = index;
                break;
              }
            }
            if (firstCharIndex != -1) {
              // delete description before adding the new one
              line.delete(firstCharIndex, line.length());
            } else {
              // no desc
              if (index == this.getDatabankFormat().getBeginDescriptionString()
                  .length()) {
                // add a space char
                line.append(" ");
              }
            }
            line.append(newDescription);
          }

          this.currentEntryDescriptionUpdated = true;
          this.ignoreNextDescriptionLines = true;

        } else { // description has been updated

          if (this.ignoreNextDescriptionLines
              && line.toString().startsWith(
                  this.getDatabankFormat().getContinueDescriptionString())) {
            // we are still in the description : ignore this line
            line.delete(0, line.length());
          } else {
            this.ignoreNextDescriptionLines = false;
          }
        }
      }

    }
    // allways write entry in the filtered file
    return true;
  }

  @Override
  public boolean isActive() {
    boolean isActiveForId = (this.isUpdateIds() && (this.getFirstId() >= -1));

    return this.isUpdateDescriptions() || isActiveForId;
  }

  public static String PARAMETER_TERM          = "rename";
  public static String PARAMETER_ID_TERM       = "id";
  public static String PARAMETER_ID_PREFIX     = "prefix";
  public static String PARAMETER_ID_SUFFIX     = "suffix";
  public static String PARAMETER_ID_NUMBER     = "first";
  public static String PARAMETER_DESC_TERM     = "desc";
  public static String PARAMETER_DESC_PREVIOUS = "previousId";
  public static String PARAMETER_DESC_TEXT     = "text";
  public static String PARAMETER_SEPARATOR     = "+";

  public SequenceValidatorRenamer() {

  }

  /**
   * Initialize a new instance using a string formatted as expected
   * 
   * @param args
   * @see toParametersForUnitTask
   */
  public SequenceValidatorRenamer(String args) throws Exception {
    try {
      int indexEnd = -1;
      StringTokenizer parameters = null;
      String parameter = null;
      int indexStart = args.indexOf(PARAMETER_ID_TERM + "[");
      if (indexStart != -1) {
        this.setUpdateIds(true);
        indexEnd = args.indexOf(']', indexStart);
        parameters = new StringTokenizer(args.substring(indexStart + 1
            + PARAMETER_ID_TERM.length(), indexEnd), PARAMETER_SEPARATOR);
        while (parameters.hasMoreTokens()) {
          parameter = parameters.nextToken().trim();
          if (parameter.startsWith(PARAMETER_ID_PREFIX)) {
            this.setIdPrefix((parameter.substring(parameter.indexOf('=') + 1))
                .trim());
          } else if (parameter.startsWith(PARAMETER_ID_SUFFIX)) {
            this.setIdSuffix((parameter.substring(parameter.indexOf('=') + 1))
                .trim());
          } else if (parameter.startsWith(PARAMETER_ID_NUMBER)) {
            this.setFirstId(Long.valueOf((parameter.substring(parameter
                .indexOf('=') + 1)).trim()));
          }
        }
      }

      indexStart = args.indexOf(PARAMETER_DESC_TERM + "[");
      if (indexStart != -1) {
        this.setUpdateDescriptions(true);
        indexEnd = args.indexOf(']', indexStart);
        parameters = new StringTokenizer(args.substring(indexStart + 1
            + PARAMETER_DESC_TERM.length(), indexEnd), PARAMETER_SEPARATOR);
        while (parameters.hasMoreTokens()) {
          parameter = parameters.nextToken().trim();
          if (parameter.startsWith(PARAMETER_DESC_PREVIOUS)) {
            this.setUsePreviousId(Boolean.valueOf(parameter.substring(
                parameter.indexOf('=') + 1).trim()));
          } else if (parameter.startsWith(PARAMETER_DESC_TEXT)) {
            this.setNewDescription((parameter.substring(parameter.indexOf('=') + 1))
                .trim());
          }
        }
      }
    } catch (Exception ex) {
      throw new Exception("bad format for renaming task (" + args + ") : "
          + ex.getMessage());
    }
  }

  @Override
  /**
   * @return Example : rename=id[prefix=my prefix+suffix=my suffix+first=158]
   * 					 rename=desc[previousId=true]
   * 					 rename=desc[previousId=false+text=my new description]
   * 					 rename=id[first=12]desc[text=new desc] 
   */
  public String toParametersForUnitTask() {
    StringBuilder result = new StringBuilder(PARAMETER_TERM);
    result.append('=');

    if (this.isUpdateIds()) {
      result.append(PARAMETER_ID_TERM);
      result.append("[");
      this.addToParameter(result, PARAMETER_ID_PREFIX, this.idPrefix);
      this.addToParameter(result, PARAMETER_ID_SUFFIX, this.idSuffix);
      this.addToParameter(result, PARAMETER_ID_NUMBER,
          String.valueOf(this.firstId));
      result.append(']');
    }
    if (this.isUpdateDescriptions()) {
      result.append(PARAMETER_DESC_TERM);
      result.append("[");
      this.addToParameter(result, PARAMETER_DESC_PREVIOUS,
          String.valueOf(this.isUsePreviousId()));
      this.addToParameter(result, PARAMETER_DESC_TEXT, this.getNewDescription());
      result.append(']');
    }

    return result.toString();
  }

  private void addToParameter(StringBuilder parameters, String name,
      String value) {
    if (StringUtils.isNotBlank(value)) {
      parameters.append(name);
      parameters.append('=');
      parameters.append(value.trim());
      parameters.append(PARAMETER_SEPARATOR);
    }
  }
}
