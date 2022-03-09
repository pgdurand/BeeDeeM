/* Copyright (C) 2007-2022 Patrick G. Durand
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
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;

import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * This class defines a graphical log viewer.
 * 
 * @author Patrick G. Durand
 */
public class DBMSLogViewer extends JPanel {
  private static final long   serialVersionUID = -5262581552205137348L;
  private static final int    FONT_SIZE        = 12;
  private static final String FONT_FAM         = "Monospaced";
  private static final Font   MAINFONT         = new Font(FONT_FAM, Font.PLAIN,
                                                   FONT_SIZE);
  private static final String ERR_STYLE        = "red";
  private static final String OK_STYLE         = "blue";
  private static final String OK_LINE          = "| INFO ";
  private static final String WARN_STYLE       = "orange";
  private static final String WARN_LINE        = "| WARN ";

  private JTextPane           _text;
  private JScrollPane         _jsp;

  /**
   * Default constructor.
   */
  public DBMSLogViewer() {

    _text = new JTextPane() {
      /**
       * 
       */
      private static final long serialVersionUID = 6671706236225587670L;

      public boolean getScrollableTracksViewportWidth() {
        return false; // force display of horizontal scroll bar
      }
    };
    _text.setFont(MAINFONT);

    _text.setEditable(false);
    _text.setEnabled(true);
    _text.setBackground(new Color(255, 255, 255));
    initStyles();

    _jsp = new JScrollPane(_text);
    _jsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    _jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

    this.setLayout(new BorderLayout());
    this.add(_jsp, BorderLayout.CENTER);

    JPanel btnPanel = new JPanel(new BorderLayout());
    JButton btn = new JButton(new CopyClipBoardAction("Copy log to clipboard"));
    btnPanel.add(btn, BorderLayout.EAST);
    this.add(btnPanel, BorderLayout.SOUTH);

    this.setPreferredSize(new Dimension(100, 100));
  }

  @SuppressWarnings("deprecation")
  public Appender getAppender() {
    LoggerContext context = LoggerContext.getContext(false);
    Configuration config = context.getConfiguration();
    PatternLayout pl = PatternLayout.createLayout(LoggerCentral.SIMPLE_PATTERN_LAYOUT, 
        null, config, null, null, 
        false, false, null, null);
    Appender appender = DBMSLogViewerAppender.createAppender("TextAreaAppender", pl, null);
    DBMSLogViewerAppender.setLogViewer(this);
    return appender;
  }

  /**
   * Initialize some text styles: red colored-text for messages written on
   * System.err, and black colored-text for messages written on System.out.
   */
  private void initStyles() {
    StyleContext context = StyleContext.getDefaultStyleContext();
    Style defaultStyle = context.getStyle(StyleContext.DEFAULT_STYLE);
    Style newStyle;

    newStyle = _text.addStyle(OK_STYLE, defaultStyle);
    StyleConstants.setFontFamily(newStyle, FONT_FAM);
    StyleConstants.setForeground(newStyle, new Color(0, 0, 255));
    StyleConstants.setFontSize(newStyle, FONT_SIZE);

    newStyle = _text.addStyle(ERR_STYLE, defaultStyle);
    StyleConstants.setFontFamily(newStyle, FONT_FAM);
    StyleConstants.setForeground(newStyle, new Color(255, 0, 0));
    StyleConstants.setFontSize(newStyle, FONT_SIZE);

    newStyle = _text.addStyle(WARN_STYLE, defaultStyle);
    StyleConstants.setFontFamily(newStyle, FONT_FAM);
    StyleConstants.setForeground(newStyle, new Color(255, 140, 0));
    StyleConstants.setFontSize(newStyle, FONT_SIZE);
  }

  private String compactLogLine(String logLine, String symbol) {
    StringBuffer buf;
    int idx1, idx2;

    buf = new StringBuffer();
    idx1 = logLine.indexOf('|');
    idx2 = logLine.indexOf('|', idx1 + 1);
    buf.append(logLine.substring(0, idx1));
    buf.append(symbol);
    buf.append(logLine.substring(idx2 + 1));
    return buf.toString();
  }

  public void appendText(String line) {
    Document doc;
    doc = _text.getDocument();
    try {
      if (line.indexOf(OK_LINE) != -1) {
        doc.insertString(doc.getLength(), compactLogLine(line, "|"),
            _text.getStyle(OK_STYLE));
      } else if (line.indexOf(WARN_LINE) != -1) {
        doc.insertString(doc.getLength(), compactLogLine(line, "/!\\"),
            _text.getStyle(WARN_STYLE));
      } else {
        doc.insertString(doc.getLength(), compactLogLine(line, "-X-"),
            _text.getStyle(ERR_STYLE));
      }
    } catch (BadLocationException e) {

    }
  }

  public void clearContent() {
    _text.setText("");
    System.gc();
  }

  /*
  // tip from : http://textareaappender.zcage.com/
  private class MyAppender extends WriterAppender {
    public MyAppender() {
      this.setLayout(new PatternLayout("%d{HH:mm:ss} | %-5p| %m%n"));
    }

    public void append(LoggingEvent loggingEvent) {
      final String message = this.getLayout().format(loggingEvent);

      // Append formatted message to textarea using the Swing Thread.
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          appendText(message);
        }
      });
    }
  }
 LOG4J2*/
  private class CopyClipBoardAction extends AbstractAction implements
      ClipboardOwner {
    /**
     * 
     */
    private static final long serialVersionUID = -6942379511165594084L;

    /**
     * Action constructor.
     * 
     * @param name
     *          the name of the action.
     */
    public CopyClipBoardAction(String name) {
      super(name);
    }

    public void actionPerformed(ActionEvent event) {
      StringSelection stringSelection = new StringSelection(_text.getText());
      Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      clipboard.setContents(stringSelection, this);
    }

    @Override
    public void lostOwnership(Clipboard arg0, Transferable arg1) {
    }
  }
  
}
