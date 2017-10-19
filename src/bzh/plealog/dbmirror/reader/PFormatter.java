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
package bzh.plealog.dbmirror.reader;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
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

  public static Hashtable<String, String> AVAILABLE_FORMATS;

  // use a singleton
  private static VelocityEngine           _velocityEngine = null;

  // output formats available
  public static final String              TXT_FORMAT      = "txt";
  public static final String              HTML_FORMAT     = "html";
  public static final String              INSD_FORMAT     = "insd";
  public static final String              F_INSD_FORMAT   = "finsd";
  public static final String              FASTA_FORMAT    = "fas";

  static {
    AVAILABLE_FORMATS = new Hashtable<String, String>();
    AVAILABLE_FORMATS.put(TXT_FORMAT, "simpleAsciiText.vm");
    AVAILABLE_FORMATS.put(HTML_FORMAT, "simpleHtmlText.vm");
    AVAILABLE_FORMATS.put(INSD_FORMAT, "simpleINSDseq.vm");
    AVAILABLE_FORMATS.put(F_INSD_FORMAT, "fullINSDseq.vm");
    AVAILABLE_FORMATS.put(FASTA_FORMAT, "simpleFasta.vm");
  }

  public PFormatter() {
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
   * @param text
   *          the string to format
   * @param format
   *          the format code
   */
  public void dump(Writer outWriter, String text, String dbName, String id,
      String format) {
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
   * @param outWriter
   *          the writer
   * @param seq
   *          the sequence to format
   * @param format
   *          the format code
   */
  public void dump(Writer outWriter, PSequence seq, String format) {
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
      context.put("xFomatter", new PFormatter());
      writer = new StringWriter();
      t.merge(context, writer);
      outWriter.write(writer.toString());
    } catch (Exception e) {
      LOGGER.warn("unable to print out sequence: " + e);
    }
  }

  private void dumpPE(Writer outWriter, String template) {
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

  public void startEpilogue(Writer w, String format) {
    if (w == null)
      return;
    dumpPE(w, "E" + AVAILABLE_FORMATS.get(format));
  }

  public void startPrologue(Writer w, String format) {
    if (w == null)
      return;
    dumpPE(w, "P" + AVAILABLE_FORMATS.get(format));
  }

  public void dumpError(Writer w, String format, String msg) {
    if (w == null)
      return;
    try {
      if (HTML_FORMAT.equals(format)) {
        w.write("<B>" + msg + "</B>\n");
      } else if (INSD_FORMAT.equals(format)) {
        w.write("<Error>" + msg + "</Error>\n");
      } else {
        w.write(msg + "\n");
      }
    } catch (IOException e) {
    }
  }
}
