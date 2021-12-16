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
package test.other.system;

import bzh.plealog.dbmirror.main.StarterUtils;
import bzh.plealog.dbmirror.reader.PQueryMirrorBase;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * This code snippet illustrates how to query databanks to get sequence by ID.
 * 
 * @author Patrick G. Durand
 */
public class CmdLineQueryTest {
  /**
   * @param args
   */
  public static void main(String[] args) {
    LoggerCentral.configure();

    StarterUtils.configureApplication(null, "kdmsUI", true, true, false);

    // start the Job
    PQueryMirrorBase qm = new PQueryMirrorBase();
    // OPD_FLAS2 ->ok
    // sp|OPD_FLAS2 ->ok
    // gi|1231386|sp|OPD_FLAS2 ->ok
    // gi|1231386| -> not ok
    // gi|1231386|pdb|OPD_FLAS2|A ->ok
    // gi|1231386|pdb|OPD_FLAS2X|A-> not ok
    // sp|A0AQI4|A0AQI4_9ARCH ->ok (Trembl only)
    // A0AQI4|A0AQI4_9ARCH ->ok (Trembl only)
    qm.executeJob("protein", // index to query
        "1433B_HUMAN", // id to get
        null, // start coord. Not set: full sequence
        null, // stop coord. Not set: full sequence
        null, // Adjust coord. Not set (i.e. False): do not adjust
        "html", // format. One of: fas, insd (xml), html, txt
        System.out, // out stream
        DBMSAbstractConfig.getLocalMirrorConfFile());
  }
}
