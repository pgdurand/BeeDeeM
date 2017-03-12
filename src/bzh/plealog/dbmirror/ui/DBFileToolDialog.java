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

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.text.JTextComponent;

import bzh.plealog.dbmirror.ui.resources.DBMSMessages;
import bzh.plealog.dbmirror.util.runner.DBMSExecNativeCommand;
import bzh.plealog.dbmirror.util.runner.UnarchiveDBMonitor;
import bzh.plealog.dbmirror.util.runner.UnarchiveDBRunner;

import com.plealog.genericapp.api.EZEnvironment;

/**
 * This class displays a dialogue box embedding a DBFileToolPanel.
 * 
 * @author Patrick G. Durand
 */
@SuppressWarnings("serial")
public class DBFileToolDialog extends JDialog {

  private DBFileToolPanel _editor;
  private JButton         _okBtn;
  private MyMonitor       _jobMonitor;
  private boolean         _jobSucceeded;
  private ImageIcon       _animIcon    = EZEnvironment
                                           .getImageIcon("circle_all.gif");
  private ImageIcon       _notAnimIcon = EZEnvironment
                                           .getImageIcon("circle_back.gif"); ;
  private JLabel          _animLbl;

  /**
   * Constructor.
   * 
   * @param owner
   *          the owner of this dialogue box.
   */
  public DBFileToolDialog(Frame owner) {
    super(owner, DBMSMessages.getString("DBFileToolPanel.dlg.header"), true);
    buildGUI();
    _jobMonitor = new MyMonitor(_editor.getHelpArea());
    _editor.setMonitor(_jobMonitor);
    this.pack();
    this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    this.addWindowListener(new FDBDialogAdapter());
  }

  /**
   * Creates the GUI.
   */
  private void buildGUI() {
    JPanel mainPnl, btnPnl;
    JButton okBtn, cancelBtn;
    boolean macOS = DBMSExecNativeCommand.getOSType() == DBMSExecNativeCommand.MAC_OS;

    okBtn = new JButton(DBMSMessages.getString("DBFileToolDialog.btn.ok"));
    cancelBtn = new JButton(
        DBMSMessages.getString("DBFileToolDialog.btn.cancel"));
    okBtn.addActionListener(new UnarchiveDBAction());
    cancelBtn.addActionListener(new CloseDialogAction());
    btnPnl = new JPanel();
    btnPnl.setLayout(new BoxLayout(btnPnl, BoxLayout.X_AXIS));
    btnPnl.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
    _animLbl = new JLabel(_notAnimIcon);
    btnPnl.add(_animLbl);
    btnPnl.add(Box.createHorizontalGlue());
    btnPnl.add(macOS ? cancelBtn : okBtn);
    btnPnl.add(Box.createRigidArea(new Dimension(10, 0)));
    btnPnl.add(macOS ? okBtn : cancelBtn);
    if (!macOS)
      btnPnl.add(Box.createHorizontalGlue());

    _okBtn = okBtn;

    _editor = new DBFileToolPanel(_okBtn);

    mainPnl = new JPanel(new BorderLayout());
    mainPnl.add(_editor, BorderLayout.CENTER);
    mainPnl.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(mainPnl, BorderLayout.CENTER);
    getContentPane().add(btnPnl, BorderLayout.SOUTH);
  }

  private void displayWarnMessage(String msg) {
    JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(this), msg,
        DBMSMessages.getString("DBFileToolPanel.dlg.header"),
        JOptionPane.WARNING_MESSAGE);
  }

  /**
   * Shows the dialog box on screen.
   */
  public void showDlg() {
    UIUtils.centerOnScreen(this);
    setVisible(true);
  }

  /**
   * Returns the DB location.
   */
  public String getDBPath() {
    return (_editor.getDBPath());
  }

  /**
   * Figures out whether the unarchive process job terminated successfully.
   */
  public boolean doesJobSucceed() {
    return _jobSucceeded;
  }

  /**
   * This inner class manages actions coming from the JButton CloseDialog.
   */
  private class CloseDialogAction extends AbstractAction {
    /**
     * Manages JButton action
     */
    public void actionPerformed(ActionEvent e) {
      if (_jobMonitor.isJobRunning()) {
        _jobMonitor.cancelJob();
      } else {
        dispose();
      }
    }
  }

  /**
   * This inner class manages actions coming from the JButton Unarchive.
   */
  private class UnarchiveDBAction extends AbstractAction {

    /**
     * Manages JButton action
     */
    public void actionPerformed(ActionEvent e) {
      UnarchiveDBRunner runner;
      if (!_editor.checkData())
        return;
      enableControls(false);
      _jobMonitor.reset();
      _editor.setMonitor(_jobMonitor);
      _animLbl.setIcon(_animIcon);
      runner = new UnarchiveDBRunner(_editor.getFileList(),
          _editor.getDBPath(), _jobMonitor);
      runner.start();
    }
  }

  /**
   * Listener of JDialog events.
   */
  private class FDBDialogAdapter extends WindowAdapter {
    /**
     * Manages windowClosing event: hide the dialog.
     */
    public void windowClosing(WindowEvent e) {
      if (_jobMonitor.isJobRunning())
        return;
      dispose();
    }
  }

  private void enableControls(boolean enable) {
    _okBtn.setEnabled(enable);
    _editor.enableControls(enable);
  }

  private class MyMonitor extends UnarchiveDBMonitor {
    public MyMonitor(JTextComponent txtCompo) {
      super(txtCompo);
    }

    public void jobDone(boolean success) {
      _animLbl.setIcon(_notAnimIcon);
      if (success) {
        displayWarnMessage(DBMSMessages.getString("DBFileToolDialog.msg1"));
        _jobSucceeded = true;
        dispose();
      } else {
        _jobSucceeded = false;
        displayWarnMessage(DBMSMessages.getString("DBFileToolDialog.err1")
            + ": " + this.getErrMsg());
        enableControls(true);
      }
      _jobMonitor.reset();
      _editor.setMonitor(null);
    }
  }
}
