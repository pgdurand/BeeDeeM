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
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
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
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.plealog.genericapp.api.EZEnvironment;
import com.plealog.genericapp.api.file.EZFileManager;

import bzh.plealog.dbmirror.fetcher.DBServerConfig;
import bzh.plealog.dbmirror.fetcher.PFTPLoaderDescriptor;
import bzh.plealog.dbmirror.fetcher.PLocalLoader;
import bzh.plealog.dbmirror.fetcher.UserProcessingMonitor;
import bzh.plealog.dbmirror.lucenedico.Dicos;
import bzh.plealog.dbmirror.reader.DBFormatEntry;
import bzh.plealog.dbmirror.reader.DBUtils;
import bzh.plealog.dbmirror.ui.resources.DBMSMessages;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.conf.DBMirrorConfig;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptor;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptorUtils;
import bzh.plealog.dbmirror.util.descriptor.DatabankFormat;
import bzh.plealog.dbmirror.util.descriptor.DescriptorEntry;
import bzh.plealog.dbmirror.util.log.LoggerCentral;
import bzh.plealog.dbmirror.util.runner.DBMSExecNativeCommand;
import bzh.plealog.dbmirror.util.sequence.SeqIOUtils;
import bzh.plealog.dbmirror.util.sequence.SequenceFileManager;
import bzh.plealog.dbmirror.util.sequence.SequenceValidatorTaxon;
import bzh.plealog.dbmirror.util.sequence.TaxonMatcherHelper;

/**
 * This class implements a panel enabling a user to use the KDMS Lucene Indexer
 * system given a set of data files.
 * 
 * @author Patrick G. Durand
 */
public class LocalDBPanel extends JPanel {
  /**
	 * 
	 */
  private static final long        serialVersionUID        = 1L;
  private JListWithHelp            _dbList;
  private JToolBar                 _tBar;
  private JTextField               _nameField;
  private JTextField               _descriptionField;
  private JTextField               _pathField;
  private JTextField               _taxIncludeField;
  private JTextField               _taxExcludeField;
  private JLabel                   _helpArea;
  private AddFileAction            _addDbAction;
  private RemoveFileAction         _removeDbAction;
  private JComboBox<DBFormatEntry> _dbFormat;
  private JRadioButton             _useNcbiIdBtn;
  private JRadioButton             _doNotUseNcbiIdBtn;
  private JRadioButton             _checkInFilesBtn;
  private JRadioButton             _doNotCheckInFilesBtn;
  private RunningMirrorPanel       _runningPanel;
  private boolean                  _enableUserDir;
  private JTabbedPane              _filterTabs;
  private SequenceFileManagerUI    _sfmUIFilter;
  private SequenceFileManagerUI    _sfmUIRename;
  private JPanel                   _panelFilterTaxonomy;
  private JButton                  _testFilteringBtn;
  private boolean                  _changedSinceLastFilter = true;
  private boolean                  _filterBeforeInstall    = true;

  private static final ImageIcon   DOC_ICO                 = EZEnvironment
                                                               .getImageIcon("document.png");
  private static final Logger         LOGGER                  = LogManager.getLogger(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                                   + ".LocalDBPanel");

  /**
   * @wbp.parser.constructor
   */
  public LocalDBPanel(RunningMirrorPanel runningPanel) {
    this(false, runningPanel);
  }

  public LocalDBPanel(boolean enableUserDir, RunningMirrorPanel runningPanel) {
    _runningPanel = runningPanel;
    _enableUserDir = enableUserDir;
    buildGUI();
    enableCommands(false);
  }

  public void setRunningPanel(RunningMirrorPanel runningPanel) {
    _runningPanel = runningPanel;
  }

