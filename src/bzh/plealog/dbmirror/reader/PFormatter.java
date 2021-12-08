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
package bzh.plealog.dbmirror.reader;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Hashtable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

import bzh.plealog.bioinfo.api.data.feature.FeatureTable;
import bzh.plealog.dbmirror.util.conf.Configuration;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;

/**
 * This class contains methods do prepare sequence data using various format.
 * 
 * @author Patrick G. Durand
 */
public class PFormatter {
  private static final Log                LOGGER          = LogFactory
                                                              .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                                  + ".PFormatter");

  public static Hashtable<FORMAT, String> AVAILABLE_FORMATS;

  // use a singleton
  private static VelocityEngine           _velocityEngine = null;

  // output formats available
  public enum FORMAT {
    TXT_FORMAT("txt"), 
    HTML_FORMAT("html"), 
    INSD_FORMAT("insd"), 
    F_INSD_FORMAT("finsd"), 
    FASTA_FORMAT("fas");
    
    private String type;
    
    FORMAT(String type){
      this.type = type;
    }
    
    public String getType() {
      return type;
    }
    public String toString() {
      return type;
    }
    public static FORMAT findByType(final String type){
      return Arrays.stream(values()).filter(value -> value.getType().equals(type)).findFirst().orElse(null);
  }
  }
  
  static {
    AVAILABLE_FORMATS = new Hashtable<FORMAT, String>();
    AVAILABLE_FORMATS.put(FORMAT.TXT_FORMAT, "simpleAsciiText.vm");
    AVAILABLE_FORMATS.put(FORMAT.HTML_FORMAT, "simpleHtmlText.vm");
    AVAILABLE_FORMATS.put(FORMAT.INSD_FORMAT, "simpleINSDseq.vm");
    AVAILABLE_FORMATS.put(FORMAT.F_INSD_FORMAT, "fullINSDseq.vm");
    AVAILABLE_FORMATS.put(FORMAT.FASTA_FORMAT, "simpleFasta.vm");
  }

  private Writer outWriter = null;
  private Writer errWriter = null;
  private FORMAT format = FORMAT.TXT_FORMAT;
  
  /**
   * Creates a data formatter. Please note that both outWriter and errWriter
   * are null by default.
   */
  public PFormatter(FORMAT format) {
    this.format = format;
  }

  /**
   * Creates a data formatter.
   */
  public PFormatter(FORMAT format, Writer outWriter, Writer errWriter) {
    this(format);
    this.outWriter = outWriter;
    this.errWriter = errWriter;
  }
  
  public void setOutWriter(Writer outWriter) {
    this.outWriter = outWriter;
  }

  public Writer getOutWriter() {
    return outWriter;
  }

  public void setErrWriter(Writer errWriter) {
    this.errWriter = errWriter;
  }
  
  public Writer getErrWriter() {
    return errWriter;
  }

  public void setFormat(FORMAT format) {
    this.format = format;
  }

  public FORMAT getFormat() {
    return format;
  }

  public void closeOutWriter() {
    if (outWriter == null)
      return;
    try {
      outWriter.flush();
    } catch (IOException ex) {
    }
    try {
      outWriter.close();
    } catch (IOException ex) {
    }
  }
  public void closeErrWriter() {
    if (errWriter == null)
      return;
    try {
      errWriter.flush();
    } catch (IOException ex) {
    }
    try {
      errWriter.close();
    } catch (IOException ex) {
    }
  }

  public void closeWriters() {
    //Flush all
    if (outWriter!=null) {
      try {
        outWriter.flush();
      } catch (IOException ex) {
      }
    }
    if (errWriter!=null) {
      try {
        errWriter.flush();
      } catch (IOException ex) {
      }
    }
    // Then close all
    if (outWriter!=null) {
      try {
        outWriter.close();
      } catch (IOException ex) {
      }
    }
    if (errWriter!=null) {
      try {
        errWriter.close();
      } catch (IOException ex) {
      }
    }
  }
  /**
   * Cleanup string for display or for XML transmission. Replace all known chars
   * to cause XML problems with their XML safe equivalents or replace the XML
   * safe codes with their ASCII equivalents when displaying to users depending
   * on the value of decode.
   * 
   * Example. The first line will be converted to the second line if decode is
   * true &lt;TheJonz&gt; said &quot;It&apos;s over here&quot;
   * 
   * @param str
   *          string to be cleaned up
   * @param decode
   *          true if you are converting FROM XML, false if converting TO XML
   * @return the result of the conversion
   */
  public String cleanup(String str, boolean decode) {
    String saXMLEquivalent[] = { "&amp;", "&apos;", "&quot;", "&lt;", "&gt;" };
    // String saSpecialChars[] = {"&", "\'", "\"", "<", ">"};
    String saSpecialChars[] = { "and", "|", "|", "<", ">" };
    String sFind;
    String sReplace;
    boolean bFound;
    int iPos = -1;
    int i;

    if (!decode)
      i = 1;
    else
      i = 0;
    while (i < saXMLEquivalent.length) {
      String newStr = "";
      if (decode) {
        // Search for XML encodeded string and convert it back to plain ASCII
        sFind = saXMLEquivalent[i];
        sReplace = saSpecialChars[i];
      } else {
        // Search for special chars in ASCII and replace with XML safe chars
        sFind = saSpecialChars[i];
        sReplace = saXMLEquivalent[i];
      }
      do {
        iPos = str.indexOf(sFind, ++iPos);
        if (iPos > -1) {
          newStr = newStr + str.substring(0, iPos) + sReplace
              + str.substring(iPos + sFind.length(), str.length());
          str = newStr;
          newStr = "";
          bFound = true;
        } else {
          bFound = false;
        }
      } while (bFound);
      i++;
    }
    return (str);
  }

  private synchronized VelocityEngine prepareEngine() throws Exception {
    VelocityEngine ve;
    if (_velocityEngine != null)
      return _velocityEngine;
    ve = new VelocityEngine();
    /*
     * Another way to configure Logger so that Velocity uses an existing one :
     * http
     * ://velocity.apache.org/engine/devel/developer-guide.html#Configuring_Logging
     * ve.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS,
     * "org.apache.velocity.runtime.log.Log4JLogChute");
     * ve.setProperty(Log4JLogChute.RUNTIME_LOG_LOG4J_LOGGER, "KLFormatter");
     */
    // use a specific log file for Velocity
    // ve.setProperty(RuntimeConstants.RUNTIME_LOG,
    // KDMSAbstractConfig.getLogAppPath()+LOGGER_FILE);
    // disbale Velocity loggin (not useful)
    // from:
    ve.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS,
        "org.apache.velocity.runtime.log.NullLogSystem");
    ve.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH,
        DBMSAbstractConfig.getConfPath(Configuration.SYSTEM));
    ve.init();
    _velocityEngine = ve;
    return ve;
  }

  /**
   * Dump some text data using a particular Velocity template. This method dumps
   * formatted String to standard output.
   * 
   * @param text the string to format
   * @param dbName name of the databank
   * @param id sequence ID
   *        
   */
  public void dump(String text, String dbName, String id) {
    VelocityEngine ve;
    VelocityContext context;
    Template t;
    StringWriter writer;

    if (outWriter == null)
      return;
    try {
      ve = prepareEngine();
      t = ve.getTemplate(AVAILABLE_FORMATS.get(format));
      context = new VelocityContext();
      context.put("data", text);
      context.put("dbname", dbName);
      context.put("seqid", id);
      writer = new StringWriter();
      t.merge(context, writer);
      outWriter.write(writer.toString());
    } catch (Exception e) {
      LOGGER.warn("unable to print out sequence: " + e);
    }
  }

  /**
   * Dump some sequence data using a particular Velocity template. This method
   * dumps formatted data to standard output.
   * 
   * @param seq the sequence to format
   */
  public void dump(PSequence seq) {
    VelocityEngine ve;
    VelocityContext context;
    Template t;
    StringWriter writer;

    if (outWriter == null)
      return;
    try {
      ve = prepareEngine();
      t = ve.getTemplate(AVAILABLE_FORMATS.get(format));
      context = new VelocityContext();
      seq.getFeatTable().sort(FeatureTable.POS_SORTER);
      context.put("seqinfo", seq);
      context.put("xFomatter", new PFormatter(format));
      writer = new StringWriter();
      t.merge(context, writer);
      outWriter.write(writer.toString());
    } catch (Exception e) {
      LOGGER.warn("unable to print out sequence: " + e);
    }
  }

  private void dumpPE(String template) {
    VelocityEngine ve;
    VelocityContext context;
    Template t;
    StringWriter writer;

    try {
      ve = prepareEngine();
      t = ve.getTemplate(template);
      context = new VelocityContext();
      writer = new StringWriter();
      t.merge(context, writer);
      outWriter.write(writer.toString());
      outWriter.write("\n");
    } catch (Exception e) {
      LOGGER.warn("unable to print out sequence: " + e);
    }
  }

  public void startEpilogue() {
    if (outWriter == null)
      return;
    dumpPE("E" + AVAILABLE_FORMATS.get(format));
  }

  public void startPrologue() {
    if (outWriter == null)
      return;
    dumpPE("P" + AVAILABLE_FORMATS.get(format));
  }

  public void dumpError(String msg) {
    Writer w = errWriter;
    if (w == null)
      w = outWriter;
    if (w == null)
      return;
    try {
      if (FORMAT.HTML_FORMAT.equals(format)) {
        w.write("<B>" + msg + "</B>\n");
      } else if (FORMAT.INSD_FORMAT.equals(format)) {
        w.write("<Error>" + msg + "</Error>\n");
      } else {
        w.write(msg + "\n");
      }
    } catch (IOException e) {
    }
  }
}
