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
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import bzh.plealog.dbmirror.ui.resources.DBMSMessages;
import bzh.plealog.dbmirror.util.Utils;

/**
 * This class is used to display the information about the files to download.
 * 
 * @author Patrick G. Durand
 */
@SuppressWarnings("serial")
public class FileIntoTablePanel extends JPanel {
  private MyJTable              _resultTable;

  private static final Color    TOTAL_CELL_BK_COLOR = new Color(184, 207, 229);

  private static final String[] COL_NAMES           = {
      DBMSMessages.getString("FileIntoTablePanel.msg1"),
      DBMSMessages.getString("FileIntoTablePanel.msg2"),
      DBMSMessages.getString("FileIntoTablePanel.msg3"),
      DBMSMessages.getString("FileIntoTablePanel.msg4") };

  /**
   * Constructor.
   */
  public FileIntoTablePanel(List<FileInfo> fileInfo) {
    ArrayList<FileInfo> fis;
    int nTotFiles = 0;
    long nTotBytes1 = 0;
    long nTotBytes2 = 0;
    JScrollPane scroll;
    JPanel pnl;

    fis = new ArrayList<FileInfo>();
    for (FileInfo fi : fileInfo) {
      fis.add(fi);
      nTotFiles += fi.getnFiles();
      nTotBytes1 += fi.getBytes();
      nTotBytes2 += fi.getDbSize();
    }

    fis.add(new FileInfo(DBMSMessages.getString("FileIntoTablePanel.msg5"),
        nTotFiles, nTotBytes1, nTotBytes2));

    _resultTable = new MyJTable(new MyTableModel(fis));
    _resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
    _resultTable.getTableHeader().setReorderingAllowed(false);
    _resultTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    _resultTable.setColumnSelectionAllowed(false);
    _resultTable.setRowSelectionAllowed(true);
    _resultTable.setGridColor(Color.LIGHT_GRAY);

    scroll = new JScrollPane(_resultTable);

    this.setLayout(new BorderLayout());
    this.add(scroll, BorderLayout.CENTER);

    pnl = new JPanel(new BorderLayout());
    pnl.add(new JLabel(DBMSMessages.getString("FileIntoTablePanel.msg6")),
        BorderLayout.WEST);
    pnl.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    this.add(pnl, BorderLayout.SOUTH);
    this.setPreferredSize(new Dimension(450, 180));
  }

  /**
   * The table. We redefine the cell renderer to align data in the table.
   */
  private class MyJTable extends JTable {
    public MyJTable(TableModel model) {
      super(model);
    }

    public TableCellRenderer getCellRenderer(int row, int column) {
      TableCellRenderer tcr;
      boolean totRow;

      tcr = super.getCellRenderer(row, column);

      if (tcr instanceof JLabel) {
        JLabel lbl;
        lbl = (JLabel) tcr;
        totRow = (row == this.getModel().getRowCount() - 1);
        if (column < 1) {
          if (totRow) {
            lbl.setHorizontalAlignment(SwingConstants.RIGHT);
          } else {
            lbl.setHorizontalAlignment(SwingConstants.LEFT);
          }
        } else {
          lbl.setHorizontalAlignment(SwingConstants.CENTER);
        }
        if (totRow) {
          lbl.setBackground(TOTAL_CELL_BK_COLOR);
        } else {
          lbl.setBackground(Color.WHITE);
        }
      }
      return tcr;
    }
  }

  /**
   * The table data model.
   */
  private class MyTableModel extends AbstractTableModel {
    private List<FileInfo> _fileInfo;

    public MyTableModel(List<FileInfo> fileInfo) {
      _fileInfo = fileInfo;
    }

    public String getColumnName(int column) {
      return COL_NAMES[column];
    }

    public int getColumnCount() {
      return COL_NAMES.length;
    }

    public int getRowCount() {
      return _fileInfo.size();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
      FileInfo fi;
      Object val;
      fi = _fileInfo.get(rowIndex);

      switch (columnIndex) {
        case 0:
          val = fi.getDescName();
          break;
        case 1:
          val = fi.getnFiles();
          break;
        case 2:
          val = Utils.getBytes(fi.getBytes());
          break;
        case 3:
          val = Utils.getBytes(fi.getDbSize());
          break;
        default:
          val = "-";
      }
      return val;
    }

  }
}
