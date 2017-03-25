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
package bzh.plealog.dbmirror.main;

import java.awt.BorderLayout;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Properties;
import java.util.ResourceBundle;

import javax.swing.JPanel;

import bzh.plealog.dbmirror.ui.DBMSPanel;
import bzh.plealog.dbmirror.ui.DBMSUserInterface;
import bzh.plealog.dbmirror.ui.resources.DBMSMessages;
import bzh.plealog.dbmirror.util.conf.Configuration;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;

import com.plealog.genericapp.api.EZApplicationBranding;
import com.plealog.genericapp.api.EZEnvironment;
import com.plealog.genericapp.api.EZGenericApplication;
import com.plealog.genericapp.api.EZUIStarterListener;

public class UiInstaller {

  public static void main(String[] args) {
    // This has to be done at the very beginning, i.e. first method call within
    // main().
    EZGenericApplication.initialize("DB-Manager");
    // Add application branding
    Properties props = StarterUtils.getVersionProperties();
    EZApplicationBranding.setAppName(props.getProperty("prg.app.name"));
    EZApplicationBranding.setAppVersion(props.getProperty("prg.version"));
    EZApplicationBranding.setCopyRight(props.getProperty("prg.copyright"));
    EZApplicationBranding.setProviderName(props.getProperty("prg.provider"));

    EZEnvironment.addResourceLocator(DBMSMessages.class);

    // ...before any attempt to load a resource
    EZApplicationBranding.setAppIcon(EZEnvironment.getImageIcon("appicon.png"));

    // we load our ResourceBundle containing the main menu declarations
    ResourceBundle rb = ResourceBundle.getBundle(DBMSMessages.class
        .getPackage().getName() + ".menu");
    EZEnvironment.setUserDefinedActionsResourceBundle(rb);

    // we load our message resource bundle
    rb = ResourceBundle.getBundle(DBMSMessages.class.getPackage().getName()
        + ".messages");
    EZEnvironment.setUserDefinedMessagesResourceBundle(rb);

    StarterUtils.configureApplication(null, "kdmsUI", true, true, true);
    DBMSAbstractConfig.setStandalone(true);

    // install the action manager listener to easily handle actions
    EZEnvironment.getActionsManager().addActionMenuListener(
        new MyActionManager());
    // Add a listener to application startup cycle (see below)
    EZEnvironment.setUIStarterListener(new MyStarterListener());

    // We setup the Preferences Dialogue Box
    String confPath = DBMSAbstractConfig.getInstallAppConfPath(Configuration.SYSTEM);
    confPath += "prefEditor.config";
    EZEnvironment.setPreferencesConfigurationFile(confPath);

    // Start the application
    EZGenericApplication.startApplication(args);
  }

  private static class MyStarterListener implements EZUIStarterListener {
    private JPanel mainPanel = null;

    @Override
    public Component getApplicationComponent() {
      if (mainPanel != null)
        return mainPanel;

      mainPanel = new JPanel(new BorderLayout());
      DBMSPanel pnl = DBMSUserInterface.getUserInterface(
          DBMSAbstractConfig.getInstallAppConfPath(Configuration.DESCRIPTOR), true);
      mainPanel.add(pnl, BorderLayout.CENTER);
      return mainPanel;
    }

    @Override
    public boolean isAboutToQuit() {
      // You can add some code to figure out if application can exit.

      // Return false to prevent application from exiting (e.g. a background
      // task is still running).
      // Return true otherwise.

      // Do not add a Quit dialogue box to ask user confirmation: the framework
      // already does that for you.
      return true;
    }

    @Override
    public void postStart() {
      // This method is called by the framework just before displaying UI
      // (main frame).
    }

    @Override
    public void preStart() {
      // This method is called by the framework at the very beginning of
      // application startup.
    }
  }

  /*
   * Show how to work with generic action (see ui.properties for the definition
   * of action FileOpen)
   */
  private static class MyActionManager implements PropertyChangeListener {

    @Override
    public void propertyChange(PropertyChangeEvent event) {
    }
  }
}
