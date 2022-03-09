/* Copyright (C) 2022 Patrick G. Durand
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

import java.io.Serializable;

import javax.swing.SwingUtilities;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;

// adapted from https://blog.pikodat.com/2015/10/11/frontend-logging-with-javafx/
@Plugin(
    name = "TextAreaAppender",
    category = "Core",
    elementType = "appender",
    printObject = true)
public final class DBMSLogViewerAppender extends AbstractAppender {

  private static DBMSLogViewer _logViewer;


  @SuppressWarnings("deprecation")
  protected DBMSLogViewerAppender(String name, Filter filter,
                             Layout<? extends Serializable> layout,
                             final boolean ignoreExceptions) {
    super(name, filter, layout, ignoreExceptions);
  }

  /**
   * This method is where the appender does the work.
   *
   * @param event Log event with log data
   */
  @Override
  public void append(LogEvent event) {
    final String line = new String(getLayout().toByteArray(event));
    // Append formatted message to textarea using the Swing Thread.
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        _logViewer.appendText(line);
      }
    });
  }

  /**
   * Factory method. Log4j will parse the configuration and call this factory 
   * method to construct the appender with
   * the configured attributes.
   *
   * @param name   Name of appender
   * @param layout Log layout of appender
   * @param filter Filter for appender
   * @return The TextAreaAppender
   */
  @PluginFactory
  public static DBMSLogViewerAppender createAppender(
      @PluginAttribute("name") String name,
      @PluginElement("Layout") Layout<? extends Serializable> layout,
      @PluginElement("Filter") final Filter filter) {
    if (name == null) {
      LOGGER.error("No name provided for TextAreaAppender");
      return null;
    }
    if (layout == null) {
      layout = PatternLayout.createDefaultLayout();
    }
    return new DBMSLogViewerAppender(name, filter, layout, true);
  }


  /**
   * Set TextArea to append
   *
   * @param textArea TextArea to append
   */
  public static void setLogViewer(DBMSLogViewer logViewer) {
    DBMSLogViewerAppender._logViewer = logViewer;
  }
}