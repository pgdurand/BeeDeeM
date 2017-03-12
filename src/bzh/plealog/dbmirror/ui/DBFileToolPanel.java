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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
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
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.JTextComponent;

import bzh.plealog.dbmirror.ui.resources.DBMSMessages;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.runner.UnarchiveDBMonitor;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.plealog.genericapp.api.EZEnvironment;
import com.plealog.genericapp.api.file.EZFileFilter;
import com.plealog.genericapp.api.file.EZFileManager;

/**
 * This class implements a panel enabling a user to process archived sequence
 * databanks.
 * 
 * @author Patrick G. Durand
 */
@SuppressWarnings("serial")
public class DBFileToolPanel extends JPanel {
  private JListWithHelp          _dbList;
  private JToolBar               _tBar;
  private JButton                _browseBtn;
  private JButton                _processBtn;
  private JTextField             _pathField;
  private JTextArea              _helpArea;
  private AddDbAction            _addDbAction;
  private RemoveDbAction         _removeDbAction;
  private UnarchiveDBMonitor     _monitor;

  private static final ImageIcon DOC_ICO = EZEnvironment
                                             .getImageIcon("document.png");

  /**
   * Constructor.
   */
  public DBFileToolPanel(JButton createDBBtn) {
    _processBtn = createDBBtn;
    buildGUI();
    enableControls(false);
    _addDbAction.setEnabled(true);
  }

  /**
   * Enables all the controls of this panel.
   */
  public void enableControls(boolean enable) {

    _pathField.setEnabled(enable);
    _processBtn.setEnabled(enable);
    _browseBtn.setEnabled(enable);
    _removeDbAction.setEnabled(enable);
    _addDbAction.setEnabled(enable);
  }

  /**
   * Creates the GUI.
   */
  private void buildGUI() {
    JPanel panel1, panel2, mainPnl;

    panel1 = new JPanel(new BorderLayout());
    panel2 = new JPanel(new BorderLayout());
    mainPnl = new JPanel(new BorderLayout());

    panel1.add(getSourceFilesPanel(), BorderLayout.NORTH);
    panel2.add(getLocationPanel(), BorderLayout.NORTH);
    panel1.add(panel2, BorderLayout.SOUTH);
    panel1.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

    mainPnl.add(panel1, BorderLayout.NORTH);
    mainPnl.add(createHelper(), BorderLayout.SOUTH);
    mainPnl.setBorder(BorderFactory.createEtchedBorder());
    _helpArea.setText(DBMSMessages.getString("ChooseMirrorConfigPanel.msg1"));

    this.setLayout(new BorderLayout());
    this.add(mainPnl, BorderLayout.CENTER);
  }

  protected void setMonitor(UnarchiveDBMonitor mon) {
    _monitor = mon;
  }

