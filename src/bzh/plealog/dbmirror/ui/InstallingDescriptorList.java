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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import bzh.plealog.dbmirror.ui.resources.DBMSMessages;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

import com.plealog.genericapp.api.EZEnvironment;

/**
 * This class is used to display the list of databank being installed.
 * 
 * @author Patrick G. Durand
 */
public class InstallingDescriptorList extends JPanel {
  private static final long serialVersionUID = -9085792848235995908L;
  private JLabel            _processingLbl;
  private Timer             _processingTimer;
  private JButton           _stopBtn;
  private ImageIcon         _animIcon        = EZEnvironment
                                                 .getImageIcon("circle_all.gif");
  private ImageIcon         _notAnimIcon     = EZEnvironment
                                                 .getImageIcon("circle_back.gif"); ;

  // unit: second
  private static final int  PERIOD           = 1;

  public InstallingDescriptorList() {
    createUI();
  }

  private void createUI() {
    JPanel pnl;

    _processingLbl = new JLabel(_notAnimIcon);
    pnl = new JPanel(new BorderLayout());
    pnl.add(_processingLbl, BorderLayout.WEST);
    _stopBtn = new JButton(
        DBMSMessages.getString("InstallingDescriptorList.msg3"),
        EZEnvironment.getImageIcon("data_stop.png"));
    _stopBtn.setToolTipText(DBMSMessages
        .getString("InstallingDescriptorList.msg2"));
    _stopBtn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent arg0) {
        SwingUtilities.invokeLater(new Runnable() {// avoid to lock UI during
                                                   // actio processing
              public void run() {
                _stopBtn.setEnabled(false);
                LoggerCentral.abortProcess();
              }
            });
      }
    });
    _stopBtn.setEnabled(false);
    pnl.add(_stopBtn, BorderLayout.EAST);
    this.setLayout(new BorderLayout());
    this.add(pnl, BorderLayout.NORTH);
    // this.setMinimumSize(new Dimension(100, 100));
    // this.setPreferredSize(new Dimension(100, 100));
  }

  public void clearTimeLalbel() {
    _processingLbl.setText("");
  }

  public void setProcessingTime(Date time) {
    _processingLbl.setText(DBMSMessages
        .getString("InstallingDescriptorList.msg1")
        + " "
        + RunnerSchedulerDlg.getScheduleFormatter().format(time));
  }

  public void startProcessing() {
    _processingLbl.setIcon(_animIcon);
    _processingTimer = new Timer();
    _processingTimer.schedule(new ProcessingTask(), 0l, PERIOD * 1000l);
    _stopBtn.setEnabled(true);

  }

  public void stopProcessing() {
    _processingLbl.setIcon(_notAnimIcon);
    _processingTimer.cancel();
    _stopBtn.setEnabled(false);

  }

  private class ProcessingTask extends TimerTask {
    private int curSeconds;
    private int curMinutes;
    private int curHours;

    public void run() {
      curSeconds += PERIOD;
      if (curSeconds == 60) {
        curSeconds = 0;
        curMinutes++;
      }
      if (curMinutes == 60) {
        curMinutes = 0;
        curHours++;
      }
      _processingLbl.setText(String.format("%02d:%02d:%02d", curHours,
          curMinutes, curSeconds));
      // for an unknown reason, using a SimpleDateFormatter results with the
      // following problem:
      // HH starts at 01 !!! In addition, HH is limited in the range 0-23 or
      // 1-24
    }
  }

}
