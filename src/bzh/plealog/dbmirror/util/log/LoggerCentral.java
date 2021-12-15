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
package bzh.plealog.dbmirror.util.log;

import org.apache.logging.log4j.Logger;

import bzh.plealog.dbmirror.util.sequence.SequenceFileManager;

/**
 * This class is used to centralize all logs. In addition, this class can be
 * used to figure whether some warnings where emitted during application life
 * cycle.
 * 
 * @author Patrick G. Durand
 */
public class LoggerCentral {
  public static boolean               _errorEmitted;
  public static boolean               _processAborted;
  public static boolean               _isRunning;

  private static LoggerCentralGateway _logGateway;

  public static LoggerCentralGateway getLogGateway() {
    return _logGateway;
  }

  public static void setLogGateway(LoggerCentralGateway logGateway) {
    _logGateway = logGateway;
  }


  public static synchronized void info(Logger logger, String msg) {
    logger.info(msg);
    if (_logGateway != null) {
      _logGateway.info(msg);
    }
  }


  public static synchronized void warn(Logger logger, String msg) {
    logger.warn(msg);
    if (_logGateway != null) {
      _logGateway.warn(msg);
    }
  }

  public static synchronized void error(Logger logger, String msg) {
    _errorEmitted = true;
    logger.error(msg);
    if (_logGateway != null) {
      _logGateway.error(msg);
    }
  }

  public static synchronized boolean errorMsgEmitted() {
    return _errorEmitted;
  }

  public static synchronized void abortProcess() {
    _processAborted = true;
    if (LoggerCentral.sfm != null) {
      LoggerCentral.sfm.abortProcess();
    }
  }

  public static synchronized boolean processAborted() {
    return _processAborted;
  }

  public static synchronized void reset() {
    _errorEmitted = false;
    _processAborted = false;
    _isRunning = false;
  }

  public static synchronized void setRunning(boolean running) {
    _isRunning = running;
  }

  public static synchronized boolean isRunning() {
    return _isRunning;
  }

  private static SequenceFileManager sfm = null;

  public static synchronized void stopThisSfmIfAbort(SequenceFileManager sfm) {
    LoggerCentral.sfm = sfm;
  }

  public static synchronized void removeSfmToAbort() {
    LoggerCentral.sfm = null;
  }
}