  private void displayWarnMessage(String msg) {
    JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(this), msg,
        DBMSMessages.getString("DBFileToolPanel.dlg.header"),
        JOptionPane.WARNING_MESSAGE);
  }

  private JTextField createTxtField() {
    JTextField t;
    t = new JTextField();
    return t;
  }

  private JTextArea createHelper() {
    _helpArea = new JTextArea();
    _helpArea.setRows(5);
    _helpArea.setLineWrap(true);
    _helpArea.setWrapStyleWord(true);
    _helpArea.setEditable(false);
    _helpArea.setOpaque(false);
    return _helpArea;
  }

  private JPanel getSourceFilesPanel() {
    JPanel panel2;
    JScrollPane scroll;
    JLabel lbl;
    Dimension dim;

    panel2 = new JPanel(new BorderLayout());
    // list of FASTA files
    _dbList = new JListWithHelp();
    _dbList.setMessage(DBMSMessages.getString("DBFileToolPanel.list.help"));
    _dbList.setCellRenderer(new MyCellRenderer());
    _dbList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    _dbList.getSelectionModel().addListSelectionListener(
        new DBListSelectionListener());
    _dbList.setTransferHandler(new FilesTransferHandler());
    scroll = new JScrollPane(_dbList);
    dim = new Dimension(340, 80);
    scroll.setPreferredSize(dim);
    scroll.setMaximumSize(dim);
    scroll.setMinimumSize(dim);
    // label put on top of the list
    lbl = new JLabel(DBMSMessages.getString("DBFileToolPanel.flist.lbl"));
    lbl.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 0));

    // toolbar to add/remove files
    _tBar = getCommands();
    panel2.add(_tBar, BorderLayout.EAST);
    panel2.add(lbl, BorderLayout.NORTH);
    panel2.add(scroll, BorderLayout.CENTER);
    panel2.setBorder(BorderFactory.createTitledBorder(DBMSMessages
        .getString("DBFileToolPanel.source.lbl")));

    return panel2;
  }

  /**
   * Utility method used to create the DBLocation selector. Enable a user to set
   * a DB name and Location.
   */
  private Component getLocationPanel() {
    FormLayout layout;
    DefaultFormBuilder builder;
    JButton btn;
    ImageIcon icon;
    HelpListener hlpL;
    JPanel panel;

    _pathField = createTxtField();
    hlpL = new HelpListener(DBMSMessages.getString("DBFileToolPanel.path.tip"));
    _pathField.addFocusListener(hlpL);
    _pathField.addMouseListener(hlpL);
    icon = EZEnvironment.getImageIcon("openFile.png");
    if (icon != null) {
      btn = new JButton(icon);
    } else {
      btn = new JButton("...");
    }
    btn.addActionListener(new BrowseAction());
    btn.setToolTipText(DBMSMessages.getString("DBFileToolPanel.browse.tip"));
    _browseBtn = btn;
    layout = new FormLayout("right:60dlu, 2dlu, 150dlu, 2dlu, 15dlu", "");
    builder = new DefaultFormBuilder(layout);
    builder.setDefaultDialogBorder();
    builder.append(DBMSMessages.getString("DBFileToolPanel.path.lbl"),
        _pathField, btn);

    panel = new JPanel(new BorderLayout());
    panel.add(builder.getContainer(), BorderLayout.CENTER);
    panel.setBorder(BorderFactory.createTitledBorder(DBMSMessages
        .getString("DBFileToolPanel.target.lbl")));
    return panel;
  }

  /**
   * Utility method used to create the toolbar to add or remove Fasta files.
   */
  private JToolBar getCommands() {
    JToolBar tBar;
    ImageIcon icon;
    JButton btn;

    tBar = new JToolBar();
    tBar.setOrientation(JToolBar.VERTICAL);
    tBar.setFloatable(false);

    // add action
    icon = EZEnvironment.getImageIcon("doc_add.png");
    if (icon != null) {
      _addDbAction = new AddDbAction(
          DBMSMessages.getString("DBFileToolPanel.tbar.add.lbl"), icon);
    } else {
      _addDbAction = new AddDbAction(
          DBMSMessages.getString("DBFileToolPanel.tbar.add.lbl"));
    }
    btn = tBar.add(_addDbAction);
    btn.setToolTipText(DBMSMessages.getString("DBFileToolPanel.tbar.add.tip"));
    // remove action
    icon = EZEnvironment.getImageIcon("doc_delete.png");
    if (icon != null) {
      _removeDbAction = new RemoveDbAction(
          DBMSMessages.getString("DBFileToolPanel.tbar.remove.lbl"), icon);
    } else {
      _removeDbAction = new RemoveDbAction(
          DBMSMessages.getString("DBFileToolPanel.tbar.remove.lbl"));
    }
    btn = tBar.add(_removeDbAction);
    btn.setToolTipText(DBMSMessages
        .getString("DBFileToolPanel.tbar.remove.tip"));

    return tBar;
  }

  /**
   * Returns the list of Fasta files.
   */
  public List<String> getFileList() {
    ArrayList<String> dbs;
    ListModel<String> model;
    int i, size;

    dbs = new ArrayList<String>();
    model = _dbList.getModel();
    size = model.getSize();
    for (i = 0; i < size; i++) {
      dbs.add(model.getElementAt(i));
    }
    return dbs;
  }

  /**
   * Returns the DB location.
   */
  public String getDBPath() {
    return (_pathField.getText());
  }

  /**
   * ets a message in the helper area of this panel.
   */
  public void setMessage(String msg) {
    _helpArea.setText(msg);
  }

  public JTextComponent getHelpArea() {
    return _helpArea;
  }

  /**
   * Checks whether the data given by the user are valid.
   */
  public boolean checkData() {
    MessageFormat mf;
    String str, dbPath;
    File f;

    // check fasta list
    if (getFileList().isEmpty()) {
      displayWarnMessage(DBMSMessages.getString("DBFileToolPanel.err1"));
      return false;
    }
    // check the db location
    str = getDBPath();
    if (str.length() == 0) {
      displayWarnMessage(DBMSMessages.getString("DBFileToolPanel.err6"));
      return false;
    }
    if (!DBMSAbstractConfig.authorizeLongFileName()
        && Utils.isPathNameContainsSpaceChar(str)) {
      mf = new MessageFormat(DBMSMessages.getString("DBFileToolPanel.err3"));
      displayWarnMessage(mf.format(new Object[] { str }));
      return false;
    }
    dbPath = str;
    f = new File(dbPath);
    if (!f.exists()) {
      if (!f.mkdirs()) {
        mf = new MessageFormat(DBMSMessages.getString("DBFileToolPanel.err4"));
        displayWarnMessage(mf.format(new Object[] { dbPath }));
        return false;
      }
    }
    try {
      f = File.createTempFile("klb", null, f);
      f.delete();
    } catch (IOException e) {
      mf = new MessageFormat(DBMSMessages.getString("DBFileToolPanel.err7"));
      displayWarnMessage(mf.format(new Object[] { dbPath }));
      return false;
    }

    return true;
  }

  private class DBListSelectionListener implements ListSelectionListener {
    public void valueChanged(ListSelectionEvent e) {
      if (e.getValueIsAdjusting())
        return;
      enableControls(!_dbList.getSelectionModel().isSelectionEmpty());
      _addDbAction.setEnabled(true);
    }
  }

  private void addFiles(List<File> files) {
    if (files == null || files.size() == 0)
      return;
    addFiles((File[]) files.toArray(new File[0]));
  }

  private void addFiles(File[] f) {
    DefaultListModel<String> newModel;
    ListModel<String> model;
    MessageFormat mf;
    String str;
    int i, size;
    HashSet<String> curDB;

    if (f == null || f.length == 0)
      return;
    if (!DBMSAbstractConfig.authorizeLongFileName()) {
      for (i = 0; i < f.length; i++) {
        // formatdb does not allow space char in the path...
        str = f[i].getAbsolutePath();
        if (Utils.isPathNameContainsSpaceChar(str)) {
          mf = new MessageFormat(DBMSMessages.getString("DBFileToolPanel.err3"));
          displayWarnMessage(mf.format(new Object[] { str }));
          return;
        }
      }
    }
    // prepare a copy of the file list, then add the new file at the end
    curDB = new HashSet<String>();
    newModel = new DefaultListModel<>();
    model = _dbList.getModel();
    size = model.getSize();
    curDB = new HashSet<String>();
    for (i = 0; i < size; i++) {
      curDB.add((model.getElementAt(i).toString()));
    }

    for (i = 0; i < size; i++) {
      newModel.addElement(model.getElementAt(i));
    }
    for (i = 0; i < f.length; i++) {
      str = f[i].getAbsolutePath();
      if (!curDB.contains(str))
        newModel.addElement(str);
    }
    // set the new model, then select the newly added db descriptor
    _dbList.clearSelection();
    _dbList.setModel(newModel);
    _dbList.setSelectedIndex(newModel.getSize() - 1);
    // _dbList.repaint();

    if (_pathField.getText().length() == 0) {
      _pathField.setText(f[0].getParent());
    }

  }

  /**
   * Actions used to add new Fasta files.
   */
  private class AddDbAction extends AbstractAction {
    /**
     * Action constructor.
     * 
     * @param name
     *          the name of the action.
     */
    public AddDbAction(String name) {
      super(name);
    }

    /**
     * Action constructor.
     * 
     * @param name
     *          the name of the action.
     * @param icon
     *          the icon of the action.
     */
    public AddDbAction(String name, Icon icon) {
      super(name, icon);
    }

    public void actionPerformed(ActionEvent event) {
      File[] f;

      EZFileFilter ff = new EZFileFilter(new String[] { "gz", "tgz", "zip",
          "tar" }, "Compressed files");

      f = EZFileManager.chooseFilesForOpenAction(DBFileToolPanel.this,
          DBMSMessages.getString("DBFileToolPanel.dlg.open.header"), ff);
      addFiles(f);
    }
  }

  /**
   * Actions used to remove Fasta file.
   */
  private class RemoveDbAction extends AbstractAction {
    /**
     * Action constructor.
     * 
     * @param name
     *          the name of the action.
     */
    public RemoveDbAction(String name) {
      super(name);
    }

    /**
     * Action constructor.
     * 
     * @param name
     *          the name of the action.
     * @param icon
     *          the icon of the action.
     */
    public RemoveDbAction(String name, Icon icon) {
      super(name, icon);
    }

    public void actionPerformed(ActionEvent event) {
      ListSelectionModel lsm;
      DefaultListModel<String> newModel;
      ListModel<String> model;
      String db, dbSel;
      int idx, i, size;

      lsm = _dbList.getSelectionModel();
      if (lsm.isSelectionEmpty())
        return;
      dbSel = (String) _dbList.getSelectedValue();
      idx = _dbList.getSelectedIndex();
      newModel = new DefaultListModel<>();
      model = _dbList.getModel();
      size = model.getSize();
      for (i = 0; i < size; i++) {
        db = (String) model.getElementAt(i);
        if (!db.equalsIgnoreCase(dbSel))
          newModel.addElement(db);
      }
      _dbList.clearSelection();
      _dbList.setModel(newModel);
      _dbList.setSelectedIndex(Math.max(0, idx - 1));
    }
  }

  /**
   * Listener used to set some help message when the user clicks on the various
   * radio buttons.
   */
  private class HelpListener implements FocusListener, MouseListener {
    private String _hlpMsg;

    public HelpListener(String msg) {
      _hlpMsg = msg;
    }

    private void setMyText(String str) {
      if (_monitor != null && _monitor.isJobRunning())
        return;
      _helpArea.setText(str);
    }

    public void focusGained(FocusEvent event) {
      setMyText(_hlpMsg);
    }

    public void focusLost(FocusEvent event) {
      setMyText("");
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
      setMyText(_hlpMsg);
    }

    public void mouseExited(MouseEvent e) {
      setMyText("");
    }
  }

  /**
   * Actions used to choose an existing DB location.
   */
  private class BrowseAction implements ActionListener {
    public void actionPerformed(ActionEvent event) {
      File path = EZFileManager.chooseDirectory(DBFileToolPanel.this,
          DBMSMessages.getString("FormatDBPanel.dlg.browse.header"), null);
      if (path == null)
        return;
      _pathField.setText(path.getAbsolutePath());
    }
  }

  /**
   * This is the renderer used to display Fasta files in the list.
   */
  private class MyCellRenderer extends JLabel implements
      ListCellRenderer<String> {

    public MyCellRenderer() {
      this.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 1));
      this.setOpaque(true);
    }

    public Component getListCellRendererComponent(JList<? extends String> list,
        String value, int index, boolean isSelected, boolean cellHasFocus) {
      String db;

      db = (String) value;
      this.setText(db);
      this.setIcon(DOC_ICO);

      UIDefaults defaults = UIManager.getDefaults();
      if (isSelected) {
        this.setBackground((Color) defaults.get("List.selectionBackground"));
        this.setForeground(Color.WHITE);
      } else {
        this.setBackground((Color) defaults.get("List.background"));
        this.setForeground(Color.BLACK);
      }
      return this;
    }
  }

  /**
   * This class handles the drop of a set of files within the JList component
   * displaying the list of files.
   */
  private class FilesTransferHandler extends TransferHandler {
    private DataFlavor _fileFlavor;

    public FilesTransferHandler() {
      _fileFlavor = DataFlavor.javaFileListFlavor;
    }

    @SuppressWarnings("unchecked")
    public boolean importData(JComponent c, Transferable t) {

      if (!canImport(c, t.getTransferDataFlavors())) {
        return false;
      }
      if (hasFileFlavor(t.getTransferDataFlavors())) {
        try {
          addFiles((List<File>) t.getTransferData(_fileFlavor));
        } catch (Exception e) {
          displayWarnMessage("Unable to get files.");
        }
      }
      return true;
    }

    public boolean canImport(JComponent c, DataFlavor[] flavors) {
      return hasFileFlavor(flavors);
    }

    private boolean hasFileFlavor(DataFlavor[] flavors) {
      for (int i = 0; i < flavors.length; i++) {
        if (_fileFlavor.equals(flavors[i])) {
          return true;
        }
      }
      return false;
    }
  }
}
