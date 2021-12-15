/* Copyright (C) 2007-2021 Patrick G. Durand
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
package bzh.plealog.dbmirror.fetcher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Compiler;
import org.apache.oro.text.regex.Perl5Matcher;

import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * This class defines a Name matcher. It is used to filter files given a file
 * name pattern based on Perl 5 regular expressions.
 * 
 * @author Patrick G. Durand
 */
public class NameMatcher {

  private Pattern[]        _includePatterns;
  private Pattern[]        _excludePatterns;
  private boolean          _initOk = false;

  private static final Logger LOGGER = LogManager.getLogger(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                           + ".NameMatcher");

  /**
   * Constructor taking as input two arrays of Perl 5 expressions. First array
   * specifies valid patterns, whereas second array specifies invalid patterns.
   */
  public NameMatcher(String[] includePatterns, String[] excludePatterns) {
    Perl5Compiler compiler;
    String pat;
    int i, size;

    if (includePatterns == null)
      return;
    size = includePatterns.length;
    if (size == 0)
      return;
    _includePatterns = new Pattern[size];
    compiler = new Perl5Compiler();
    for (i = 0; i < size; i++) {
      try {
        pat = includePatterns[i];
        _includePatterns[i] = compiler.compile(pat,
            Perl5Compiler.READ_ONLY_MASK);
      } catch (MalformedPatternException e) {
        LoggerCentral.error(LOGGER, "Invalid pattern: " + includePatterns[i]);
        return;
      }
    }
    _initOk = true;
    if (excludePatterns == null || excludePatterns.length == 0)
      return;
    size = excludePatterns.length;
    if (size == 0)
      return;
    _excludePatterns = new Pattern[size];
    compiler = new Perl5Compiler();
    for (i = 0; i < size; i++) {
      try {
        pat = excludePatterns[i];
        _excludePatterns[i] = compiler.compile(pat,
            Perl5Compiler.READ_ONLY_MASK);
      } catch (MalformedPatternException e) {
        LoggerCentral.error(LOGGER, "Invalid pattern: " + excludePatterns[i]);
        _initOk = false;
        return;
      }
    }
  }

  /**
   * Figures out if this matcher has been correctly initialized.
   */
  public boolean initialized() {
    return _initOk;
  }

  /**
   * Checks whether parameter fName matches a valid file name pattern.
   */
  public synchronized boolean match(String fName) {
    if (!_initOk)
      return false;
    Perl5Matcher matcher;
    int i;

    matcher = new Perl5Matcher();
    if (_excludePatterns != null) {
      for (i = 0; i < _excludePatterns.length; i++) {
        if (matcher.matches(fName, _excludePatterns[i])) {
          return false;
        }
      }
    }
    for (i = 0; i < _includePatterns.length; i++) {
      if (matcher.matches(fName, _includePatterns[i])) {
        return true;
      }
    }
    return false;
  }

}
