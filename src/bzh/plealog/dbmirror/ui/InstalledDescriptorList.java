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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import bzh.plealog.dbmirror.lucenedico.DicoTermQuerySystem;
import bzh.plealog.dbmirror.ui.resources.DBMSMessages;
import bzh.plealog.dbmirror.util.ant.PAntTasks;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.conf.DBMirrorConfig;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptor;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptorUtils;
import bzh.plealog.dbmirror.util.descriptor.IdxDescriptor;
import bzh.plealog.dbmirror.util.event.DBMirrorEvent;
import bzh.plealog.dbmirror.util.event.DBMirrorListener;
import bzh.plealog.dbmirror.util.log.LoggerCentral;
import bzh.plealog.dbmirror.util.runner.DBMSExecNativeCommand;

import com.plealog.genericapp.api.EZEnvironment;

/**
 * This class is used to display the list of installed databank.
 * 
 * @author Patrick G. Durand
 */
public class InstalledDescriptorList extends JPanel implements DBMirrorListener {
  /**
   * 
   */
  private static final long          serialVersionUID = -2042922121003301971L;
  private JTextArea                  _helpArea;
  private InstalledDescriptorTable   _dbList;
  private JButton                    _btnUp;
  private JButton                    _btnDown;
  private DeleteAction               _deleteAction;
  private ReinstallDbAction          _reInstallAction = null;
  private SetUserPermissionsAction   _userPermAction;
  private ChangeMirrorAction         _changeMirror;
  private HashSet<DBDescriptor.TYPE> _types;
  private List<IdxDescriptor>        _curDescs;
  private StartJobController         _jobController;
  private boolean                    _enableReordering;
  private boolean                    _enableUserPermissions;
  private DBMSPanel                  _kdmsPanel;
  private JButton                    btnReinstall;

  private static final ImageIcon     upIco            = EZEnvironment
                                                          .getImageIcon("navigate_up.png");
  private static final ImageIcon     downIco          = EZEnvironment
                                                          .getImageIcon("navigate_down.png");

  public InstalledDescriptorList(HashSet<DBDescriptor.TYPE> types,
      String helpMsg, boolean enableReordering, boolean showCommands,
      boolean enableUserPermissions, boolean showBtnText, DBMSPanel kdmsPanel) {
    _enableReordering = enableReordering;
    _types = types;
    _enableUserPermissions = enableUserPermissions;
    buildGUI(showCommands, showBtnText);
    _helpArea.setText(helpMsg);
    _kdmsPanel = kdmsPanel;
  }

  public void setJobController(StartJobController controller) {
    _jobController = controller;
  }

  private void buildGUI(boolean showCommands, boolean showBtnText) {
    JPanel panel, mainPnl, btnPnl, helpPnl;
    JScrollPane scroll;
    String msg;
    MoveActionListener actListener;
    JComponent cmds;

    panel = new JPanel(new BorderLayout());

    _dbList = new InstalledDescriptorTable(new InstalledDescriptorTableModel(
        DBMSAbstractConfig.isStandalone() && _enableUserPermissions));
    _dbList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    _dbList.getSelectionModel().addListSelectionListener(
        new DBListSelectionListener());
    // size is based on icons (24x24)
    _dbList.setRowHeight(30);
    scroll = new JScrollPane(_dbList);

    this.addComponentListener(_dbList.getComponentAdapter());

    mainPnl = new JPanel(new BorderLayout());
    mainPnl.add(scroll, BorderLayout.CENTER);
    cmds = getCommands(showBtnText);
    if (showCommands)
      mainPnl.add(cmds, BorderLayout.SOUTH);

    msg = DBMSMessages.getString("InstalledDescriptorList.msg1");
    if (upIco != null)
      _btnUp = new JButton(upIco);
    else
      _btnUp = new JButton(msg);
    _btnUp.setToolTipText(msg);

    msg = DBMSMessages.getString("InstalledDescriptorList.msg2");
    if (downIco != null)
      _btnDown = new JButton(downIco);
    else
      _btnDown = new JButton(msg);
    _btnDown.setToolTipText(msg);

    actListener = new MoveActionListener();
    _btnUp.addActionListener(actListener);
    _btnDown.addActionListener(actListener);

    btnPnl = new JPanel();
    btnPnl.setLayout(new BoxLayout(btnPnl, BoxLayout.Y_AXIS));
    btnPnl.add(Box.createVerticalGlue());
    btnPnl.add(_btnUp);
    btnPnl.add(Box.createRigidArea(new Dimension(0, 10)));
    btnPnl.add(_btnDown);
    btnPnl.add(Box.createVerticalGlue());

    panel.add(mainPnl, BorderLayout.CENTER);
    if (showCommands)
      panel.add(btnPnl, BorderLayout.EAST);

    helpPnl = new JPanel(new BorderLayout());
    helpPnl.add(createHelper(), BorderLayout.CENTER);
    helpPnl.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
    this.setLayout(new BorderLayout());
    this.add(panel, BorderLayout.CENTER);
    if (showCommands)
      this.add(helpPnl, BorderLayout.SOUTH);
    _btnUp.setEnabled(_enableReordering);
    _btnDown.setEnabled(_enableReordering);
    enableCommands(false);
    // the following are required to enable a correct use of JSplitPane (see
    // KDMSPanel)
    this.setPreferredSize(new Dimension(350, 250));
    this.setMinimumSize(new Dimension(50, 50));
    this.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
  }

