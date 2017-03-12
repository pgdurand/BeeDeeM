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
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.WindowConstants;

import bzh.plealog.dbmirror.ui.resources.DBMSMessages;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.runner.DBMSExecNativeCommand;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.plealog.genericapp.api.EZEnvironment;
import com.toedter.calendar.JDateChooser;

/**
 * This class is used to select the scheduling date/time.
 * 
 * @author Patrick G. Durand
 */
@SuppressWarnings("serial")
public class RunnerSchedulerDlg extends JDialog {
  private JDateChooser     _dateChooser;
  private JSpinner         _hour;
  private JSpinner         _minute;
  private JRadioButton     _now;
  private JRadioButton     _pickADate;
  private JCheckBox        _closeApp;
  private Date             _scheduleTime;
  private JTextArea        _helpArea;

  private SimpleDateFormat hourFormatter   = new SimpleDateFormat("HH");
  private SimpleDateFormat minuteFormatter = new SimpleDateFormat("mm");

  private SimpleDateFormat dateFormatter   = new SimpleDateFormat("yyyy-MM-dd");

  public static SimpleDateFormat getScheduleFormatter() {
    return new SimpleDateFormat("yyyy-MM-dd, HH:mm");
  }

  public RunnerSchedulerDlg(Frame parent) {
    super(parent, DBMSMessages.getString("RunnerSchedulerDlg.msg1"), true);

    buildGUI();
    this.pack();
    this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    this.addWindowListener(new FDBDialogAdapter());
  }

  /**
   * Shows the dialog box on screen.
   */
  public void showDlg() {
    // center on screen
    Dimension screenSize = this.getToolkit().getScreenSize();
    Dimension dlgSize = this.getSize();

    dlgSize.width += 80;
    this.setSize(dlgSize);

    this.setLocation(screenSize.width / 2 - dlgSize.width / 2,
        screenSize.height / 2 - dlgSize.height / 2);
    // show
    setVisible(true);
  }

  private JSpinner createSpinner(IntRange range) {
    JSpinner spin;
    SpinnerModel model;

    model = new SpinnerNumberModel(((IntRange) range).getRangeDef(),
        ((IntRange) range).getRangeFrom(), ((IntRange) range).getRangeTo(), 1);
    spin = new JSpinner(model);
    return spin;
  }

  private JTextArea createHelper() {
    _helpArea = new JTextArea();
    _helpArea.setRows(4);
    _helpArea.setLineWrap(true);
    _helpArea.setWrapStyleWord(true);
    _helpArea.setEditable(false);
    _helpArea.setOpaque(false);
    _helpArea.setForeground(EZEnvironment.getSystemTextColor());
    // _helpArea.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
    _helpArea.setBorder(BorderFactory.createEmptyBorder());
    return _helpArea;
  }

  private JComponent createHourChooser() {
    JPanel pnl;
    Calendar cal;

    cal = Calendar.getInstance();
    _scheduleTime = cal.getTime();
    pnl = new JPanel();
    _hour = createSpinner(new IntRange(0, 23, Integer.valueOf(hourFormatter
        .format(_scheduleTime))));
    _minute = createSpinner(new IntRange(0, 59, Integer.valueOf(minuteFormatter
        .format(_scheduleTime))));

    pnl.add(_hour);
    pnl.add(new JLabel(":"));
    pnl.add(_minute);

    return pnl;
  }

