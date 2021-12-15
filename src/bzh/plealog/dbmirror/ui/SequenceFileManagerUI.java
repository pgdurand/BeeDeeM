/* Copyright (C) 2007-2021 Ludovic Antin
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
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;

import bzh.plealog.dbmirror.ui.resources.DBMSMessages;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.descriptor.DatabankFormat;
import bzh.plealog.dbmirror.util.sequence.ISequenceValidator;
import bzh.plealog.dbmirror.util.sequence.SequenceFileManager;
import bzh.plealog.dbmirror.util.sequence.SequenceValidatorCutFile;
import bzh.plealog.dbmirror.util.sequence.SequenceValidatorDescription;
import bzh.plealog.dbmirror.util.sequence.SequenceValidatorRenamer;
import bzh.plealog.dbmirror.util.sequence.SequenceValidatorSize;
import bzh.plealog.dbmirror.util.sequence.SequenceValidatorSubset;

/**
 * Create an UI to instanciate a SequenceFileManager
 * 
 * @author Ludovic Antin
 * 
 */
public class SequenceFileManagerUI {

  private JPanel              mainPanel;
  private DefaultFormBuilder  filterBuilder;
  private DefaultFormBuilder  renameBuilder;
  private FormLayout          filterLayout;
  private FormLayout          renameLayout;
  // cut file parameters
  private boolean             showCutFile           = false;
  private JFormattedTextField tfCutFileMin;
  private JFormattedTextField tfCutFileMax;
  // filter on size parameters
  private boolean             showFilterSize        = false;
  private JFormattedTextField tfFilterSizeMin;
  private JFormattedTextField tfFilterSizeMax;
  // filter on description
  private boolean             showFilterDescription = false;
  private JTextField          tfFilterDescription;
  private JTextField          tfFilterDescriptionNot;
  private JCheckBox           cbFilterDexcriptionExact;
  // for rename
  private boolean             showRename            = false;
  private JCheckBox           cbRenameId;
  private JTextField          tfPrefix;
  private JFormattedTextField tfNumbering;
  private JTextField          tfSuffix;
  private JCheckBox           cbRenameDesc;
  private JRadioButton        rbUsePreviousId;
  private JRadioButton        rbText;
  private JRadioButton        rbRemove;
  private JTextField          tfNewDescription;
  // for subsets
  private boolean             showSusbsets          = false;
  private JFormattedTextField tfNbSubsets;

  // for editing help
  private JLabel              helpArea;
  // progress bar
  private boolean             showProgressBar       = false;
  private JProgressBar        progressBar;
  // for checking changes
  private boolean             hasChanged            = false;
  private JLabel              lbSetDescText;

  /**
   * 
   * @param showCutFile
   *          true if it is allowed to cut the file
   * @param showFilterOnSize
   *          true if it is allowed to filter by sequence size
   * @param showFilterOnDescription
   *          true if it is allowed to filter by description
   * @param showSubsets
   *          true if it is allowed to create subsets from the query file
   * @param showRename
   *          true if it is allowed to rename ids and/or descriptions
   * @param showProgressBar
   *          true if you want to use a progress bar
   * @param progressBar
   *          progress bar to use. If null and showProgressBar=true, a progress
   *          bar is created in the south part of the main panel
   */
  public SequenceFileManagerUI(boolean showCutFile, boolean showFilterOnSize,
      boolean showFilterOnDescription, boolean showSubsets, boolean showRename,
      boolean showProgressBar, JProgressBar progressBar) {
    this.filterLayout = new FormLayout("right:130dlu, 1dlu, 100dlu", "");
    this.filterBuilder = new DefaultFormBuilder(this.filterLayout);
    this.filterBuilder.setDefaultDialogBorder();

    this.renameLayout = new FormLayout(
        "fill:75dlu, 1dlu,fill:75dlu, 1dlu,fill:75dlu", "");
    this.renameBuilder = new DefaultFormBuilder(this.renameLayout);
    this.renameBuilder.setDefaultDialogBorder();

    this.showCutFile = showCutFile;
    this.showFilterSize = showFilterOnSize;
    this.showFilterDescription = showFilterOnDescription;
    this.showSusbsets = showSubsets;
    this.showRename = showRename;
    this.showProgressBar = showProgressBar;
    if (this.showCutFile) {
      this.filterLayout.setColumnSpec(1, new ColumnSpec("right:40dlu"));
      this.createSectionCutFile();
    }
    if (this.showFilterSize) {
      this.filterLayout.setColumnSpec(1, new ColumnSpec("right:130dlu"));
      this.createSectionFilterOnSize();
    }
    if (this.showFilterDescription) {
      this.filterLayout.setColumnSpec(1, new ColumnSpec("right:130dlu"));
      this.createSectionFilterOnDescription();
    }
    if (this.showSusbsets) {
      this.filterLayout.setColumnSpec(1, new ColumnSpec("right:130dlu"));
      this.createSectionSubset();
    }
    if (this.showRename) {
      this.createSectionRename();
    }
    if (this.showProgressBar) {
      this.progressBar = progressBar;
    }

  }

