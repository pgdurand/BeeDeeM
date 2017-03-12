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
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import bzh.plealog.dbmirror.ui.resources.DBMSMessages;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.conf.DBMSConfigurator;
import bzh.plealog.dbmirror.util.conf.DBMirrorConfig;
import bzh.plealog.dbmirror.util.runner.DBMSExecNativeCommand;

/**
 * This class is used to open a Databank Descriptor Editor.
 * 
 * @author Patrick G. Durand
 */
@SuppressWarnings("serial")
public class ChooseMirrorConfigDlg extends JDialog {
  private ChooseMirrorConfigPanel _editor;
  private Frame                   _parent;
  private boolean                 _isOk;

  public ChooseMirrorConfigDlg(Frame parent) {
    super(parent, DBMSMessages.getString("ChooseMirrorConfigDlg.header"), true);

    _parent = parent;

    buildGUI();

    this.pack();
    this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    this.addWindowListener(new FDBDialogAdapter());
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

  private void buildGUI() {
    JPanel mainPnl, btnPnl;
    JButton okBtn, cancelBtn;
    boolean macOS = DBMSExecNativeCommand.getOSType() == DBMSExecNativeCommand.MAC_OS;

    okBtn = new JButton(DBMSMessages.getString("RunnerSchedulerDlg.msg2"));
    cancelBtn = new JButton(DBMSMessages.getString("RunnerSchedulerDlg.msg3"));
    okBtn.addActionListener(new OkAction());
    cancelBtn.addActionListener(new CancelAction());

    btnPnl = new JPanel();
    btnPnl.setLayout(new BoxLayout(btnPnl, BoxLayout.X_AXIS));
    btnPnl.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
    btnPnl.add(Box.createHorizontalGlue());
    btnPnl.add(macOS ? cancelBtn : okBtn);
    btnPnl.add(Box.createRigidArea(new Dimension(10, 0)));
    btnPnl.add(macOS ? okBtn : cancelBtn);
    if (!macOS)
      btnPnl.add(Box.createHorizontalGlue());

    _editor = new ChooseMirrorConfigPanel();
    mainPnl = new JPanel(new BorderLayout());
    mainPnl.add(_editor, BorderLayout.CENTER);
    mainPnl.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(mainPnl, BorderLayout.CENTER);
    getContentPane().add(btnPnl, BorderLayout.SOUTH);
  }

  /**
   * Figure out is the users has provided a path.
   */
  public boolean isOk() {
    return _isOk;
  }

  /**
   * This inner class manages actions coming from the JButton Ok.
   */
  private class OkAction extends AbstractAction {
    /**
     * Manages JButton action
     */
    public void actionPerformed(ActionEvent e) {
      DBMSConfigurator config;
      String path, dPath;
      File fPath;

      _isOk = false;
      path = _editor.getDirectory();
      if (path == null || path.length() == 0) {
        JOptionPane.showMessageDialog(_parent,
            DBMSMessages.getString("ChooseMirrorConfigPanel.msg7") + "\n"
                + path, DBMSMessages.getString("DescriptorEditorDlg.msg1"),
            JOptionPane.WARNING_MESSAGE);
        return;
      }
      if (!DBMSAbstractConfig.authorizeLongFileName()
          && Utils.isPathNameContainsSpaceChar(path)) {
        JOptionPane.showMessageDialog(_parent,
            DBMSMessages.getString("ChooseMirrorConfigPanel.msg9"),
            DBMSMessages.getString("DescriptorEditorDlg.msg1"),
            JOptionPane.WARNING_MESSAGE);
        return;
      }
      path = Utils.terminatePath(path);
      // this was introduced for ngPlast 4.2+ because of new method
      // DBMirrorConfig.getMirrorPath()
      dPath = Utils.transformCode(path, false);
      if (dPath.indexOf(DBMirrorConfig.P_CODE) != -1
          || dPath.indexOf(DBMirrorConfig.N_CODE) != -1
          || dPath.indexOf(DBMirrorConfig.D_CODE) != -1) {
        JOptionPane.showMessageDialog(_parent,
            DBMSMessages.getString("ChooseMirrorConfigPanel.msg10"),
            DBMSMessages.getString("DescriptorEditorDlg.msg1"),
            JOptionPane.WARNING_MESSAGE);
        return;
      }

      // path is ok...
      fPath = new File(path);
      if (fPath.exists() == false) {
        if (fPath.mkdirs() == false) {
          JOptionPane.showMessageDialog(_parent,
              DBMSMessages.getString("ChooseMirrorConfigPanel.msg8") + "\n"
                  + path, DBMSMessages.getString("DescriptorEditorDlg.msg1"),
              JOptionPane.WARNING_MESSAGE);
          return;
        }
      }
      // save the value in the config file
      config = DBMSAbstractConfig.getConfigurator();
      if (config == null)
        return;
      config.setProperty(DBMSConfigurator.MIRROR_PATH, path);
      config.save();
      _isOk = true;
      dispose();
    }
  }

  /**
   * This inner class manages actions coming from the JButton Cancel.
   */
  private class CancelAction extends AbstractAction {
    /**
     * Manages JButton action
     */
    public void actionPerformed(ActionEvent e) {
      dispose();
    }
  }

  private class FDBDialogAdapter extends WindowAdapter {
    /**
     * Manages windowClosing event: hide the dialog.
     */
    public void windowClosing(WindowEvent e) {
      _isOk = false;
      dispose();
    }
  }
}
