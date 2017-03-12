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
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * A task capable of renaming a Fasta database file.
 * 
 * @author Ludovic Antin
 */
public class PTaskFastaRenamer extends PAbstractTask {

  private String                   _errMsg;
  private String                   _currentFilePath;
  private String                   _newFileName;
  private PTaskLuceneFastaIndexer _indexingTask;

  private static final Log         LOGGER = LogFactory
                                              .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                  + ".PTaskEngine");

  public PTaskFastaRenamer(String currentFilePath, String newFileName,
      PTaskLuceneFastaIndexer indexingTask) {
    _currentFilePath = currentFilePath;
    _newFileName = newFileName;
    _indexingTask = indexingTask;
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getName() {
    return "KLTaskFastaRenamer";
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getUserFriendlyName() {
    return "Renaming fasta file";
  }

  /**
   * Implementation of KLTask interface.
   */
  public void setParameters(String params) {
    // Nothing to do
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getErrorMsg() {
    return _errMsg;
  }

  /**
   * Rename the current file and create a new file 'volumes.ok' indicating that
   * the fasta volume is created and is ok for the formatdb task
   */
  public boolean execute() {
    try {
      // execute a rename task only if the pas indexing task did not found
      // redundant sequences
      if (!_indexingTask.getParserMonitor().redundantSequenceFound()) {
        LoggerCentral.info(LOGGER, getName() + " started for "
            + _currentFilePath);

        File currentFile = new File(_currentFilePath);
        File renamedFile = new File(currentFile.getParent() + File.separator
            + _newFileName);
        File volumeOkFile = new File(currentFile.getParent(),
            PTaskFormatDB.VOL_FILES_OK);
        File d1File = new File(currentFile.getParent(), "d1");

        // first : rename the file
        if (!(volumeOkFile.exists() && (renamedFile.exists()))) // still done
        {
          if (!currentFile.renameTo(renamedFile)) {
            _errMsg = "Renaming file " + _currentFilePath + " into "
                + _newFileName + " failed";
            return false;
          }
        }

        // second : change the filename in the d1 file created by the indexing
        // task
        if (d1File.exists()) {
          FileInputStream fis = null;
          FileOutputStream fos = null;
          try {
            fis = new FileInputStream(d1File);
            String content = IOUtils.toString(fis);
            int pos = content.lastIndexOf(currentFile.getName());
            if (pos > -1) {
              content = content.substring(0, pos) + renamedFile.getName();
            }

            fos = new FileOutputStream(d1File);
            IOUtils.write(content, fos);
          } catch (Exception e) {
            _errMsg = "Renaming filename into d1 file failed";
            return false;
          } finally {
            IOUtils.closeQuietly(fis);
            IOUtils.closeQuietly(fos);
          }
        }

        // third : create the volumes.ok file
        try {
          volumeOkFile.createNewFile();
        } catch (Exception ex) {
          // Do not stop the process if the volumes.ok file is not created
          // In this case, the databank will still be installed with unnecessary
          // tasks
          LOGGER.debug("Unable to create the " + PTaskFormatDB.VOL_FILES_OK
              + " file for " + _currentFilePath, ex);
        }

      }
      return true;

    } catch (Exception ex) {
      _errMsg = "Renaming file " + _currentFilePath + " into " + _newFileName
          + " failed : " + ex.getMessage();
      return false;
    }
  }

  public String getNewFilepath() {
    return new File(new File(_currentFilePath).getParent() + File.separator
        + _newFileName).getAbsolutePath();
  }

}
