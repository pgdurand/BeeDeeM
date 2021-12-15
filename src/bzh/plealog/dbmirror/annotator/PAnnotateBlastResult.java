/* Copyright (C) 2006-2019 Patrick G. Durand
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
package bzh.plealog.dbmirror.annotator;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import bzh.plealog.bioinfo.api.data.searchresult.SROutput;
import bzh.plealog.bioinfo.io.searchresult.ncbi.BlastLoader;
import bzh.plealog.bioinfo.io.searchresult.srnative.NativeBlastWriter;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;

/**
 * This class is used to introduce sequence information within Blast results.
 * 
 * @author Patrick G. Durand
 */
public class PAnnotateBlastResult {
  public static final String    annot_type                = "type";
  public static final String    annot_type_bio_class_only = "bco";
  public static final String    annot_type_full           = "full";
  public static final String    annot_type_none           = "none";

  public static final String    input_file                = "i";
  public static final String    output_file               = "o";

  public static final String    writer_type               = "writer";
  public static final String    writer_type_xml           = "xml";
  
  public static final String    include_bco               = "incbc";
  
  protected static final Logger                    LOGGER = LogManager.getLogger(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                                  + ".PAnnotateBlastResult");
  public boolean annotate(String input, String output, String writer,
      String type) {
    return annotate(input, output, writer, type, false);
  }
  
  public boolean annotate(String input, String output, String writer,
      String type, boolean include_BC) {
    SROutputAnnotator annotator = null;
    SROutput bo;
    BlastLoader loader;
    NativeBlastWriter kbWriter;
    boolean bRet = true;

    try {
      LOGGER.debug("--> annotate");
      LOGGER.debug("input : " + input);
      LOGGER.debug("output: " + output);
      LOGGER.debug("writer: " + writer);
      LOGGER.debug("type  : " + type);
      LOGGER.debug("incbc : " + include_BC);
      
      // get an NCBI blast data loader
      LOGGER.debug("loading blast result file");
      loader = new BlastLoader();
      bo = loader.load(new File(input));
      if (bo == null)
        return true;// simply nothing to do with no data!

      LOGGER.debug("annotating data");
      // start the Job
      annotator = new SROutputAnnotator(include_BC);
      if (annot_type_full.equals(type))
        annotator.doFullAnnotation(bo);
      else
        annotator.doClassificationAnnotation(bo);

      LOGGER.debug("writing results");
      // write the result
      if (writer_type_xml.equals(writer)) {
        loader.write(new File(output), bo);
      } else {
        kbWriter = new NativeBlastWriter();
        kbWriter.write(new File(output), bo);
      }
    } catch (Exception e) {
      LOGGER.warn("unable to annotate data file: " + input + ": " + e);
      bRet = false;
    } finally {
      try {
        if (annotator != null) {
          annotator.close();
        }
      } catch (Exception e) {
        LOGGER.warn("unable to close annotator : " + e.getMessage());
      }
    }
    LOGGER.debug("<-- annotate");
    return bRet;
  }

}
