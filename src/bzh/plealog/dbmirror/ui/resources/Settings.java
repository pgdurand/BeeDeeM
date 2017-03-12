/*
 * Copyright (c) 2003 JGoodies Karsten Lentzsch. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *  o Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer. 
 *     
 *  o Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution. 
 *     
 *  o Neither the name of JGoodies Karsten Lentzsch nor the names of 
 *    its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission. 
 *     
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */

package bzh.plealog.dbmirror.ui.resources;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.util.Properties;

import javax.swing.JInternalFrame;
import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.UIResource;

import com.jgoodies.looks.BorderStyle;
import com.jgoodies.looks.HeaderStyle;
import com.jgoodies.looks.plastic.PlasticLookAndFeel;
import com.jgoodies.looks.plastic.PlasticTheme;
import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;
import com.plealog.genericapp.api.EZEnvironment;

/**
 * Describes most of the optional settings of the JGoodies Looks. Used by the
 * <code>DemoFrame</code> to configure the UI.
 * 
 * @author Karsten Lentzsch
 * @version $Revision: 1.3 $
 * 
 * @see com.jgoodies.looks.BorderStyle
 * @see com.jgoodies.looks.HeaderStyle
 * @see com.jgoodies.looks.Options
 */
public final class Settings {
  protected static final String USE_LEGACY_THEME_PROPERTY_NAME = "KLegacyTheme" /*
                                                                                 * =
                                                                                 * >
                                                                                 * "legacy.theme"
                                                                                 */;
  private LookAndFeel           selectedLookAndFeel;

  private PlasticTheme          selectedTheme;

  private boolean               useNarrowButtons;

  private boolean               tabIconsEnabled;

  private String                plasticTabStyle;

  private boolean               plasticHighContrastFocusEnabled;

  private Boolean               popupDropShadowEnabled;

  private HeaderStyle           menuBarHeaderStyle;

  private BorderStyle           menuBarPlasticBorderStyle;

  private BorderStyle           menuBarWindowsBorderStyle;

  private Boolean               menuBar3DHint;

  private HeaderStyle           toolBarHeaderStyle;

  private BorderStyle           toolBarPlasticBorderStyle;

  private BorderStyle           toolBarWindowsBorderStyle;

  private Boolean               toolBar3DHint;

  // Instance Creation ******************************************************

  private Settings() {
    // Override default constructor; prevents instantiability.
  }

  public static Settings createDefault() {
    Settings settings = new Settings();
    settings.setSelectedLookAndFeel(new PlasticXPLookAndFeel());
    if (useLegacyTheme()) {
      settings.setSelectedTheme(PlasticLookAndFeel.createMyDefaultTheme());
    } else {
      settings.setSelectedTheme(new PThemeJGoodies());
      EZEnvironment.setSystemTextColor(Color.WHITE);
    }
    settings.setUseNarrowButtons(true);
    settings.setTabIconsEnabled(true);
    settings.setPlasticTabStyle(PlasticLookAndFeel.TAB_STYLE_DEFAULT_VALUE);
    settings.setPlasticHighContrastFocusEnabled(false);
    settings.setPopupDropShadowEnabled(null);
    settings.setMenuBarHeaderStyle(null);
    settings.setMenuBarPlasticBorderStyle(null);
    settings.setMenuBarWindowsBorderStyle(null);
    settings.setMenuBar3DHint(null);
    settings.setToolBarHeaderStyle(null);
    settings.setToolBarPlasticBorderStyle(null);
    settings.setToolBarWindowsBorderStyle(null);
    settings.setToolBar3DHint(null);
    overrideDefaults();
    return settings;
  }

  private static void overrideDefaults() {
    UIDefaults def = UIManager.getDefaults();
    Object[] defaults = { "InternalFrame.border",
        new BorderUIResource(new InternalFrameBorder()) /*
                                                         * ,
                                                         * "InternalFrame.paletteBorder"
                                                         * , new
                                                         * BorderUIResource(new
                                                         * PaletteBorder())
                                                         */};
    def.putDefaults(defaults);
  }

