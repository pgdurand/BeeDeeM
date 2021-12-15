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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.ListCellRenderer;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.plealog.genericapp.api.EZEnvironment;
import com.plealog.genericapp.api.file.EZFileFilter;
import com.plealog.genericapp.api.file.EZFileManager;
import com.plealog.genericapp.api.file.EZFileUtils;

import bzh.plealog.dbmirror.fetcher.DBServerConfig;
import bzh.plealog.dbmirror.fetcher.PFTPLoaderDescriptor;
import bzh.plealog.dbmirror.lucenedico.DicoTermQuerySystem;
import bzh.plealog.dbmirror.lucenedico.Dicos;
import bzh.plealog.dbmirror.ui.resources.DBMSMessages;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.conf.DBMirrorConfig;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptorUtils;
import bzh.plealog.dbmirror.util.descriptor.DescriptorEntry;
import bzh.plealog.dbmirror.util.runner.DBMSExecNativeCommand;

/**
 * This class is used to display the list of available databank descriptors.
 * 
 * @author Patrick G. Durand
 */
@SuppressWarnings("serial")
public class DescriptorList extends JPanel {

  private JList<DescriptorEntry>            _dscList;
  private DefaultListModel<DescriptorEntry> _dscModel;
  private StartProcessingAction             _startProcessAction;
  private EditDescriptorAction              _editDescriptorAction;
  private CopyDescriptorAction              _copyDescriptorAction;
  private DeleteDescriptorAction            _deleteDescriptorAction;
  private RunningMirrorPanel                _runnerPanel;
  private MyCellRenderer                    _cellRenderer;
  private String                            _dscPath;
  private DBDescComparator                  _descComparator;
  private StartJobController                _jobController;
  private ExportDescriptorAction            _exportDescriptorAction;
  private ImportDescriptorAction            _importDescriptorAction;
  private PreferencesAction                 _prefsAction;

  private static final Logger                  LOGGER               = LogManager.getLogger(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                                         + ".DescriptorList");

  // remove the dot defore dsc
  private static final String               DESCRIPTOR_FNAME_EXT = DBMSAbstractConfig.FEXT_DD
                                                                     .substring(1);

  public DescriptorList(String dscPath, boolean showBtnText) {
    _descComparator = new DBDescComparator();
    _dscPath = Utils.terminatePath(dscPath);
    _dscModel = new DefaultListModel<>();
    createUI(showBtnText);
  }

