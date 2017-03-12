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

import org.apache.log4j.BasicConfigurator;

import bzh.plealog.dbmirror.util.sequence.DBFileCutter;
import bzh.plealog.dbmirror.util.sequence.SeqIOConvertMonitor;
import bzh.plealog.dbmirror.util.sequence.SeqIOUtils;

/**
 * This snippet illustrates how to cut a sequence file.
 * 
 * @author Patrick G. Durand
 */
public class DBFileCutterTest {

  /**
   * @param args
   *          cmdline arguments. Expected: [0] is the sequence file to cut, [1]
   *          is the output file and [2]-[3] is the sequence range [from..to].
   */
  public static void main(String[] args) {
    BasicConfigurator.configure();
    System.out.println("Start cutting: " + args[0]);

    try {
      int formatType = SeqIOUtils.guessFileFormat(args[0]);
      DBFileCutter.cutFile(args[0], args[1], formatType, new MyMonitor(),
          Integer.valueOf(args[2]) - 1, Integer.valueOf(args[3]) - 1);
    } catch (Exception e) {
      System.err.println("Error: " + e.toString());
    }

  }

  private static class MyMonitor implements SeqIOConvertMonitor {

    public void seqFound(String seqID) {
      System.out.println("ID: " + seqID);
    }

    public void startProcessing() {
      System.out.println("Start processing");
    }

    public void stopProcessing(long time) {
      System.out.println("Running time: " + time + " ms.");
    }

  }

}