  // Accessors **************************************************************

  public Boolean getMenuBar3DHint() {
    return menuBar3DHint;
  }

  public void setMenuBar3DHint(Boolean menuBar3DHint) {
    this.menuBar3DHint = menuBar3DHint;
  }

  public HeaderStyle getMenuBarHeaderStyle() {
    return menuBarHeaderStyle;
  }

  public void setMenuBarHeaderStyle(HeaderStyle menuBarHeaderStyle) {
    this.menuBarHeaderStyle = menuBarHeaderStyle;
  }

  public BorderStyle getMenuBarPlasticBorderStyle() {
    return menuBarPlasticBorderStyle;
  }

  public void setMenuBarPlasticBorderStyle(BorderStyle menuBarPlasticBorderStyle) {
    this.menuBarPlasticBorderStyle = menuBarPlasticBorderStyle;
  }

  public BorderStyle getMenuBarWindowsBorderStyle() {
    return menuBarWindowsBorderStyle;
  }

  public void setMenuBarWindowsBorderStyle(BorderStyle menuBarWindowsBorderStyle) {
    this.menuBarWindowsBorderStyle = menuBarWindowsBorderStyle;
  }

  public Boolean isPopupDropShadowEnabled() {
    return popupDropShadowEnabled;
  }

  public void setPopupDropShadowEnabled(Boolean popupDropShadowEnabled) {
    this.popupDropShadowEnabled = popupDropShadowEnabled;
  }

  public boolean isPlasticHighContrastFocusEnabled() {
    return plasticHighContrastFocusEnabled;
  }

  public void setPlasticHighContrastFocusEnabled(
      boolean plasticHighContrastFocusEnabled) {
    this.plasticHighContrastFocusEnabled = plasticHighContrastFocusEnabled;
  }

  public String getPlasticTabStyle() {
    return plasticTabStyle;
  }

  public void setPlasticTabStyle(String plasticTabStyle) {
    this.plasticTabStyle = plasticTabStyle;
  }

  public LookAndFeel getSelectedLookAndFeel() {
    return selectedLookAndFeel;
  }

  public void setSelectedLookAndFeel(LookAndFeel selectedLookAndFeel) {
    this.selectedLookAndFeel = selectedLookAndFeel;
  }

  public void setSelectedLookAndFeel(String selectedLookAndFeelClassName) {
    try {
      Class<?> theClass = Class.forName(selectedLookAndFeelClassName);
      setSelectedLookAndFeel((LookAndFeel) theClass.newInstance());
    } catch (Exception e) {
      System.out.println("Can't instantiate " + selectedLookAndFeelClassName);
      e.printStackTrace();
    }
  }

  public PlasticTheme getSelectedTheme() {
    return selectedTheme;
  }

  public void setSelectedTheme(PlasticTheme selectedTheme) {
    this.selectedTheme = selectedTheme;
  }

  public boolean isTabIconsEnabled() {
    return tabIconsEnabled;
  }

  public void setTabIconsEnabled(boolean tabIconsEnabled) {
    this.tabIconsEnabled = tabIconsEnabled;
  }

  public Boolean getToolBar3DHint() {
    return toolBar3DHint;
  }

  public void setToolBar3DHint(Boolean toolBar3DHint) {
    this.toolBar3DHint = toolBar3DHint;
  }

  public HeaderStyle getToolBarHeaderStyle() {
    return toolBarHeaderStyle;
  }

  public void setToolBarHeaderStyle(HeaderStyle toolBarHeaderStyle) {
    this.toolBarHeaderStyle = toolBarHeaderStyle;
  }

  public BorderStyle getToolBarPlasticBorderStyle() {
    return toolBarPlasticBorderStyle;
  }

  public void setToolBarPlasticBorderStyle(BorderStyle toolBarPlasticBorderStyle) {
    this.toolBarPlasticBorderStyle = toolBarPlasticBorderStyle;
  }

