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

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class SetPermissionsOnDescriptor {
  private JTextField _permsField;
  private JPanel     _uPanel;

  public SetPermissionsOnDescriptor() {
    getUserPermPanel();
  }

  private JPanel getUserPermPanel() {
    JPanel idPanel;

    if (_uPanel != null)
      return _uPanel;
    _permsField = new JTextField();

    idPanel = new JPanel(new BorderLayout());
    idPanel.add(new JLabel("Authorized user names: "), BorderLayout.WEST);
    idPanel.add(_permsField, BorderLayout.CENTER);
    idPanel.add(new JLabel(
        "(use a comma-separated list of user names; '*' = all users)"),
        BorderLayout.SOUTH);
    _uPanel = idPanel;

    return _uPanel;
  }

  public String getUserNames(JComponent parent, String initText) {
    String uName = null;
    int ret;

    _permsField.setText(initText);
    ret = JOptionPane.showOptionDialog(
        JOptionPane.getFrameForComponent(parent), getUserPermPanel(),
        "Set user permissions on Descriptor", JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE, null, null, null);

    // exit when cancel or close
    if (ret == JOptionPane.CANCEL_OPTION || ret == JOptionPane.CLOSED_OPTION) {
      return null;
    }
    uName = _permsField.getText();
    if (uName.length() < 1)
      return null;
    return uName;
  }
}