  private JTextArea createHelper() {
    _helpArea = new JTextArea();
    _helpArea.setRows(5);
    _helpArea.setLineWrap(true);
    _helpArea.setWrapStyleWord(true);
    _helpArea.setEditable(false);
    _helpArea.setOpaque(false);
    _helpArea.setForeground(EZEnvironment.getSystemTextColor());
    _helpArea.setBorder(BorderFactory.createLineBorder(Color.GRAY));
    return _helpArea;
  }

  private JComponent getCommands(boolean showBtnText) {
    JPanel pnl;
    JToolBar tBar;
    ImageIcon icon;
    JButton btn;

    tBar = new JToolBar();
    tBar.setFloatable(false);
    pnl = new JPanel(new BorderLayout());
    pnl.add(tBar, BorderLayout.EAST);

    icon = EZEnvironment.getImageIcon("repositoryEdit.png");
    if (icon == null)
      _changeMirror = new ChangeMirrorAction(
          DBMSMessages.getString("InstalledDescriptorList.msg19"));
    else
      _changeMirror = new ChangeMirrorAction(icon);
    btn = tBar.add(_changeMirror);
    btn.setToolTipText(DBMSMessages.getString("InstalledDescriptorList.msg20"));
    if (showBtnText)
      btn.setText(DBMSMessages.getString("InstalledDescriptorList.msg19"));

    icon = EZEnvironment.getImageIcon("dbdelete2.png");
    if (icon == null)
      _deleteAction = new DeleteAction(
          DBMSMessages.getString("InstalledDescriptorList.msg6"));
    else
      _deleteAction = new DeleteAction(icon);
    btn = tBar.add(_deleteAction);
    btn.setToolTipText(DBMSMessages.getString("InstalledDescriptorList.msg7"));
    if (showBtnText)
      btn.setText(DBMSMessages.getString("InstalledDescriptorList.msg6"));

    // Reinstall DB
    tBar.addSeparator();
    icon = EZEnvironment.getImageIcon("dbinstall.png");
    if (icon == null) {
      _reInstallAction = new ReinstallDbAction(
          DBMSMessages.getString("InstalledDescriptorList.ReInstall"));
    } else {
      _reInstallAction = new ReinstallDbAction(icon);
    }
    btnReinstall = tBar.add(_reInstallAction);
    btnReinstall.setToolTipText(DBMSMessages
        .getString("InstalledDescriptorList.ReInstall.ToolTip"));
    if (showBtnText)
      btnReinstall.setText(DBMSMessages
          .getString("InstalledDescriptorList.ReInstall"));

    // user permissions
    if (DBMSAbstractConfig.isStandalone() && _enableUserPermissions) {
      tBar.addSeparator();
      icon = EZEnvironment.getImageIcon("user_edit.png");
      if (icon == null)
        _userPermAction = new SetUserPermissionsAction(
            DBMSMessages.getString("InstalledDescriptorList.msg25"));
      else
        _userPermAction = new SetUserPermissionsAction(icon);
      btn = tBar.add(_userPermAction);
      btn.setToolTipText(DBMSMessages
          .getString("InstalledDescriptorList.msg26"));
      if (showBtnText)
        btn.setText(DBMSMessages.getString("InstalledDescriptorList.msg25"));

    }

    return pnl;
  }

  private void enableCommands(boolean enable) {
    if (_enableReordering) {
      _btnDown.setEnabled(enable);
      _btnUp.setEnabled(enable);
    }
    _deleteAction.setEnabled(enable);
    _reInstallAction.setEnabled(enable);
    if (_userPermAction != null) {
      _userPermAction.setEnabled(enable);
    }
  }

