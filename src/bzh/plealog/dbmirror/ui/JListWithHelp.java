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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

import javax.swing.JList;
import javax.swing.ListModel;

import bzh.plealog.bioinfo.util.CoreUtil;

/**
 * A standard JList capable of displaying a help message in the background when
 * the ListModel is empty.
 * 
 * @author Patrick G. Durand
 */
public class JListWithHelp extends JList<String> {
  private static final long serialVersionUID = -2308553448678734425L;
  private String            _msg;

  public JListWithHelp() {
    super();
  }

  public JListWithHelp(ListModel<String> lm) {
    super(lm);
  }

  public void setMessage(String msg) {
    _msg = msg;
  }

  public void paintComponent(Graphics g) {
    super.paintComponent(g);
    if (this.getModel().getSize() != 0 || _msg == null)
      return;
    Color oldClr;
    FontMetrics fm;
    Font oldFnt;
    String[] elements;
    int i, n, h;

    elements = CoreUtil.tokenize(_msg, "|");
    fm = this.getFontMetrics(this.getFont());
    oldClr = g.getColor();
    oldFnt = g.getFont();
    g.setFont(this.getFont());
    g.setColor(Color.LIGHT_GRAY);
    h = fm.getHeight();
    n = 1;
    for (i = 0; i < elements.length; i++) {
      g.drawString(elements[i], 2, n * h);
      n++;
      if (elements[i].indexOf('.') >= 0)
        n++;
    }
    g.setColor(oldClr);
    g.setFont(oldFnt);
  }
}