  public BorderStyle getToolBarWindowsBorderStyle() {
    return toolBarWindowsBorderStyle;
  }

  public void setToolBarWindowsBorderStyle(BorderStyle toolBarWindowsBorderStyle) {
    this.toolBarWindowsBorderStyle = toolBarWindowsBorderStyle;
  }

  public boolean isUseNarrowButtons() {
    return useNarrowButtons;
  }

  public void setUseNarrowButtons(boolean useNarrowButtons) {
    this.useNarrowButtons = useNarrowButtons;
  }

  // Code retrieved from JGoodies com.jgoodies.looks.plastic.PlasticBorders
  // to overcome the 1 px unit for the border and set it to something clickable
  // by the user.
  @SuppressWarnings("serial")
  private static final class InternalFrameBorder extends AbstractBorder
      implements UIResource {

    private static final Insets NORMAL_INSETS    = new Insets(5, 5, 5, 5);
    private static final Insets MAXIMIZED_INSETS = new Insets(1, 1, 0, 0);

    // method from com.jgoodies.looks.plastic.PlastiUtils and adapted
    // to draw selected/unselected border
    void drawThinFlush3DBorder(Graphics g, int x, int y, int w, int h,
        boolean isSelected) {
      g.translate(x, y);
      if (isSelected)
        g.setColor(PlasticLookAndFeel.getWindowTitleBackground());
      else
        g.setColor(PlasticLookAndFeel.getWindowTitleInactiveBackground());
      g.fillRect(0, 0, w - 2, 5);// top
      g.fillRect(0, 0, 5, h - 2);// left
      g.fillRect(0, h - 5, w - 2, 5);// bottom
      g.fillRect(w - 5, 0, 5, h - 2);// right
      g.setColor(PlasticLookAndFeel.getControlDarkShadow());
      g.drawLine(0, 0, w - 2, 0);
      g.drawLine(0, 0, 0, h - 2);
      g.drawLine(w - 1, 0, w - 1, h - 1);
      g.drawLine(0, h - 1, w - 1, h - 1);
      g.translate(-x, -y);
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
      JInternalFrame frame = (JInternalFrame) c;
      if (frame.isMaximum())
        paintMaximizedBorder(g, x, y, w, h);
      else
        drawThinFlush3DBorder(g, x, y, w, h, frame.isSelected());

    }

    private void paintMaximizedBorder(Graphics g, int x, int y, int w, int h) {
      g.translate(x, y);
      g.setColor(PlasticLookAndFeel.getControlHighlight());
      g.drawLine(0, 0, w - 2, 0);
      g.drawLine(0, 0, 0, h - 2);
      g.translate(-x, -y);
    }

    public Insets getBorderInsets(Component c) {
      return ((JInternalFrame) c).isMaximum() ? MAXIMIZED_INSETS
          : NORMAL_INSETS;
    }
  }

  public static boolean useLegacyTheme() {
    Properties props = null;
    String argValue = "";

    argValue = "false";
    props = System.getProperties(); // get server JVM properties
    if (props.containsKey(USE_LEGACY_THEME_PROPERTY_NAME)) {
      argValue = props.getProperty(USE_LEGACY_THEME_PROPERTY_NAME);
    }
    return argValue.equals("true");
  }

  public static boolean useBlackTheme() {
    if (useLegacyTheme()) {
      return false;
    }
    return true;
  }

  /*
   * A border used for the palette of <code>JInternalFrame</code>s.
   */
  /*
   * private static final class PaletteBorder extends AbstractBorder implements
   * UIResource {
   * 
   * private static final Insets INSETS = new Insets(1, 1, 1, 1);
   * 
   * public void paintBorder(Component c, Graphics g, int x, int y, int w, int h
   * ) {
   * 
   * g.translate(x,y); g.setColor(PlasticLookAndFeel.getControlDarkShadow());
   * g.drawRect(0, 0, w-1, h-1); g.translate(-x,-y); }
   * 
   * public Insets getBorderInsets(Component c) { return INSETS; } }
   */
}