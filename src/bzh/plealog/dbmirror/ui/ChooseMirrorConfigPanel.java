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
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import bzh.plealog.dbmirror.ui.resources.DBMSMessages;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.plealog.genericapp.api.EZEnvironment;
import com.plealog.genericapp.api.file.EZFileManager;

@SuppressWarnings("serial")
public class ChooseMirrorConfigPanel extends JPanel {
  private JTextField _pathField;
  private JTextArea  _helpArea;

  public ChooseMirrorConfigPanel() {
    buildGUI();
  }

  public String getDirectory() {
    return _pathField.getText();
  }

  /**
   * Creates the GUI.
   */
  private void buildGUI() {
    JPanel panel1, panel2, mainPnl;

    panel1 = new JPanel(new BorderLayout());
    panel2 = new JPanel(new BorderLayout());
    mainPnl = new JPanel(new BorderLayout());

    panel2.add(getLocationPanel(), BorderLayout.NORTH);
    panel1.add(panel2, BorderLayout.SOUTH);
    panel1.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

    mainPnl.add(panel1, BorderLayout.NORTH);
    mainPnl.add(createHelper(), BorderLayout.SOUTH);
    mainPnl.setBorder(BorderFactory.createEtchedBorder());
    _helpArea.setText(DBMSMessages.getString("ChooseMirrorConfigPanel.msg1"));

    this.setLayout(new BorderLayout());
    this.add(mainPnl, BorderLayout.CENTER);
  }

  private JTextField createTxtField() {
    JTextField t;
    t = new JTextField();
    return t;
  }

  private JTextArea createHelper() {
    _helpArea = new JTextArea();
    _helpArea.setRows(5);
    _helpArea.setLineWrap(true);
    _helpArea.setWrapStyleWord(true);
    _helpArea.setEditable(false);
    _helpArea.setOpaque(false);
    _helpArea.setForeground(EZEnvironment.getSystemTextColor());
    return _helpArea;
  }

  /**
   * Utility method used to create the Mirror Installation Directory selector.
   */
  private Component getLocationPanel() {
    FormLayout layout;
    DefaultFormBuilder builder;
    JButton btn;
    ImageIcon icon;
    JPanel panel;
    HelpListener hlpL;
    String curPath;

    curPath = DBMSAbstractConfig.getLocalMirrorPath();
    _pathField = createTxtField();
    if (curPath != null)
      _pathField.setText(curPath);
    hlpL = new HelpListener(
        DBMSMessages.getString("ChooseMirrorConfigPanel.msg2"));
    _pathField.addFocusListener(hlpL);
    _pathField.addMouseListener(hlpL);
    icon = EZEnvironment.getImageIcon("openFile.png");
    if (icon != null) {
      btn = new JButton(icon);
    } else {
      btn = new JButton("...");
    }
    btn.addActionListener(new BrowseAction());
    btn.setToolTipText(DBMSMessages.getString("ChooseMirrorConfigPanel.msg3"));
    layout = new FormLayout("right:60dlu, 2dlu, 150dlu, 2dlu, 15dlu", "");
    builder = new DefaultFormBuilder(layout);
    builder.setDefaultDialogBorder();
    builder.append(DBMSMessages.getString("ChooseMirrorConfigPanel.msg4"),
        _pathField, btn);

    panel = new JPanel(new BorderLayout());
    panel.add(builder.getContainer(), BorderLayout.CENTER);
    panel.setBorder(BorderFactory.createTitledBorder(DBMSMessages
        .getString("ChooseMirrorConfigPanel.msg5")));
    return panel;
  }

  /**
   * Listener used to set some help message when the user clicks on the various
   * radio buttons.
   */
  private class HelpListener implements FocusListener, MouseListener {
    private String _hlpMsg;

    public HelpListener(String msg) {
      _hlpMsg = msg;
    }

    private void setMyText(String str) {
      _helpArea.setText(str);
    }

    public void focusGained(FocusEvent event) {
      setMyText(_hlpMsg);
    }

    public void focusLost(FocusEvent event) {
      setMyText("");
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
      // setMyText(_hlpMsg);
    }

    public void mouseExited(MouseEvent e) {
      // setMyText("");
    }
  }

  /**
   * Actions used to choose an existing DB location.
   */
  private class BrowseAction implements ActionListener {
    public void actionPerformed(ActionEvent event) {
      File path = EZFileManager.chooseDirectory(ChooseMirrorConfigPanel.this,
          DBMSMessages.getString("ChooseMirrorConfigPanel.msg6"), null);
      if (path == null)
        return;
      _pathField.setText(path.getAbsolutePath());
    }
  }

}
