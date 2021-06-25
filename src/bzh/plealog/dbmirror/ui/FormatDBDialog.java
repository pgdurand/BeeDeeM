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
import java.io.File;
import java.text.MessageFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.text.JTextComponent;

import org.apache.commons.io.FileUtils;

import com.plealog.genericapp.api.EZEnvironment;

import bzh.plealog.dbmirror.lucenedico.DicoTermQuerySystem;
import bzh.plealog.dbmirror.reader.DBUtils;
import bzh.plealog.dbmirror.ui.resources.DBMSMessages;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.conf.DBMirrorConfig;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptor;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptorUtils;
import bzh.plealog.dbmirror.util.descriptor.IdxDescriptor;
import bzh.plealog.dbmirror.util.event.DBMirrorEvent;
import bzh.plealog.dbmirror.util.log.LoggerCentral;
import bzh.plealog.dbmirror.util.runner.DBMSExecNativeCommand;
import bzh.plealog.dbmirror.util.runner.DBStampProperties;
import bzh.plealog.dbmirror.util.runner.FormatDBMonitor;
import bzh.plealog.dbmirror.util.runner.FormatDBRunner;
import bzh.plealog.dbmirror.util.runner.UniqueIDGenerator;
import bzh.plealog.dbmirror.util.sequence.TaxonMatcherHelper;

/**
 * This class displays a dialogue box embedding a FormatDBPanel and a FormatDB
 * job runner thread.
 * 
 * @deprecated @see {@link bzh.plealog.dbmirror.ui.LocalIndexInstallPanel}
 * @author Patrick G. Durand
 */
@SuppressWarnings("serial")
@Deprecated
public class FormatDBDialog extends JDialog {

  private FormatDBPanel                _editor;
  private JButton                      _okBtn;
  private String                       _formatDBCmd;
  private String                       _dbxrefsConfig;
  private MyMonitor                    _jobMonitor;
  private HashSet<String>              _curDBNames;
  private boolean                      _jobSucceeded;
  private ImageIcon                    _animIcon      = EZEnvironment
                                                          .getImageIcon("circle_all.gif");
  private ImageIcon                    _notAnimIcon   = EZEnvironment
                                                          .getImageIcon("circle_back.gif");              ;
  private JLabel                       _animLbl;

  protected static final MessageFormat REPLACE_DB_MSG = new MessageFormat(
                                                          DBMSMessages
                                                              .getString("FormatDBDialog.replace.msg"));

  /**
   * Constructor.
   * 
   * @param owner
   *          the owner of this dialogue box.
   * @param formatDBCmd
   *          the absolute path to the FormatDB executable. If not available,
   *          just pass null.
   */
  public FormatDBDialog(Frame owner, String formatDBCmd, String dbxrefsConfig,
      HashSet<String> curDBNames, boolean enableUserDir) {
    super(owner, DBMSMessages.getString("FormatDBDialog.dlg.header"), true);
    _curDBNames = curDBNames;
    _formatDBCmd = formatDBCmd;
    _dbxrefsConfig = dbxrefsConfig;
    buildGUI(enableUserDir);
    _jobMonitor = new MyMonitor(_editor.getHelpArea());
    _editor.setMonitor(_jobMonitor);
    this.pack();
    this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    this.addWindowListener(new FDBDialogAdapter());
  }

  /**
   * Creates the GUI.
   */
  private void buildGUI(boolean enableUserDir) {
    JPanel mainPnl, btnPnl;
    JButton okBtn, cancelBtn;
    boolean macOS = DBMSExecNativeCommand.getOSType() == DBMSExecNativeCommand.MAC_OS;

    okBtn = new JButton(DBMSMessages.getString("FormatDBPanel.btn.ok"));
    cancelBtn = new JButton(DBMSMessages.getString("FormatDBPanel.btn.cancel"));
    okBtn.addActionListener(new FormatDBAction());
    cancelBtn.addActionListener(new CloseDialogAction());

    btnPnl = new JPanel();
    btnPnl.setLayout(new BoxLayout(btnPnl, BoxLayout.X_AXIS));
    btnPnl.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
    _animLbl = new JLabel(_notAnimIcon);
    btnPnl.add(_animLbl);
    btnPnl.add(Box.createHorizontalGlue());
    btnPnl.add(macOS ? cancelBtn : okBtn);
    btnPnl.add(Box.createRigidArea(new Dimension(10, 0)));
    btnPnl.add(macOS ? okBtn : cancelBtn);
    if (!macOS)
      btnPnl.add(Box.createHorizontalGlue());

    _okBtn = okBtn;

    _editor = new FormatDBPanel(_okBtn, enableUserDir);

    mainPnl = new JPanel(new BorderLayout());
    mainPnl.add(_editor, BorderLayout.CENTER);
    mainPnl.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(mainPnl, BorderLayout.CENTER);

    getContentPane().add(btnPnl, BorderLayout.SOUTH);
  }

