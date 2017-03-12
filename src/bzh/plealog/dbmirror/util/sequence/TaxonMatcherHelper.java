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

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;

import bzh.plealog.dbmirror.lucenedico.DicoTermQuerySystem;
import bzh.plealog.dbmirror.lucenedico.DicoUtils;
import bzh.plealog.dbmirror.lucenedico.Dicos;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.conf.DBMirrorConfig;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptorUtils;
import bzh.plealog.dbmirror.util.log.LoggerCentral;
import bzh.plealog.dbmirror.util.xref.DBXrefTagManager;

/**
 * This class is used to figure our whether or not a particular taxon ID matches
 * some constraints. To use this class, proceed as follows: instantiate it, call
 * setTaxonomyFilter, then call the method initTaxonMatcher. Then, you can call
 * isSeqTaxonvalid as many times as needed. When job is finished, do not forget
 * to call closeTaxonMatcher.
 * 
 * @author Patrick G. Durand
 */
public class TaxonMatcherHelper {
  private HashSet<String>                 _taxInclude;
  private HashSet<String>                 _taxExclude;
  private Hashtable<String, TaxonCounter> _taxNotFound    = new Hashtable<String, TaxonCounter>();
  private DicoTermQuerySystem             _termRepository = null;
  private DBXrefTagManager                _xrefManager    = null;

  public static final String              ERR1            = "cannot use taxonomy constraints without NCBI Taxonomy Classification installed.";

  public static boolean isNCBITaxonomyInstalled() {
    DBMirrorConfig mirrorConfig = DBDescriptorUtils
        .getDBMirrorConfig(DBMSAbstractConfig.getLocalMirrorConfFile());
    DicoTermQuerySystem termRepository = DicoTermQuerySystem
        .getDicoTermQuerySystem(mirrorConfig);
    return termRepository.hasDicoAvailable(Dicos.NCBI_TAXONOMY);
  }

  private HashSet<String> handleData(String tax) {
    StringTokenizer tokenizer;
    HashSet<String> set;

    set = new HashSet<String>();
    tokenizer = new StringTokenizer(tax, ",.");
    while (tokenizer.hasMoreTokens()) {
      set.add(DicoUtils.TAX_ID_NAME_PREFIX + tokenizer.nextToken().trim());
    }
    return set;
  }

  /**
   * Provides the constraints.
   * 
   * @param taxInclude
   *          dot or comma separated list of valid taxonomy ID
   * @param taxExclude
   *          dot or comma separated list of invalid taxonomy ID
   * */
  public void setTaxonomyFilter(String taxInclude, String taxExclude) {
    if ((StringUtils.isBlank(taxInclude))
        && (StringUtils.isNotBlank(taxExclude))) {
      taxInclude = "1";
    }

    if (taxInclude != null) {
      _taxInclude = handleData(taxInclude);
    }

    if (taxExclude != null) {
      _taxExclude = handleData(taxExclude);
    }
  }

  /**
   * Check a taxon ID value.
   * 
   * @param values
   *          contains the results of this method. Must be an allocated array of
   *          three booleans. First boolean is set to true when this method
   *          detects a taxon db_xref. Second boolean is set to true if the xref
   *          validates the constraints. Third boolean is set to true if the
   *          taxon ID cannot be found in the dictionary.
   * @param taxonID
   *          ID a taxonomy ID
   * */
  public void isTaxonvalid(boolean[] values, String taxonID) {
    boolean seqOk, foundInInclude, foundInExclude;
    StringTokenizer tokenizer;
    String taxPath, token;
    TaxonCounter counter;

    values[0] = false;
    values[1] = true;

    if (_termRepository == null) {
      return;
    }
    seqOk = false;
    if (taxonID != null) {
      values[0] = true;
      taxPath = _termRepository.getTaxPathIds(taxonID, false);
      if (taxPath != null) {
        values[2] = true;
        taxPath += DicoUtils.TAX_ID_NAME_PREFIX;
        taxPath += taxonID;
        foundInInclude = foundInExclude = false;
        tokenizer = new StringTokenizer(taxPath, ";");
        while (tokenizer.hasMoreTokens()) {
          token = tokenizer.nextToken();
          if (_taxInclude != null && _taxInclude.contains(token)) {
            foundInInclude = true;
          }
          if (_taxExclude != null && _taxExclude.contains(token)) {
            foundInExclude = true;
          }
        }
        seqOk = (foundInInclude && !foundInExclude);
      } else {
        if (_taxNotFound.containsKey(taxonID)) {
          counter = _taxNotFound.get(taxonID);
          counter.increment();
        } else {
          _taxNotFound.put(taxonID, new TaxonCounter(taxonID, 1));
        }
        values[2] = false;
      }
    }
    values[1] = seqOk;
  }

