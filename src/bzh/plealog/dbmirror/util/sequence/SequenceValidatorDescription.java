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

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;

/**
 * A sequence validator which validate only sequences whose
 * 'searchedDescription' is inside the sequence description<br>
 * 
 * It is possible to look for a word approching the 'searchedDescription'
 * setting the 'exactSearch' property to false
 * 
 * @author Ludovic Antin
 * 
 */
public class SequenceValidatorDescription extends SequenceValidatorAbstract {

  public static final String PARAMETER_TERM         = "desc";     // desc=kinase@glucuronidase@!sapiens
                                                                   // => look
                                                                   // for kinase
                                                                   // or
                                                                   // glucuronidase
                                                                   // but never
                                                                   // sapiens
  public static final String PARAMETER_EXACT_TERM   = "exactdesc";

  public static String       KEYWORD_DELIMITER      = "@";
  public static String       NOT_CONTAINS_CHARACTER = "!";

  private String             searchedDescription    = "";
  private List<String>       mustContainsOneOfThem;
  private List<String>       mustNotContains;
  private boolean            exactSearch            = false;
  private boolean            analyseInProgress      = false;

  public boolean isExactSearch() {
    return exactSearch;
  }

  /**
   * @param exactSearch
   *          set to true if the search description must be exact in the
   *          sequence file <br>
   *          set to false, if the search description approching a description
   *          in the sequence file
   * 
   *          Aproching search method id the levensthein algorythm and it is
   *          used only if the search description contains one word
   */
  public void setExactSearch(boolean exactSearch) {
    this.exactSearch = exactSearch;
  }

  public SequenceValidatorDescription(String searchedDescription) {
    this.setSearchedDescription(searchedDescription);
  }

  public String getSearchedDescription() {
    return searchedDescription;
  }

  /**
   * 
   * @param searchedDescription
   * @see toParametersForUnitTask for 'searchedDescription' format
   */
  public void setSearchedDescription(String searchedDescription) {
    this.searchedDescription = searchedDescription;

    mustContainsOneOfThem = new ArrayList<String>();
    mustNotContains = new ArrayList<String>();
    // Cut to find keywords
    StringTokenizer tokens = new StringTokenizer(searchedDescription,
        KEYWORD_DELIMITER);
    String token;
    while (tokens.hasMoreTokens()) {
      token = tokens.nextToken().trim();
      if (token.startsWith(NOT_CONTAINS_CHARACTER)) {
        this.mustNotContains.add(token.substring(1));
      } else {
        this.mustContainsOneOfThem.add(token);
      }
    }
  }

  @Override
  public boolean startSequence() {
    analyseInProgress = false;
    return true;
  }

  @Override
  public boolean analyseLine(StringBuilder line) {
    boolean result = true;
    if ((analyseInProgress)
        && (line.toString().startsWith(this.databankFormat
            .getBeginDescriptionString()))) {
      result = (this.mustContainsOneOfThem.size() == 0);
      // test keyword which can be in the description
      for (String keyword : this.mustContainsOneOfThem) {
        if (this.contains(line.toString(), keyword)) {
          result = true;
          break;
        }
      }
      // test keywords which must NOT be in the description
      for (String keyword : this.mustNotContains) {
        if (this.contains(line.toString(), keyword)) {
          return false;
        }
      }
    }
    return result;
  }

  private boolean contains(String line, String searchKeyword) {
    boolean result = line.toUpperCase().contains(searchKeyword.toUpperCase());
    // is the searched keyword incorrect ?
    // we try to see if a word in the line is near
    // only if the searched description contains one word
    if ((!this.isExactSearch()) && (!result) && (!searchKeyword.contains(" "))) {
      StringTokenizer tokens = new StringTokenizer(line, " ");
      String token = null;
      int distance = 100;
      while (tokens.hasMoreTokens()) {
        token = tokens.nextToken();
        distance = StringUtils.getLevenshteinDistance(token, searchKeyword);
        if (distance <= 3) {
          return true;
        }
      }
    }
    return result;
  }

  @Override
  public boolean isActive() {
    return (StringUtils.isNotBlank(this.searchedDescription));
  }

  @Override
  public boolean startEntry() {
    analyseInProgress = true;
    return true;
  }

  @Override
  public boolean stopEntry() {
    return true;
  }

  @Override
  /**
   * @ return examples : desc=kinase@glucuronidase@!sapiens;exactdesc=true => look for kinase or glucuronidase but never sapiens
   */
  public String toParametersForUnitTask() {
    StringBuffer result = new StringBuffer();
    if (this.isActive()) {
      result.append(PARAMETER_TERM);
      result.append("=");
      result.append(this.searchedDescription);
      result.append(";");
      result.append(PARAMETER_EXACT_TERM);
      result.append("=");
      result.append(this.exactSearch);
    }
    return result.toString();
  }
}
