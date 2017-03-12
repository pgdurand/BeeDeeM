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
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
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

import bzh.plealog.dbmirror.fetcher.DBServerConfig;
import bzh.plealog.dbmirror.reader.DBUtils;
import bzh.plealog.dbmirror.ui.resources.DBMSMessages;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.conf.DBMirrorConfig;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.plealog.genericapp.api.EZEnvironment;
import com.plealog.genericapp.api.file.EZFileManager;

/**
 * This class implements a panel enabling a user to use the KDMS Lucene Indexer
 * system given a set of data files.
 * 
 * @deprecated @see {@link bzh.plealog.dbmirror.ui.LocalIndexInstallPanel}
 * 
 * @author Patrick G. Durand
 */
@SuppressWarnings("serial")
public class IndexDBPanel extends JPanel {
  private JList<String>            _dbList;
  private JToolBar                 _tBar;
  private JButton                  _browseBtn;
  private JTextField               _nameField;
  private JTextField               _pathField;
  private JTextField               _taxIncludeField;
  private JTextField               _taxExcludeField;
  private JTextArea                _helpArea;
  private AddFileAction            _addDbAction;
  private RemoveFileAction         _removeDbAction;
  private JComboBox<DBFormatEntry> _dbFormat;
  private JProgressBar             _progressBar;

  private boolean                  _enableUserDir;

  private static final ImageIcon   DOC_ICO = EZEnvironment
                                               .getImageIcon("document.png");

  /**
   * Constructor.
   */
  public IndexDBPanel() {
    this(false);
  }

  public IndexDBPanel(boolean enableUserDir) {
    _enableUserDir = enableUserDir;
    buildGUI();
    enableCommands(false);
  }

