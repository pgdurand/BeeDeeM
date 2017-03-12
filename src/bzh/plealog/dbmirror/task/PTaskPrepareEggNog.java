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
package bzh.plealog.dbmirror.task;

import java.io.File;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import bzh.plealog.dbmirror.fetcher.DBServerConfig;
import bzh.plealog.dbmirror.fetcher.UserProcessingMonitor;
import bzh.plealog.dbmirror.indexer.eggnog.EggNogPreparator;
import bzh.plealog.dbmirror.util.Utils;

/**
 * This task will prepare the installation of an eggnog db.
 * 
 * It will create the lucene index which associates
 * "taxon id <=> nog <=> sequence" and the three uniprot files for all
 * superkingdoms
 * 
 * @author Ludovic Antin
 * 
 */
public class PTaskPrepareEggNog extends PAbstractTask {

  private static final String   PARAM_MEMBERS_FILE_PATH      = "members";
  private static final String   PARAM_DESCRIPTIONS_FILE_PATH = "descriptions";
  private static final String   PARAM_FUNCCATS_FILE_PATH     = "funccats";
  private static final String   PARAM_SEQUENCES_FILE_PATH    = "sequences";

  private String                errorMsg                     = null;
  private DBServerConfig        config                       = null;

  private File                  membersTarFile               = null;
  private File                  descriptionsTarFile          = null;
  private File                  funcCatsTarFile              = null;

  private UserProcessingMonitor monitor;

  public PTaskPrepareEggNog(DBServerConfig config) {
    this.config = config;
  }

  @Override
  public String getName() {
    return "EggNogPreparation";
  }

  @Override
  public String getUserFriendlyName() {
    return "preparing EggNog databanks and dictionary";
  }

  @Override
  public void setParameters(String params) {

    if (StringUtils.isBlank(params)) {
      this.errorMsg = "Unable to prepare the EggNog databanks because of no parameters provided for the '"
          + PTask.TASK_G_NOG_PREPARE + "' task";
      return;
    }

    Map<String, String> args = Utils.getTaskArguments(params);

    // the tar files
    this.membersTarFile = readParameter(args, PARAM_MEMBERS_FILE_PATH);
    this.descriptionsTarFile = readParameter(args, PARAM_DESCRIPTIONS_FILE_PATH);
    this.funcCatsTarFile = readParameter(args, PARAM_FUNCCATS_FILE_PATH);
    // the sequence file stored in a static value to be reused by other indexers
    EggNogPreparator.sequencesFile = readParameter(args,
        PARAM_SEQUENCES_FILE_PATH);

  }

  private File readParameter(Map<String, String> args, String paramName) {
    String value = args.get(paramName);
    if (StringUtils.isBlank(value)) {
      this.errorMsg = "Unable to prepare the EggNog databanks because of no parameter '"
          + paramName
          + "' provided for the '"
          + PTask.TASK_G_NOG_PREPARE
          + "' task";
      return null;
    } else {
      return new File(this.config.getLocalTmpFolder(), value);
    }
  }

  @Override
  public String getErrorMsg() {
    return this.errorMsg;
  }

  @Override
  public boolean execute() {
    if (StringUtils.isNotBlank(this.errorMsg)) {
      return false;
    }

    // first : prepare the lucene index
    EggNogPreparator indexer = new EggNogPreparator(this.membersTarFile,
        this.descriptionsTarFile, this.funcCatsTarFile, this.config,
        this.monitor);
    try {
      indexer.execute();
    } catch (Exception e) {
      this.errorMsg = e.getMessage();
      return false;
    }

    return true;
  }

  public void setMonitor(UserProcessingMonitor monitor) {
    this.monitor = monitor;
  }

}