  private void createSectionSubset() {
    this.tfNbSubsets = new JFormattedTextField(UIUtils.getIntNumberFormatter());
    this.initTextField(this.tfNbSubsets, "SequenceFileRenamerUI.help.subset");
    FormLayout layout = new FormLayout("40dlu, 1dlu, 59dlu", "");
    DefaultFormBuilder builder = new DefaultFormBuilder(layout);
    builder.append(this.tfNbSubsets, new JLabel());
    this.filterBuilder.append(
        DBMSMessages.getString("SequenceFileRenamerUI.subset.label"),
        builder.getContainer());
  }

  private void createSectionCutFile() {
    this.tfCutFileMin = new JFormattedTextField(UIUtils.getIntNumberFormatter());
    this.tfCutFileMax = new JFormattedTextField(UIUtils.getIntNumberFormatter());
    this.initTextField(this.tfCutFileMin, "SequenceFileManagerUI.help.cutMin");
    this.initTextField(this.tfCutFileMax, "SequenceFileManagerUI.help.cutMax");
    FormLayout layout = new FormLayout(
        "40dlu, 1dlu, center:18dlu, 1dlu, 40dlu", "");
    DefaultFormBuilder builder = new DefaultFormBuilder(layout);
    builder.append(this.tfCutFileMin,
        new JLabel(DBMSMessages.getString("SequenceFileManagerUI.cutFile2")),
        this.tfCutFileMax);
    this.filterBuilder.append(
        DBMSMessages.getString("SequenceFileManagerUI.cutFile1"),
        builder.getContainer());
  }

  private void createSectionFilterOnSize() {
    this.tfFilterSizeMin = new JFormattedTextField(
        UIUtils.getIntNumberFormatter());
    this.tfFilterSizeMax = new JFormattedTextField(
        UIUtils.getIntNumberFormatter());
    initTextField(this.tfFilterSizeMin, "SequenceFileManagerUI.help.sizeMin");
    initTextField(this.tfFilterSizeMax, "SequenceFileManagerUI.help.sizeMax");

    FormLayout layout = new FormLayout(
        "40dlu, 1dlu, center:18dlu, 1dlu, 40dlu", "");
    DefaultFormBuilder builder = new DefaultFormBuilder(layout);
    builder
        .append(
            this.tfFilterSizeMin,
            new JLabel(DBMSMessages
                .getString("SequenceFileManagerUI.filterSize2")),
            this.tfFilterSizeMax);
    this.filterBuilder.append(
        DBMSMessages.getString("SequenceFileManagerUI.filterSize1"),
        builder.getContainer());
  }

  private void createSectionFilterOnDescription() {
    this.tfFilterDescription = new JTextField();
    this.tfFilterDescriptionNot = new JTextField();
    this.cbFilterDexcriptionExact = new JCheckBox();
    this.initTextField(this.tfFilterDescription,
        "SequenceFileManagerUI.help.description");
    this.initTextField(this.tfFilterDescriptionNot,
        "SequenceFileManagerUI.help.descriptionNot");
    this.initCheckBox(this.cbFilterDexcriptionExact,
        "SequenceFileManagerUI.help.descriptionExact");

    this.appendToFilterBuilder("SequenceFileManagerUI.filterDescription",
        this.tfFilterDescription);
    this.appendToFilterBuilder("SequenceFileManagerUI.filterDescriptionNot",
        this.tfFilterDescriptionNot);
    this.appendToFilterBuilder("SequenceFileManagerUI.filterDescriptionApprox",
        this.cbFilterDexcriptionExact);
  }