  private void createUI(boolean showBtnText) {
    JPanel tBarPnl, tHeaderPnl;
    FontMetrics fm;

    _dscList = new JList<>(_dscModel);
    _dscList.addListSelectionListener(new MyListSelectionListener());
    _cellRenderer = new MyCellRenderer(_dscList);
    _dscList.setCellRenderer(_cellRenderer);
    fm = _dscList.getFontMetrics(_dscList.getFont());
    _dscList.setFixedCellHeight(3 * fm.getHeight());

    tBarPnl = new JPanel(new BorderLayout());

    tBarPnl.add(createTBar(showBtnText), BorderLayout.EAST);

    enableCommand(0);

    tHeaderPnl = new JPanel(new BorderLayout());
    tHeaderPnl.add(new JLabel(DBMSMessages.getString("DescriptorList.msg21")),
        BorderLayout.WEST);
    tHeaderPnl.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 0));
    this.setLayout(new BorderLayout());
    this.add(tHeaderPnl, BorderLayout.NORTH);
    this.add(new JScrollPane(_dscList), BorderLayout.CENTER);
    this.add(tBarPnl, BorderLayout.SOUTH);
    // the following are required to enable a correct use of JSplitPane (see
    // KDMSPanel)
    this.setPreferredSize(new Dimension(350, 250));
    this.setMinimumSize(new Dimension(50, 50));
    this.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

  }

  public void registerRunningMirrorPanel(RunningMirrorPanel pnl) {
    _runnerPanel = pnl;
  }

  public void setJobController(StartJobController controller) {
    _jobController = controller;
  }

  /**
   * Adds a new descriptor in the list. Since all added descriptors are
   * reordered by their type, this method returns the position (zero-based
   * value) of the added descriptor in the list.
   */
  public int addDescriptor(DescriptorEntry descriptor) {
    Hashtable<String, ArrayList<DescriptorEntry>> allDescs;
    ArrayList<DescriptorEntry> descs;
    DescriptorEntry de;
    DefaultListModel<DescriptorEntry> model;
    int i, size, sel = 0;

    if (DBMSAbstractConfig.enableBioClassifOnly()
        && !descriptor.getDescriptor().isDictionary()) {
      return 0;
    }

    // do not display descriptor with an empty description
    // this kind of descriptors are certainly a depends one and must be masked
    if (StringUtils.isBlank(descriptor.getDescription())) {
      return 0;
    }

    allDescs = new Hashtable<String, ArrayList<DescriptorEntry>>();
    descs = new ArrayList<DescriptorEntry>();
    size = _dscModel.getSize();
    // add descriptor by type ordering
    for (String type : DBServerConfig.TYPE_ORDER) {
      descs = allDescs.get(type);
      if (descs == null) {
        descs = new ArrayList<DescriptorEntry>();
        allDescs.put(type, descs);
      }
      for (i = 0; i < size; i++) {
        de = _dscModel.getElementAt(i);
        if (type.equals(de.getDescriptor().getTypeCode())) {
          descs.add(de);
        }
      }
      if (type.equals(descriptor.getDescriptor().getTypeCode())) {
        descs.add(descriptor);
      }
    }
    model = new DefaultListModel<>();
    i = 0;
    for (String type : DBServerConfig.TYPE_ORDER) {
      descs = allDescs.get(type);
      Collections.sort(descs, _descComparator);
      for (DescriptorEntry entry : descs) {
        model.addElement(entry);
        if (entry.getDescriptor().getName()
            .equals(descriptor.getDescriptor().getName())) {
          sel = i;
        }
        i++;
      }
    }
    descriptor.setHolder(_dscList);
    _dscList.setModel(model);
    _dscModel = model;
    return sel;
  }

  private int getDescriptorListPositionByName(String name) {
    DescriptorEntry de;
    int i, size;

    size = _dscModel.getSize();
    for (i = 0; i < size; i++) {
      de = _dscModel.getElementAt(i);
      if (de.getName().equals(name)) {
        return i;
      }
    }
    return -1;
  }

  public List<DescriptorEntry> getSelectedDescriptors() {
    ArrayList<DescriptorEntry> descs;
    int[] selIdx;

    descs = new ArrayList<DescriptorEntry>();
    selIdx = _dscList.getSelectedIndices();
    for (int i : selIdx) {
      descs.add(_dscModel.getElementAt(i));
    }
    return descs;
  }

  private JToolBar createTBar(boolean showBtnText) {
    JToolBar tBar;
    JButton btn;
    ImageIcon icon;

    tBar = new JToolBar();
    tBar.setFloatable(false);

    if (DBMSAbstractConfig.enablePrefsInToolBar()) {
      icon = EZEnvironment.getImageIcon("prefs.png");
      if (icon == null)
        _prefsAction = new PreferencesAction(
            DBMSMessages.getString("DescriptorList.msg37"));
      else
        _prefsAction = new PreferencesAction(icon);
      btn = tBar.add(_prefsAction);
      btn.setToolTipText(DBMSMessages.getString("DescriptorList.msg38"));
      if (showBtnText)
        btn.setText(DBMSMessages.getString("DescriptorList.msg37"));

      tBar.addSeparator();
    }

    icon = EZEnvironment.getImageIcon("dbinstall.png");
    if (icon == null)
      _startProcessAction = new StartProcessingAction(
          DBMSMessages.getString("DescriptorList.msg17"));
    else
      _startProcessAction = new StartProcessingAction(icon);
    btn = tBar.add(_startProcessAction);
    btn.setToolTipText(DBMSMessages.getString("DescriptorList.msg18"));
    if (showBtnText)
      btn.setText(DBMSMessages.getString("DescriptorList.msg17"));

    tBar.addSeparator();

    icon = EZEnvironment.getImageIcon("dbnew.png");
    if (icon == null)
      _copyDescriptorAction = new CopyDescriptorAction(
          DBMSMessages.getString("DescriptorList.msg8"));
    else
      _copyDescriptorAction = new CopyDescriptorAction(icon);
    btn = tBar.add(_copyDescriptorAction);
    btn.setToolTipText(DBMSMessages.getString("DescriptorList.msg9"));
    if (showBtnText)
      btn.setText(DBMSMessages.getString("DescriptorList.msg8"));

    icon = EZEnvironment.getImageIcon("dbedit.png");
    if (icon == null)
      _editDescriptorAction = new EditDescriptorAction(
          DBMSMessages.getString("DescriptorList.msg1"));
    else
      _editDescriptorAction = new EditDescriptorAction(icon);
    btn = tBar.add(_editDescriptorAction);
    btn.setToolTipText(DBMSMessages.getString("DescriptorList.msg2"));
    if (showBtnText)
      btn.setText(DBMSMessages.getString("DescriptorList.msg1"));

    icon = EZEnvironment.getImageIcon("dbdelete.png");
    if (icon == null)
      _deleteDescriptorAction = new DeleteDescriptorAction(
          DBMSMessages.getString("DescriptorList.msg11"));
    else
      _deleteDescriptorAction = new DeleteDescriptorAction(icon);
    btn = tBar.add(_deleteDescriptorAction);
    btn.setToolTipText(DBMSMessages.getString("DescriptorList.msg12"));
    if (showBtnText)
      btn.setText(DBMSMessages.getString("DescriptorList.msg11"));

    tBar.addSeparator();

    icon = EZEnvironment.getImageIcon("dbexport.png");
    if (icon == null)
      _exportDescriptorAction = new ExportDescriptorAction(
          DBMSMessages.getString("DescriptorList.msg28"));
    else
      _exportDescriptorAction = new ExportDescriptorAction(icon);
    btn = tBar.add(_exportDescriptorAction);
    btn.setToolTipText(DBMSMessages.getString("DescriptorList.msg29"));
    if (showBtnText)
      btn.setText(DBMSMessages.getString("DescriptorList.msg28"));

    icon = EZEnvironment.getImageIcon("dbimport.png");
    if (icon == null)
      _importDescriptorAction = new ImportDescriptorAction(
          DBMSMessages.getString("DescriptorList.msg32"));
    else
      _importDescriptorAction = new ImportDescriptorAction(icon);
    btn = tBar.add(_importDescriptorAction);
    btn.setToolTipText(DBMSMessages.getString("DescriptorList.msg33"));
    if (showBtnText)
      btn.setText(DBMSMessages.getString("DescriptorList.msg32"));

    return tBar;
  }

  private void enableCommand(int selectedEntries) {
    _startProcessAction.setEnabled(selectedEntries != 0);
    _editDescriptorAction.setEnabled(selectedEntries == 1);
    _copyDescriptorAction.setEnabled(selectedEntries == 1);
    _deleteDescriptorAction.setEnabled(selectedEntries == 1);
    _exportDescriptorAction.setEnabled(selectedEntries == 1);
    _importDescriptorAction.setEnabled(true);
  }

  private DBServerConfig readConfig(File f) {
    // update edited DescriptorEntry in the JList
    DBServerConfig desc;
    try {
      desc = new DBServerConfig();
      desc.load(f.getAbsolutePath());
    } catch (Exception ex) {
      // should not happen...
      LOGGER.warn(ex);
      JOptionPane.showMessageDialog(
          JOptionPane.getFrameForComponent(DescriptorList.this),
          DBMSMessages.getString("DescriptorList.msg3"),
          DBMSMessages.getString("DescriptorList.msg0"),
          JOptionPane.WARNING_MESSAGE);
      return null;
    }
    return desc;
  }

  private boolean updateDbConfig(DBServerConfig config, File cfgFile) {
    FileOutputStream fos = null;
    String cfgName;
    boolean bRet = false;

    cfgName = cfgFile.getName();
    cfgName = cfgName.substring(0, cfgName.indexOf(DBMSAbstractConfig.FEXT_DD));

    config.setName(cfgName);
    config.setLocalFolder(DBMSExecNativeCommand.MIRRORDIR_VAR_NAME + "|"
        + config.getTypeCode() + "|" + cfgName);
    try {
      fos = new FileOutputStream(cfgFile);
      config.store(fos, cfgName);
      bRet = true;
    } catch (Exception ex) {
      LOGGER.warn(ex);
      JOptionPane.showMessageDialog(
          JOptionPane.getFrameForComponent(DescriptorList.this),
          DBMSMessages.getString("DescriptorList.msg10"),
          DBMSMessages.getString("DescriptorList.msg0"),
          JOptionPane.WARNING_MESSAGE);
    } finally {
      IOUtils.closeQuietly(fos);
    }
    return bRet;
  }

  private int sortDescriptors(DescriptorEntry descriptorToReselect) {
    Hashtable<String, ArrayList<DescriptorEntry>> allDescs;
    ArrayList<DescriptorEntry> descs;
    DescriptorEntry de;
    DefaultListModel<DescriptorEntry> model;
    int i, size, sel = 0;

    allDescs = new Hashtable<String, ArrayList<DescriptorEntry>>();
    descs = new ArrayList<DescriptorEntry>();
    size = _dscModel.getSize();
    // add descriptor by type ordering
    for (String type : DBServerConfig.TYPE_ORDER) {
      descs = allDescs.get(type);
      if (descs == null) {
        descs = new ArrayList<DescriptorEntry>();
        allDescs.put(type, descs);
      }
      for (i = 0; i < size; i++) {
        de = _dscModel.getElementAt(i);
        if (type.equals(de.getDescriptor().getTypeCode())) {
          descs.add(de);
        }
      }
    }
    model = new DefaultListModel<>();
    i = 0;
    for (String type : DBServerConfig.TYPE_ORDER) {
      descs = allDescs.get(type);
      Collections.sort(descs, _descComparator);
      for (DescriptorEntry entry : descs) {
        model.addElement(entry);
        if (entry.getDescriptor().getName()
            .equals(descriptorToReselect.getDescriptor().getName())) {
          sel = i;
        }
        i++;
      }
    }
    _dscList.setModel(model);
    _dscModel = model;
    return sel;
  }

  private void editDescriptor(DescriptorEntry de) {
    DescriptorEditorDlg editor;
    int sel;

    editor = new DescriptorEditorDlg(_dscPath,
        JOptionPane.getFrameForComponent(DescriptorList.this), new File(
            de.getFile()));
    editor.showDlg();
    if (editor.emitError())
      return;
    // update edited DescriptorEntry in the JList
    DBServerConfig desc = readConfig(new File(de.getFile()));
    if (desc == null)
      return;
    de.setDescriptor(desc);
    sel = sortDescriptors(de);
    _dscList.setSelectedIndex(sel);
    _dscList.ensureIndexIsVisible(sel);
  }

  private String getNewName(DBServerConfig desc) {
    Object value;
    String name = null;
    File newDesc;
    // ask for a new name
    while (true) {
      value = JOptionPane.showInputDialog(
          JOptionPane.getFrameForComponent(DescriptorList.this),
          DBMSMessages.getString("DescriptorList.msg4b"),
          DBMSMessages.getString("DescriptorList.msg4a"),
          JOptionPane.QUESTION_MESSAGE, null, null, desc.getName());

      // cancel ?
      if (value == null)
        return null;
      name = value.toString();
      // invalid name ?
      if (name.length() == 0 || !Utils.isValidFileName(name)) {
        JOptionPane.showMessageDialog(
            JOptionPane.getFrameForComponent(DescriptorList.this),
            DBMSMessages.getString("DescriptorList.msg5"),
            DBMSMessages.getString("DescriptorList.msg0"),
            JOptionPane.WARNING_MESSAGE);
        continue;
      }
      newDesc = new File(_dscPath + name + DBMSAbstractConfig.FEXT_DD);
      if (newDesc.exists()) {
        JOptionPane.showMessageDialog(
            JOptionPane.getFrameForComponent(DescriptorList.this),
            DBMSMessages.getString("DescriptorList.msg6"),
            DBMSMessages.getString("DescriptorList.msg0"),
            JOptionPane.WARNING_MESSAGE);
        continue;
      }
      break;
    }
    return name;
  }

  private class DeleteDescriptorAction extends AbstractAction {
    public DeleteDescriptorAction(Icon icon) {
      super("", icon);
    }

    public DeleteDescriptorAction(String name) {
      super(name);
    }

    public void actionPerformed(ActionEvent e) {
      DescriptorEntry de;
      int[] idx;
      int ret;

      idx = _dscList.getSelectedIndices();
      if (idx.length != 1) {
        return;
      }
      ret = JOptionPane.showConfirmDialog(
          JOptionPane.getFrameForComponent(DescriptorList.this),
          DBMSMessages.getString("DescriptorList.msg13"),
          DBMSMessages.getString("DescriptorList.msg0"),
          JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
      if (ret == JOptionPane.NO_OPTION)
        return;
      de = _dscList.getModel().getElementAt(idx[0]);
      if (de.getStatus().equals(DescriptorEntry.STATUS.waiting)
          || de.getStatus().equals(DescriptorEntry.STATUS.running)) {
        JOptionPane.showMessageDialog(
            JOptionPane.getFrameForComponent(DescriptorList.this),
            DBMSMessages.getString("DescriptorList.msg24"),
            DBMSMessages.getString("DescriptorList.msg0"),
            JOptionPane.INFORMATION_MESSAGE);
        return;
      }
      try {
        new File(de.getFile()).delete();
      } catch (Exception e1) {
        LOGGER.warn(e1);
        JOptionPane.showMessageDialog(
            JOptionPane.getFrameForComponent(DescriptorList.this),
            DBMSMessages.getString("DescriptorList.msg14"),
            DBMSMessages.getString("DescriptorList.msg0"),
            JOptionPane.WARNING_MESSAGE);
        return;
      }
      _dscModel.removeElementAt(idx[0]);
      ret = idx[0] - 1;
      _dscList.setSelectedIndex(ret < 0 ? 0 : ret);
    }
  }

  private class PreferencesAction extends AbstractAction {
    public PreferencesAction(Icon icon) {
      super("", icon);
    }

    public PreferencesAction(String name) {
      super(name);
    }

    public void actionPerformed(ActionEvent e) {
      // PreferencesSystem.displayPreferencesDialog();
    }
  }

  private class CopyDescriptorAction extends AbstractAction {
    public CopyDescriptorAction(Icon icon) {
      super("", icon);
    }

    public CopyDescriptorAction(String name) {
      super(name);
    }

    public void actionPerformed(ActionEvent e) {
      DescriptorEntry de;
      String name;
      File newDesc;
      int[] idx;
      int sel;

      idx = _dscList.getSelectedIndices();
      if (idx.length != 1) {
        return;
      }
      de = _dscList.getModel().getElementAt(idx[0]);
      name = getNewName(de.getDescriptor());
      if (name == null)
        return;
      newDesc = new File(_dscPath + name + DBMSAbstractConfig.FEXT_DD);
      try {
        Utils.copyBinFile(new File(de.getFile()), newDesc);
      } catch (IOException e1) {
        LOGGER.warn(e1);
        JOptionPane.showMessageDialog(
            JOptionPane.getFrameForComponent(DescriptorList.this),
            DBMSMessages.getString("DescriptorList.msg7"),
            DBMSMessages.getString("DescriptorList.msg0"),
            JOptionPane.WARNING_MESSAGE);
        return;
      }
      DBServerConfig desc = readConfig(newDesc);
      if (desc == null)
        return;
      if (!updateDbConfig(desc, newDesc)) {
        newDesc.delete();
        return;
      }
      _dscList.clearSelection();
      de = new DescriptorEntry(desc, newDesc.getAbsolutePath());
      sel = addDescriptor(de);
      _dscList.setSelectedIndex(sel);
      _dscList.ensureIndexIsVisible(sel);
      editDescriptor(de);
    }
  }

  private class EditDescriptorAction extends AbstractAction {
    public EditDescriptorAction(Icon icon) {
      super("", icon);
    }

    public EditDescriptorAction(String name) {
      super(name);
    }

    public void actionPerformed(ActionEvent e) {
      DescriptorEntry de;
      int[] idx;

      idx = _dscList.getSelectedIndices();
      if (idx.length != 1) {
        return;
      }
      de = _dscList.getModel().getElementAt(idx[0]);
      if (de.getStatus().equals(DescriptorEntry.STATUS.waiting)
          || de.getStatus().equals(DescriptorEntry.STATUS.running)) {
        JOptionPane.showMessageDialog(
            JOptionPane.getFrameForComponent(DescriptorList.this),
            DBMSMessages.getString("DescriptorList.msg23"),
            DBMSMessages.getString("DescriptorList.msg0"),
            JOptionPane.INFORMATION_MESSAGE);
        return;
      }
      editDescriptor(de);
    }
  }

  private class StartProcessingAction extends AbstractAction {

    public StartProcessingAction(Icon icon) {
      super("", icon);
    }

    public StartProcessingAction(String name) {
      super(name);
    }

    public void actionPerformed(ActionEvent e) {
      ArrayList<DescriptorEntry> entries;
      ArrayList<DescriptorEntry> selectedEntries;
      DBMirrorConfig mirrorConfig;
      DicoTermQuerySystem dicoSystem;
      // DescriptorEntry de = null;
      int[] idx;
      int i, ret;
      boolean ok = false;
      String workingMode;

      // first use ?
      if (!UIUtils.askForMirrorPath(JOptionPane
          .getFrameForComponent(DescriptorList.this))) {
        return;
      }
      // another job is running ?
      if (_jobController != null && _jobController.canStartAJob() == false) {
        String msg;

        msg = _jobController.getControllerMsg();
        if (msg == null)
          msg = DBMSMessages.getString("DescriptorList.msg19");
        JOptionPane.showMessageDialog(
            JOptionPane.getFrameForComponent(DescriptorList.this), msg,
            DBMSMessages.getString("DescriptorList.msg0"),
            JOptionPane.INFORMATION_MESSAGE);
        return;
      }

      // selected entries
      selectedEntries = new ArrayList<DescriptorEntry>();
      idx = _dscList.getSelectedIndices();
      if (idx.length == 0) {
        return;
      }
      for (i = 0; i < idx.length; i++) {
        selectedEntries.add(_dscList.getModel().getElementAt(idx[i]));
      }
      if (!_runnerPanel.canStartAJob(selectedEntries))
        return;

      // ask for info before loading dbs ?
      ret = JOptionPane.showConfirmDialog(
          JOptionPane.getFrameForComponent(DescriptorList.this),
          DBMSMessages.getString("DescriptorList.msg22"),
          DBMSMessages.getString("DescriptorList.msg25"),
          JOptionPane.YES_NO_OPTION);
      if (ret == JOptionPane.YES_OPTION)
        workingMode = PFTPLoaderDescriptor.MAINTASK_INFO;
      else
        workingMode = PFTPLoaderDescriptor.MAINTASK_DOWNLOAD;

      // ensure that current DicoTermQuerySystem singleton is closed
      DicoTermQuerySystem.closeDicoTermQuerySystem();
      entries = new ArrayList<DescriptorEntry>();
      // for (i = 0; i < idx.length; i++) {
      for (DescriptorEntry entry : selectedEntries) {
        // de = _dscList.getModel().getElementAt(idx[i]);
        if (DBDescriptorUtils.hasTaxonomyConstrainArgs(entry.getDescriptor())) {
          mirrorConfig = DBDescriptorUtils.getDBMirrorConfig(DBMSAbstractConfig
              .getLocalMirrorConfFile());
          if (mirrorConfig != null) {
            dicoSystem = DicoTermQuerySystem
                .getDicoTermQuerySystem(mirrorConfig);
            ok = dicoSystem.hasDicoAvailable(Dicos.NCBI_TAXONOMY);
            DicoTermQuerySystem.closeDicoTermQuerySystem();
          }
          if (!ok) {
            JOptionPane
                .showMessageDialog(
                    JOptionPane.getFrameForComponent(DescriptorList.this),
                    new MessageFormat(DBMSMessages
                        .getString("DescriptorList.msg26"))
                        .format(new Object[] { entry.getName() }), DBMSMessages
                        .getString("DescriptorList.msg25"),
                    JOptionPane.WARNING_MESSAGE);
            return;
          }
        }
        entry.setStatus(DescriptorEntry.STATUS.waiting);
        entries.add(entry);
      }

      _runnerPanel.startLoadingEntries(entries, workingMode);
    }
  }

  private class ExportDescriptorAction extends AbstractAction {
    public ExportDescriptorAction(Icon icon) {
      super("", icon);
    }

    public ExportDescriptorAction(String name) {
      super(name);
    }

    private File chooseFile(String defFname) {
      File f;

      f = EZFileManager.chooseFileForSaveAction(
          (JComponent) DescriptorList.this,
          DBMSMessages.getString("DescriptorList.msg30"),
          new EZFileFilter(DESCRIPTOR_FNAME_EXT, DBMSMessages
              .getString("DescriptorList.msg0b")));
      if (f != null)
        f = EZFileUtils.forceFileExtension(f, DESCRIPTOR_FNAME_EXT);
      return f;
    }

    public void actionPerformed(ActionEvent e) {
      DescriptorEntry de;
      File f;
      int[] idx;

      idx = _dscList.getSelectedIndices();
      if (idx.length != 1) {
        return;
      }

      de = _dscList.getModel().getElementAt(idx[0]);
      f = chooseFile(de.getName() + "." + DESCRIPTOR_FNAME_EXT);

      if (f == null)// cancel
        return;
      try {
        Utils.copyBinFile(new File(de.getFile()), f);
      } catch (IOException e1) {
        LOGGER.warn(e1);
        JOptionPane.showMessageDialog(
            JOptionPane.getFrameForComponent(DescriptorList.this),
            DBMSMessages.getString("DescriptorList.msg27"),
            DBMSMessages.getString("DescriptorList.msg0"),
            JOptionPane.WARNING_MESSAGE);
        return;
      }
    }
  }

  private class ImportDescriptorAction extends AbstractAction {
    public ImportDescriptorAction(Icon icon) {
      super("", icon);
    }

    public ImportDescriptorAction(String name) {
      super(name);
    }

    private File chooseFile() {
      File f;
      f = EZFileManager.chooseFileForOpenAction(
          (JComponent) DescriptorList.this,
          DBMSMessages.getString("DescriptorList.msg34"),
          new EZFileFilter(DESCRIPTOR_FNAME_EXT, DBMSMessages
              .getString("DescriptorList.msg0b")));
      return f;
    }

    public void actionPerformed(ActionEvent e) {
      DescriptorEntry de;
      DBServerConfig desc;
      File f, newDesc;
      String name;
      boolean needUpdate = false;

      f = chooseFile();

      if (f == null)// cancel
        return;

      // does not allow importing descriptor from current dsc path
      if (f.getAbsolutePath().startsWith(_dscPath)) {
        JOptionPane.showMessageDialog(
            JOptionPane.getFrameForComponent(DescriptorList.this),
            DBMSMessages.getString("DescriptorList.msg35"),
            DBMSMessages.getString("DescriptorList.msg0"),
            JOptionPane.WARNING_MESSAGE);
        return;
      }
      // read the descriptor
      desc = readConfig(f);
      if (desc == null) {
        JOptionPane.showMessageDialog(
            JOptionPane.getFrameForComponent(DescriptorList.this),
            DBMSMessages.getString("DescriptorList.msg31"),
            DBMSMessages.getString("DescriptorList.msg0"),
            JOptionPane.WARNING_MESSAGE);
        return;
      }
      if (DBMSAbstractConfig.enableBioClassifOnly() && !desc.isDictionary()) {
        return;
      }
      // descriptor already exists (with its name) in the system path ?
      newDesc = new File(_dscPath + desc.getName() + DBMSAbstractConfig.FEXT_DD);
      if (newDesc.exists()) {
        int ret = JOptionPane.showConfirmDialog(
            JOptionPane.getFrameForComponent(DescriptorList.this),
            DBMSMessages.getString("DescriptorList.msg36"),
            DBMSMessages.getString("DescriptorList.msg0"),
            JOptionPane.YES_NO_CANCEL_OPTION);
        if (ret == JOptionPane.CANCEL_OPTION)
          return;
        if (ret == JOptionPane.NO_OPTION) {
          name = getNewName(desc);
          if (name == null)
            return;
          newDesc = new File(_dscPath + name + DBMSAbstractConfig.FEXT_DD);
          needUpdate = true;
        } else {
          // replace existing by remove/add again
          int idx = getDescriptorListPositionByName(desc.getName());
          if (idx == -1)
            return;
          _dscModel.removeElementAt(idx);
        }
      }
      // install new descriptor in the system path
      try {
        Utils.copyBinFile(f, newDesc);
      } catch (IOException e1) {
        LOGGER.warn(e1);
        JOptionPane.showMessageDialog(
            JOptionPane.getFrameForComponent(DescriptorList.this),
            DBMSMessages.getString("DescriptorList.msg27"),
            DBMSMessages.getString("DescriptorList.msg0"),
            JOptionPane.WARNING_MESSAGE);
        return;
      }

      if (needUpdate) {
        if (!updateDbConfig(desc, newDesc)) {
          newDesc.delete();
          return;
        }
      }
      _dscList.clearSelection();
      de = new DescriptorEntry(desc, newDesc.getAbsolutePath());
      int sel = addDescriptor(de);
      _dscList.setSelectedIndex(sel);
      _dscList.ensureIndexIsVisible(sel);
    }
  }

  private class MyListSelectionListener implements ListSelectionListener {
    public void valueChanged(ListSelectionEvent e) {
      enableCommand(_dscList.getSelectedIndices().length);
    }
  }

  private class MyCellRenderer extends JPanel implements
      ListCellRenderer<DescriptorEntry> {
    private JList<DescriptorEntry> _list;
    private String                 _dbName;
    private String                 _dbDescription;
    private Font                   _bold;
    private boolean                _selected;
    private int                    _border       = 4;
    private int                    _iconSize     = 24;
    private Image                  _curImage;
    private Image                  _protImg;
    private Image                  _nucImg;
    private Image                  _dicoImg;
    private Image                  _runnerIcon;

    private Image                  _waitImg;
    private Image                  _runImg;
    private Image                  _okImg;
    private Image                  _errImg;

    private Border                 noFocusBorder = new EmptyBorder(1, 1, 1, 1);

    public MyCellRenderer(JList<DescriptorEntry> list) {
      super();
      // super(Color.WHITE, Color.LIGHT_GRAY);
      // setGradientOrientation(GradientPanel.GRAD_ORIENTATION_LtoR);
      Font fnt = list.getFont();
      _bold = fnt.deriveFont(Font.BOLD, fnt.getSize2D());
      _list = list;
      _protImg = EZEnvironment.getImageIcon("p_descriptor.png").getImage();
      _nucImg = EZEnvironment.getImageIcon("n_descriptor.png").getImage();
      _dicoImg = EZEnvironment.getImageIcon("d_descriptor.png").getImage();
      _waitImg = EZEnvironment.getImageIcon("data_pause.png").getImage();
      _runImg = EZEnvironment.getImageIcon("data_run.png").getImage();
      _okImg = EZEnvironment.getImageIcon("data_ok.png").getImage();
      _errImg = EZEnvironment.getImageIcon("data_warning.png").getImage();
    }

    private Border getNoFocusBorder() {
      return noFocusBorder;
    }

    public Component getListCellRendererComponent(
        JList<? extends DescriptorEntry> list, DescriptorEntry value,
        int index, boolean isSelected, boolean cellHasFocus) {
      Border border = null;

      _dbName = value.getName();
      _dbDescription = value.getDescription();
      if (value.getDescriptor().isProteic()) {
        _curImage = _protImg;
      } else if (value.getDescriptor().isNucleic()) {
        _curImage = _nucImg;
      } else if (value.getDescriptor().isDictionary()) {
        _curImage = _dicoImg;
      } else {
        _curImage = null;
      }
      UIDefaults defaults = UIManager.getDefaults();
      if (isSelected) {
        setBackground((Color) defaults.get("List.selectionBackground"));
      } else {
        /*
         * if(index%2 == 0) { setBackground(Color.blue.brighter()); } else{
         * setBackground((Color) defaults.get("List.background")); }
         */
        setBackground((Color) defaults.get("List.background"));
      }

      if (cellHasFocus) {
        if (isSelected) {
          border = UIManager.getBorder("List.focusSelectedCellHighlightBorder");
        }
        if (border == null) {
          border = UIManager.getBorder("List.focusCellHighlightBorder");
        }
      } else {
        border = getNoFocusBorder();
      }
      setBorder(border);
      // setSelected(isSelected);
      _selected = isSelected;
      if (value.getStatus().equals(DescriptorEntry.STATUS.waiting)) {
        _runnerIcon = _waitImg;
      } else if (value.getStatus().equals(DescriptorEntry.STATUS.error)) {
        _runnerIcon = _errImg;
      } else if (value.getStatus().equals(DescriptorEntry.STATUS.running)) {
        _runnerIcon = _runImg;
      } else if (value.getStatus().equals(DescriptorEntry.STATUS.ok)) {
        _runnerIcon = _okImg;
      } else {
        _runnerIcon = null;
      }
      return this;
    }

    // Could be make a new one using: (java 1.6 required)
    // http://weblogs.java.net/blog/zixle/archive/2006/12/extreme_list_vi.html
    public void paintComponent(Graphics g) {
      super.paintComponent(g);

      FontMetrics fm;
      Font fnt;
      Color oldClr;
      Insets insets;
      String str;
      int height, width, xDecal, yDecal, left, rightClip;

      fnt = g.getFont();
      oldClr = g.getColor();
      insets = _list.getInsets();
      width = _list.getWidth() - (insets.left + insets.right);
      height = _list.getFixedCellHeight();
      fm = _list.getFontMetrics(_list.getFont());

      left = fm.stringWidth("wwww");

      // rect where to display query num.
      g.drawRect(_border / 2, _border / 2, left, height - _border);

      // rect where to put all other data
      g.drawRect(left + _border, _border / 2, width - (left + 2 * _border),
          height - _border);

      if (_selected
          && DBMSExecNativeCommand.getOSType() != DBMSExecNativeCommand.MAC_OS)
        g.setColor(Color.white);
      else
        g.setColor(Color.black);

      // query number
      // far left: status icon
      xDecal = 2 * _border;
      yDecal = (height - _iconSize) / 2;
      if (_curImage != null) {
        g.drawImage(_curImage, xDecal, yDecal, null, null);
      }
      if (_runnerIcon != null) {
        xDecal = width - _iconSize - 2 * _border;
        g.drawImage(_runnerIcon, xDecal, yDecal, null, null);
      }

      rightClip = width - 2 * _iconSize - 2 * _border - 5;

      // first line: query name
      yDecal = fm.getHeight() + _border;
      str = _dbName;
      xDecal = left + 2 * _border;
      g.setFont(_bold);
      str = UIUtils.clipText(this, _bold, str, xDecal, rightClip);
      g.drawString(str, xDecal, yDecal);

      // second line: description
      yDecal = 2 * fm.getHeight() + _border + 2;
      g.setFont(fnt);
      str = _dbDescription;
      str = UIUtils.clipText(this, fnt, str, xDecal, rightClip);
      g.drawString(str, xDecal, yDecal);

      g.setColor(oldClr);
    }
  }

  private class DBDescComparator implements Comparator<DescriptorEntry> {
    public int compare(DescriptorEntry db1, DescriptorEntry db2) {
      return db1.getName().toLowerCase().compareTo(db2.getName().toLowerCase());
    }
  }
}