  /**
   * Creates the GUI.
   */
  private void buildGUI() {
    JPanel panel1, panel2, panel3, mainPnl;
    JScrollPane scroll;
    JLabel lbl;

    panel1 = new JPanel(new BorderLayout());
    panel2 = new JPanel(new BorderLayout());
    panel3 = new JPanel(new BorderLayout());
    mainPnl = new JPanel(new BorderLayout());

    // list of files
    _dbList = new JList<>();
    _dbList.setCellRenderer(new MyCellRenderer());
    _dbList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    _dbList.getSelectionModel().addListSelectionListener(
        new DBListSelectionListener());
    _dbList.setTransferHandler(new FilesTransferHandler());

    scroll = new JScrollPane(_dbList);
    // label put on top of the list
    lbl = new JLabel(DBMSMessages.getString("IndexDBPanel.flist.lbl"));
    lbl.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 0));
    panel2.add(lbl, BorderLayout.NORTH);
    panel2.add(scroll, BorderLayout.CENTER);
    // toolbar to add/remove files
    _tBar = getCommands();
    panel2.add(_tBar, BorderLayout.EAST);

    panel1.add(panel2, BorderLayout.CENTER);
    // add the panel used to set DB name and path
    panel1.add(getLocationPanel(), BorderLayout.SOUTH);
    // panel1.setPreferredSize(new Dimension(550,300));
    panel1.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

    panel3.add(panel1, BorderLayout.CENTER);
    panel3.add(getPreprocessingPanel(), BorderLayout.SOUTH);
    panel3.setBorder(BorderFactory.createEtchedBorder());

    mainPnl.add(panel3, BorderLayout.CENTER);
    mainPnl.add(createHelper(), BorderLayout.SOUTH);

    _helpArea.setText(DBMSMessages.getString("IndexDBPanel.help.defMsg"));

    this.setLayout(new BorderLayout());
    this.add(mainPnl, BorderLayout.CENTER);
  }

  private void createDbFormatCombo() {
    _dbFormat = new JComboBox<>();

    _dbFormat.addItem(new DBFormatEntry(-1, "Select", "unknown"));
    _dbFormat.addItem(new DBFormatEntry(DBUtils.SW_DB_FORMAT,
        "Uniprot (TrEMBL, SwissProt)", DBMirrorConfig.UP_READER));
    _dbFormat.addItem(new DBFormatEntry(DBUtils.GP_DB_FORMAT,
        "Genpept/Refseq Protein", DBMirrorConfig.GP_READER));
    _dbFormat.addItem(new DBFormatEntry(DBUtils.GB_DB_FORMAT,
        "Genbank/Refseq Nucleic", DBMirrorConfig.GB_READER));
    _dbFormat.addItem(new DBFormatEntry(DBUtils.EM_DB_FORMAT, "EMBL",
        DBMirrorConfig.EM_READER));
    // _dbFormat.addItem(new DBFormatEntry(DBUtils.GO_DICO_FORMAT,
    // "GeneOntology Terms", DBMirrorConfig.GO_READER));
    // _dbFormat.addItem(new DBFormatEntry(DBUtils.IPR_DICO_FORMAT,
    // "InterPro Names", DBMirrorConfig.IPR_READER));
    // _dbFormat.addItem(new DBFormatEntry(DBUtils.PFAM_DICO_FORMAT,
    // "Pfam Names", DBMirrorConfig.PFAM_READER));
    // _dbFormat.addItem(new DBFormatEntry(DBUtils.ENZYME_DICO_FORMAT,
    // "Enzyme nomenclature", DBMirrorConfig.ENZYME_READER));
    // _dbFormat.addItem(new DBFormatEntry(DBUtils.TAX_DICO_FORMAT,
    // "NCBI Taxonomy Names", DBMirrorConfig.TAX_READER));
    _dbFormat.setSelectedIndex(0);
  }

  private JTextField createTxtField() {
    JTextField t;
    t = new JTextField();
    return t;
  }

  private JPanel createHelper() {
    JPanel main, pnl;

    main = new JPanel(new BorderLayout());
    pnl = new JPanel(new BorderLayout());
    pnl.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
    _progressBar = new JProgressBar(0, 100);
    pnl.add(_progressBar, BorderLayout.CENTER);

    _helpArea = new JTextArea();
    _helpArea.setRows(5);
    _helpArea.setLineWrap(true);
    _helpArea.setWrapStyleWord(true);
    _helpArea.setEditable(false);
    _helpArea.setOpaque(false);
    main.add(pnl, BorderLayout.NORTH);
    main.add(_helpArea, BorderLayout.CENTER);
    return main;
  }

  protected void setProgressBarMax(int max) {
    _progressBar.setMaximum(max);
  }

  protected void setProgressBarValue(int val) {
    _progressBar.setValue(val);
  }

  /**
   * Utility method used to create the IndexLocation selector. Enable a user to
   * set aa index name and Location.
   */
  private Component getLocationPanel() {
    FormLayout layout;
    DefaultFormBuilder builder;
    JButton btn;
    ImageIcon icon;

    _nameField = createTxtField();
    _nameField.addFocusListener(new NameFocusListener());
    _pathField = createTxtField();
    _pathField.addFocusListener(new PathFocusListener());
    icon = EZEnvironment.getImageIcon("openFile.png");
    if (icon != null) {
      btn = new JButton(icon);
    } else {
      btn = new JButton("...");
    }
    btn.addActionListener(new BrowseAction());
    btn.setToolTipText(DBMSMessages.getString("IndexDBPanel.browse.tip"));
    _browseBtn = btn;
    layout = new FormLayout("right:90dlu, 2dlu, 150dlu, 2dlu, 15dlu", "");
    builder = new DefaultFormBuilder(layout);
    builder.setDefaultDialogBorder();
    createDbFormatCombo();
    _dbFormat.addFocusListener(new TypeFocusListener());
    builder.append(DBMSMessages.getString("IndexDBPanel.type.lbl"), _dbFormat,
        new JLabel(""));
    builder.append(DBMSMessages.getString("IndexDBPanel.name.lbl"), _nameField,
        new JLabel(""));
    if (_enableUserDir)
      builder.append(DBMSMessages.getString("IndexDBPanel.path.lbl"),
          _pathField, btn);

    return builder.getContainer();
  }

  private Component getPreprocessingPanel() {
    FormLayout layout;
    DefaultFormBuilder builder;
    JPanel panel;
    HelpListener fl;

    layout = new FormLayout("right:90dlu, 2dlu, 150dlu", "");
    builder = new DefaultFormBuilder(layout);
    builder.setDefaultDialogBorder();
    _taxIncludeField = createTxtField();
    fl = new HelpListener(DBMSMessages.getString("IndexDBPanel.taxInclude.msg"));
    _taxIncludeField.addFocusListener(fl);
    _taxExcludeField = createTxtField();
    fl = new HelpListener(DBMSMessages.getString("IndexDBPanel.taxExclude.msg"));
    _taxExcludeField.addFocusListener(fl);
    builder.append(DBMSMessages.getString("FormatDBPanel.taxInclude.lbl"),
        _taxIncludeField);
    builder.append(DBMSMessages.getString("FormatDBPanel.taxExclude.lbl"),
        _taxExcludeField);

    panel = new JPanel(new BorderLayout());
    panel.add(builder.getContainer(), BorderLayout.CENTER);
    panel.setBorder(BorderFactory.createTitledBorder(DBMSMessages
        .getString("IndexDBPanel.msg2")));
    return panel;
  }

  /**
   * Utility method used to create the toolbar to add or remove data files.
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
      _addDbAction = new AddFileAction(
          DBMSMessages.getString("IndexDBPanel.tbar.add.lbl"), icon);
    } else {
      _addDbAction = new AddFileAction(
          DBMSMessages.getString("IndexDBPanel.tbar.add.lbl"));
    }
    btn = tBar.add(_addDbAction);
    btn.setToolTipText(DBMSMessages.getString("IndexDBPanel.tbar.add.tip"));
    // remove action
    icon = EZEnvironment.getImageIcon("doc_delete.png");
    if (icon != null) {
      _removeDbAction = new RemoveFileAction(
          DBMSMessages.getString("IndexDBPanel.tbar.remove.lbl"), icon);
    } else {
      _removeDbAction = new RemoveFileAction(
          DBMSMessages.getString("IndexDBPanel.tbar.remove.lbl"));
    }
    btn = tBar.add(_removeDbAction);
    btn.setToolTipText(DBMSMessages.getString("IndexDBPanel.tbar.remove.tip"));

    return tBar;
  }

  /**
   * Returns the list of data files.
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
   * Returns taxonomic constraint.
   */
  public String getTaxInclude() {
    return _taxIncludeField.getText();
  }

  /**
   * Returns taxonomic constraint.
   */
  public String getTaxExclude() {
    return _taxExcludeField.getText();
  }

  /**
   * Returns the index name.
   */
  public String getDBName() {
    return (_nameField.getText());
  }

  /**
   * Returns the index location.
   */
  public String getDBPath() {
    if (_enableUserDir) {
      return _pathField.getText();
    } else {
      String path, reader, type;

      reader = getDBType();
      if (reader.equals(DBMirrorConfig.GB_READER)
          || reader.equals(DBMirrorConfig.EM_READER))
        type = DBServerConfig.NUCLEIC_TYPE;
      else if (reader.equals(DBMirrorConfig.UP_READER)
          || reader.equals(DBMirrorConfig.GP_READER))
        type = DBServerConfig.PROTEIN_TYPE;
      else
        type = DBServerConfig.DICO_TYPE;
      path = DBMSAbstractConfig.getLocalMirrorPath() + type + File.separator
          + getDBName();
      return (path);
    }
  }

  /**
   * Returns the db format type. Values are one of DBMirrorConfig.XXX_READER
   * values. This is because it is the reader type that is stored in dbmirror
   * config file.
   */
  public String getDBType() {
    return ((DBFormatEntry) _dbFormat.getSelectedItem()).getReader();
  }

  /**
   * Returns the index type.
   */
  public boolean isProteic() {
    return true;
  }

  /**
   * Set a message in the helper area of this panel.
   */
  public void setMessage(String msg) {
    _helpArea.setText(msg);
  }

  public JTextComponent getHelpArea() {
    return _helpArea;
  }

  /**
   * Enables all the controls of this panel.
   */
  public void enableControls(boolean enable) {
    int i, size;

    _nameField.setEnabled(enable);
    _pathField.setEnabled(enable);
    _browseBtn.setEnabled(enable);
    size = _tBar.getComponentCount();
    for (i = 0; i < size; i++) {
      _tBar.getComponent(i).setEnabled(enable);
    }
  }

  private void displayWarnMessage(String msg) {
    JOptionPane.showMessageDialog(
        JOptionPane.getFrameForComponent(IndexDBPanel.this), msg,
        DBMSMessages.getString("IndexDBDialog.dlg.header"),
        JOptionPane.WARNING_MESSAGE);
  }

  /**
   * Checks whether the data given by the user are valid.
   */
  public boolean checkData() {
    MessageFormat mf;
    String str, dbPath;
    File f;

    // check data files list
    if (getFileList().isEmpty()) {
      displayWarnMessage(DBMSMessages.getString("IndexDBPanel.err1"));
      return false;
    }
    // check the data type
    if (((DBFormatEntry) _dbFormat.getSelectedItem()).getType() == -1) {
      displayWarnMessage(DBMSMessages.getString("IndexDBPanel.err8"));
      return false;
    }
    // check the index name
    str = getDBName();
    if (str.length() == 0) {
      displayWarnMessage(DBMSMessages.getString("IndexDBPanel.err5"));
      return false;
    }
    if (!Utils.isValidFileName(str)) {
      mf = new MessageFormat(DBMSMessages.getString("IndexDBPanel.err2"));
      displayWarnMessage(mf.format(new Object[] { str }));
      return false;
    }
    // check the index location
    str = getDBPath();
    if (str.length() == 0) {
      displayWarnMessage(DBMSMessages.getString("IndexDBPanel.err6"));
      return false;
    }
    if (!DBMSAbstractConfig.authorizeLongFileName()
        && Utils.isPathNameContainsSpaceChar(str)) {
      mf = new MessageFormat(DBMSMessages.getString("IndexDBPanel.err3"));
      displayWarnMessage(mf.format(new Object[] { str }));
      return false;
    }
    dbPath = str;
    f = new File(dbPath);
    if (!f.exists()) {
      if (!f.mkdirs()) {
        mf = new MessageFormat(DBMSMessages.getString("IndexDBPanel.err4"));
        displayWarnMessage(mf.format(new Object[] { dbPath }));
        return false;
      }
    } else {
      mf = new MessageFormat(DBMSMessages.getString("IndexDBDialog.err2"));
      int ret = JOptionPane.showConfirmDialog(
          JOptionPane.getFrameForComponent(IndexDBPanel.this),
          mf.format(new Object[] { dbPath }),
          DBMSMessages.getString("IndexDBDialog.dlg.header"),
          JOptionPane.YES_NO_OPTION);
      if (ret == JOptionPane.NO_OPTION)
        return false;
    }
    try {
      f = File.createTempFile("klb", null, f);
      f.delete();
    } catch (IOException e) {
      mf = new MessageFormat(DBMSMessages.getString("IndexDBPanel.err7"));
      displayWarnMessage(mf.format(new Object[] { dbPath }));
      return false;
    }

    return true;
  }

  private void enableCommands(boolean enable) {
    _removeDbAction.setEnabled(enable);
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

    if (f == null || f.length == 0)
      return;
    for (i = 0; i < f.length; i++) {
      // do not allow space char in the path...
      str = f[i].getAbsolutePath();
      if (!DBMSAbstractConfig.authorizeLongFileName()
          && Utils.isPathNameContainsSpaceChar(str)) {
        mf = new MessageFormat(DBMSMessages.getString("IndexDBPanel.err3"));
        displayWarnMessage(mf.format(new Object[] { str }));
        return;
      }
    }
    // prepare a copy of the file list, then add the new file at the end
    newModel = new DefaultListModel<>();
    model = _dbList.getModel();
    size = model.getSize();
    for (i = 0; i < size; i++) {
      newModel.addElement(model.getElementAt(i));
    }
    for (i = 0; i < f.length; i++) {
      newModel.addElement(f[i].getAbsolutePath());
    }
    // set the new model, then select the newly added db descriptor
    _dbList.clearSelection();
    _dbList.setModel(newModel);
    _dbList.setSelectedIndex(newModel.getSize() - 1);
    _dbList.repaint();

    // prepare default values for dbName and dbLocation
    if (_nameField.getText().length() == 0) {
      str = Utils.getFileName(f[0].getAbsoluteFile());
      if (str != null) {
        // added for multi-extension file; '.gz', for example
        i = str.indexOf('.');
        if (i != -1)
          str = str.substring(0, i);
        _nameField.setText(UIUtils.cleanName(str));
      }
    }
    if (_pathField.getText().length() == 0) {
      _pathField.setText(f[0].getParent());
    }
  }

  private class DBListSelectionListener implements ListSelectionListener {
    public void valueChanged(ListSelectionEvent e) {
      if (e.getValueIsAdjusting())
        return;
      enableCommands(!_dbList.getSelectionModel().isSelectionEmpty());
    }
  }

  /**
   * Actions used to add new data files.
   */
  private class AddFileAction extends AbstractAction {
    /**
     * Action constructor.
     * 
     * @param name
     *          the name of the action.
     */
    public AddFileAction(String name) {
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
    public AddFileAction(String name, Icon icon) {
      super(name, icon);
    }

    public void actionPerformed(ActionEvent event) {
      File[] f;

      f = EZFileManager.chooseFilesForOpenAction(IndexDBPanel.this,
          DBMSMessages.getString("IndexDBPanel.dlg.open.header"), null);
      addFiles(f);
    }
  }

  /**
   * Actions used to remove data files.
   */
  private class RemoveFileAction extends AbstractAction {
    /**
     * Action constructor.
     * 
     * @param name
     *          the name of the action.
     */
    public RemoveFileAction(String name) {
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
    public RemoveFileAction(String name, Icon icon) {
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
   * Listener used to set some help message when the user clicks within the text
   * area to enter the type combo box.
   */
  private class TypeFocusListener implements FocusListener {
    public void focusGained(FocusEvent event) {
      _helpArea.setText(DBMSMessages.getString("IndexDBPanel.type.tip"));
    }

    public void focusLost(FocusEvent event) {
      _helpArea.setText("");
    }
  }

  /**
   * Listener used to set some help message when the user clicks within the text
   * area to enter the DB name.
   */
  private class NameFocusListener implements FocusListener {
    public void focusGained(FocusEvent event) {
      _helpArea.setText(DBMSMessages.getString("IndexDBPanel.name.tip"));
    }

    public void focusLost(FocusEvent event) {
      _helpArea.setText("");
    }
  }

  /**
   * Listener used to set some help message when the user clicks within the text
   * area to enter the DB location.
   */
  private class PathFocusListener implements FocusListener {
    public void focusGained(FocusEvent event) {
      _helpArea.setText(DBMSMessages.getString("IndexDBPanel.path.tip"));
    }

    public void focusLost(FocusEvent event) {
      _helpArea.setText("");
    }
  }

  private class HelpListener implements FocusListener {
    private String _hlpMsg;

    public HelpListener(String msg) {
      _hlpMsg = msg;
    }

    public void focusGained(FocusEvent event) {
      _helpArea.setText(_hlpMsg);
    }

    public void focusLost(FocusEvent event) {
      _helpArea.setText("");
    }
  }

  /**
   * Actions used to choose an existing DB location.
   */
  private class BrowseAction implements ActionListener {
    public void actionPerformed(ActionEvent event) {
      File path = EZFileManager.chooseDirectory(IndexDBPanel.this,
          DBMSMessages.getString("IndexDBPanel.dlg.browse.header"), null);
      if (path == null)
        return;
      _pathField.setText(path.getAbsolutePath());
    }
  }

  /**
   * This is the renderer used to display data files in the list.
   */
  private class MyCellRenderer extends JLabel implements
      ListCellRenderer<String> {

    public MyCellRenderer() {
      this.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 1));
      this.setOpaque(true);
    }

    public Component getListCellRendererComponent(JList<? extends String> list,
        String db, int index, boolean isSelected, boolean cellHasFocus) {
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

  private class DBFormatEntry {
    private int    type;
    private String name;
    private String reader;

    public DBFormatEntry(int type, String name, String reader) {
      super();
      this.type = type;
      this.name = name;
      this.reader = reader;
    }

    public int getType() {
      return type;
    }

    public String getReader() {
      return reader;
    }

    public String toString() {
      return name;
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
