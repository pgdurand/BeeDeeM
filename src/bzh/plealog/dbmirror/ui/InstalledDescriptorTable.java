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

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EtchedBorder;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import bzh.plealog.dbmirror.util.descriptor.DBDescriptor;
import bzh.plealog.dbmirror.util.descriptor.IdxDescriptor;
import bzh.plealog.dbmirror.util.runner.DBMSExecNativeCommand;

/**
 * This is the JTable responsible for displaying the list of installed
 * databanks.
 * 
 * @author Patrick G. Durand
 */
public class InstalledDescriptorTable extends JTable {
  /**
   * 
   */
  private static final long            serialVersionUID = -2720647252620099053L;
  private QueryStatusTableCellRenderer _qStatusCellRenderer;
  private QueryTableComponentAdapter   _compoAdapter;

  public InstalledDescriptorTable(TableModel dm) {
    super(dm);
    _qStatusCellRenderer = new QueryStatusTableCellRenderer();
    _compoAdapter = new QueryTableComponentAdapter();
  }

  public TableCellRenderer getCellRenderer(int row, int column) {
    TableCellRenderer tcr;

    tcr = super.getCellRenderer(row, column);

    if (this
        .getModel()
        .getColumnName(column)
        .equals(
            InstalledDescriptorTableModel.HEADERS[InstalledDescriptorTableModel.ICON_HEADER])) {
      return _qStatusCellRenderer;
    }

    if (tcr instanceof JLabel) {
      JLabel lbl;
      lbl = (JLabel) tcr;
      if (this
          .getModel()
          .getColumnName(column)
          .equals(
              InstalledDescriptorTableModel.HEADERS[InstalledDescriptorTableModel.DESCRIPTION_HEADER])
          || this
              .getModel()
              .getColumnName(column)
              .equals(
                  InstalledDescriptorTableModel.HEADERS[InstalledDescriptorTableModel.NAME_HEADER])) {
        lbl.setHorizontalAlignment(SwingConstants.LEFT);
      } else {
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
      }
      if (row % 2 == 0) {
        lbl.setBackground(Color.WHITE);
      } else {
        lbl.setBackground(InstalledDescriptorTable.this.getBackground());
      }
    }
    return tcr;
  }

  /**
   * This is the renderer displaying the execution status of a query.
   */
  private class QueryStatusTableCellRenderer extends JLabel implements
      TableCellRenderer {

    /**
       * 
       */
    private static final long serialVersionUID = -6559886961428093184L;

    public QueryStatusTableCellRenderer() {
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int column) {

      IdxDescriptor entry = (IdxDescriptor) table.getValueAt(row, -1);
      if (entry.getType().equals(DBDescriptor.TYPE.proteic)) {
        setIcon(InstalledDescriptorTableModel._protImg);
      } else if (entry.getType().equals(DBDescriptor.TYPE.nucleic)) {
        setIcon(InstalledDescriptorTableModel._nucImg);
      } else if (entry.getType().equals(DBDescriptor.TYPE.dico)) {
        setIcon(InstalledDescriptorTableModel._dicoImg);
      } else if (entry.getType().equals(DBDescriptor.TYPE.blastn)) {
        setIcon(InstalledDescriptorTableModel._nucBlastImg);
      } else if (entry.getType().equals(DBDescriptor.TYPE.blastp)) {
        setIcon(InstalledDescriptorTableModel._protBlastImg);
      } else {
        setIcon(null);
      }
      setText("[" + entry.getKbCode() + "]");
      if (isSelected) {
        setBackground(table.getSelectionBackground());
        setForeground(table.getSelectionForeground());
      } else {
        if (row % 2 == 0) {
          setBackground(Color.white);
        } else {
          setBackground(table.getBackground());
        }
        setForeground(table.getForeground());
      }
      setEnabled(table.isEnabled());
      setFont(table.getFont());
      if (DBMSExecNativeCommand.getOSType() == DBMSExecNativeCommand.MAC_OS) {
        setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        setBackground(UIManager.getColor("Panel.background"));
      } else {
        setBorder(UIManager.getBorder("TableHeader.cellBorder"));
      }
      return this;
    }
  }

  public void initColumnSize(int width) {
    FontMetrics fm;
    TableColumnModel tcm;
    TableColumn tc, lastTc = null;
    String header;
    int i, size, tot, val;

    fm = this.getFontMetrics(InstalledDescriptorTable.this.getFont());
    tcm = this.getColumnModel();
    size = tcm.getColumnCount();
    tot = 0;
    for (i = 0; i < size; i++) {
      tc = tcm.getColumn(i);
      header = tc.getHeaderValue().toString();
      if (!header
          .equals(InstalledDescriptorTableModel.HEADERS[InstalledDescriptorTableModel.DESCRIPTION_HEADER])) {
        if (header
            .equals(InstalledDescriptorTableModel.HEADERS[InstalledDescriptorTableModel.ICON_HEADER])) {
          val = 30;
        } else if (header
            .equals(InstalledDescriptorTableModel.HEADERS[InstalledDescriptorTableModel.NAME_HEADER])) {
          val = 3 * fm.stringWidth(header) + 20;
        } else if (header
            .equals(InstalledDescriptorTableModel.HEADERS[InstalledDescriptorTableModel.NBSEQS_HEADER])) {
          val = 2 * fm.stringWidth(header) + 20;
        } else if (header
            .equals(InstalledDescriptorTableModel.HEADERS[InstalledDescriptorTableModel.DATE_HEADER])) {
          val = 2 * fm.stringWidth(header) + 20;
        } else {
          val = fm.stringWidth(header) + 20;
        }
        tc.setPreferredWidth(val);
        tot += val;
      } else {
        lastTc = tc;
      }
    }
    if (lastTc != null) {
      lastTc.setPreferredWidth(width - tot - 2);
    }
  }

  /**
   * This class is in charge of resizing the query table when the parent panel
   * is resized.
   */
  private class QueryTableComponentAdapter extends ComponentAdapter {
    public void componentResized(ComponentEvent e) {
      Component parent;

      int width;
      parent = (Component) e.getSource();
      width = parent.getBounds().width;
      initColumnSize(width);
    }
  }

  public ListSelectionModel getSelectionModel() {
    return super.getSelectionModel();
  }

  public void removeEntry(int row) {
    ((InstalledDescriptorTableModel) this.getModel()).removeEntry(row);
  }

  public void insertDescriptor(IdxDescriptor desc, int index) {
    ((InstalledDescriptorTableModel) this.getModel()).insertDescriptor(desc,
        index);
  }

  public int[] getRowsSelected() {
    return getSelectedRows();
  }

  public void setSelectedRow(int row) {
    this.getSelectionModel().setSelectionInterval(row, row);
  }

  public void recompteColumnSize() {
    initColumnSize(this.getParent().getBounds().width);
  }

  public int nbEntries() {
    return ((InstalledDescriptorTableModel) this.getModel()).getRowCount();
  }

  public ComponentAdapter getComponentAdapter() {
    return _compoAdapter;
  }

  public void repaintView() {
    this.repaint();
  }

  public void updateView() {
    this.updateUI();
  }

  public void addDescriptor(IdxDescriptor desc) {
    ((InstalledDescriptorTableModel) this.getModel()).addDescriptor(desc);
  }

}