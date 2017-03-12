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
package bzh.plealog.dbmirror.task;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import org.apache.commons.io.IOUtils;

/**
 * A task capable of transforming invalid SW fasta header to valid ones.
 * 
 * @author Patrick G. Durand
 */
public class PTaskConvSPForBlast extends PAbstractTask {

  private String _src;
  private String _errMsg;

  public PTaskConvSPForBlast(String srcFile) {
    _src = srcFile;
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getName() {
    return "convspb";
  }

  public String getUserFriendlyName() {
    return "converting Fasta header";
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getErrorMsg() {
    return _errMsg;
  }

  /**
   * Implementation of KLTask interface. <br>
   * <br>
   * This task will transform the FASTA definition line (the one starting with
   * &gt;) of Uniprot/TrEmbl FASTA files as follows. Assuming such def lines
   * start with &gt;tr|B (where B is an accession number), this procedure will
   * transform the def line into &gt;sp|B. In that way, the resulting FASTA file
   * will conform to the NCBI&apos;formatdb def line recommandations. It is then
   * possible to pass that file to the NCBI&apos;formatdb program using the
   * option &apos;-o T&apos; to process correctly the def lines. Then, a BLAST
   * result obtained by scanning this formatted FASTA file will present
   * correctly the Uniprot/TrEmbl identifiers and accession numbers.
   */
  public boolean execute() {
    BufferedReader reader = null;
    BufferedWriter writer = null;
    String line;
    File srcFile, tmpFile;

    if (_src == null) {
      _errMsg = "source file is unknown";
      return false;
    }

    tmpFile = new File(_src + ".tmp");
    srcFile = new File(_src);
    try {
      reader = new BufferedReader(new InputStreamReader(new FileInputStream(
          srcFile), "UTF-8"));
      writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
          tmpFile)));
      while ((line = reader.readLine()) != null) {
        if (line.charAt(0) == '>') {
          if (line.startsWith(">tr|", 0) == true) {// trembl->sw
            writer.write(">sp|" + line.substring(4));
          } else {
            writer.write(line);
          }
        } else {
          writer.write(line);
        }
        writer.write("\n");
      }
      reader.close();
      writer.flush();
      writer.close();
      srcFile.delete();
      tmpFile.renameTo(srcFile);
    } catch (Exception e) {
      _errMsg = "unable to convert defline of " + _src;
      return false;
    } finally {
      IOUtils.closeQuietly(reader);
      IOUtils.closeQuietly(writer);
    }
    return true;
  }

  public void setParameters(String params) {
  }

  /*
   * public static void main(String[] args) { KLTaskConvSPForBlast task = new
   * KLTaskConvSPForBlast(args[0]); long tim = System.currentTimeMillis(); if
   * (!task.execute()){ System.err.println("error: "+task.getErrorMsg()); }
   * else{ System.out.println("ok"); }
   * System.out.println("Time: "+(System.currentTimeMillis()-tim)+" ms."); }
   */
}