  /**
   * Shows the dialog box on screen.
   */
  public void showDlg() {
    UIUtils.centerOnScreen(this);
    setVisible(true);
  }

  /**
   * Returns the DB name.
   */
  public String getDBName() {
    return (_editor.getDBName());
  }

  /**
   * Returns the DB location.
   */
  public String getDBPath() {
    return (_editor.getDBPath());
  }

  /**
   * Returns the DB type.
   */
  public boolean isProteic() {
    return _editor.isProteic();
  }

  /**
   * Figures out whether the FormatDB job terminated successfully.
   */
  public boolean doesJobSucceed() {
    return _jobSucceeded;
  }

  /**
   * Returns the content of this editor as a new DBDescriptor object.
   */
  public DBDescriptor getData() {
    if (!_jobSucceeded)
      return null;
    else
      return new DBDescriptor(getDBName(), getDBName() + " "
          + DBMSMessages.getString("FormatDBPanel.name.desc.suffix"),
          Utils.terminatePath(getDBPath()) + getDBName(),
          UniqueIDGenerator.getUniqueKey("dbc"),
          isProteic() ? DBDescriptor.TYPE.proteic : DBDescriptor.TYPE.nucleic,
          0, 0, 0);
  }

  /**
   * This inner class manages actions coming from the JButton CloseDialog.
   */
  private class CloseDialogAction extends AbstractAction {
    /**
     * Manages JButton action
     */
    public void actionPerformed(ActionEvent e) {
      if (_jobMonitor.isJobRunning()) {
        LoggerCentral.abortProcess();
      } else {
        dispose();
      }
    }
  }

  /**
   * This inner class manages actions coming from the JButton FormatDB.
   */
  private class FormatDBAction extends AbstractAction {
    private boolean confirmMessage(String msg) {
      int ret;

      ret = JOptionPane.showConfirmDialog(FormatDBDialog.this, msg,
          DBMSMessages.getString("FormatDBDialog.dlg.header"),
          JOptionPane.YES_NO_OPTION);
      return (ret == JOptionPane.YES_OPTION);
    }

    /**
     * Manages JButton action
     */
    public void actionPerformed(ActionEvent e) {
      if (!_editor.checkData())
        return;
      TaxonMatcherHelper taxMatcher;
      DBMirrorConfig mirrorConfig;
      DicoTermQuerySystem dico;
      FormatDBRunner runner;
      String dbName, taxInclude, taxExclude;

      mirrorConfig = DBDescriptorUtils.getDBMirrorConfig(DBMSAbstractConfig
          .getLocalMirrorConfFile());
      dico = DicoTermQuerySystem.getDicoTermQuerySystem(mirrorConfig);

      dbName = _editor.getDBName();
      if (_curDBNames.contains(dbName)) {
        if (!confirmMessage(REPLACE_DB_MSG.format(new Object[] { dbName })))
          return;
      }
      enableControls(false);
      taxInclude = _editor.getTaxInclude();
      if (taxInclude.length() == 0)
        taxInclude = null;
      taxExclude = _editor.getTaxExclude();
      if (taxExclude.length() == 0)
        taxExclude = null;
      if (taxInclude != null || taxExclude != null) {
        taxMatcher = new TaxonMatcherHelper();
        taxMatcher.setTaxonomyFilter(taxInclude, taxExclude);
        taxMatcher.initTaxonMatcher();
        if (taxMatcher.hasTaxonomyAvailable() == false) {
          JOptionPane.showMessageDialog(FormatDBDialog.this,
              DBMSMessages.getString("FormatDBDialog.noTaxErr"),
              DBMSMessages.getString("FormatDBDialog.dlg.header"),
              JOptionPane.WARNING_MESSAGE);
          taxMatcher.closeTaxonMatcher();
          return;
        }
      } else {
        taxMatcher = null;
      }
      _jobMonitor.reset();
      _editor.setMonitor(_jobMonitor);
      _animLbl.setIcon(_animIcon);
      // 06/06/2014 new KLib => use blast databank instead of volume files
      runner = new FormatDBRunner(_jobMonitor, _formatDBCmd, /* _formatDBCmd, */
      _dbxrefsConfig, _editor.getDBPath(), _editor.getDBName(),
          _editor.getFileList(), taxMatcher, dico, _editor.checkForNrID()
              || _editor.useNcbiIdFormat(), _editor.useNcbiIdFormat(),
          _editor.isProteic(), _editor.checkInputFiles(),
          DBUtils.NO_HEADER_FORMAT,
          DBMSAbstractConfig.getDefaultFastaVolumeSize());
      runner.start();
    }
  }

