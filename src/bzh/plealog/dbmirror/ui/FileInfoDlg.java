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
package bzh.plealog.dbmirror.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

import org.apache.commons.io.FileSystemUtils;

import bzh.plealog.dbmirror.ui.resources.DBMSMessages;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.runner.DBMSExecNativeCommand;

import com.plealog.genericapp.api.EZEnvironment;

/**
 * This class is used to display the information about the files to download.
 * 
 * @author Patrick G. Durand
 */
@SuppressWarnings("serial")
public class FileInfoDlg extends JDialog {
  private boolean        dlgCancelled;
  private List<FileInfo> fis;

  public FileInfoDlg(Frame parent, List<FileInfo> fis) {
    super(parent, DBMSMessages.getString("RunnerSchedulerDlg.msg1"), true);
    this.fis = fis;
    buildGUI(fis);
    this.pack();
    this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    this.addWindowListener(new MyDialogAdapter());
  }

  /**
   * Shows the dialog box on screen.
   */
  public void showDlg() {
    // center on screen
    Dimension screenSize = this.getToolkit().getScreenSize();
    Dimension dlgSize = this.getSize();

    this.setLocation(screenSize.width / 2 - dlgSize.width / 2,
        screenSize.height / 2 - dlgSize.height / 2);
    // show
    setVisible(true);
  }

  private JTextArea createHelper() {
    JTextArea helpArea = new JTextArea();
    helpArea.setRows(4);
    helpArea.setLineWrap(true);
    helpArea.setWrapStyleWord(true);
    helpArea.setEditable(false);
    helpArea.setOpaque(false);
    helpArea.setBorder(BorderFactory.createEmptyBorder());
    helpArea.setForeground(EZEnvironment.getSystemTextColor());
    helpArea.setText(DBMSMessages.getString("RunnerSchedulerDlg.msg9"));
    return helpArea;
  }

  private void buildGUI(List<FileInfo> fis) {
    JPanel btnPnl, mainPnl, noticePnl;
    JButton okBtn, cancelBtn;
    boolean macOS = DBMSExecNativeCommand.getOSType() == DBMSExecNativeCommand.MAC_OS;

    okBtn = new JButton(DBMSMessages.getString("FilInfoDlg.msg1"));
    okBtn.addActionListener(new OkAction());
    cancelBtn = new JButton(DBMSMessages.getString("FilInfoDlg.msg2"));
    cancelBtn.addActionListener(new CloseAction());

    btnPnl = new JPanel();
    btnPnl.setLayout(new BoxLayout(btnPnl, BoxLayout.X_AXIS));
    btnPnl.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
    btnPnl.add(Box.createHorizontalGlue());
    btnPnl.add(macOS ? cancelBtn : okBtn);
    btnPnl.add(Box.createRigidArea(new Dimension(10, 0)));
    btnPnl.add(macOS ? okBtn : cancelBtn);
    if (!macOS)
      btnPnl.add(Box.createHorizontalGlue());

    noticePnl = new JPanel(new BorderLayout());
    noticePnl.add(createHelper(), BorderLayout.CENTER);
    noticePnl.setBorder(BorderFactory.createTitledBorder(DBMSMessages
        .getString("RunnerSchedulerDlg.msg11")));
    try {
      long size = FileSystemUtils.freeSpaceKb(DBMSAbstractConfig
          .getLocalMirrorPath()) * 1024;
      noticePnl.add(
          new JLabel("Remaining disk space: " + Utils.getBytes(size)),
          BorderLayout.SOUTH);
    } catch (IOException e) {
      noticePnl.add(new JLabel("unknown remaining disk space."),
          BorderLayout.SOUTH);
    }

    mainPnl = new JPanel(new BorderLayout());
    mainPnl.add(new FileIntoTablePanel(fis), BorderLayout.CENTER);
    mainPnl.add(noticePnl, BorderLayout.SOUTH);

    getContentPane().setLayout(new BorderLayout());

    getContentPane().add(mainPnl, BorderLayout.CENTER);
    getContentPane().add(btnPnl, BorderLayout.SOUTH);
  }

  public boolean dlgCancelled() {
    return dlgCancelled;
  }

  /**
   * This inner class manages actions coming from the JButton Ok.
   */
  private class OkAction extends AbstractAction {
    /**
     * Manages JButton action
     */
    public void actionPerformed(ActionEvent e) {
      long nTotBytes = 0;
      long size;

      for (FileInfo fi : fis) {
        nTotBytes += fi.getDbSize();
      }
      try {
        size = FileSystemUtils.freeSpaceKb(DBMSAbstractConfig
            .getLocalMirrorPath()) * 1024;
      } catch (IOException e1) {
        size = -1;
      }
      if (size != -1 && nTotBytes > size) {
        MessageFormat formatter = new MessageFormat(
            DBMSMessages.getString("FilInfoDlg.msg3"));
        int ret = JOptionPane.showConfirmDialog(
            FileInfoDlg.this,
            formatter.format(new Object[] { Utils.getBytes(nTotBytes),
                Utils.getBytes(size) }),
            DBMSMessages.getString("RunnerSchedulerDlg.msg1"),
            JOptionPane.YES_NO_OPTION);
        dlgCancelled = (ret == JOptionPane.NO_OPTION);
      } else {
        dlgCancelled = false;
      }
      dispose();
    }
  }

  private class CloseAction extends AbstractAction {
    /**
     * Manages JButton action
     */
    public void actionPerformed(ActionEvent e) {
      dlgCancelled = true;
      dispose();
    }
  }

  private class MyDialogAdapter extends WindowAdapter {
    /**
     * Manages windowClosing event: hide the dialog.
     */
    public void windowClosing(WindowEvent e) {
      dlgCancelled = true;
      dispose();
    }
  }

}
