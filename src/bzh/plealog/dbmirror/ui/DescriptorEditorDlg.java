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
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.fetcher.DBServerConfig;
import bzh.plealog.dbmirror.ui.resources.DBMSMessages;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.runner.DBMSExecNativeCommand;

import com.plealog.prefs4j.api.DataConnector;
import com.plealog.prefs4j.api.DataConnectorFactory;
import com.plealog.prefs4j.api.PreferenceSection;
import com.plealog.prefs4j.implem.core.PreferenceSectionImplem;
import com.plealog.prefs4j.ui.PreferenceEditor;
import com.plealog.prefs4j.ui.PreferenceEditorFactory;

/**
 * This class is used to open a Databank Descriptor Editor.
 * 
 * @author Patrick G. Durand
 */
@SuppressWarnings("serial")
public class DescriptorEditorDlg extends JDialog {
  private PreferenceEditor    _editor;
  private Frame               _parent;
  private File                _providedDscPath;
  private File                _editedDscPath;
  private boolean             _error;

  private static final String DEF_RES  = "cfgEditorDefault.dsc";
  private static final String CFG_RES  = "cfgEditorDefaultDesc.config";
  private static final String CFG_PROP = "cfgEditorDefaultMsg.properties";

  private static final Log    LOGGER   = LogFactory
                                           .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                               + ".DescriptorEditorDlg");

  // note: all parameters are mandatory. Do not provide descriptor sets to null.
  public DescriptorEditorDlg(String dscPath, Frame parent, File descriptor) {
    super(parent, DBMSMessages.getString("DescriptorEditorDlg.msg1"), true);

    _parent = parent;
    _providedDscPath = descriptor;

    buildGUI(descriptor);

    this.pack();
    this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    this.addWindowListener(new FDBDialogAdapter());
  }

  /**
   * Shows the dialog box on screen.
   */
  public void showDlg() {
    if (_error)
      return;
    // center on screen
    Dimension screenSize = this.getToolkit().getScreenSize();
    Dimension dlgSize = this.getSize();

    this.setLocation(screenSize.width / 2 - dlgSize.width / 2,
        screenSize.height / 2 - dlgSize.height / 2);
    // show
    setVisible(true);
  }

  private File prepareTmpFile(String fName, String resourceFileName,
      File resourceFile) throws IOException {
    BufferedInputStream bis;
    File f;
    String tmpDir;
    int n;
    byte[] buf = new byte[2048];
    File resFile;
    FileOutputStream fos = null;
    InputStream in = null;

    try {
      if (resourceFile != null) {
        in = new FileInputStream(resourceFile);
      } else {
        resFile = new File(DBMSAbstractConfig.getLocalMirrorPath(),
            resourceFileName);
        // first, try to locate the file in the user conf dir
        if (resFile.exists()) {
          in = new FileInputStream(resFile);
        } else {// load from Jar
          in = DBMSMessages.class.getResourceAsStream(resourceFileName);
        }
      }

      tmpDir = Utils.terminatePath(System.getProperty("java.io.tmpdir"));
      if (fName == null)
        f = File.createTempFile("kdms", ".cfg");
      else
        f = new File(tmpDir + fName);
      fos = new FileOutputStream(f);
      bis = new BufferedInputStream(in);
      while ((n = bis.read(buf)) != -1) {
        fos.write(buf, 0, n);
      }
      fos.flush();
      fos.close();
      in.close();

    } catch (Exception ex) {
      throw new IOException("Unable to locate resource: " + fName + ": " + ex);
    } finally {
      IOUtils.closeQuietly(in);
      IOUtils.closeQuietly(fos);
    }

    return f;
  }

  private PreferenceSection getConfig(File resourceFile, File descFile,
      File configFile) throws Exception {
    PreferenceSectionImplem co = new PreferenceSectionImplem("co");
    co.setConfType(PreferenceEditor.TYPE.kvp.name());
    co.setName(descFile.getName());
    co.setDescriptorLocator(descFile.getAbsolutePath());
    co.setConfigurationLocator(configFile.getAbsolutePath());
    co.setResourceLocator(resourceFile.getAbsolutePath());
    co.setDataConnector(DataConnectorFactory
        .getDataConnector(DataConnector.TYPE.props.toString()));
    return co;
  }

  private void buildGUI(File descFile) {
    JPanel mainPnl, btnPnl;
    JButton okBtn, cancelBtn;
    File f1, f2, f3;
    PreferenceSection co;
    boolean macOS = DBMSExecNativeCommand.getOSType() == DBMSExecNativeCommand.MAC_OS;

    try {
      f1 = prepareTmpFile(CFG_PROP, CFG_PROP, null);
      f2 = prepareTmpFile(CFG_RES, CFG_RES, null);
      // this has to be done since all the config files (messages bundle,
      // descriptor
      // and form descriptor) have to be in the same directory.
      if (descFile != null) {
        f3 = prepareTmpFile(descFile.getName(), null, descFile);
      } else {
        f3 = prepareTmpFile(null, DEF_RES, null);
      }
      _editedDscPath = f3;

      co = getConfig(f1, f2, f3);
      _editor = PreferenceEditorFactory.getEditor(co, null);
    } catch (Exception e) {
      _error = true;
      LOGGER.warn("unable to prepare descriptor editor : " + e);
      JOptionPane.showMessageDialog(_parent,
          DBMSMessages.getString("DescriptorEditorDlg.msg2"),
          DBMSMessages.getString("DescriptorEditorDlg.msg1"),
          JOptionPane.WARNING_MESSAGE);
      return;
    }

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

    mainPnl = new JPanel(new BorderLayout());
    mainPnl.add(_editor.getEditor(), BorderLayout.CENTER);
    mainPnl.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(mainPnl, BorderLayout.CENTER);
    getContentPane().add(btnPnl, BorderLayout.SOUTH);
  }

  /**
   * Method to be called after a call to method showDlg.
   */
  public boolean emitError() {
    return _error;
  }

  /**
   * This inner class manages actions coming from the JButton Ok.
   */
  private class OkAction extends AbstractAction {
    private void checkDBPath(File f) {
      DBServerConfig dbConf;
      FileOutputStream fos = null;
      String name, type, dbPath;
      int idx;

      // load the descriptor
      dbConf = new DBServerConfig();
      try {
        dbConf.load(f.getAbsolutePath());
      } catch (Exception e) {
        throw new RuntimeException(
            DBMSMessages.getString("DescriptorEditorDlg.msg4"));
      }

      // compute update db.ldir value given name and db.type
      name = dbConf.getName();
      type = dbConf.getTypeCode();
      dbPath = dbConf.getDBLocalInstallDir();
      idx = dbPath.indexOf('|');
      dbPath = dbPath.substring(0, idx) + "|" + type + "|" + name;
      dbConf.setDBLocalInstallDir(dbPath);

      // save the updated descriptor
      try {
        fos = new FileOutputStream(f);
        dbConf.store(fos, name);
      } catch (Exception ex) {
        throw new RuntimeException(
            DBMSMessages.getString("DescriptorEditorDlg.msg5"));
      } finally {
        IOUtils.closeQuietly(fos);
      }
    }

    /**
     * Manages JButton action
     */
    public void actionPerformed(ActionEvent e) {
      if (_editor.isEdited() == false) {
        dispose();
        return;
      }
      _editor.saveData();
      /*
       * String msg; msg = _editor.saveData(); if (msg != null) {
       * JOptionPane.showMessageDialog(_parent, msg,
       * KDMSMessages.getString("DescriptorEditorDlg.msg1"),
       * JOptionPane.WARNING_MESSAGE); dispose(); _error = true; return; }
       */
      // save ok: copy tmp file to the config one
      if (_providedDscPath != null) {
        try {
          // after edition we need to check "db.ldir" vs. "db.type" (bug fix
          // KDMS-12)
          checkDBPath(_editedDscPath);
          // copy new descriptor to KDMS conf dir
          Utils.copyBinFile(_editedDscPath, _providedDscPath);
        } catch (Exception e1) {
          LOGGER.warn(DBMSMessages.getString("DescriptorEditorDlg.msg6") + ": "
              + e1);
          JOptionPane.showMessageDialog(_parent,
              DBMSMessages.getString("DescriptorEditorDlg.msg3"),
              DBMSMessages.getString("DescriptorEditorDlg.msg1"),
              JOptionPane.WARNING_MESSAGE);
          _error = true;
        }
      }

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
      dispose();
    }
  }
}
