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

import java.awt.event.ActionEvent;
import java.io.IOException;

import javax.swing.Icon;

import com.Ostermiller.util.Browser;
import com.plealog.genericapp.api.EZEnvironment;
import com.plealog.genericapp.ui.menu.EZBasicAction;

/**
 * Show HelpModule action.
 * 
 * @author Patrick G. Durand
 */
@SuppressWarnings("serial")
public class ActionShowHelp extends EZBasicAction {
  
  /**
   * Default constructor.
   */
  public ActionShowHelp() {
    super();
  }

  /**
   * Constructor with a user object.
   */
  public ActionShowHelp(Object usrObject) {
    super(usrObject);
  }

  /**
   * Constructor with an action name.
   */
  public ActionShowHelp(String name) {
    super(name);
  }

  /**
   * Constructor with a user object and an action name.
   */
  public ActionShowHelp(Object usrObject, String name) {
    super(usrObject, name);
  }

  /**
   * Constructor with a user object, an action name and an action icon.
   */
  public ActionShowHelp(Object usrObject, String name, Icon icon) {
    super(usrObject, name, icon);
  }

  /**
   * Constructor with an action name and an action icon.
   */
  public ActionShowHelp(String name, Icon icon) {
    super(name, icon);
  }

  /**
   * Performs the action.
   */
  public void actionPerformed(ActionEvent e) {
    try {
      Browser.init();
      EZEnvironment.setWaitCursor();
      Browser.displayURL("https://pgdurand.gitbooks.io/databank-manager-system/");
      EZEnvironment.setDefaultCursor();
    } catch (IOException ex) {
      EZEnvironment.displayWarnMessage(EZEnvironment.getParentFrame(), "Unable to start web browser.");
    }
  }

}
