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

import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class LocalIndexInstallPanel extends JPanel {

  private static final long serialVersionUID = 6419022676810771866L;
  private LocalDBPanel      editor;

  public LocalIndexInstallPanel(RunningMirrorPanel runningPanel) {
    buildGUI(runningPanel);
  }

  public LocalDBPanel getEditor() {
    return this.editor;
  }

  public void setRunningPanel(RunningMirrorPanel runningPanel) {
    this.editor.setRunningPanel(runningPanel);
  }

  private void buildGUI(RunningMirrorPanel runningPanel) {
    JScrollPane scroller;
    this.editor = new LocalDBPanel(runningPanel);

    this.setLayout(new BorderLayout());
    scroller = new JScrollPane(this.editor,
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    this.add(scroller, BorderLayout.CENTER);
  }

}
