/* Copyright (C) 2007-2022 Patrick G. Durand
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;

/**
 * This interface defines a task.
 * 
 * @author Patrick G. Durand
 */
public interface PTask {
  // unit tasks
  public static final String TASK_U_GUNZIP           = "gunzip";
  public static final String TASK_U_UNTAR            = "untar";
  public static final String TASK_U_GB_IDX           = "idxgb";
  public static final String TASK_U_SW_IDX           = "idxsw";
  public static final String TASK_U_GP_IDX           = "idxgp";
  public static final String TASK_U_EM_IDX           = "idxem";
  public static final String TASK_U_DICO_IDX         = "idxdico";
  public static final String TASK_U_FAS_IDX          = "idxfas";
  public static final String TASK_U_NOG_IDX          = "idxnog";
  public static final String TASK_U_CONVSPB          = "convspb";
  public static final String TASK_U_BOLD2GB          = "bold2gb";
  public static final String TASK_U_EXTSCRIPT        = "script";
  
  // global tasks
  public static final String TASK_G_FORMATDB         = "formatdb";
  public static final String TASK_G_DELETEGZ         = "delgz";
  public static final String TASK_G_DELETETAR        = "deltar";
  public static final String TASK_G_DELETETMPIDX     = "deltmpidx";
  public static final String TASK_G_MAKEALIAS        = "makealias";
  public static final String TASK_G_NOTINSTALLINPROD = "noiip";
  public static final String TASK_G_NOG_PREPARE      = "eggnog";
  public static final String TASK_G_RELDATE          = "reldate";
  public static final String TASK_G_EXTSCRIPT        = "script";
  
  // special common arguments
  public static final String TAX_INCLUDE             = "taxinc";
  public static final String TAX_EXCLUDE             = "taxexc";
  public static final String CHECK_NR                = "nr";

  // task done with success (to enable easiest resume of bank installation)
  public static final String TASK_OK_FEXT            = ".T_OK";
  
  // system task (e.g. debug, test, etc)
  public static final String TASK_FORCE_STOP         = "stop";
  
  public static final Log    LOGGER = LogFactory.getLog(
      DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY + ".PTaskEngine");
  /**
   * Returns the name of the task. For internal use.
   */
  public String getName();

  /**
   * Sets the name of the DBServer Config to which belongs this task.
   */
  public void setDbConfName(String name);

  /**
   * Gets the name of the DBServer Config to which belongs this task.
   */
  public String getDbConfName();

  /**
   * Returns the name of the task. For user display.
   */
  public String getUserFriendlyName();

  /**
   * Sets the parameters of this tasks.
   */
  public void setParameters(String params);

  /**
   * Returns the error message. This method is intended to be called just after
   * calling execute().
   */
  public String getErrorMsg();

  /**
   * Starts this task execution.
   * 
   * @return true if execution succeeded, false otherwise. In case of failure,
   *         one can call getErrorMsg to get some information of what happened.
   */
  public boolean execute();
}