  public void setDBList(DBMirrorConfig config) {
    InstalledDescriptorTableModel model;
    Iterator<IdxDescriptor> iter;
    IdxDescriptor db;

    _dbList.clearSelection();
    _curDescs = DBDescriptorUtils.prepareIndexDBList(config);
    // display DB by ascending order of their name only if reordering is not
    // allowed
    if (!_enableReordering)
      _curDescs = DBDescriptorUtils.sort2(_curDescs);
    model = new InstalledDescriptorTableModel(DBMSAbstractConfig.isStandalone()
        && _enableUserPermissions);
    iter = _curDescs.iterator();
    while (iter.hasNext()) {
      db = iter.next();
      if (_types != null && _types.contains(db.getType())) {
        model.addDescriptor(db);
      }
    }
    _dbList.setModel(model);
    _dbList.recompteColumnSize();

  }

  public IdxDescriptor getSelectedDescriptor() {
    try {
      return (IdxDescriptor) _dbList.getValueAt(_dbList.getSelectedRow(), -1);
    } catch (Exception ex) {
      return null;
    }
  }

  public void mirrorChanged(DBMirrorEvent event) {
    setDBList(event.getMirrorConfig());
  }

  private boolean canDoAction() {
    // another external job is running ? (KB Blast for example when KDMS is
    // running there)
    if (_jobController != null && _jobController.canStartAJob() == false) {
      String msg;

      msg = _jobController.getControllerMsg();
      if (msg == null)
        msg = DBMSMessages.getString("DescriptorList.msg19");
      JOptionPane.showMessageDialog(
          JOptionPane.getFrameForComponent(InstalledDescriptorList.this), msg,
          DBMSMessages.getString("ChooseMirrorConfigDlg.header"),
          JOptionPane.INFORMATION_MESSAGE);
      return false;
    }
    // KDMS installer is running ?
    if (LoggerCentral.isRunning()) {
      JOptionPane.showMessageDialog(
          JOptionPane.getFrameForComponent(InstalledDescriptorList.this),
          DBMSMessages.getString("InstalledDescriptorList.msg22"),
          DBMSMessages.getString("ChooseMirrorConfigDlg.header"),
          JOptionPane.INFORMATION_MESSAGE | JOptionPane.OK_OPTION);
      return false;
    }
    return true;
  }

  private class DBListSelectionListener implements ListSelectionListener {
    public void valueChanged(ListSelectionEvent e) {
      if (e.getValueIsAdjusting())
        return;
      enableCommands(!_dbList.getSelectionModel().isSelectionEmpty());
      if (_userPermAction != null) {
        _userPermAction.setEnabled(_dbList.getSelectionModel()
            .getMinSelectionIndex() == _dbList.getSelectionModel()
            .getMaxSelectionIndex());
      }
      if (getSelectedDescriptor() != null) {
        btnReinstall
            .setEnabled(getSelectedDescriptor().getType() != DBDescriptor.TYPE.dico);
      }
    }
  }

  private class MoveActionListener implements ActionListener {
    private void saveConfig(IdxDescriptor idx1, int delta) {
      DBMirrorConfig curConf;
      int pos1 = -1, i = 0;

      for (IdxDescriptor idx : _curDescs) {
        if (idx.getKbCode().equalsIgnoreCase(idx1.getKbCode())) {
          pos1 = i;
          break;
        }
        i++;
      }
      if (pos1 == -1)// shoudn't happened, but...
        return;
      IdxDescriptor idx = _curDescs.get(pos1);
      _curDescs.remove(idx);
      _curDescs.add(pos1 + delta, idx);

      curConf = DBDescriptorUtils.getDBMirrorConfig(DBMSAbstractConfig
          .getLocalMirrorConfFile());
      DBDescriptorUtils.saveDBMirrorConfig(
          DBMSAbstractConfig.getLocalMirrorConfFile(),
          DBDescriptorUtils.getMirrorConfig(_curDescs, curConf));
    }

    private void move(int delta) {
      InstalledDescriptorTableModel dlm;
      IdxDescriptor idx1, idx2;
      int idx;

      idx = _dbList.getSelectedRow();
      dlm = (InstalledDescriptorTableModel) _dbList.getModel();
      if (delta == -1 && idx == 0)
        return;
      if (delta == 1 && idx == dlm.getRowCount() - 1)
        return;
      // can move Idx of same types
      idx1 = (IdxDescriptor) dlm.getValueAt(idx, -1);
      idx2 = (IdxDescriptor) dlm.getValueAt(idx + delta, -1);
      if (idx1.getType().equals(idx2.getType()) == false)
        return;
      // obj = dlm.get(idx);
      dlm.removeEntry(idx);
      dlm.insertDescriptor(idx1, idx + delta);
      _dbList.setSelectedRow(idx + delta);
      saveConfig(idx1, delta);
    }