  /**
   * Listener of JDialog events.
   */
  private class FDBDialogAdapter extends WindowAdapter {
    /**
     * Manages windowClosing event: hide the dialog.
     */
    public void windowClosing(WindowEvent e) {
      if (_jobMonitor.isJobRunning())
        return;
      dispose();
    }
  }

  private void enableControls(boolean enable) {
    _okBtn.setEnabled(enable);
    _editor.enableControls(enable);
  }

  private class MyMonitor extends FormatDBMonitor {
    public MyMonitor(JTextComponent txtCompo) {
      super(txtCompo);
    }

    private void displayWarnMessage(String msg) {
      JOptionPane.showMessageDialog(FormatDBDialog.this, msg,
          DBMSMessages.getString("FormatDBDialog.dlg.header"),
          JOptionPane.WARNING_MESSAGE);
    }

    private void displayInfoMessage(String msg) {
      JOptionPane.showMessageDialog(FormatDBDialog.this, msg,
          DBMSMessages.getString("FormatDBDialog.dlg.header"),
          JOptionPane.INFORMATION_MESSAGE);
    }

    private void saveNewDescriptor() {
      List<IdxDescriptor> mirrorDescriptors = null;
      DBMirrorConfig mirrorConfig, newConfig;
      String reader;
      String dbPath;

      mirrorConfig = DBDescriptorUtils.getDBMirrorConfig(DBMSAbstractConfig
          .getLocalMirrorConfFile());
      mirrorDescriptors = DBDescriptorUtils.prepareIndexDBList(mirrorConfig);
      dbPath = getDBPath();
      // create the time stamp
      int totSeq = getTotalSequences();
      long bankSize = FileUtils.sizeOfDirectory(new File(dbPath));
      Date now = Calendar.getInstance().getTime();
      String installDate = DBStampProperties.BANK_DATE_FORMATTER.format(now); 
      String releaseDate = DBStampProperties.readReleaseDate(dbPath);
      DBStampProperties.writeDBStamp(dbPath, installDate, releaseDate, 
          new int[] { -1, totSeq == 0 ? -1 : totSeq }, bankSize);
      dbPath = Utils.terminatePath(getDBPath()) + getDBName()
          + FormatDBRunner.BLAST_ALIAS_TAG;
      if (isProteic()) {
        dbPath += FormatDBRunner.PROTEIN_ALIAS_EXT;
        reader = DBMirrorConfig.BLASTP_READER;
      } else {
        dbPath += FormatDBRunner.NUCLEIC_ALIAS_EXT;
        reader = DBMirrorConfig.BLASTN_READER;
      }
      if (new File(dbPath).exists()) {
        DBDescriptorUtils.addNewIndex(
            mirrorDescriptors,
            getDBName(),
            getDBName() + " "
                + DBMSMessages.getString("FormatDBPanel.name.desc.suffix"),
            Utils.transformCode(dbPath, false), reader,
            Utils.transformCode(dbPath, false));
      }
      newConfig = DBDescriptorUtils.getMirrorConfig(mirrorDescriptors,
          mirrorConfig);
      DBDescriptorUtils.saveDBMirrorConfig(
          DBMSAbstractConfig.getLocalMirrorConfFile(), newConfig);
      DBMSAbstractConfig.fireMirrorEvent(new DBMirrorEvent(newConfig,
          DBMirrorEvent.TYPE.dbAdded));

    }

    public void jobDone(boolean success) {
      _animLbl.setIcon(_notAnimIcon);
      if (success) {
        saveNewDescriptor();
        MessageFormat mf = new MessageFormat(
            DBMSMessages.getString("FormatDBPanel.msg1"));
        displayInfoMessage(mf.format(new Object[] { _editor.getDBName() }));
        _jobSucceeded = true;
        dispose();
      } else {
        _jobSucceeded = false;
        displayWarnMessage(DBMSMessages.getString("FormatDBDialog.err1")
            + this.getErrMsg());
        enableControls(true);
      }
      _jobMonitor.reset();
      _editor.setMonitor(null);
    }
  }
}
