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

import java.io.File;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.table.AbstractTableModel;

import bzh.plealog.dbmirror.ui.resources.DBMSMessages;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptor;
import bzh.plealog.dbmirror.util.descriptor.IdxDescriptor;
import bzh.plealog.dbmirror.util.runner.DBStampProperties;

import com.plealog.genericapp.api.EZEnvironment;

/**
 * This is the TableModel that must be used with InstalledDescriptorTable.
 * 
 * @author Patrick G. Durand
 */
public class InstalledDescriptorTableModel extends AbstractTableModel {
  private static final long        serialVersionUID   = -2236124292797424948L;
  private ArrayList<IdxDescriptor> _descriptors;
  private boolean                  _enableUserPermissions;

  protected static final int       ICON_HEADER        = 0;
  protected static final int       NAME_HEADER        = 1;
  protected static final int       DESCRIPTION_HEADER = 2;
  protected static final int       NBSEQS_HEADER      = 3;
  protected static final int       DBSIZE_HEADER      = 4;
  protected static final int       DATE_HEADER        = 5;
  protected static final int       PERM_HEADER        = 6;

  protected static final int[]     HEADERS_INT        = { ICON_HEADER,
      NAME_HEADER, DESCRIPTION_HEADER, NBSEQS_HEADER, DBSIZE_HEADER,
      DATE_HEADER, PERM_HEADER                       };
  protected static final String[]  HEADERS            = { " ", "  Name   ",
      "  Description   ", "  Size (entries)  ", "  Size on disk  ",
      "  Update date   ", "  Permissions   "         };

  private NumberFormat             numFormatter       = DecimalFormat
                                                          .getInstance();
  private static final String      MSG3               = DBMSMessages
                                                          .getString("InstalledDescriptorList.msg3");

  protected static ImageIcon       _protImg           = EZEnvironment
                                                          .getImageIcon("prot_db.png");
  protected static ImageIcon       _nucImg            = EZEnvironment
                                                          .getImageIcon("nuc_db.png");
  protected static ImageIcon       _dicoImg           = EZEnvironment
                                                          .getImageIcon("dico_db.png");
  protected static ImageIcon       _protBlastImg      = EZEnvironment
                                                          .getImageIcon("prot_blast.png");
  protected static ImageIcon       _nucBlastImg       = EZEnvironment
                                                          .getImageIcon("nuc_blast.png");

  public InstalledDescriptorTableModel(boolean enableUserPermissions) {
    _descriptors = new ArrayList<IdxDescriptor>();
    _enableUserPermissions = enableUserPermissions;
  }

  public void insertDescriptor(IdxDescriptor desc, int index) {
    _descriptors.add(index, desc);
    this.fireTableDataChanged();
  }

  public void addDescriptor(IdxDescriptor desc) {
    _descriptors.add(desc);
    this.fireTableDataChanged();
  }

  public String getColumnName(int column) {
    return HEADERS[column];
  }

  public int getColumnCount() {
    return _enableUserPermissions ? HEADERS.length : HEADERS.length - 1;
  }

  public int getRowCount() {
    return _descriptors.size();
  }

  public void removeEntry(int idx) {
    _descriptors.remove(idx);
    this.fireTableDataChanged();
  }

  private String getSizeOnDisk(DBDescriptor.TYPE type, Properties props,
      IdxDescriptor entry) {
    StringBuffer buf;
    String value;

    buf = new StringBuffer();
    value = props.getProperty(DBStampProperties.DB_SIZE);
    if (value != null) {
      buf.append(Utils.getBytes(Long.valueOf(value)));
    }
    return buf.toString();
  }

  private String getNbSequences(DBDescriptor.TYPE type, Properties props,
      IdxDescriptor entry) {
    StringBuffer buf;
    String value;

    buf = new StringBuffer();
    if (type.equals(DBDescriptor.TYPE.blastn)
        || type.equals(DBDescriptor.TYPE.blastp)) {
      value = props.getProperty(DBStampProperties.NB_SEQUENCES);
    } else {
      value = props.getProperty(DBStampProperties.NB_ENTRIES);
    }
    if (value != null && !value.equals("-1")) {
      buf.append(numFormatter.format(Long.valueOf(value)));
    }
    return buf.toString();
  }

  private String getUpdateDate(Properties props) {
    StringBuffer buf;
    String value;

    buf = new StringBuffer();
    value = props.getProperty(DBStampProperties.TIME_STAMP);
    if (value != null) {
      buf.append(value);
    }
    if (buf.length() == 0)
      return MSG3;
    else
      return buf.toString();
  }

  public Object getValueAt(int row, int col) {
    IdxDescriptor desc;
    Object val = null;
    String path;
    Properties props;

    desc = (IdxDescriptor) _descriptors.get(row);
    if (col < 0)
      return desc;
    path = desc.getCode();
    // todo: optimize this code since we access the file at rendering time
    props = DBMSAbstractConfig.readDBStamp(new File(path).getParent());
    switch (HEADERS_INT[col]) {
      case ICON_HEADER:
        val = "-";
        break;
      case NAME_HEADER:
        val = desc.getName();
        break;
      case DESCRIPTION_HEADER:
        val = desc.getDescription();
        break;
      case NBSEQS_HEADER:
        val = getNbSequences(desc.getType(), props, desc);
        break;
      case DBSIZE_HEADER:
        val = getSizeOnDisk(desc.getType(), props, desc);
        break;
      case DATE_HEADER:
        val = getUpdateDate(props);
        break;
      case PERM_HEADER:
        val = desc.getAuthorizedUsers();
        break;
    }
    if (val == null) {
      val = "-";
    }
    return (val);
  }

}
