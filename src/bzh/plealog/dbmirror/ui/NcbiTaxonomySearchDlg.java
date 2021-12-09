/* Copyright (C) 2007-2017 Ludovic Antin
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
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.apache.log4j.Logger;

import bzh.plealog.dbmirror.lucenedico.DicoStorageSystemException;
import bzh.plealog.dbmirror.lucenedico.DicoTerm;
import bzh.plealog.dbmirror.lucenedico.DicoTermQuerySystem;
import bzh.plealog.dbmirror.ui.resources.DBMSMessages;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptorUtils;

public class NcbiTaxonomySearchDlg extends JDialog {

  private static final Logger LOGGER           = Logger
                                                   .getLogger("kdms.NcbiTaxonomySearchDlg");

  /**
	 * 
	 */
  private static final long   serialVersionUID = -8333827350075933508L;
  private JTextField          tfSearch;
  private DefaultTableModel   tblModel;
  private DicoTermQuerySystem dicoQuerySystem;
  private JTable              tblResults;
  private List<String>        selectedIds;
  private JLabel              lblNbResults;

  @SuppressWarnings("serial")
  public NcbiTaxonomySearchDlg(Frame parent) {
    super(parent);
    setLocationByPlatform(true);
    setPreferredSize(new Dimension(400, 500));
    setSize(new Dimension(400, 500));
    setTitle(DBMSMessages.getString("NcbiTaxonomySearchDlg.this.title"));

    JPanel searchPanel = new JPanel();
    getContentPane().add(searchPanel, BorderLayout.NORTH);
    searchPanel.setLayout(new BorderLayout());

    JLabel lblSearch = new JLabel(
        DBMSMessages.getString("NcbiTaxonomySearchDlg.lblSearch.text"));
    lblSearch.setHorizontalAlignment(SwingConstants.CENTER);
    searchPanel.add(lblSearch, BorderLayout.WEST);

    tfSearch = new JTextField();
    searchPanel.add(tfSearch, BorderLayout.CENTER);
    tfSearch.addActionListener(new SearchListener());

    JButton btnSearch = new JButton(
        DBMSMessages.getString("NcbiTaxonomySearchDlg.btnSearch.text"));
    btnSearch.addActionListener(new SearchListener());
    searchPanel.add(btnSearch, BorderLayout.EAST);

    lblNbResults = new JLabel();
    lblNbResults.setText(" ");
    lblNbResults.setFont(new Font("Tahoma", Font.PLAIN, 10));
    lblNbResults.setHorizontalAlignment(SwingConstants.CENTER);
    searchPanel.add(lblNbResults, BorderLayout.SOUTH);

    JPanel resultPanel = new JPanel();
    getContentPane().add(resultPanel, BorderLayout.CENTER);

    tblResults = new JTable();
    tblResults.setFillsViewportHeight(true);
    tblResults.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

    tblResults.setBounds(new Rectangle(10, 10, 10, 10));
    tblModel = new DefaultTableModel() {
      @Override
      public boolean isCellEditable(int row, int column) {
        // all cells false
        return false;
      }
    };
    tblModel.addColumn("Identifier");
    tblModel.addColumn("Name");
    tblModel.addRow(new String[] { "<id>", "<Name>" });
    resultPanel.setLayout(new BorderLayout());
    tblResults.setModel(tblModel);
    tblResults.getColumn("Identifier").setPreferredWidth(100);
    tblResults.getColumn("Identifier").setMaxWidth(100);
    tblResults.setRowSorter(new FeatureRowSorter(tblModel));
    JScrollPane scrollPane = new JScrollPane(tblResults);
    resultPanel.add(scrollPane);

    JPanel panel = new JPanel();
    getContentPane().add(panel, BorderLayout.SOUTH);
    panel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

    JButton btnOk = new JButton(
        DBMSMessages.getString("NcbiTaxonomySearchDlg.btnNewButton.text"));
    btnOk.setPreferredSize(new Dimension(65, 23));
    btnOk.setMinimumSize(new Dimension(65, 23));
    btnOk.setMaximumSize(new Dimension(65, 23));
    btnOk.addActionListener(new OkListener());
    panel.add(btnOk);

    JButton btnCancel = new JButton(
        DBMSMessages.getString("NcbiTaxonomySearchDlg.btnNewButton_1.text")); //$NON-NLS-1$
    btnCancel.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        close();
      }
    });
    panel.add(btnCancel);

    this.setModal(true);
    this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    // to center
    this.setLocationRelativeTo(null);
  }

  public List<String> getSelectedIds() {
    return this.selectedIds;
  }

  private void close() {
    if (dicoQuerySystem != null) {
      try {
        DicoTermQuerySystem.closeDicoTermQuerySystem();
      } catch (Exception ex) {
        LOGGER.warn(ex.getMessage(), ex);
      }
    }
    dispose();
  }

  private class SearchListener implements ActionListener {

    public void actionPerformed(ActionEvent e) {
      if (dicoQuerySystem == null) {
        dicoQuerySystem = DicoTermQuerySystem
            .getDicoTermQuerySystem(DBDescriptorUtils
                .getDBMirrorConfig(DBMSAbstractConfig.getLocalMirrorConfFile()));
      }
      try {

        List<DicoTerm> terms = dicoQuerySystem.getApprochingTerms(tfSearch
            .getText(), 1500);
        int rows = tblModel.getRowCount();
        for (int i = rows - 1; i >= 0; i--) {
          tblModel.removeRow(i);
        }
        for (DicoTerm term : terms) {
          tblModel.addRow(new String[] { term.getId().trim(),
              term.getDataField().trim() });
        }

        if (terms.size() == 0) {
          lblNbResults.setText(DBMSMessages
              .getString("NcbiTaxonomySearchDlg.lblResult.textNothing"));
        } else {
          lblNbResults.setText(terms.size()
              + " "
              + DBMSMessages
                  .getString("NcbiTaxonomySearchDlg.lblResult.textFound"));
        }

      } catch (DicoStorageSystemException ex) {
        LOGGER.warn(ex.getMessage(), ex);
        lblNbResults.setText(DBMSMessages
            .getString("NcbiTaxonomySearchDlg.lblResult.textNothing"));
      }
    }
  }

  private class OkListener implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
      int[] selectedRows = tblResults.getSelectedRows();
      selectedIds = new ArrayList<String>();
      for (int selectedRow : selectedRows) {
        selectedIds.add(tblResults.getValueAt(selectedRow, 0).toString());
      }
      close();
    }

  }

  private class FeatureRowSorter extends TableRowSorter<TableModel> {
    public FeatureRowSorter(TableModel tableModel) {
      super(tableModel);
    }

    @Override
    public Comparator<?> getComparator(int column) {
      if (column == 0) {
        return new Comparator<Object>() {
          @Override
          public int compare(Object o1, Object o2) {
            return Integer.compare(Integer.valueOf(o1.toString()),
                Integer.valueOf(o2.toString()));
          }
        };
      } else {
        return super.getComparator(column);
      }
    }
  }

}