  private void buildGUI() {
    DefaultFormBuilder builder;
    FormLayout layout;
    JPanel mainPnl, btnPnl, questionPanel, datePnl, hlpPnl, pnl;
    JButton okBtn, cancelBtn;
    MyRadioBtnListener radioActionListener;
    boolean macOS = DBMSExecNativeCommand.getOSType() == DBMSExecNativeCommand.MAC_OS;

    okBtn = new JButton(DBMSMessages.getString("RunnerSchedulerDlg.msg2"));
    cancelBtn = new JButton(DBMSMessages.getString("RunnerSchedulerDlg.msg3"));
    okBtn.addActionListener(new OkAction());
    cancelBtn.addActionListener(new CancelAction());

    btnPnl = new JPanel();
    btnPnl.setLayout(new BoxLayout(btnPnl, BoxLayout.X_AXIS));
    btnPnl.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
    btnPnl.add(Box.createHorizontalGlue());
    btnPnl.add(macOS ? cancelBtn : okBtn);
    btnPnl.add(Box.createRigidArea(new Dimension(10, 0)));
    btnPnl.add(macOS ? okBtn : cancelBtn);
    if (!macOS)
      btnPnl.add(Box.createHorizontalGlue());

    _now = new JRadioButton(DBMSMessages.getString("RunnerSchedulerDlg.msg6"));
    _pickADate = new JRadioButton(
        DBMSMessages.getString("RunnerSchedulerDlg.msg7"));
    radioActionListener = new MyRadioBtnListener();
    _now.addActionListener(radioActionListener);
    _pickADate.addActionListener(radioActionListener);
    ButtonGroup group = new ButtonGroup();
    group.add(_now);
    group.add(_pickADate);

    _dateChooser = new JDateChooser(new Date());
    layout = new FormLayout("left:max(20dlu;p), 4dlu, 70dlu", "");
    builder = new DefaultFormBuilder(layout);
    builder.setDefaultDialogBorder();
    builder.append(new JLabel(DBMSMessages.getString("RunnerSchedulerDlg.msg4")
        + ": "), _dateChooser);
    builder.append(new JLabel(DBMSMessages.getString("RunnerSchedulerDlg.msg5")
        + ": "), createHourChooser());

    questionPanel = new JPanel(new GridLayout(0, 1));
    questionPanel.add(_now);
    questionPanel.add(_pickADate);
    questionPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

    datePnl = new JPanel(new BorderLayout());
    datePnl.add(builder.getContainer(), BorderLayout.CENTER);
    datePnl.setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 20));

    mainPnl = new JPanel(new BorderLayout());
    mainPnl.add(questionPanel, BorderLayout.NORTH);
    mainPnl.add(datePnl, BorderLayout.CENTER);
    // mainPnl.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    mainPnl.setBorder(BorderFactory.createTitledBorder(DBMSMessages
        .getString("RunnerSchedulerDlg.msg10")));

    _closeApp = new JCheckBox(DBMSMessages.getString("RunnerSchedulerDlg.msg8"));
    _closeApp.setSelected(false);
    if (DBMSAbstractConfig.isEnableExitApplication()) {
      mainPnl.add(_closeApp, BorderLayout.SOUTH);
    }
    Dimension dim = mainPnl.getPreferredSize();
    dim.height += 30;
    mainPnl.setPreferredSize(dim);
    hlpPnl = new JPanel(new BorderLayout());
    pnl = new JPanel(new BorderLayout());
    pnl.setBorder(BorderFactory.createEmptyBorder(5, 4, 4, 4));
    _helpArea = createHelper();
    _helpArea.setText(DBMSMessages.getString("RunnerSchedulerDlg.msg9"));
    pnl.add(_helpArea, BorderLayout.CENTER);
    hlpPnl.add(mainPnl, BorderLayout.CENTER);
    hlpPnl.add(pnl, BorderLayout.SOUTH);

    pnl.setBorder(BorderFactory.createTitledBorder(DBMSMessages
        .getString("RunnerSchedulerDlg.msg11")));

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(hlpPnl, BorderLayout.CENTER);
    getContentPane().add(btnPnl, BorderLayout.SOUTH);
    // hlpPnl.setPreferredSize(new Dimension(320, 320));
    _now.setSelected(true);
    enablePickADate(false);
  }

  public Date getScheduleTime() {
    return _scheduleTime;
  }

  public boolean closeAppAfterProcessing() {
    return _closeApp.isSelected();
  }

  /**
   * This inner class manages actions coming from the JButton Ok.
   */
  private class OkAction extends AbstractAction {
    /**
     * Manages JButton action
     */
    public void actionPerformed(ActionEvent e) {
      String day, time;
      int h, m;

      if (_pickADate.isSelected()) {
        day = dateFormatter.format(_dateChooser.getDate());
        h = Integer.valueOf(_hour.getValue().toString());
        m = Integer.valueOf(_minute.getValue().toString());
        time = String.format("%02d:%02d", h, m);
        try {
          _scheduleTime = getScheduleFormatter().parse(day + ", " + time);
        } catch (ParseException e1) {
          _scheduleTime = Calendar.getInstance().getTime();
        }
      } else {
        // now
        _scheduleTime = new Date();
      }
      dispose();
    }
  }

  /**
   * This inner class manages actions coming from the JButton Cancel.
   */
  private class CancelAction extends AbstractAction {
    /**
     * Manages JButton action
     */
    public void actionPerformed(ActionEvent e) {
      _scheduleTime = null;
      dispose();
    }
  }

  private class FDBDialogAdapter extends WindowAdapter {
    /**
     * Manages windowClosing event: hide the dialog.
     */
    public void windowClosing(WindowEvent e) {
      _scheduleTime = null;
      dispose();
    }
  }

  private class IntRange {
    private int _rangeFrom;
    private int _rangeTo;
    private int _rangeDef = -1;

    public IntRange(int from, int to, int def) {
      _rangeFrom = from;
      _rangeTo = to;
      _rangeDef = def;
    }

    /**
     * Returns the default value of this range.
     */
    public int getRangeDef() {
      if (_rangeDef < 0)
        return _rangeFrom;
      return _rangeDef;
    }

    /**
     * Returns the lower limit value of this range.
     */
    public int getRangeFrom() {
      return _rangeFrom;
    }

    /**
     * Returns the upper limit value of this range.
     */
    public int getRangeTo() {
      return _rangeTo;
    }

    /**
     * Returns a string representation of this entry.
     */
    public String toString() {
      return (_rangeFrom + " - " + _rangeTo + " (" + _rangeDef + ")");
    }
  }

  private void enablePickADate(boolean enable) {
    _dateChooser.setEnabled(enable);
    _hour.setEnabled(enable);
    _minute.setEnabled(enable);
  }

  private class MyRadioBtnListener implements ActionListener {

    public void actionPerformed(ActionEvent e) {
      boolean enable;

      enable = _pickADate.isSelected();
      enablePickADate(enable);
    }

  }
}