    public void actionPerformed(ActionEvent event) {
      ListSelectionModel lsm;

      lsm = _dbList.getSelectionModel();
      if (lsm.isSelectionEmpty())
        return;
      move(event.getSource() == _btnUp ? -1 : 1);
    }
  }

  private class ReinstallDbAction extends AbstractAction {
    /**
     * 
     */
    private static final long serialVersionUID = 571018358614605750L;

    public ReinstallDbAction(Icon icon) {
      super("", icon);
    }

    public ReinstallDbAction(String name) {
      super(name);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      _kdmsPanel.displayPanelForReinstall(getSelectedDescriptor());
    }

  }

  private class ChangeMirrorAction extends AbstractAction {
    /**
     * 
     */
    private static final long serialVersionUID = 7647032770432897819L;

    public ChangeMirrorAction(Icon icon) {
      super("", icon);
    }

    public ChangeMirrorAction(String name) {
      super(name);
    }

    public void actionPerformed(ActionEvent e) {
      if (!canDoAction())
        return;
      ChooseMirrorConfigDlg configDlg = new ChooseMirrorConfigDlg(
          JOptionPane.getFrameForComponent(InstalledDescriptorList.this));
      configDlg.showDlg();
      if (configDlg.isOk()) {
        DBMirrorConfig mirrorConfig = DBDescriptorUtils
            .getDBMirrorConfig(DBMSAbstractConfig.getLocalMirrorConfFile());
        DBMSAbstractConfig.fireMirrorEvent(new DBMirrorEvent(mirrorConfig,
            DBMirrorEvent.TYPE.dbChanged));
      }
    }
  }

  private class SetUserPermissionsAction extends AbstractAction {
    /**
     * 
     */
    private static final long          serialVersionUID = -9188479993791509330L;
    private SetPermissionsOnDescriptor descEditor;

    public SetUserPermissionsAction(Icon icon) {
      super("", icon);
      descEditor = new SetPermissionsOnDescriptor();
    }

    public SetUserPermissionsAction(String name) {
      super(name);
      descEditor = new SetPermissionsOnDescriptor();
    }

    public void actionPerformed(ActionEvent e) {
      DBMirrorConfig curConf;
      IdxDescriptor desc;
      String uName;

      desc = (IdxDescriptor) _dbList.getValueAt(_dbList.getSelectedRow(), -1);
      if (desc == null)
        return;
      uName = descEditor.getUserNames(InstalledDescriptorList.this,
          desc.getAuthorizedUsers());
      if (uName == null)
        return;
      desc.setAuthorizedUsers(uName);
      curConf = DBDescriptorUtils.getDBMirrorConfig(DBMSAbstractConfig
          .getLocalMirrorConfFile());
      DBDescriptorUtils.saveDBMirrorConfig(
          DBMSAbstractConfig.getLocalMirrorConfFile(),
          DBDescriptorUtils.getMirrorConfig(_curDescs, curConf));
      ((InstalledDescriptorTableModel) _dbList.getModel())
          .fireTableDataChanged();

    }
  }

  private class DeleteAction extends AbstractAction {
    /**
     * 
     */
    private static final long serialVersionUID = -3121042073274111263L;

    public DeleteAction(Icon icon) {
      super("", icon);
    }

    public DeleteAction(String name) {
      super(name);
    }

    public void actionPerformed(ActionEvent e) {
      if (!canDoAction())
        return;
      // this is to avoid lock of the UI during time consuming delete operation
      new DeleteThread().start();
    }
  }

  private class DeleteThread extends Thread {
    private MessageFormat _formatter = new MessageFormat(
                                         DBMSMessages
                                             .getString("InstalledDescriptorList.msg9b"));

    private String getPotentialyDeletedBanks(String path, boolean osWin) {
      StringBuffer buf;
      IdxDescriptor d;
      String lPath, path2;
      int i, size;

      buf = new StringBuffer();
      // this was added because sometimes disk letter can be in lower or upper
      // case!
      if (osWin)
        lPath = path.toUpperCase();
      else
        lPath = path;
      size = _curDescs.size();
      for (i = 0; i < size; i++) {
        d = (IdxDescriptor) _curDescs.get(i);
        // when creating simultaneously data index and blast db, both types
        // of databank are in the same directory : when deleting one, actually
        // all the content of 'path' is removed. As a consequence, we have
        // to discard the other databank.
        path2 = d.getCode();
        if (osWin)
          path2 = path2.toUpperCase();
        if (path2.startsWith(lPath)) {
          buf.append(d.getName());
          buf.append("\n");
        }
      }
      return buf.toString();
    }

