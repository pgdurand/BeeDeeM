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

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.apache.commons.net.io.CopyStreamEvent;
import org.apache.commons.net.io.CopyStreamListener;
import org.apache.commons.net.io.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * This class is used to monitor the copy of two I/O streams.
 * 
 * @author Patrick G. Durand
 */
public class MyCopyStreamListener implements CopyStreamListener {

  private static final Logger LOGGER = LogManager.getLogger(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                        + ".Streamer");

  private UserProcessingMonitor _userMonitor;
  private String                fName;
  private String                dbConfName;
  private String                workerID;
  private long                  stepSize;
  private long                  step            = 1;
  private long                  stepPlastRunner = 1;
  private long                  restartOffset   = 0;
  private Date                  start           = null;
  private int                   waitDelay       = 3;                    // unit:
                                                                        // seconds
  private Calendar              nextRefresh;

  public MyCopyStreamListener(String workerID,
      UserProcessingMonitor userMonitor, String dbConfName, String fName,
      long streamSize) {
    this(workerID, userMonitor, dbConfName, fName, streamSize, 0l);
  }
  public MyCopyStreamListener(String workerID,
      UserProcessingMonitor userMonitor, String dbConfName, String fName,
      long streamSize, long restartOffset) {
    _userMonitor = userMonitor;
    this.dbConfName = dbConfName;
    this.fName = fName;
    this.workerID = workerID;
    this.stepSize = streamSize / 100l;// 20l;
    this.restartOffset = restartOffset;
    if (this.stepSize < Util.DEFAULT_COPY_BUFFER_SIZE)
      this.stepSize = Util.DEFAULT_COPY_BUFFER_SIZE;
    // set the wait delay depending on the streamSize
    if (streamSize < 5 * Utils.MEGA) {
      // no delay
      waitDelay = -1;
    } else if (streamSize < 50 * Utils.MEGA) {
      waitDelay = 1;
    } else if (streamSize < 500 * Utils.MEGA) {
      waitDelay = 2;
    } else {
      waitDelay = 3;
    }

    this.start = new Date();
    nextRefresh = Calendar.getInstance();
    nextRefresh.add(Calendar.SECOND, waitDelay);
  }

  public void bytesTransferred(CopyStreamEvent event) {
    // not used by org.apache.commons.net.io.Util static methods.
  }

  public void bytesTransferred(long totalBytesTransferred,
      int bytesTransferred, long streamSize) {

    long tbr = restartOffset + totalBytesTransferred;
    // keep this log for Plastrunner
    if (tbr >= (stepPlastRunner * stepSize * 10)
        || tbr == streamSize) {
      LoggerCentral.info(LOGGER, this.fName + " - download in progress: "
          + (tbr * 100 / streamSize) + " %");
      stepPlastRunner++;
    }

    // avoid too many messages
    if (tbr >= (step * stepSize)
        || tbr == streamSize) {
      if (_userMonitor != null) {
        _userMonitor.processingFile(workerID, dbConfName,
            UserProcessingMonitor.PROCESS_TYPE.FTP_LOADING, fName,
            tbr, streamSize);
      }

      step++;
    } else {
      if ((waitDelay < 0) || (nextRefresh.before(Calendar.getInstance()))) {
        try {
          if (_userMonitor != null) {
            // msg to display in the label
            StringBuffer msg = new StringBuffer(
                bzh.plealog.dbmirror.util.Utils.getBytes(tbr)
                    + "/"
                    + bzh.plealog.dbmirror.util.Utils.getBytes(streamSize));
            // debit calcul
            long seconds = (new Date().getTime() - this.start.getTime()) / 1000;
            long koPerSeconds = (tbr / 1024) / seconds;
            // msg += " - " + koPerSeconds + " Ko/s";
            // estimate end
            long totalSecondsToEnd = ((streamSize - tbr) / 1024)
                / koPerSeconds;
            long hoursToEnd = TimeUnit.SECONDS.toHours(totalSecondsToEnd);
            long minutesToEnd = TimeUnit.SECONDS.toMinutes(totalSecondsToEnd)
                - TimeUnit.HOURS.toMinutes(hoursToEnd);
            long secondsToEnd = TimeUnit.SECONDS.toSeconds(totalSecondsToEnd)
                - TimeUnit.MINUTES.toSeconds(minutesToEnd);
            if (hoursToEnd > 0) {
              msg.append(String.format(" - %01dhr %01dmin to finish",
                  hoursToEnd, minutesToEnd));
            } else if (minutesToEnd > 0) {
              msg.append(String.format(" - %01dmin %01ds to finish",
                  minutesToEnd, secondsToEnd));
            } else {
              msg.append(String.format(" - %01ds to finish", secondsToEnd));
            }

            _userMonitor.processingFileMessage(workerID,
                UserProcessingMonitor.PROCESS_TYPE.FTP_LOADING, msg.toString());
          }
        } catch (Exception e) {
          // Nothing to do
        }
        nextRefresh.add(Calendar.SECOND, waitDelay);
      }
    }

    // check for job cancellation
    if (_userMonitor != null) {
      if (_userMonitor.jobCancelled()) {
        throw new MyCopyInteruptException();
      }
    }
  }
}