  /**
   * Creates the GUI.
   */
  private void buildGUI() {
    JPanel panel1, panel2, panel3, panel4, mainPnl;
    JScrollPane scroll;
    JLabel lbl;

    panel1 = new JPanel(new BorderLayout());
    panel2 = new JPanel(new BorderLayout());
    panel3 = new JPanel(new BorderLayout());
    panel4 = new JPanel(new BorderLayout());
    mainPnl = new JPanel(new BorderLayout());

    // list of files
    Dimension dimScroll = new Dimension(100, 90);
    _dbList = new JListWithHelp();
    _dbList.setCellRenderer(new MyCellRenderer());
    _dbList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    _dbList.getSelectionModel().addListSelectionListener(
        new DBListSelectionListener());
    _dbList.setTransferHandler(new FilesTransferHandler());
    _dbList.setMessage(DBMSMessages.getString("IndexDBPanel.flist.lbl"));

    scroll = new JScrollPane(_dbList);
    scroll.setMinimumSize(dimScroll);
    scroll.setMaximumSize(dimScroll);
    scroll.setPreferredSize(dimScroll);
    panel2.add(scroll, BorderLayout.CENTER);
    // toolbar to add/remove files
    _tBar = getCommands();
    panel2.add(_tBar, BorderLayout.EAST);

    panel1.add(panel2, BorderLayout.CENTER);
    // add the panel used to set DB name and path
    panel1.add(getLocationPanel(), BorderLayout.SOUTH);

    panel3.add(panel1, BorderLayout.CENTER);
    panel3.add(getPreprocessingPanel(), BorderLayout.SOUTH);
    panel3.setBorder(BorderFactory.createEtchedBorder());

    panel4.add(panel3, BorderLayout.CENTER);
    panel4.add(getCommandPanel(), BorderLayout.SOUTH);

    mainPnl.add(panel4, BorderLayout.CENTER);
    mainPnl.add(createHelper(), BorderLayout.SOUTH);

    _helpArea.setText(DBMSMessages.getString("IndexDBPanel.help.defMsg"));
    enableFastaControls(false);
    this.setLayout(new BorderLayout());
    lbl = new JLabel(DBMSMessages.getString("IndexDBPanel.pnl.help"));
    lbl.setBorder(BorderFactory.createEmptyBorder(1, 2, 1, 0));
    this.add(lbl, BorderLayout.NORTH);
    this.add(mainPnl, BorderLayout.CENTER);
    // this.add(getCommandPanel(), BorderLayout.SOUTH);
  }

