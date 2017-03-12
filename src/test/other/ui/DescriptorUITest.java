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
package test.other.ui;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import bzh.plealog.dbmirror.ui.DBMSPanel;
import bzh.plealog.dbmirror.ui.DBMSUserInterface;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

@SuppressWarnings("serial")
public class DescriptorUITest extends JFrame {

  public static void main(String[] args) {
    // exemple:
    // args[0] = C:\temp\biobase
    // args[1] = H:\devel\plealog\dbmirror\conf\kb (put there: tasks.xml)
    DBMSAbstractConfig.configureLog4J("autoMirror");
    // KDMSAbstractConfig.setLocalMirrorPath(args[0]);
    // KDMSAbstractConfig.setLocalMirrorConfFile(Utils.terminatePath(args[0])+"dbmirror.config");
    DBMSAbstractConfig.setInstallAppConfPath(args[1]);

    DBMSAbstractConfig.initializeConfigurator(Utils.terminatePath(args[1])
        + DBMSAbstractConfig.MASTER_CONF_FILE);
    LoggerCentral.reset();

    DescriptorUITest frame = new DescriptorUITest();
    DBMSPanel pnl = DBMSUserInterface.getUserInterface(args[1], true);
    frame.getContentPane().add(pnl);
    frame.setTitle("DBMS User Interface - test application");

    frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    frame.addWindowListener(new MainWindowAdapter(pnl));
    frame.pack();
    frame.setVisible(true);
  }

  private static class MainWindowAdapter extends WindowAdapter {
    private DBMSPanel pnl;

    public MainWindowAdapter(DBMSPanel pnl) {
      this.pnl = pnl;
    }

    public void windowClosing(WindowEvent e) {
      if (pnl.canClose())
        System.exit(0);
    }
  }
}