  /**
   * Check a db_xref value. This method checks if the db_xref starts with taxon
   * string, then it validates it against constraints
   * 
   * @param values
   *          contains the results of this method. Must be an allocated array of
   *          three booleans. First boolean is set to true when this method
   *          detects a taxon db_xref. Second boolean is set to true if the xref
   *          validates the constraints. Third boolean is set to true if the
   *          taxon ID cannot be found in the dictionary.
   * @param xref
   *          a taxonomy cross-reference, i.e. a string in the form taxon:xxx
   *          where xxx is a taxonomy ID
   * */
  public void isSeqTaxonvalid(boolean[] values, String xref) {
    if (xref != null && xref.startsWith(Dicos.NCBI_TAXONOMY.xrefId)) {
      isTaxonvalid(values, xref.substring(xref.indexOf(':') + 1));
    } else {
      values[0] = false;
      values[1] = true;
      values[2] = false;
    }
  }

  /**
   * Check a db_xref value. This method checks if the db_xref starts with taxon,
   * then it validates it against constraints
   * 
   * @param line
   *          a data line to analyse. Processing is done using a
   *          DBXrefTagManager object.
   * @param values
   *          contains the results of this method. First boolean is set to true
   *          when this method detects a taxon db_xref. Second boolean is set to
   *          true if the xref validates the constraints.
   * */
  public void isSeqTaxonvalid(String line, boolean[] values) {
    isSeqTaxonvalid(values, _xrefManager.getDbXref(line));
  }

  public void setDicoTermQuerySystem(DicoTermQuerySystem termRepository) {
    _termRepository = termRepository;
  }

  /**
   * This method can be called to dump in a logger the list of taxon that
   * coudn't be called during the use of this TaxonMatherHelper.
   */
  public void dumpTaxonNotFound(Log logger) {
    Enumeration<String> keys;
    int size;

    size = _taxNotFound.size();
    if (size == 0) {
      return;
    }
    LoggerCentral.info(logger, "List of taxons not found: " + size);
    keys = _taxNotFound.keys();
    while (keys.hasMoreElements()) {
      LoggerCentral.info(logger, "  "
          + _taxNotFound.get(keys.nextElement()).toString());
    }
  }

  /**
   * Call this method to initialize the TaxonMatcher. Basically, this method
   * creates a DicoTermQuerySystem and a DBXrefTagManager using the KDMS default
   * configuration.
   */
  public void initTaxonMatcher() {
    DBMirrorConfig mirrorConfig;
    if (_taxInclude != null || _taxExclude != null) {
      mirrorConfig = DBDescriptorUtils.getDBMirrorConfig(DBMSAbstractConfig
          .getLocalMirrorConfFile());
      if (mirrorConfig != null)
        _termRepository = DicoTermQuerySystem
            .getDicoTermQuerySystem(mirrorConfig);
      // dico NCBI Taxonomy not available
      if (_termRepository.hasDicoAvailable(Dicos.NCBI_TAXONOMY) == false)
        _termRepository = null;
      else
        _xrefManager = new DBXrefTagManager(
            DBMSAbstractConfig.getDbXrefTagConfiguration());
    }
  }

  /**
   * Figures out if the NCBI Taxonomy index is available.
   */
  public boolean hasTaxonomyAvailable() {
    if (_termRepository != null) {
      return _termRepository.hasDicoAvailable(Dicos.NCBI_TAXONOMY);
    } else {
      return false;
    }
  }

  /**
   * Figures out if the taxonMatcher has to be used.
   */
  public boolean hasTaxoConstraints() {
    if (_taxInclude != null || _taxExclude != null)
      return true;
    else
      return false;
  }

  /**
   * Call this method when you have finished to use this taxon matcher. This
   * method will properly close the created DicoTermQuerySystem.
   */
  public void closeTaxonMatcher() {
    DicoTermQuerySystem.closeDicoTermQuerySystem();
  }

  private class TaxonCounter {
    private String id;
    private int    times;

    public TaxonCounter(String id, int times) {
      super();
      this.id = id;
      this.times = times;
    }

    public void increment() {
      times++;
    }

    public String toString() {
      return "taxon " + id + ": not found " + times + " times";
    }
  }
}
