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
package bzh.plealog.dbmirror.fetcher;

import java.io.File;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.indexer.BOLDParser;
import bzh.plealog.dbmirror.indexer.LuceneUtils;
import bzh.plealog.dbmirror.lucenedico.task.PTaskDicoIndexer;
import bzh.plealog.dbmirror.task.PTask;
import bzh.plealog.dbmirror.task.PTaskBold2Genbank;
import bzh.plealog.dbmirror.task.PTaskDeleteFiles;
import bzh.plealog.dbmirror.task.PTaskEggNogIndexer;
import bzh.plealog.dbmirror.task.PTaskEndProcessing;
import bzh.plealog.dbmirror.task.PTaskEngine;
import bzh.plealog.dbmirror.task.PTaskExecScript;
import bzh.plealog.dbmirror.task.PTaskFastaRenamer;
import bzh.plealog.dbmirror.task.PTaskFormatDB;
import bzh.plealog.dbmirror.task.PTaskGunzip;
import bzh.plealog.dbmirror.task.PTaskLuceneDirMerge;
import bzh.plealog.dbmirror.task.PTaskLuceneFastaIndexer;
import bzh.plealog.dbmirror.task.PTaskLuceneGBIndexer;
import bzh.plealog.dbmirror.task.PTaskLuceneSWIndexer;
import bzh.plealog.dbmirror.task.PTaskMakeBlastAlias;
import bzh.plealog.dbmirror.task.PTaskPrepareEggNog;
import bzh.plealog.dbmirror.task.PTaskReleaseDate;
import bzh.plealog.dbmirror.task.PTaskUntar;
import bzh.plealog.dbmirror.task.PTaskUntarTgz;
import bzh.plealog.dbmirror.task.PTaskUnzip;
import bzh.plealog.dbmirror.util.Formatters;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * This class monitors the processing of database files.
 * 
 * @author Patrick G. Durand
 */
public class DefaultLoaderMonitor implements LoaderMonitor {
  private PTaskEngine      _taskEngine;
  private DBServerConfig    _dbConf;
  private ArrayList<String> _luceneDirs;
  private ArrayList<String> _formatDBfiles;
  // nb. of sequences to retrieve
  private int               _files;
  // nb. of sequences successfully downloaded
  private int               _fileCounter;

