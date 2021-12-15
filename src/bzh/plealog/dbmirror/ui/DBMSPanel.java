/* Copyright (C) 2007-2021 Patrick G. Durand
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
import java.util.HashSet;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.plealog.genericapp.api.EZEnvironment;
import com.plealog.genericapp.ui.common.JHeadPanel;

import bzh.plealog.dbmirror.ui.resources.DBMSMessages;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.conf.DBMirrorConfig;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptor;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptor.TYPE;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptorUtils;
import bzh.plealog.dbmirror.util.descriptor.DescriptorEntry;

/**
 * This is the KDMS UserInterface. Do not use directly, see KDMSUserInterface
 * class.
 * 
 * @author Patrick G. Durand
 */
public class DBMSPanel extends JPanel {
  private static final long       serialVersionUID = 6056025700072879808L;
  private RunningMirrorPanel      _runner;
  private DescriptorList          _dscList;
  private LocalIndexInstallPanel  _lclIdxInstaller;
  private InstalledDescriptorList _dList;
  private InstalledDescriptorList _bList;
  private InstalledDescriptorList _iList;
  private JTabbedPane             _paneDescriptor;
  private static final Logger        LOGGER           = LogManager.getLogger(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                           + ".DBMSPanel");

  /**
   * Creates the user interface to KDMS.
   * 
   * @param dscPath
   *          the absolute path to the databank descriptor files. (.dsc files)
   */
  public DBMSPanel(String dscPath, boolean showBtnText) {
    JTabbedPane paneDB;
    JPanel pnl2 = new JPanel();
    JPanel pnl3 = new JPanel(new BorderLayout());
    JSplitPane jsp;
    JPanel sourcesDBPanel, installedDBPanel;
    List<DescriptorEntry> entries;
    HashSet<DBDescriptor.TYPE> types;
    DBMirrorConfig mConfig;

    pnl2.setLayout(new BorderLayout());

    entries = UIUtils.getDescriptors(dscPath);
    _dscList = new DescriptorList(dscPath, showBtnText);

    for (DescriptorEntry entry : entries) {
      _dscList.addDescriptor(entry);
    }

    _runner = new RunningMirrorPanel();
    _dscList.registerRunningMirrorPanel(_runner);
    _lclIdxInstaller = new LocalIndexInstallPanel(_runner);
    // _lclIdxInstaller.setRunningPanel(_runner);

    _paneDescriptor = new JTabbedPane();
    _paneDescriptor.add(DBMSMessages.getString("KDMSPanel.msg1"), _dscList);
    if (!DBMSAbstractConfig.enableBioClassifOnly()) {
      _paneDescriptor.add(DBMSMessages.getString("KDMSPanel.msg4"),
          _lclIdxInstaller);
    }
    _paneDescriptor.setFocusable(false);

    // pnl2.add(panePersonal, BorderLayout.NORTH);
    pnl2.add(_paneDescriptor, BorderLayout.CENTER);
    pnl2.add(_runner, BorderLayout.SOUTH);

    sourcesDBPanel = new JPanel(new BorderLayout());
    /*
     * sourcesDBPanel.add(getTitlePanel(KDMSMessages.getString("KDMSPanel.msg2"))
     * , BorderLayout.NORTH); sourcesDBPanel.add(pnl2, BorderLayout.CENTER);
     * sourcesDBPanel.setBorder(BorderFactory.createLineBorder(HEADER_CLR, 2));
     */
    sourcesDBPanel.add(
        getTitlePanel(EZEnvironment.getImageIcon("descriptors.png"),
            DBMSMessages.getString("KDMSPanel.msg2"), pnl2),
        BorderLayout.CENTER);

    mConfig = DBDescriptorUtils.getLocalDBMirrorConfig();

    // list of seq indexes
    types = new HashSet<TYPE>();
    types.add(DBDescriptor.TYPE.nucleic);
    types.add(DBDescriptor.TYPE.proteic);
    _iList = new InstalledDescriptorList(types,
        DBMSMessages.getString("KDMSPanel.msg8"), true, true, false,
        showBtnText, this);
    _iList.setDBList(mConfig);
    DBMSAbstractConfig.addDBMirrorListener(_iList);

    // list of blast dbs
    types = new HashSet<TYPE>();
    types.add(DBDescriptor.TYPE.blastn);
    types.add(DBDescriptor.TYPE.blastp);
    _bList = new InstalledDescriptorList(types,
        DBMSMessages.getString("KDMSPanel.msg9"), false, true, false,
        showBtnText, this);
    _bList.setDBList(mConfig);
    DBMSAbstractConfig.addDBMirrorListener(_bList);

    // list of dico indexes
    types = new HashSet<TYPE>();
    types.add(DBDescriptor.TYPE.dico);
    _dList = new InstalledDescriptorList(types,
        DBMSMessages.getString("KDMSPanel.msg10"), false, true, false,
        showBtnText, this);
    _dList.setDBList(mConfig);
    DBMSAbstractConfig.addDBMirrorListener(_dList);

    paneDB = new JTabbedPane();
    if (!DBMSAbstractConfig.enableBioClassifOnly()) {
      paneDB.add(DBMSMessages.getString("KDMSPanel.msg6"), _bList);
      paneDB.add(DBMSMessages.getString("KDMSPanel.msg5"), _iList);
    }
    paneDB.add(DBMSMessages.getString("KDMSPanel.msg7"), _dList);
    paneDB.setFocusable(false);

    installedDBPanel = new JPanel(new BorderLayout());
    /*
     * installedDBPanel.add(getTitlePanel(KDMSMessages.getString("KDMSPanel.msg3"
     * )), BorderLayout.NORTH); installedDBPanel.add(paneDB,
     * BorderLayout.CENTER);
     * installedDBPanel.setBorder(BorderFactory.createLineBorder(HEADER_CLR,
     * 2));
     */
    installedDBPanel.add(
        getTitlePanel(EZEnvironment.getImageIcon("databanks.png"),
            DBMSMessages.getString("KDMSPanel.msg3"), paneDB),
        BorderLayout.CENTER);
    pnl3.add(installedDBPanel, BorderLayout.CENTER);

    jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sourcesDBPanel, pnl3);

    this.setLayout(new BorderLayout());
    this.add(jsp, BorderLayout.CENTER);
  }

  /**
   * Display the local panel and initialize fields using the db descriptor.
   * 
   * @param db
   */
  public void displayPanelForReinstall(DBDescriptor db) {
    if (db != null) {
      if (db.getType().equals(DBDescriptor.TYPE.blastn)
          || db.getType().equals(DBDescriptor.TYPE.blastp)
          || db.getType().equals(DBDescriptor.TYPE.proteic)
          || db.getType().equals(DBDescriptor.TYPE.nucleic))
        try {
          // tab number for local panel is 1
          this._paneDescriptor.setSelectedIndex(1);
          this._lclIdxInstaller.getEditor().initForReinstall(db);
        } catch (Exception ex) {
          LOGGER.warn("Unable to init for reinstall", ex);
        }
    }
  }

  private JPanel getTitlePanel(Icon icon, String title, JComponent compo) {
    return new JHeadPanel(icon, title, compo);
  }

  public void setJobController(StartJobController controller) {
    _dscList.setJobController(controller);
    _bList.setJobController(controller);
    _dList.setJobController(controller);
    _iList.setJobController(controller);
  }

  public boolean hasJobRunning() {
    return _runner.hasJobRunning();
  }

  public boolean hasJobScheduled() {
    return _runner.hasJobScheduled();
  }

  public boolean canClose() {
    return _runner.canClose();
  }
}