  private void createDbFormatCombo() {
    _dbFormat = new JComboBox<>();

    _dbFormat.addItem(new DBFormatEntry(-1, "Select", "unknown"));
    _dbFormat.addItem(DBFormatEntry.uniprot);
    _dbFormat.addItem(DBFormatEntry.genpept);
    _dbFormat.addItem(DBFormatEntry.genbank);
    _dbFormat.addItem(DBFormatEntry.embl);
    _dbFormat.addItem(DBFormatEntry.fastaProteic);
    _dbFormat.addItem(DBFormatEntry.fastaNucleic);
    _dbFormat.addItem(DBFormatEntry.silva);
    _dbFormat.addItem(DBFormatEntry.bold);
    _dbFormat.addItem(DBFormatEntry.fastaProteicWithTaxon);
    _dbFormat.addItem(DBFormatEntry.fastaNucleicWithTaxon);
    _dbFormat.addItem(DBFormatEntry.geneOntology);
    _dbFormat.addItem(DBFormatEntry.interPro);
    _dbFormat.addItem(DBFormatEntry.enzyme);
    _dbFormat.addItem(DBFormatEntry.pfam);
    _dbFormat.addItem(DBFormatEntry.cdd);
    _dbFormat.addItem(DBFormatEntry.taxonomy);
    _dbFormat.setSelectedIndex(0);
    _dbFormat.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent arg0) {
        _changedSinceLastFilter = true;

      }
    });
  }

  private JTextField createTxtField() {
    JTextField t;
    t = new JTextField();
    return t;
  }

  @SuppressWarnings("serial")
  private JPanel createHelper() {
    JPanel main;

    main = new JPanel(new BorderLayout());

    // due to a resizing pb with the parent JScrollPane in
    // LocalIndexInstallPanel
    // the JTextArea is replaced by a JLabel
    // the JLabel displays an HTML text (to fit automatically in multines if
    // necessary)
    // min/max/preferred sizes are sets
    _helpArea = new JLabel() {
      @Override
      public void setText(String text) {
        super.setText("<html>" + text + "</html>");
      }
    };
    Dimension maxDimension = new Dimension(100, 60);
    _helpArea.setPreferredSize(maxDimension);
    _helpArea.setMaximumSize(maxDimension);
    _helpArea.setMinimumSize(maxDimension);
    _helpArea.setOpaque(false);
    _helpArea.setForeground(EZEnvironment.getSystemTextColor());
    if (this._sfmUIFilter != null) {
      this._sfmUIFilter.setHelpArea(_helpArea);
    }
    if (this._sfmUIRename != null) {
      this._sfmUIRename.setHelpArea(_helpArea);
    }
    // main.add(pnl, BorderLayout.NORTH);
    main.add(_helpArea, BorderLayout.CENTER);
    return main;
  }

  /*
   * protected void setProgressBarMax(int max){ _progressBar.setMaximum(max); }
   * protected void setProgressBarValue(int val){ _progressBar.setValue(val); }
   */
  /**
   * Utility method used to create the IndexLocation selector. Enable a user to
   * set aa index name and Location.
   */
  private Component getLocationPanel() {
    FormLayout layout;
    DefaultFormBuilder builder;
    JButton btn;
    ImageIcon icon;
    TypeFocusListener tfl;

    _nameField = createTxtField();
    _nameField.addFocusListener(new NameFocusListener());
    _descriptionField = createTxtField();
    _descriptionField.addFocusListener(new DescriptionFocusListener());
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
    layout = new FormLayout("right:90dlu, 2dlu, 150dlu, 2dlu, 15dlu", "");
    builder = new DefaultFormBuilder(layout);
    builder.setDefaultDialogBorder();
    createDbFormatCombo();
    tfl = new TypeFocusListener();
    _dbFormat.addFocusListener(tfl);
    _dbFormat.addActionListener(tfl);
    builder.append(DBMSMessages.getString("IndexDBPanel.type.lbl"), _dbFormat,
        new JLabel(""));
    builder.append(DBMSMessages.getString("IndexDBPanel.name.lbl"), _nameField,
        new JLabel(""));
    builder.append(DBMSMessages.getString("IndexDBPanel.desc.lbl"),
        _descriptionField, new JLabel(""));
    if (_enableUserDir)
      builder.append(DBMSMessages.getString("IndexDBPanel.path.lbl"),
          _pathField, btn);

    return builder.getContainer();
  }

  private Component getPreprocessingPanel() {
    FormLayout layout;
    DefaultFormBuilder builder;
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
    DocumentListener changedListener = new DocumentListener() {
      public void changedUpdate(DocumentEvent e) {
        _changedSinceLastFilter = true;
      }

      public void removeUpdate(DocumentEvent e) {
        _changedSinceLastFilter = true;
      }

      public void insertUpdate(DocumentEvent e) {
        _changedSinceLastFilter = true;
      }
    };
    _taxIncludeField.getDocument().addDocumentListener(changedListener);
    _taxExcludeField.getDocument().addDocumentListener(changedListener);

    JPanel panelInclude = new JPanel(new BorderLayout());
    panelInclude.add(this._taxIncludeField, BorderLayout.CENTER);
    JButton searchInclude = new JButton(
        DBMSMessages.getString("FormatDBPanel.taxInclude.help"));
    searchInclude
        .addActionListener(new SearchTaxonIdListener(_taxIncludeField));
    panelInclude.add(searchInclude, BorderLayout.EAST);
    builder.append(DBMSMessages.getString("FormatDBPanel.taxInclude.lbl"),
        panelInclude);
    JPanel panelExclude = new JPanel(new BorderLayout());
    panelExclude.add(this._taxExcludeField, BorderLayout.CENTER);
    JButton searchExclude = new JButton(
        DBMSMessages.getString("FormatDBPanel.taxExclude.help"));
    searchExclude
        .addActionListener(new SearchTaxonIdListener(_taxExcludeField));
    panelExclude.add(searchExclude, BorderLayout.EAST);
    builder.append(DBMSMessages.getString("FormatDBPanel.taxExclude.lbl"),
        panelExclude);
    // ngK 4.3: for now, create controls but do not show them.
    // Indeed, setLocalSeqIds cannot work anymore: it is only used by formatdb
    // for NCBI DBs
    // creation.
    getNcbiIDPanel();
    // builder.append(KDMSMessages.getString("FormatDBPanel.ncbiId.lbl"),
    // getNcbiIDPanel());
    // lan 08/04/2014 : no more use because allways true
    getCheckInputFilesPanel();
    // builder.append(KDMSMessages.getString("FormatDBPanel.chkInFiles.lbl"),
    // getCheckInputFilesPanel());

    // tabs for filtering
    this._filterTabs = new JTabbedPane();
    // first tab : sequence file validators for filter
    this._sfmUIFilter = new SequenceFileManagerUI(true, true, true, false,
        false, true, _runningPanel.getTaskProgress());
    this._sfmUIFilter.setHelpArea(_helpArea);
    this._filterTabs.addTab(
        DBMSMessages.getString("IndexDBPanel.filterProperties.title"),
        _sfmUIFilter.createPanel());
    // second tab : taxonomy
    _panelFilterTaxonomy = new JPanel(new BorderLayout());
    _panelFilterTaxonomy.add(builder.getContainer(), BorderLayout.CENTER);
    this._filterTabs.addTab(
        DBMSMessages.getString("IndexDBPanel.filterTaxonomy.title"),
        _panelFilterTaxonomy);
    if (!TaxonMatcherHelper.isNCBITaxonomyInstalled()) {
      disableFilterTaxonomy();
    }
    // third tab : rename
    this._sfmUIRename = new SequenceFileManagerUI(false, false, false, false,
        true, true, _runningPanel.getTaskProgress());
    this._sfmUIRename.setHelpArea(_helpArea);
    this._filterTabs.addTab(
        DBMSMessages.getString("IndexDBPanel.filterRename.title"),
        _sfmUIRename.createPanel());

    this._filterTabs.setBorder(BorderFactory.createTitledBorder(DBMSMessages
        .getString("IndexDBPanel.msg2")));

    return this._filterTabs;
  }

  private void disableFilterTaxonomy() {
    _panelFilterTaxonomy.setEnabled(false);
    _taxIncludeField.setEditable(false);
    _taxExcludeField.setEditable(false);
  }

  private JPanel getCheckInputFilesPanel() {
    HelpListener fl;
    JRadioButton yesBtn, noBtn;
    ButtonGroup group;
    JPanel ncbiIdPanel, btnPanel;

    yesBtn = new JRadioButton(DBMSMessages.getString("FormatDBPanel.yes.lbl"));
    noBtn = new JRadioButton(DBMSMessages.getString("FormatDBPanel.no.lbl"));
    group = new ButtonGroup();
    group.add(yesBtn);
    group.add(noBtn);
    yesBtn.setSelected(true);
    fl = new HelpListener(
        DBMSMessages.getString("FormatDBPanel.chkInFiles.msg"));
    yesBtn.addFocusListener(fl);
    noBtn.addFocusListener(fl);
    // yesBtn.addMouseListener(fl);
    // noBtn.addMouseListener(fl);
    btnPanel = new JPanel();
    btnPanel.add(yesBtn);
    btnPanel.add(noBtn);
    ncbiIdPanel = new JPanel(new BorderLayout());
    ncbiIdPanel.add(btnPanel, BorderLayout.WEST);

    _checkInFilesBtn = yesBtn;
    _doNotCheckInFilesBtn = noBtn;
    ncbiIdPanel.setBorder(null);
    return ncbiIdPanel;
  }

  private JPanel getNcbiIDPanel() {
    HelpListener fl;
    JRadioButton yesBtn, noBtn;
    ButtonGroup group;
    JPanel ncbiIdPanel, btnPanel;

    yesBtn = new JRadioButton(DBMSMessages.getString("FormatDBPanel.yes.lbl"));
    noBtn = new JRadioButton(DBMSMessages.getString("FormatDBPanel.no.lbl"));
    group = new ButtonGroup();
    group.add(yesBtn);
    group.add(noBtn);
    fl = new HelpListener(
        DBMSMessages.getString("FormatDBPanel.useNcbiSeqId.msg"));
    yesBtn.addFocusListener(fl);
    noBtn.addFocusListener(fl);
    // yesBtn.addMouseListener(fl);
    // noBtn.addMouseListener(fl);
    noBtn.setSelected(true);
    btnPanel = new JPanel();
    btnPanel.add(yesBtn);
    btnPanel.add(noBtn);
    ncbiIdPanel = new JPanel(new BorderLayout());
    ncbiIdPanel.add(btnPanel, BorderLayout.WEST);
    ncbiIdPanel.setBorder(null);
    _useNcbiIdBtn = yesBtn;
    _doNotUseNcbiIdBtn = noBtn;
    return ncbiIdPanel;
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

  private JComponent getCommandPanel() {
    JPanel btnPnl;
    JButton okBtn;

    boolean macOS = DBMSExecNativeCommand.getOSType() == DBMSExecNativeCommand.MAC_OS;

    okBtn = new JButton(DBMSMessages.getString("IndexDBPanel.btn.ok"));
    okBtn.addActionListener(new IndexingDBAction());
    okBtn.setIcon(EZEnvironment.getImageIcon("dbinstall.png"));
    _testFilteringBtn = new JButton(
        DBMSMessages.getString("IndexDBPanel.btn.testFilter"));
    _testFilteringBtn.addActionListener(new FilteringDBAction());
    _testFilteringBtn.setIcon(EZEnvironment.getImageIcon("db_filter.png"));

    btnPnl = new JPanel();
    btnPnl.setLayout(new BoxLayout(btnPnl, BoxLayout.X_AXIS));
    // btnPnl.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
    btnPnl.add(Box.createHorizontalGlue());
    btnPnl.add(_testFilteringBtn);
    btnPnl.add(okBtn);
    if (!macOS)
      btnPnl.add(Box.createHorizontalGlue());
    return btnPnl;
  }

  /**
   * Returns the list of data files.
   */
  public List<String> getFileList() {
    ArrayList<String> dbs;
    ListModel<String> model;
    String str;
    HashSet<String> names;
    int i, size;

    dbs = new ArrayList<String>();
    names = new HashSet<String>();
    model = _dbList.getModel();
    size = model.getSize();
    for (i = 0; i < size; i++) {
      str = model.getElementAt(i);
      if (names.contains(str) == false) {
        dbs.add(str);
        names.add(str);
      }
    }
    return dbs;
  }

  /**
   * Returns taxonomic constraint.
   */
  private String getTaxInclude() {
    return _taxIncludeField.getText();
  }

  /**
   * Returns taxonomic constraint.
   */
  private String getTaxExclude() {
    return _taxExcludeField.getText();
  }

  /**
   * Returns the index name.
   */
  private String getDBName() {
    return (_nameField.getText());
  }

  /**
   * Returns the index description.
   */
  private String getDBDescription() {
    return (_descriptionField.getText());
  }

  /**
   * Returns the index location.
   */
  private String getDBPath() {
    if (_enableUserDir) {
      return _pathField.getText();
    } else {
      String path, reader, type;

      reader = getSelectedDbFormat().getDBType();
      if (reader.equals(DBMirrorConfig.GB_READER)
          || reader.equals(DBMirrorConfig.EM_READER)
          || reader.equals(DBMirrorConfig.BLASTN_READER))
        type = DBServerConfig.NUCLEIC_TYPE;
      else if (reader.equals(DBMirrorConfig.UP_READER)
          || reader.equals(DBMirrorConfig.GP_READER)
          || reader.equals(DBMirrorConfig.BLASTP_READER))
        type = DBServerConfig.PROTEIN_TYPE;
      else
        type = DBServerConfig.DICO_TYPE;
      path = DBMSAbstractConfig.getLocalMirrorPath() + type + File.separator
          + getDBName();
      return (path);
    }
  }

  private DBFormatEntry getSelectedDbFormat() {
    return (DBFormatEntry) _dbFormat.getSelectedItem();
  }

  private void enableFastaControls(boolean enable) {
    _useNcbiIdBtn.setEnabled(enable);
    _doNotUseNcbiIdBtn.setEnabled(enable);
    _checkInFilesBtn.setEnabled(enable);
    _doNotCheckInFilesBtn.setEnabled(enable);
  }

  private void enableNamesControls(boolean enable) {
    _nameField.setEnabled(enable);
    _descriptionField.setEnabled(enable);
  }

  private void enableFiltering(boolean enable) {
    this._testFilteringBtn.setEnabled(enable);
  }

  private void displayWarnMessage(String msg) {
    JOptionPane.showMessageDialog(
        JOptionPane.getFrameForComponent(LocalDBPanel.this), msg,
        DBMSMessages.getString("IndexDBDialog.dlg.header"),
        JOptionPane.WARNING_MESSAGE);
  }

  /**
   * Checks whether the data given by the user are valid.
   */
  /**
   * Checks whether the data given by the user are valid.
   * 
   * @param testIfDbExists
   *          : if set to true => will ask for overwrite the existed directory
   * @param createDestDir
   *          : if set to false => do not create a new directory neither test if
   *          exists
   * @return
   */
  private boolean checkData(boolean testIfDbExists, boolean createDestDir) {
    MessageFormat mf;
    String str, dbPath;
    File f;

    // check data files list
    if (getFileList().isEmpty()) {
      displayWarnMessage(DBMSMessages.getString("IndexDBPanel.err1"));
      return false;
    }
    // check the data type
    if (getSelectedDbFormat().getType() == -1) {
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
    if (createDestDir) {
      dbPath = str;
      f = new File(dbPath);
      if (!f.exists()) {
        if (!f.mkdirs()) {
          mf = new MessageFormat(DBMSMessages.getString("IndexDBPanel.err4"));
          displayWarnMessage(mf.format(new Object[] { dbPath }));
          return false;
        }
      } else if (testIfDbExists) {
        mf = new MessageFormat(DBMSMessages.getString("IndexDBDialog.err2"));
        int ret = JOptionPane.showConfirmDialog(
            JOptionPane.getFrameForComponent(LocalDBPanel.this),
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

  public void initForReinstall(DBDescriptor db) {

    DBMirrorConfig config = DBDescriptorUtils.getLocalDBMirrorConfig();
    int i = 2;
    String dbName = db.getName() + "_" + i;
    while (config.containsDbName(dbName)) {
      i++;
      dbName = db.getName() + "_" + i;
    }
    this._nameField.setText(dbName);

    if (db.getType().equals(DBDescriptor.TYPE.nucleic)
        || db.getType().equals(DBDescriptor.TYPE.blastn)) {
      this._dbFormat.setSelectedItem(DBFormatEntry.fastaNucleic);
    } else if (db.getType().equals(DBDescriptor.TYPE.proteic)
        || db.getType().equals(DBDescriptor.TYPE.blastp)) {
      this._dbFormat.setSelectedItem(DBFormatEntry.fastaProteic);
    }

    if ((_dbList.getModel() != null) && (_dbList.getModel().getSize() > 0)) {
      ((DefaultListModel<String>) this._dbList.getModel()).removeAllElements();
    }

    List<File> filesToAdd = DBDescriptorUtils.getOriginalFile(db);
    // check for original files
    if (CollectionUtils.isEmpty(filesToAdd)) {
      // not found => use the created volume files
      List<String> volumeFilePaths = DBDescriptorUtils.getFastaVolumes(db);
      if (volumeFilePaths != null) {
        for (String volumeFilePath : volumeFilePaths) {
          filesToAdd.add(new File(volumeFilePath));
        }
      }
    }

    if (CollectionUtils.isNotEmpty(filesToAdd)) {
      File first = filesToAdd.get(0);
      int dbFormat = SeqIOUtils.guessFileFormat(first.getAbsolutePath());
      switch (dbFormat) {
        case SeqIOUtils.SWISSPROT:
          this._dbFormat.setSelectedItem(DBFormatEntry.uniprot);
          break;
        case SeqIOUtils.GENBANK:
          this._dbFormat.setSelectedItem(DBFormatEntry.genbank);
          break;
        case SeqIOUtils.EMBL:
          this._dbFormat.setSelectedItem(DBFormatEntry.embl);
          break;
        case SeqIOUtils.FASTAPROT:
          this._dbFormat.setSelectedItem(DBFormatEntry.fastaProteic);
          break;
        case SeqIOUtils.FASTADNA:
        case SeqIOUtils.FASTARNA:
          this._dbFormat.setSelectedItem(DBFormatEntry.fastaNucleic);
          break;
        case SeqIOUtils.GENPEPT:
          this._dbFormat.setSelectedItem(DBFormatEntry.genpept);
          break;
      }

      this.addFiles(filesToAdd);
    }

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
        _descriptionField.setText(_nameField.getText() + " databank");
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
		 * 
		 */
    private static final long serialVersionUID = 1L;

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

      f = EZFileManager.chooseFilesForOpenAction(LocalDBPanel.this,
          DBMSMessages.getString("IndexDBPanel.dlg.open.header"), null);
      addFiles(f);

      _changedSinceLastFilter = true;
    }
  }

  /**
   * Actions used to remove data files.
   */
  private class RemoveFileAction extends AbstractAction {
    /**
		 * 
		 */
    private static final long serialVersionUID = 1L;

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
      _changedSinceLastFilter = true;
    }
  }

  /**
   * Listener used to set some help message when the user clicks within the text
   * area to enter the type combo box.
   */
  private class TypeFocusListener implements FocusListener, ActionListener {
    public void focusGained(FocusEvent event) {
      _helpArea.setText(DBMSMessages.getString("IndexDBPanel.type.tip"));
    }

    public void focusLost(FocusEvent event) {
      _helpArea.setText("");
    }

    @SuppressWarnings("unchecked")
    public void actionPerformed(ActionEvent e) {
      JComboBox<DBFormatEntry> cb = (JComboBox<DBFormatEntry>) e.getSource();
      Object obj = cb.getSelectedItem();
      if (obj == null)
        return;
      DBFormatEntry entry = (DBFormatEntry) obj;
      boolean select = (entry.getType() == DBUtils.FN_DB_FORMAT || entry
          .getType() == DBUtils.FP_DB_FORMAT);
      enableFastaControls(select);

      select = false;
      _helpArea.setText("");
      // is a dico selected ?
      for (Dicos dico : Dicos.values()) {
        if (entry.getType() == dico.format) {
          select = true;
          _nameField.setText(dico.name);
          _descriptionField.setText(dico.description);
          _helpArea.setText(dico.helpInstall);
          break;
        }
      }

      enableNamesControls(!select);
      enableFiltering(!select);

      if (entry.getHeaderFormat() == DBUtils.BOLD_HEADER_FORMAT) {
        _helpArea
            .setText("To install BOLD files, provide files available at http://www.boldsystems.org/index.php/datarelease");
      }
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

  private class DescriptionFocusListener implements FocusListener {
    public void focusGained(FocusEvent event) {
      _helpArea.setText(DBMSMessages.getString("IndexDBPanel.desc.tip"));
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
      File path = EZFileManager.chooseDirectory(LocalDBPanel.this,
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

    /**
		 * 
		 */
    private static final long serialVersionUID = 1L;

    public MyCellRenderer() {
      this.setBorder(BorderFactory.createEmptyBorder(1, 4, 1, 1));
      this.setOpaque(true);
    }

    public Component getListCellRendererComponent(JList<? extends String> list,
        String db, int index, boolean isSelected, boolean cellHasFocus) {

      try {
        File file = new File(db);
        this.setText(file.getName() + " (" + file.getAbsolutePath() + ")");
      } catch (Exception ex) {
        this.setText(db);
      }
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
    /**
		 * 
		 */
    private static final long serialVersionUID = 1L;
    private DataFlavor        _fileFlavor;

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

  private class FilteringDBAction extends AbstractAction {

    /**
		 * 
		 */
    private static final long           serialVersionUID  = 1L;
    private long                        nbSeqFound        = 0;
    private long                        nbSeqDiscarded    = 0;
    private ScheduledThreadPoolExecutor scheduler;
    private int                         nbTasksTerminated = 0;

    /**
     * Manages JButton action
     */
    public void actionPerformed(ActionEvent e) {
      if (!_runningPanel.canStartAJob(null))
        return;

      if (!checkData(false, false))
        return;

      scheduler = new ScheduledThreadPoolExecutor(3);
      nbSeqFound = 0;
      nbSeqDiscarded = 0;
      nbTasksTerminated = 0;

      _runningPanel.getMonitor().processingStarted();

      for (String file : getFileList()) {
        FilteringTask thread = new FilteringTask(file, this);
        scheduler.schedule(thread, 1, TimeUnit.SECONDS);
      }

      _changedSinceLastFilter = false;

    }

    public void taskTerminated(long nbSeq, long nbDiscarded) {
      this.nbSeqFound += nbSeq;
      this.nbSeqDiscarded += nbDiscarded;
      if (LoggerCentral.processAborted()) {
        ((RunningMirrorPanel.MyUserProcessingMonitor) _runningPanel
            .getMonitor()).processingDone();
        scheduler.shutdownNow();
        scheduler = null;
        pushMessageToMonitor(RunningMirrorPanel.ERROR_ICON,
            "Filtering aborted", UserProcessingMonitor.MSG_TYPE.ABORTED);
        LoggerCentral.reset();
      } else {
        nbTasksTerminated++;
        if (nbTasksTerminated >= getFileList().size()) {
          String msg = "";
          if (nbSeqFound != -1) {
            msg = "TEST DONE : "
                + NumberFormat.getInstance().format(nbSeqFound)
                + " sequences kept and "
                + NumberFormat.getInstance().format(nbSeqDiscarded)
                + " discarded.";
          } else {
            msg = "TEST DONE : all sequences kept with this filter.";
          }
          pushMessageToMonitor(RunningMirrorPanel.OK_ICON, msg,
              UserProcessingMonitor.MSG_TYPE.OK);
          ((RunningMirrorPanel.MyUserProcessingMonitor) _runningPanel
              .getMonitor()).processingDone();
        }
      }
    }

    public void pushMessageToMonitor(ImageIcon icon, String message,
        UserProcessingMonitor.MSG_TYPE msgType) {
      if (_runningPanel.getMonitor() != null) {
        _runningPanel.getMonitor()
            .processingMessage(icon, PLocalLoader.WORKER_ID, getDBName(),
                UserProcessingMonitor.PROCESS_TYPE.TASK_EXECUTION, msgType,
                message);
      }
    }
  }

  private class FilteringTask extends TimerTask {
    private SequenceFileManager sfm;

    private String              filePath;
    private FilteringDBAction   filteringAction;

    public FilteringTask(String filePath, FilteringDBAction filteringAction) {
      this.filePath = filePath;
      this.filteringAction = filteringAction;
    }

    public void run() {
      try {
        this.filteringAction.pushMessageToMonitor(null, "Filtering file '"
            + filePath + "'...", UserProcessingMonitor.MSG_TYPE.OK);
        sfm = _sfmUIFilter.getSequenceFileManager(this.filePath, DatabankFormat
            .getFormatTypeFromDBUtils(getSelectedDbFormat().getType()), LOGGER);
        _sfmUIRename.addValidatorsTo(sfm);
        // add taxonomy filter
        if ((StringUtils.isNotBlank(_taxIncludeField.getText()))
            || (StringUtils.isNotBlank(_taxExcludeField.getText()))) {
          SequenceValidatorTaxon validator = new SequenceValidatorTaxon(
              _taxIncludeField.getText(), _taxExcludeField.getText());
          sfm.addValidator(validator);
        }
        LoggerCentral.stopThisSfmIfAbort(sfm);
        sfm.execute();
      } catch (Exception e1) {
        LoggerCentral.warn(LOGGER, e1.getMessage());
        this.filteringAction.pushMessageToMonitor(
            RunningMirrorPanel.ERROR_ICON, e1.getMessage(),
            UserProcessingMonitor.MSG_TYPE.ERROR);
      } finally {
        long nbSeq = 0;
        long nbDiscarded = 0;
        if (sfm != null) {
          nbSeq = sfm.getNbSequencesFound();
          nbDiscarded = sfm.getNbSequencesDiscarded();
        }
        this.filteringAction.taskTerminated(nbSeq, nbDiscarded);
        LoggerCentral.removeSfmToAbort();
      }

    }
  }

  private class IndexingDBAction extends AbstractAction {
    /**
		 * 
		 */
    private static final long serialVersionUID = 1L;

    /**
     * Manages JButton action
     */
    public void actionPerformed(ActionEvent e) {
      DBServerConfig descriptor;
      String file;
      int i, size;
      List<String> files;
      File descFile = null;

      if (!_runningPanel.canStartAJob(null))
        return;

      if (!checkData(true, true))
        return;

      files = getFileList();
      size = files.size();
      List<File> databankFiles = new ArrayList<File>();

      // Trying to get the previous filtered files instead of filter again the
      // same
      _filterBeforeInstall = true;
      if (!_changedSinceLastFilter
          && !_sfmUIFilter.hasChangedSinceLastSequenceFileManager()
          && !_sfmUIRename.hasChangedSinceLastSequenceFileManager()) {
        String[] foundFiles;
        for (i = 0; i < size; i++) {
          file = files.get(i);
          // In case of a previous filter, takes the filtered files

          final File current = new File(file);
          foundFiles = new File(DBMSAbstractConfig.getWorkingFilterPath())
              .list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                  return current.getName().equalsIgnoreCase(name);
                }
              });
          if ((foundFiles != null) && (foundFiles.length == 1)) {
            databankFiles.add(new File(DBMSAbstractConfig.getWorkingFilterPath(),
                foundFiles[0]));

          } else {
            // something was wrong but what ??
            _changedSinceLastFilter = true;
            break;
          }
        }
        _filterBeforeInstall = false;
      }

      if (databankFiles.size() == 0) {
        // a pb appeared while trying to get the previous filtered files
        for (i = 0; i < size; i++) {
          databankFiles.add(new File(files.get(i)));
        }
      }

      // filter
      String filters = "";
      // set filter parameter only if necessary
      if (_testFilteringBtn.isEnabled() && _filterBeforeInstall) {
        SequenceFileManager sfm;
        try {
          sfm = _sfmUIFilter.getSequenceFileManager(files.get(0),
              DatabankFormat.getFormatTypeFromDBUtils(getSelectedDbFormat()
                  .getType()), LOGGER);
          _sfmUIRename.addValidatorsTo(sfm);

          filters += sfm.toParametersForUnitTask();
        } catch (IOException e1) {
          LoggerCentral.warn(LOGGER, e1.getMessage());
        }
      }

      // create the descriptor
      descriptor = new DBServerConfig(getDBName(), getDBDescription(),
          getSelectedDbFormat(), getTaxInclude(), getTaxExclude(), filters,
          databankFiles);

      // store it on disk
      boolean ok = true;
      try {
        descFile = descriptor.createTemporaryDscFile(null);
      } catch (Exception ex) {
        LOGGER.warn(ex);
        displayWarnMessage("Unable to save descriptor file.");
        ok = false;
      }

      if (!ok)
        return;
      DescriptorEntry entry;
      ArrayList<DescriptorEntry> lstEntry;

      entry = new DescriptorEntry(descriptor, descFile.getAbsolutePath());
      lstEntry = new ArrayList<DescriptorEntry>();
      lstEntry.add(entry);

      _runningPanel.startLoadingEntries(lstEntry,
          PFTPLoaderDescriptor.MAINTASK_DOWNLOAD);
      // Note: created descriptor file is deleted during software startup. See
      // UiUtils.getDescriptors()
    }
  }

  private class JListWithHelp extends JList<String> {
    /**
		 * 
		 */
    private static final long serialVersionUID = 1L;
    private String            _msg;

    public JListWithHelp() {
      super();
    }

    public void setMessage(String msg) {
      _msg = msg;
    }

    public void paintComponent(Graphics g) {
      super.paintComponent(g);
      if (this.getModel().getSize() != 0 || _msg == null)
        return;
      Color oldClr;
      FontMetrics fm;
      Font oldFnt;
      StringTokenizer elements = null;
      String token = null;
      int n, h;

      elements = new StringTokenizer(_msg, ",\t\n\r\f|");
      fm = this.getFontMetrics(this.getFont());
      oldClr = g.getColor();
      oldFnt = g.getFont();
      g.setFont(this.getFont());
      g.setColor(Color.LIGHT_GRAY);
      h = fm.getHeight();
      n = 1;
      while (elements.hasMoreTokens()) {
        token = elements.nextToken();
        g.drawString(token, 2, n * h);
        n++;
        if (token.indexOf('.') >= 0)
          n++;
      }

      g.setColor(oldClr);
      g.setFont(oldFnt);
    }
  }

  private class SearchTaxonIdListener implements ActionListener {
    private JTextField tf;

    public SearchTaxonIdListener(JTextField tf) {
      this.tf = tf;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      NcbiTaxonomySearchDlg dlg = new NcbiTaxonomySearchDlg(
          JOptionPane.getFrameForComponent(LocalDBPanel.this));
      dlg.setVisible(true);
      List<String> ids = dlg.getSelectedIds();
      if (ids != null) {
        String previousFilter = this.tf.getText();
        if (StringUtils.isNotBlank(previousFilter)) {
          if (!previousFilter.trim().endsWith(",")) {
            previousFilter += ",";
          }
        }
        for (String id : ids) {
          previousFilter += id.trim() + ",";
        }
        // remove the last ,
        this.tf
            .setText(previousFilter.substring(0, previousFilter.length() - 1));
      }

    }
  }

}
