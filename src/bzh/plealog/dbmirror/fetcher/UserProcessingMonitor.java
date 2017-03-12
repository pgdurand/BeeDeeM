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

import javax.swing.ImageIcon;

public interface UserProcessingMonitor {
  public static enum PROCESS_TYPE {
    FTP_LOADING, TASK_EXECUTION
  };

  public static enum MSG_TYPE {
    OK, ERROR, ABORTED
  };

  public void processingFileMessage(String workerID, PROCESS_TYPE processType,
      String message);

  /**
   * This method is called to transmit a message to this monitor.
   */
  public void processingMessage(String workerID, String dbConfName,
      PROCESS_TYPE processType, MSG_TYPE msgType, String message);

  /**
   * This method is called to transmit a message with an icon before to this
   * monitor.
   */
  public void processingMessage(ImageIcon icon, String workerID,
      String dbConfName, PROCESS_TYPE processType, MSG_TYPE msgType,
      String message);

  /**
   * This method is called to forward information about what is going to be
   * downloaded.
   */
  public void fileTransferInfo(String workerID, DBServerConfig fsc, int nFiles,
      long totalBytes);

  /**
   * This method is called to transmit to this monitor the current status
   * processing of a file.
   */
  public void processingFile(String workerID, String dbConfName,
      PROCESS_TYPE processType, String fName, long currentSteps, long totalSteps);

  public void startProcessing(String dbConfName);

  public void endProcessing(String dbConfName);

  /**
   * Call this method to check if the job has been cancelled.
   */
  public boolean jobCancelled();

  /**
   * Method call to inform this monitor that a processing is about to start.
   */
  public void processingStarted();

  /**
   * Method call to inform this monitor that a processing is done.
   */
  public void processingDone(MSG_TYPE status);

}