  private static final Log  LOGGER = LogFactory
                                       .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                           + ".PFTPLoaderSystem");

  /**
   * Constructor.
   * 
   * @param taskEngine
   *          the task engine
   * @param dbConf
   *          the object containing the database configuration
   * @param nbFilesToRetrieve
   *          the total nb of files to process
   */
  public DefaultLoaderMonitor(PTaskEngine taskEngine, DBServerConfig dbConf,
      int nbFilesToRetrieve) {
    _taskEngine = taskEngine;
    _dbConf = dbConf;
    _files = nbFilesToRetrieve;
    _luceneDirs = new ArrayList<String>();
    _formatDBfiles = new ArrayList<String>();
  }

  /**
   * Implementation of LoaderMonitor interface.
   */
  public void beginLoading(String fName) {
    LoggerCentral.info(LOGGER, "Start downloading: " + fName);
  }

  /**
   * Utility method used to retrieve the absolute path pointing to the directory
   * where the database will be locally installed.
   */
  private String getFullDBPathName() {
    String str;
    int idx;

    str = _dbConf.getName();
    idx = str.lastIndexOf('|');
    if (idx != -1)
      str = str.substring(idx + 1);
    return _dbConf.getLocalTmpFolder() + str;

  }

  /**
   * Starts the tasks processing downloaded files.
   */
  private void startPostGlobalTasks(String fName) {
    StringTokenizer tokenizer;
    String str, tasks, task, dicoPath, aName;

    // Then we manage user provided tasks if any
    tasks = _dbConf.getGlobalPostTasks();
    if (tasks == null)
      return;

    // check for any global idxdico tasks
    tokenizer = new StringTokenizer(tasks, ",");
    while (tokenizer.hasMoreTokens()) {
      task = tokenizer.nextToken();
      if (task.indexOf(PTask.TASK_U_DICO_IDX) >= 0) {
        // this code is intended to be used with EC dico only. For other
        // dico type
        // it'll be required to adapt this code.
        dicoPath = _dbConf.getLocalTmpFolder();
        if (fName.endsWith(".gz")) {
          aName = dicoPath + fName.substring(0, fName.length() - 3);
        } else {
          aName = dicoPath + fName;
        }
        // for dictionary task located in global task, we have to check
        // if
        // we have a particular file to index.
        String args = getTaskParameters(task, PTask.TASK_U_DICO_IDX);
        if (args != null) {
          String value = Utils.getTaskArguments(args).get(
              PTaskDicoIndexer.FILE_NAME);
          if (value != null) {
            aName = dicoPath + value;
          }
        }
        _luceneDirs.add(aName + LuceneUtils.DIR_OK_FEXT);
        PTaskDicoIndexer dicoTask = new PTaskDicoIndexer(aName);
        dicoTask.setParameters(args);
        _taskEngine.addTask(dicoTask, _dbConf.getName());
      }
    }
    // start Lucene indexes merging only when all individual files
    // have been indexed
    if (_luceneDirs.size() == _files) {
      str = getFullDBPathName();
      _taskEngine.addTask(new PTaskLuceneDirMerge(str, _luceneDirs),
          _dbConf.getName());
    }

    // Handle all other tasks
    tokenizer = new StringTokenizer(tasks, ",");
    while (tokenizer.hasMoreTokens()) {
      task = tokenizer.nextToken();
      // eggnog
      if (task.contains(PTask.TASK_G_NOG_PREPARE)) {
        PTaskPrepareEggNog eggNogTask = new PTaskPrepareEggNog(_dbConf);
        eggNogTask.setParameters(getTaskParameters(task,
            PTask.TASK_G_NOG_PREPARE));
        _taskEngine.addTask(eggNogTask, _dbConf.getName());
      }
      else if (task.indexOf(PTask.TASK_G_FORMATDB) >= 0) {
        // note: to figure out if we have to save total entries during
        // formatdb task
        // we rely on Lucene indexes: if some have been created, then we
        // already have
        // that information
        PTaskFormatDB fTask = new PTaskFormatDB(_formatDBfiles,
            getFullDBPathName(), _dbConf.isNucleic());
        fTask.setParameters(getTaskParameters(task, PTask.TASK_G_FORMATDB));
        _taskEngine.addTask(fTask, _dbConf.getName());
      }
      
      // user provided external script
      else if (task.indexOf(PTask.TASK_G_EXTSCRIPT) >= 0) {
        PTaskExecScript execTask = new PTaskExecScript(
            _dbConf.getLocalTmpFolder(), null, _dbConf.getName(), _dbConf.getTypeCode());
        execTask.setParameters(getTaskParameters(task, PTask.TASK_G_EXTSCRIPT));
        _taskEngine.addTask(execTask, _dbConf.getName());
      }
  
      //cleaning tasks if any are required
      else if (task.indexOf(PTask.TASK_G_DELETEGZ) >= 0) {
        _taskEngine.addTask(new PTaskDeleteFiles(_dbConf.getLocalTmpFolder(),
            "*.gz"), _dbConf.getName());
        _taskEngine.addTask(new PTaskDeleteFiles(_dbConf.getLocalTmpFolder(),
            "*.zip"), _dbConf.getName());
        _taskEngine.addTask(new PTaskDeleteFiles(_dbConf.getLocalTmpFolder(),
            PTask.TASK_OK_FEXT), _dbConf.getName());
      }
      else if (task.indexOf(PTask.TASK_G_DELETETAR) >= 0) {
        _taskEngine.addTask(new PTaskDeleteFiles(_dbConf.getLocalTmpFolder(),
            "*.tar"), _dbConf.getName());
        _taskEngine.addTask(new PTaskDeleteFiles(_dbConf.getLocalTmpFolder(),
            PTask.TASK_OK_FEXT), _dbConf.getName());
      }
      else if (task.indexOf(PTask.TASK_G_DELETETMPIDX) >= 0) {
        _taskEngine.addTask(new PTaskDeleteFiles(_dbConf.getLocalTmpFolder(),
            "*" + LuceneUtils.DIR_OK_FEXT + "/**"), _dbConf.getName());
      }
      else if (task.indexOf(PTask.TASK_G_MAKEALIAS) >= 0) {
        PTaskMakeBlastAlias mkTask = new PTaskMakeBlastAlias(
            getFullDBPathName(), _dbConf.isNucleic());
        mkTask.setParameters(getTaskParameters(task, PTask.TASK_G_MAKEALIAS));
        _taskEngine.addTask(mkTask, _dbConf.getName());
      }
      
      /*
       * str = _dbConf.getHistoryToKeep(); if (str!=null){ _taskEngine.addTask(new
       * KLTaskHandleHistory( _dbConf.getLocalFolder(), Integer.valueOf(str)),
       * _dbConf.getName()); }
       */
    }
  }

  /**
   * Starts the tasks processing individual downloaded file.
   */
  private void startPostUnitTasks(String fName) {
    String uTasks, gTasks;
    StringTokenizer toKenizer;

    uTasks = _dbConf.getUnitPostTasks();
    gTasks = _dbConf.getGlobalPostTasks();

    // at the origin of the system, list of unit tasks where not tokenized
    // using a comma (,)
    // and we allowed the use of comma for tasks parameters. Later on, we
    // start to tokenize
    // list of unit tasks, but this were not fully compatible with the use
    // of comma in parameters.
    // as a consequence, a task can be broken, i.e. parameters may be lost.
    // This is the reason
    // why we pass in to method handleUnitTask a token (i.e. a unit task
    // name) and the full list
    // of unit tasks, so that tasks parameters can be successfully
    // retrieved.
    if (uTasks.length() != 0) {
      toKenizer = new StringTokenizer(uTasks, ",");
      while (toKenizer.hasMoreTokens()) {
        handleUnitTask(toKenizer.nextToken(), uTasks, gTasks, fName);
      }
    } else {
      handleUnitTask("", "", gTasks, fName);
    }
  }

  /**
   * Construct the file path requested by a task depends on the unitTask
   * parameter
   * 
   * WARNING : special treatment in case of a bold databank (param isBold)
   * 
   * @param unitTask
   *          the current unit task
   * @param fName
   *          the file name to analyse
   * @param isBold
   *          is the current databank a bold one ? (special treatment in this
   *          case)
   * 
   * @return the value "_dbConf.getLocalTmpFolder()" concatenated with the
   *         constructed file name
   */
  private String getTaskFilepath(String unitTask, String fName, boolean isBold) {

    // default result
    String result = _dbConf.getLocalTmpFolder() + fName;

    if (fName.toLowerCase().endsWith(".tar.gz")) {
      result = StringUtils.removeEnd(_dbConf.getLocalTmpFolder() + fName,
          ".tar.gz");
    } else if (fName.toLowerCase().endsWith(".gz")) {
      result = StringUtils
          .removeEnd(_dbConf.getLocalTmpFolder() + fName, ".gz");
    } else if (fName.toLowerCase().endsWith(".zip")) {
      if (!isBold) {
        result = StringUtils.removeEnd(_dbConf.getLocalTmpFolder() + fName,
            ".zip");
      } else {
        // the BOLD zip file may contain a sequence file where the unzip
        // file name does not match the zip file name... so we check
        // that (e.g. iBOL_phase_3.0_plants.tsv.zip contains
        // iBOL_phase3.0_plants.tsv: notice the lack of '_' between
        // phase and 3)
        String boldName = BOLDParser.extractBoldFileName(new File(_dbConf
            .getLocalTmpFolder() + fName));
        if (StringUtils.isBlank(boldName)) {
          // no bold name in the zip file : just remove the extension
          result = StringUtils.removeEnd(_dbConf.getLocalTmpFolder() + fName,
              ".zip");
        } else {
          result = _dbConf.getLocalTmpFolder() + boldName;
        }
      }
    } else if (fName.toLowerCase().endsWith(".tgz")) {
      result = StringUtils.removeEnd(_dbConf.getLocalTmpFolder() + fName,
          ".tgz");
    } else if (fName.toLowerCase().endsWith(".tar")) {
      result = StringUtils.removeEnd(_dbConf.getLocalTmpFolder() + fName,
          ".tar");
    }

    if (isBold) {
      if (unitTask.indexOf(PTask.TASK_U_GB_IDX) >= 0
          || unitTask.indexOf(PTask.TASK_U_GP_IDX) >= 0) {
        // for BOLD : prepare a GB indexing (the gb file extension is
        // automatically added by the KLTaskBold2Genbank task
        result += BOLDParser.GB_FILE_EXT;
      }
    }

    return result;
  }

  private void addTaskToEngine(PTask task) {
    _taskEngine.addTask(task, _dbConf.getName());
  }

  private void handleUnitTask(String unitTask, String uTasks, String gTasks,
      String fName) {
    String aName;

    // What sort of unit task ?

    // (g)unzip ?
    if (unitTask.contains(PTask.TASK_U_GUNZIP)) {
      if (fName.toLowerCase().endsWith(".zip")) {
        addTaskToEngine(new PTaskUnzip(_dbConf.getLocalTmpFolder() + fName,
            _dbConf.getLocalTmpFolder()));
      } else if (fName.toLowerCase().endsWith(".gz")) {
        addTaskToEngine(new PTaskGunzip(_dbConf.getLocalTmpFolder() + fName,
            _dbConf.getLocalTmpFolder()));
      }
    }

    // untar ?
    else if (unitTask.contains(PTask.TASK_U_UNTAR)) {
      // case 1
      if (fName.toLowerCase().endsWith(".tar.gz")) {
        // remove ".gz" because it will be done before with an gunzip
        // task
        addTaskToEngine(new PTaskUntar(StringUtils.removeEnd(
            _dbConf.getLocalTmpFolder() + fName, ".gz"),
            _dbConf.getLocalTmpFolder()));
      }
      // case 2
      else if (fName.toLowerCase().endsWith(".tar")) {
        addTaskToEngine(new PTaskUntar(_dbConf.getLocalTmpFolder() + fName,
            _dbConf.getLocalTmpFolder()));
      }
      // case 3
      else if (fName.toLowerCase().endsWith(".tgz")) {
        addTaskToEngine(new PTaskUntarTgz(_dbConf.getLocalTmpFolder() + fName,
            _dbConf.getLocalTmpFolder()));
      }
    }

    // convert BOLD native file to Genbank? Note: only plain-text files are
    // supported
    else if (unitTask.contains(PTask.TASK_U_BOLD2GB)) {
      aName = getTaskFilepath(unitTask, fName, true);
      addTaskToEngine(new PTaskBold2Genbank(aName));
    }

    // create Lucene index for GB or GP?
    else if (unitTask.contains(PTask.TASK_U_GB_IDX)
        || unitTask.contains(PTask.TASK_U_GP_IDX)) {
      aName = getTaskFilepath(unitTask, fName,
          uTasks.indexOf(PTask.TASK_U_BOLD2GB) >= 0);
      _luceneDirs.add(aName + LuceneUtils.DIR_OK_FEXT);
      PTaskLuceneGBIndexer fTask = new PTaskLuceneGBIndexer(aName);
      fTask.setParameters(getTaskParameters(unitTask, PTask.TASK_U_GB_IDX));
      fTask.setParameters(getTaskParameters(unitTask, PTask.TASK_U_GP_IDX));
      addTaskToEngine(fTask);
      addForFormatDb(gTasks, aName);
    }

    // create Lucene index for SW/Embl ?
    else if (unitTask.contains(PTask.TASK_U_SW_IDX)
        || unitTask.contains(PTask.TASK_U_EM_IDX)) {
      aName = getTaskFilepath(unitTask, fName, false);

      _luceneDirs.add(aName + LuceneUtils.DIR_OK_FEXT);
      PTaskLuceneSWIndexer fTask = new PTaskLuceneSWIndexer(aName);
      fTask.setParameters(getTaskParameters(unitTask, PTask.TASK_U_SW_IDX));
      fTask.setParameters(getTaskParameters(unitTask, PTask.TASK_U_EM_IDX));
      addTaskToEngine(fTask);
      addForFormatDb(gTasks, aName);
    }

    // create eggnog task
    else if (unitTask.contains(PTask.TASK_U_NOG_IDX)) {
      aName = getTaskFilepath(unitTask, fName, false);
      _luceneDirs.add(aName + LuceneUtils.DIR_OK_FEXT);
      PTaskEggNogIndexer task = new PTaskEggNogIndexer(aName);
      task.setParameters(getTaskParameters(unitTask, PTask.TASK_U_NOG_IDX));
      addTaskToEngine(task);
      addForFormatDb(gTasks, aName);
    }

    // create Lucene index for Fasta ?
    else if (unitTask.contains(PTask.TASK_U_FAS_IDX)) {

      // lan 18/03/2014 jira KDMS-19
      // In case of a unique fasta file in the databank,
      // it is UNnecessary to convert this file in the fasta format.
      // To skip this step the file should be renamed as the volume name :
      // <bank_name>00
      // lan 27/03/2014 jira KDMS-22 : but AFTER the index task
      aName = getTaskFilepath(unitTask, fName, false);

      _luceneDirs.add(aName + LuceneUtils.DIR_OK_FEXT);
      PTaskLuceneFastaIndexer fTask = new PTaskLuceneFastaIndexer(aName);
      fTask.setParameters(getTaskParameters(unitTask, PTask.TASK_U_FAS_IDX));

      // the fasta renamer task to automatically create volumes and avoid a
      // conversion from fasta to fasta
      PTaskFastaRenamer taskRenamer = null;

      // special case : silva & cdd => have to convert into fasta because of the
      // special treatment to add taxonomy data
      boolean isSilva = StringUtils.isNotBlank(gTasks)
          && Formatters.replaceAll(gTasks, " ", "").toLowerCase()
              .contains(PTaskFormatDB.IS_SILVA + "=true");
      boolean isCdd = StringUtils.isNotBlank(gTasks)
          && Formatters.replaceAll(gTasks, " ", "").toLowerCase()
              .contains(PTaskFormatDB.IS_CDD + "=true");
      if ((_files == 1) && (!isSilva) && (!isCdd)) {
        // Force the check of the redundant Ids to execute or not the next task
        // which is a KLTaskFastaRenamer
        fTask.forceCheckRedundantIds();
        // getTaskFilepath one more time to manage .tar.gz
        taskRenamer = new PTaskFastaRenamer(getTaskFilepath(unitTask,
            new File(aName).getName(), false), _dbConf.getName() + "00", fTask);
      }

      addTaskToEngine(fTask);
      addForFormatDb(gTasks, aName);
      // Add the task renamer AFTER the index one
      if (taskRenamer != null) {
        addTaskToEngine(taskRenamer);
      }
    }

    // create Lucene index for dico?
    else if (unitTask.contains(PTask.TASK_U_DICO_IDX)) {
      String dicoPath = _dbConf.getLocalTmpFolder();
      aName = getTaskFilepath(unitTask, fName, false);

      // for dictionary, we allow to index several data files available
      // from the same path. For that purpose, we have to put here the path
      // (not the full file name) of the index : during index merge, KDMS will
      // collect
      // all internal indexes. This has been added to index the NCBI Tax DB
      // where two
      // files need to be integrated.
      if (!_luceneDirs.contains(dicoPath))
        _luceneDirs.add(dicoPath);
      PTaskDicoIndexer dicoTask = new PTaskDicoIndexer(aName);
      dicoTask
          .setParameters(getTaskParameters(unitTask, PTask.TASK_U_DICO_IDX));
      addTaskToEngine(dicoTask);
      addForFormatDb(gTasks, aName);
    }

    // user provided external script
    else if (unitTask.contains(PTask.TASK_U_EXTSCRIPT)) {
      String dicoPath = _dbConf.getLocalTmpFolder();
      aName = getTaskFilepath(unitTask, fName, false);
      PTaskExecScript execTask = new PTaskExecScript(dicoPath, aName,
          _dbConf.getName(), _dbConf.getTypeCode());
      execTask.setParameters(getTaskParameters(unitTask, PTask.TASK_U_DICO_IDX));
      addTaskToEngine(execTask);
    }
    
    // no unit task
    else if (unitTask.trim().equals("")) {
      addForFormatDb(gTasks, getTaskFilepath(unitTask, fName, false));
    }

  }

  private void addForFormatDb(String gTasks, String fileNameToAdd) {

    if (gTasks != null && gTasks.contains(PTask.TASK_G_FORMATDB)) {
      if (!_formatDBfiles.contains(fileNameToAdd))
        _formatDBfiles.add(fileNameToAdd);
    }

  }

  public ArrayList<String> getFormatDBFiles() {
    return this._formatDBfiles;
  }

  private String getTaskParameters(String allTasks, String curTask) {
    int idx, idx2;

    idx = allTasks.indexOf(curTask) + curTask.length();
    if (idx >= allTasks.length())
      return null;
    if (allTasks.charAt(idx) != '(')
      return null;
    idx2 = allTasks.indexOf(')', idx);
    if (idx2 == -1)
      return null;
    return allTasks.substring(idx + 1, idx2);
  }

  /**
   * Implementation of LoaderMonitor interface.
   */
  public void doneLoading(String fName, int status) {
    synchronized (this) {

      LoggerCentral.info(LOGGER, "Download status for: " + fName + ": "
          + LoaderMonitor.STATUS_STR[status]);
      if (status == LoaderMonitor.STATUS_OK)
        startPostUnitTasks(fName);
      _fileCounter++;
      if (_fileCounter == _files) {
        _taskEngine.addTask(new PTaskReleaseDate(_dbConf.getLocalTmpFolder()), _dbConf.getName());
        startPostGlobalTasks(fName);
        _taskEngine.addTask(new PTaskEndProcessing(), _dbConf.getName());
      }
    }
  }
}