    private void doTask(Frame f) {
      ArrayList<IdxDescriptor> data;
      DBMirrorConfig mConfig;
      IdxDescriptor desc, d;
      String path, path2, deletedDbs;
      boolean discard, osWin;
      int idx, i, size, ret;
      List<String> deletedCodes = new ArrayList<String>();

      desc = (IdxDescriptor) _dbList.getValueAt(_dbList.getSelectedRow(), -1);
      if (desc == null)
        return;

      path = desc.getCode();
      idx = path.indexOf(DBMSAbstractConfig.CURRENT_DIR);
      if (idx != -1) {
        // mirror handled by KDMS
        path = path.substring(0, idx);
      } else {
        // personal DB : do not delete it, since it may be contained within a
        // directory
        // that also contains other databanks.
        path = null;
      }
      ret = JOptionPane.showConfirmDialog(
          JOptionPane.getFrameForComponent(InstalledDescriptorList.this),
          DBMSMessages.getString("InstalledDescriptorList.msg9"),
          DBMSMessages.getString("InstalledDescriptorList.msg8"),
          JOptionPane.YES_NO_OPTION);
      if (ret == JOptionPane.NO_OPTION)
        return;
      osWin = DBMSExecNativeCommand.getOSType() == DBMSExecNativeCommand.WINDOWS_OS;
      discard = false;
      if (path != null) {
        deletedDbs = getPotentialyDeletedBanks(path, osWin);
        ret = JOptionPane.showConfirmDialog(
            JOptionPane.getFrameForComponent(InstalledDescriptorList.this),
            _formatter.format(new Object[] { path, deletedDbs }),
            DBMSMessages.getString("InstalledDescriptorList.msg8"),
            JOptionPane.YES_NO_CANCEL_OPTION);
        if (ret == JOptionPane.CANCEL_OPTION)
          return;
        discard = (ret == JOptionPane.YES_OPTION);
        if (discard) {
          EZEnvironment.setWaitCursor();
          if (!PAntTasks.deleteDirectory(path)) {

            DicoTermQuerySystem.closeDicoTermQuerySystem();
            if (!PAntTasks.deleteDirectory(path)) {
              EZEnvironment.setDefaultCursor();
              JOptionPane.showMessageDialog(JOptionPane
                  .getFrameForComponent(InstalledDescriptorList.this),
                  DBMSMessages.getString("InstalledDescriptorList.msg10"),
                  DBMSMessages.getString("InstalledDescriptorList.msg8"),
                  JOptionPane.WARNING_MESSAGE);
              return;
            }

          }
        }
      }
      data = new ArrayList<IdxDescriptor>();
      size = _curDescs.size();
      // this was added because sometimes disk letter can be in lower or upper
      // case!
      if (osWin)
        path = path.toUpperCase();
      for (i = 0; i < size; i++) {
        d = (IdxDescriptor) _curDescs.get(i);
        if (d != desc) {// compare by ref is ok here
          // when creating simultaneously data index and blast db, both types
          // of databank are in the same directory : when deleting one, actually
          // all the content of 'path' is removed. As a consequence, we have
          // to discard the other databank.
          path2 = d.getCode();
          if (osWin)
            path2 = path2.toUpperCase();
          if (discard) {
            if (path2.startsWith(path) == false) {
              data.add(d);
            } else {
              deletedCodes.add(d.getKbCode());
            }
          } else {
            data.add(d);
          }
        } else {
          deletedCodes.add(d.getKbCode());
        }
      }
      mConfig = DBDescriptorUtils.getMirrorConfig(data, null);
      // store this deleted index to not be re-used
      mConfig.removeMirrorCode(deletedCodes);
      DBDescriptorUtils.saveDBMirrorConfig(
          DBMSAbstractConfig.getLocalMirrorConfFile(), mConfig);
      DBMSAbstractConfig.fireMirrorEvent(new DBMirrorEvent(mConfig,
          DBMirrorEvent.TYPE.dbRemoved));
      _dbList.recompteColumnSize();
    }

    public void run() {
      Frame f;
      f = JOptionPane.getFrameForComponent(InstalledDescriptorList.this);
      doTask(f);
      EZEnvironment.setDefaultCursor();
    }
  }
}