  private void createSectionRename() {

    if (this.showCutFile || this.showFilterDescription || this.showFilterSize
        || this.showSusbsets) {
      this.renameBuilder.append(new JSeparator(),
          this.renameLayout.getColumnCount());
    }

    // rename id section
    this.cbRenameId = new JCheckBox();
    this.tfPrefix = new JTextField();
    this.tfSuffix = new JTextField();
    this.tfNumbering = new JFormattedTextField(UIUtils.getIntNumberFormatter());
    this.tfNumbering.setValue(1);
    this.initCheckBox(this.cbRenameId, "SequenceFileRenamerUI.help.cbRenameId");
    this.initTextField(this.tfPrefix, "SequenceFileRenamerUI.help.prefix");
    this.initTextField(this.tfSuffix, "SequenceFileRenamerUI.help.suffix");
    this.initTextField(this.tfNumbering, "SequenceFileRenamerUI.help.number");
    this.setEnableRenameIdSection(false);
    this.cbRenameId.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        setEnableRenameIdSection(e.getStateChange() == ItemEvent.SELECTED);
      }
    });
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(
        new JLabel(DBMSMessages.getString("SequenceFileRenamerUI.id.cbText")),
        BorderLayout.CENTER);
    panel.add(this.cbRenameId, BorderLayout.WEST);
    this.renameBuilder.append(panel, this.renameLayout.getColumnCount());
    panel = new JPanel(new BorderLayout());
    panel.add(
        new JLabel(DBMSMessages.getString("SequenceFileRenamerUI.id.prefix")),
        BorderLayout.WEST);
    panel.add(this.tfPrefix, BorderLayout.CENTER);
    this.renameBuilder.append(panel);
    panel = new JPanel(new BorderLayout());
    panel.add(
        new JLabel(DBMSMessages.getString("SequenceFileRenamerUI.id.number")),
        BorderLayout.WEST);
    panel.add(this.tfNumbering, BorderLayout.CENTER);
    this.renameBuilder.append(panel);
    panel = new JPanel(new BorderLayout());
    panel.add(
        new JLabel(DBMSMessages.getString("SequenceFileRenamerUI.id.suffix")),
        BorderLayout.WEST);
    panel.add(this.tfSuffix, BorderLayout.CENTER);
    this.renameBuilder.append(panel);

    this.renameBuilder.append(new JSeparator(),
        this.renameLayout.getColumnCount());

    // rename desc section
    this.cbRenameDesc = new JCheckBox();
    this.rbUsePreviousId = new JRadioButton(
        DBMSMessages.getString("SequenceFileRenamerUI.desc.usePreviousId"));
    this.rbRemove = new JRadioButton(
        DBMSMessages.getString("SequenceFileRenamerUI.desc.remove"));
    this.rbText = new JRadioButton();
    this.tfNewDescription = new JTextField();
    this.lbSetDescText = new JLabel(
        DBMSMessages.getString("SequenceFileRenamerUI.desc.descText"));
    this.initCheckBox(this.cbRenameDesc,
        "SequenceFileRenamerUI.help.cbRenameDesc");
    this.initTextField(this.tfNewDescription,
        "SequenceFileRenamerUI.help.descText");
    this.initRadioButton(this.rbRemove, "SequenceFileRenamerUI.help.remove");
    this.initRadioButton(this.rbText, "SequenceFileRenamerUI.help.descText");
    this.initRadioButton(this.rbUsePreviousId,
        "SequenceFileRenamerUI.help.usePreviousId");
    ButtonGroup group = new ButtonGroup();
    group.add(this.rbUsePreviousId);
    group.add(this.rbRemove);
    group.add(this.rbText);
    this.setEnableRenameDescSection(false);
    this.cbRenameDesc.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        setEnableRenameDescSection(e.getStateChange() == ItemEvent.SELECTED);
      }
    });

    panel = new JPanel(new BorderLayout());
    panel
        .add(
            new JLabel(DBMSMessages
                .getString("SequenceFileRenamerUI.desc.cbText")),
            BorderLayout.CENTER);
    panel.add(this.cbRenameDesc, BorderLayout.WEST);
    this.renameBuilder.append(panel, this.renameLayout.getColumnCount());

    this.renameBuilder.append(this.rbUsePreviousId,
        this.renameLayout.getColumnCount());
    this.renameBuilder
        .append(this.rbRemove, this.renameLayout.getColumnCount());

    FormLayout layout = new FormLayout("9dlu, 1dlu, left:30dlu, 1dlu, 100dlu",
        "");
    DefaultFormBuilder builder = new DefaultFormBuilder(layout);
    builder.append(this.rbText, this.lbSetDescText, this.tfNewDescription);

    this.renameBuilder.append(builder.getContainer(),
        this.renameLayout.getColumnCount());

  }

  private void appendToFilterBuilder(String messageKey, Component component) {
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(component, BorderLayout.CENTER);
    String message = "";
    if (StringUtils.isNotBlank(messageKey)) {
      message = DBMSMessages.getString(messageKey);
    }
    this.filterBuilder.append(message, panel);
  }

  private void setEnableRenameIdSection(boolean enable) {
    this.tfPrefix.setEditable(enable);
    this.tfNumbering.setEditable(enable);
    this.tfSuffix.setEditable(enable);
  }

  private void setEnableRenameDescSection(boolean enable) {
    this.rbUsePreviousId.setEnabled(enable);
    this.rbRemove.setEnabled(enable);
    this.rbText.setEnabled(enable);
    this.tfNewDescription.setEditable(enable);
    this.lbSetDescText.setEnabled(enable);
  }

  private int getValue(JFormattedTextField tf) {
    try {
      return ((Integer) tf.getValue()).intValue();
    } catch (Exception e) {
      return -1;
    }
  }

  public JPanel createPanel() {
    this.mainPanel = new JPanel(new BorderLayout());
    if (this.showCutFile || this.showFilterDescription || this.showFilterSize
        || this.showSusbsets) {
      if (this.showRename) {
        this.mainPanel.add(this.filterBuilder.getContainer(),
            BorderLayout.NORTH);
        this.mainPanel.add(this.renameBuilder.getContainer(),
            BorderLayout.CENTER);
      } else {
        this.mainPanel.add(this.filterBuilder.getContainer(),
            BorderLayout.CENTER);
      }
    } else if (this.showRename) {
      this.mainPanel
          .add(this.renameBuilder.getContainer(), BorderLayout.CENTER);
    }
    if (this.showProgressBar) {
      if (this.progressBar == null) {
        this.progressBar = new JProgressBar();
        this.mainPanel.add(this.progressBar, BorderLayout.SOUTH);
      }
    }
    this.mainPanel.setVisible(true);
    return this.mainPanel;
  }

  public void setHelpArea(JLabel helpArea) {
    this.helpArea = helpArea;
  }

  private List<ISequenceValidator> getValidators() {
    ArrayList<ISequenceValidator> result = new ArrayList<ISequenceValidator>();
    if ((this.showCutFile) && (this.tfCutFileMin.isEditable())) {
      SequenceValidatorCutFile validator = new SequenceValidatorCutFile(
          getValue(this.tfCutFileMin), getValue(this.tfCutFileMax));
      if (validator.isActive()) {
        result.add(validator);
      }
    }
    if (this.showFilterSize) {
      SequenceValidatorSize validator = new SequenceValidatorSize(
          getValue(this.tfFilterSizeMin), getValue(this.tfFilterSizeMax));
      if (validator.isActive()) {
        result.add(validator);
      }
    }
    if (this.showFilterDescription) {
      String searchedDescription = "";

      // add search keywords
      String contains = this.tfFilterDescription.getText();
      contains = Utils.replaceAll(contains, ",", " ");
      contains = Utils.replaceAll(contains, ";", " ");
      StringTokenizer tokens = new StringTokenizer(contains, " ");
      while (tokens.hasMoreTokens()) {
        searchedDescription += tokens.nextToken().trim()
            + SequenceValidatorDescription.KEYWORD_DELIMITER;
      }
      // add NON search keywords
      String notContains = this.tfFilterDescriptionNot.getText();
      notContains = Utils.replaceAll(notContains, ",", " ");
      notContains = Utils.replaceAll(notContains, ";", " ");
      tokens = new StringTokenizer(notContains, " ");
      while (tokens.hasMoreTokens()) {
        searchedDescription += SequenceValidatorDescription.NOT_CONTAINS_CHARACTER
            + tokens.nextToken().trim()
            + SequenceValidatorDescription.KEYWORD_DELIMITER;
      }

      if (StringUtils.isNotBlank(searchedDescription)) {
        SequenceValidatorDescription validator = new SequenceValidatorDescription(
            searchedDescription);
        validator.setExactSearch(!this.cbFilterDexcriptionExact.isSelected());
        if (validator.isActive()) {
          result.add(validator);
        }
      }
    }
    if (this.showRename) {
      SequenceValidatorRenamer validator = new SequenceValidatorRenamer();
      if (this.cbRenameId.isSelected()) {
        validator.setUpdateIds(true);
        validator.setIdPrefix(this.tfPrefix.getText());
        validator.setIdSuffix(this.tfSuffix.getText());
        validator.setFirstId(this.getValue(this.tfNumbering));
      }
      if (this.cbRenameDesc.isSelected()) {
        validator.setUpdateDescriptions(true);
        if (this.rbUsePreviousId.isSelected()) {
          validator.setUsePreviousId(true);
        } else if (this.rbRemove.isSelected()) {
          validator.setNewDescription("");
        } else if (this.rbText.isSelected()) {
          validator.setNewDescription(this.tfNewDescription.getText());
        }
      }
      if (validator.isActive()) {
        result.add(validator);
      }
    }
    if (this.showSusbsets) {
      SequenceValidatorSubset validator = new SequenceValidatorSubset(
          getValue(this.tfNbSubsets));
      result.add(validator);

    }
    return result;
  }

  public SequenceFileManager getSequenceFileManager(String sequenceFilepath,
      DatabankFormat format, Logger logger) throws IOException {
    SequenceFileManager result = new SequenceFileManager(sequenceFilepath,
        format, logger, this.progressBar);
    this.addValidatorsTo(result);
    return result;
  }

  public SequenceFileManager getSequenceFileManager(BufferedReader reader,
      BufferedWriter writer, DatabankFormat format, Logger logger)
      throws IOException {
    SequenceFileManager result = new SequenceFileManager(reader, writer,
        format, logger);
    this.addValidatorsTo(result);
    return result;
  }

  /**
   * Create all validators depending of the graphical components and add them to
   * the sfm parameter
   * 
   * @param sfm
   */
  public void addValidatorsTo(SequenceFileManager sfm) {
    List<ISequenceValidator> validators = this.getValidators();
    for (ISequenceValidator validator : validators) {
      sfm.addValidator(validator);
    }
    this.hasChanged = false;
  }

  public boolean hasChangedSinceLastSequenceFileManager() {
    return this.hasChanged;
  }

  private void initTextField(JTextField textField, String helpMessage) {
    textField.addFocusListener(new HelpListener(DBMSMessages
        .getString(helpMessage)));

    textField.getDocument().addDocumentListener(new DocumentListener() {
      public void changedUpdate(DocumentEvent e) {
        hasChanged = true;
      }

      public void removeUpdate(DocumentEvent e) {
        hasChanged = true;
      }

      public void insertUpdate(DocumentEvent e) {
        hasChanged = true;
      }
    });
  }

  private void initCheckBox(JCheckBox cb, String helpMessage) {
    cb.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        hasChanged = true;
      }
    });

    cb.addFocusListener(new HelpListener(DBMSMessages.getString(helpMessage)));
  }

  private void initRadioButton(JRadioButton rb, String helpMessage) {
    rb.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        hasChanged = true;
      }
    });

    rb.addFocusListener(new HelpListener(DBMSMessages.getString(helpMessage)));
  }

  private class HelpListener implements FocusListener {
    private String hlpMsg;

    public HelpListener(String msg) {
      hlpMsg = msg;
    }

    public void focusGained(FocusEvent event) {
      if (helpArea != null)
        helpArea.setText(this.hlpMsg);
    }

    public void focusLost(FocusEvent event) {
      if (helpArea != null)
        helpArea.setText("");
    }
  }

  @SuppressWarnings("serial")
  public static void main(String[] args) {
    final SequenceFileManagerUI sfmUI = new SequenceFileManagerUI(true, true,
        true, true, true, true, null);
    JLabel label = new JLabel() {
      @Override
      public void setText(String text) {
        super.setText("<html>" + text + "</html>");
      }
    };

    JButton test = new JButton("Test");
    test.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          SequenceFileManager sfm = sfmUI
              .getSequenceFileManager(
                  "./tests/junit/SequenceFileManager/testRename/uniprot.dat",
                  DatabankFormat.swissProt, null);
          sfm.execute();
        } catch (FileNotFoundException e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
        } catch (IOException e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
        }
      }
    });
    sfmUI.setHelpArea(label);
    JPanel panel = new JPanel(new BorderLayout());
    JPanel panel2 = new JPanel(new BorderLayout());
    panel2.add(sfmUI.createPanel(), BorderLayout.CENTER);

    panel2.add(test, BorderLayout.SOUTH);
    panel.add(panel2, BorderLayout.CENTER);
    panel.add(label, BorderLayout.SOUTH);
    JDialog dialog = new JDialog();
    dialog.setSize(500, 500);
    dialog.getContentPane().add(panel);
    dialog.setVisible(true);
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

  }
}
