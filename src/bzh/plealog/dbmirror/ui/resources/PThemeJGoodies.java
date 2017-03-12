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
package bzh.plealog.dbmirror.ui.resources;

import java.awt.Color;

import javax.swing.plaf.ColorUIResource;

import com.jgoodies.looks.plastic.theme.InvertedColorTheme;

/**
 * An inverted theme with blue foreground colors and a grey window background.
 * 
 * @author Patrick G. Durand
 */

public class PThemeJGoodies extends InvertedColorTheme {

  private final ColorUIResource softWhite  = new ColorUIResource(Color.gray);
  // new ColorUIResource(29, 32, 36); // contour panel

  // foreground
  // contour
  private final ColorUIResource primary1   = new ColorUIResource(84, 116, 140);
  // loadBar / scrollbar
  private final ColorUIResource primary2   = new ColorUIResource(70, 97, 107);
  // list selection background
  private final ColorUIResource primary3   = new ColorUIResource(114, 158, 191);

  // windows
  // contour
  private final ColorUIResource secondary1 = new ColorUIResource(29, 32, 36);
  // select background
  private final ColorUIResource secondary2 = new ColorUIResource(35, 38, 44);
  // ui background
  private final ColorUIResource secondary3 = new ColorUIResource(43, 47, 54);

  @Override
  public String getName() {
    return "KLThemeJGoodies";
  }

  @Override
  protected ColorUIResource getPrimary1() {
    return primary1;
  }

  @Override
  protected ColorUIResource getPrimary2() {
    return primary2;
  }

  @Override
  protected ColorUIResource getPrimary3() {
    return primary3;
  }

  @Override
  protected ColorUIResource getSecondary1() {
    return secondary1;
  }

  @Override
  protected ColorUIResource getSecondary2() {
    return secondary2;
  }

  @Override
  protected ColorUIResource getSecondary3() {
    return secondary3;
  }

  @Override
  protected ColorUIResource getSoftWhite() {
    return softWhite;
  }

  // selected menu font color
  public ColorUIResource getMenuSelectedForeground() {
    return new ColorUIResource(Color.BLACK);
  }

  // selected list font color
  public ColorUIResource getHighlightedTextColor() {
    return new ColorUIResource(Color.BLACK);
  }

  // inactive control font color
  public ColorUIResource getInactiveControlTextColor() {
    return new ColorUIResource(Color.GRAY);
  }

  // inactive system font color
  public ColorUIResource getInactiveSystemTextColor() {
    return new ColorUIResource(Color.GRAY);
  }

}
