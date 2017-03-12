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
package test.other.silva;

import bzh.plealog.dbmirror.lucenedico.DicoTermQuerySystem;
import bzh.plealog.dbmirror.reader.DBUtils;
import bzh.plealog.dbmirror.util.conf.DBMirrorConfig;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptorUtils;
import bzh.plealog.dbmirror.util.runner.FormatDBMonitor;
import bzh.plealog.dbmirror.util.sequence.SeqIOUtils;

public class SeqIOUtilsSilvaTest {

  /**
   * @param args
   */
  public static void main(String[] args) {
    test();
  }

  private static void test() {
    DicoTermQuerySystem dico = null;
    DBMirrorConfig mirrorConfig;
    MyMonitor jobMonitor;

    jobMonitor = new MyMonitor();

    mirrorConfig = DBDescriptorUtils
        .getDBMirrorConfig("/biobase/dbmirror.config");
    dico = DicoTermQuerySystem.getDicoTermQuerySystem(mirrorConfig);

    SeqIOUtils.convertToFasta("./tests/junit/databank/silva/silva.fasta",
        "./silva_trunc.fas", SeqIOUtils.FASTADNA, jobMonitor, null, dico,
        DBUtils.SILVA_HEADER_FORMAT);

    System.out.println(jobMonitor.success);
  }

  private static class MyMonitor extends FormatDBMonitor {
    private boolean success;

    public void setTxtMessage(String msg) {
      System.out.println(msg);
    }

    public void jobDone(boolean success) {
      this.success = success;
    }

  }

}
