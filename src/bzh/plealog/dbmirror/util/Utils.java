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
package bzh.plealog.dbmirror.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.indexer.LuceneStorageSystem;
import bzh.plealog.dbmirror.task.PTaskEngineAbortException;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * This class contains some utility methods.
 * 
 * @author Patrick G. Durand
 */
public class Utils {

  private static final DecimalFormat _bytesFormatter = new DecimalFormat(
                                                         "####.00");

  private static final Log           LOGGER          = LogFactory
                                                         .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                             + ".Utils");

  public static final long           TERA            = (long) 1024 * 1024 * 1024 * 1024;
  public static final long           GIGA            = (long) 1024 * 1024 * 1024;
  public static final long           MEGA            = (long) 1024 * 1024;
  public static final long           KILO            = (long) 1024;

  private static SimpleDateFormat getDateFormatter() {
    return new SimpleDateFormat("yyyyMMdd");
  }

  /**
   * Encodes a date object as a yyyymmdd string.
   */
  public static String encodeDate(Date date) {
    return getDateFormatter().format(date);
  }

  /**
   * Checks whether str conforms to date format yyyyMMdd.
   */
  public static boolean isValidDate(String str) {
    try {
      getDateFormatter().parse(str);
    } catch (ParseException e) {
      return false;
    }
    return true;
  }

  /**
   * Check whether or not a path terminates with a separator. If not, this
   * method returns a path ending with such separator.
   */
  public static String terminatePath(String path) {
    // even on Windows, the slash works... Java automatically understand what to
    // do!
    // this has be done to take into account the Windows and Unix-like platform
    // without testing the platform myself.
    char ch = path.charAt(path.length() - 1);
    if (ch == File.separatorChar)
      return path;
    return (path + File.separator);
  }

  /**
   * Utility method used to convert an OS-independant path to an OS-dependant
   * path and vice-versa. An OS-independant path uses the pipe character to
   * represent the path separator.
   * 
   * @param str
   *          the path to convert
   * @param direct
   *          when true, this method looks for the pipe char in str and replace
   *          every occurrence by the OS-dependant path separator character.
   *          When false, the method does the reverse operation: it looks for
   *          the OS-dependant path separator character and replace every
   *          occurrence by the pipe char.
   */
  public static String transformCode(String str, boolean direct) {
    StringBuffer buf;
    int i, size;
    char from, to, ch;
    if (direct) {
      from = '|';
      to = File.separatorChar;
    } else {
      from = File.separatorChar;
      to = '|';
    }
    if (str.indexOf(from) < 0)
      return str;
    buf = new StringBuffer();
    size = str.length();
    for (i = 0; i < size; i++) {
      ch = str.charAt(i);
      if (ch == from)
        buf.append(to);
      else
        buf.append(ch);
    }
    return buf.toString();
  }

  /**
   * Tokenizes a string.
   */
  public static String[] tokenize(String input) {
    return tokenize(input, ",\t\n\r\f");
  }

  /**
   * Tokenizes a string.
   */
  public static String[] tokenize(String input, String delim) {
    StringTokenizer tokenizer;
    String str[];
    int i = 0;

    if (input == null)
      return new String[] {};

    tokenizer = new StringTokenizer(input, delim);

    if (tokenizer.countTokens() == 0)
      return new String[] {};

    str = new String[tokenizer.countTokens()];
    while (tokenizer.hasMoreTokens()) {
      str[i] = (String) tokenizer.nextToken().trim();
      i++;
    }

    return str;
  }

  public static String replaceAll(String str, String sFind, String sReplace) {
    boolean bFound;
    int iPos = -1;

    String newStr = "";
    do {
      iPos = str.indexOf(sFind, ++iPos);
      if (iPos > -1) {
        newStr = newStr + str.substring(0, iPos) + sReplace
            + str.substring(iPos + sFind.length(), str.length());
        str = newStr;
        newStr = "";
        iPos += (sReplace.length() - 1);
        bFound = true;
      } else {
        bFound = false;
      }
    } while (bFound);
    return (str);
  }

  /**
   * Dumps within a file the current date using the format yyyyMMdd.
   */
  public static void dumpDateStamp(String file) {

    try (PrintWriter writer = new PrintWriter(new FileWriter(new File(file)))) {
      writer.print(getDateFormatter().format(new Date()));
      writer.flush();
      writer.close();
    } catch (Exception e) {
      LoggerCentral.error(LOGGER, "Unable to create DateStamp.");
    }
  }

  /**
   * 
   * Checks how many special chars we can have at the end of a line. When the
   * line terminates with \n or \r, the method returns 1 and when the line
   * terminates with \r\n, the method returns 2.
   */
  public static int getLineTerminatorSize(String file) {

    char pch, ch;
    int size = 1, i;
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(
        new FileInputStream(file), "UTF-8"))) {
      pch = ' ';
      while (true) {
        i = reader.read();
        if (i == -1)
          break;
        ch = (char) i;
        if (ch == '\n') {
          if (pch == '\r') {
            size = 2;
          }
          break;
        }
        pch = ch;
      }
      reader.close();
    } catch (Exception e) {
    }

    return size;
  }

  /**
   * Convert a number of bytes into a string suffixed with giga or mega or kilo.
   */
  public static String getBytes(long bytes) {
    StringBuffer buf = new StringBuffer();
    setBytes(buf, bytes);
    return buf.toString();
  }

  /**
   * Convert a number of bytes into a string suffixed with giga or mega or kilo.
   * Resulting string is added to a string buffer
   */
  public static void setBytes(StringBuffer buf, long bytes) {
    if (bytes > TERA) {
      buf.append(_bytesFormatter.format((double) bytes / (double) TERA));
      buf.append(" Tb");
    } else if (bytes > GIGA) {
      buf.append(_bytesFormatter.format((double) bytes / (double) GIGA));
      buf.append(" Gb");
    } else if (bytes > MEGA) {
      buf.append(_bytesFormatter.format((double) bytes / (double) MEGA));
      buf.append(" Mb");
    } else if (bytes > KILO) {
      buf.append(_bytesFormatter.format((double) bytes / (double) KILO));
      buf.append(" Kb");
    } else {
      buf.append(bytes);
      buf.append(" bytes");
    }
  }

  public static Enumeration<?> enumerator(final Iterator<?> iter) {
    return new Enumeration<Object>() {
      public boolean hasMoreElements() {
        return iter.hasNext();
      }

      public Object nextElement() {
        return iter.next();
      }
    };
  }

  public static void copyBinFile(File source, File dest) throws IOException {
    FileOutputStream fos = null;
    FileInputStream fis = null;
    BufferedInputStream bis = null;
    int n;
    byte[] buf = new byte[8192];

    try {
      fos = new FileOutputStream(dest);
      fis = new FileInputStream(source);
      bis = new BufferedInputStream(fis);
      while ((n = bis.read(buf)) != -1) {
        fos.write(buf, 0, n);
        if (LoggerCentral.processAborted()) {
          break;
        }
      }
    } finally {
      IOUtils.closeQuietly(fos);
      IOUtils.closeQuietly(fis);
      IOUtils.closeQuietly(bis);
    }

    if (LoggerCentral.processAborted()) {
      throw new PTaskEngineAbortException();
    }
  }

  private static final DecimalFormat VOL_NUMBER = new DecimalFormat("00");

  /**
   * 
   * Creates a set of data volumes given a set of files.
   * 
   * @param source
   *          list of absolute file paths to integrate within volumes
   * @param targetPath
   *          the absolute path targeting the place where the volumes are
   *          created
   * @param volumePrefixFileName
   *          prefix name of the volume files
   * @param volumeSize
   *          the max size of each volume. Unit is bytes.
   * @param deleteSourceFile
   *          if true then each source file is deleted after integration.
   * 
   * @return the list of volume paths or null in case of error. Error message is
   *         logged.
   */
  public static List<String> createFileVolumes(List<String> source,
      String targetPath, String volumePrefixFileName, long volumeSize,
      boolean deleteSourceFile) {
    FileOutputStream fos = null;
    FileInputStream fis = null;
    BufferedInputStream bis = null;
    ArrayList<String> volumes;
    int n, curVolume, readBytes, flushBytes;
    byte[] buf = new byte[8192];
    long curSize, fSize;
    String volPath, volName;
    File curFile, volFile;

    curVolume = 0;
    curSize = 0l;
    fos = null;
    fis = null;
    volumes = new ArrayList<String>();
    volPath = Utils.terminatePath(targetPath) + volumePrefixFileName;
    flushBytes = (int) (50l * Utils.MEGA);

    try {
      for (String s : source) {
        curFile = new File(s);
        fSize = curFile.length();
        // create volumes ?
        // test with 0l is there to process the creation of
        // the first volume
        if (curSize == 0l || (curSize + fSize) > volumeSize) {
          if (fos != null) {
            fos.flush();
            fos.close();
          }
          volName = volPath + VOL_NUMBER.format(curVolume);
          volumes.add(volName);
          curVolume++;
          // set to 1, not to zero (see above comment)
          curSize = 1l;
          volFile = new File(volName);
          if (volFile.exists()) {
            if (!volFile.delete()) {
              throw new Exception("unable to delete volume file: " + volName);
            }
          }
          fos = new FileOutputStream(volName, true);
        }
        // transfer data
        fis = new FileInputStream(curFile);
        bis = new BufferedInputStream(fis);
        readBytes = 0;
        while ((n = bis.read(buf)) != -1) {
          fos.write(buf, 0, n);
          readBytes += n;
          if (readBytes > flushBytes) {
            fos.flush();
          }
          if (LoggerCentral.processAborted()) {
            IOUtils.closeQuietly(fis);
            IOUtils.closeQuietly(bis);
            IOUtils.closeQuietly(fos);
            throw new PTaskEngineAbortException();
          }
        }
        fis.close();
        fis = null;
        if (deleteSourceFile) {
          curFile.delete();
        }
        curSize += fSize;
      }
    } catch (Exception e) {
      volumes = null;
    } finally {
      IOUtils.closeQuietly(fis);
      IOUtils.closeQuietly(bis);
      IOUtils.closeQuietly(fos);
    }
    return volumes;
  }

  /**
   * 
   * Gets the existed data volumes files.
   * 
   * @param targetPath
   *          the absolute path targeting the place where the volumes are
   *          created
   * @param volumePrefixFileName
   *          prefix name of the volume files
   * 
   * @return the list of volume paths.
   */
  public static List<String> getFileVolumes(String targetPath,
      final String volumePrefixFileName) {
    List<String> volumes = new ArrayList<String>();

    File[] files = new File(targetPath).listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return (name.matches(volumePrefixFileName + "\\d+$"));
      }
    });

    if (files != null) {
      for (int i = 0; i < files.length; i++) {
        volumes.add(files[i].getAbsolutePath());
      }
    }

    return volumes;
  }

  /**
   * 
   * Gets the cumulative size of BLAST volumes.
   * 
   * @param targetPath
   *          the absolute path targeting the place where the volumes are
   *          created
   * 
   * @return the cumulative size of BLAST volumes.
   */
  public static long getBlastVolumesSize(String targetPath) {
    long size = 0;

    String[] extensions = { "phd", "phi", "phr", "pin", "pnd", "pni", "pog",
        "ppd", "psd", "psi", "psq",
        "pal", // Protein
        "nhd", "nhi", "nhr", "nin", "nnd", "nni", "nog", "nnd", "nsd", "nsi",
        "nsq", "nal" // Nucleic
    };

    for (File file : FileUtils.listFiles(new File(targetPath), extensions,
        false)) {
      size += file.length();
    }

    return size;
  }

  /**
   * Get the original file paths regarding the 'd' files
   * 
   * @param targetPath
   * @return a list of files or an empty list if no 'd' files where found
   * 
   * @see LuceneStorageSystem.getIdxKeyName to learn more about 'd' files
   */
  public static List<File> getOriginalFiles(File directory) {
    List<File> result = new ArrayList<File>();
    HashSet<String> paths = new HashSet<String>();
    File[] files = directory.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return (name.matches(LuceneStorageSystem.FP_KEY_FILE_PREFIX + "\\d+$"));
      }
    });

    if (files != null) {
      for (int i = 0; i < files.length; i++) {
        File file = getFilePathFromDFile(files[i]);
        if (file != null) {
          if (paths.add(file.getAbsolutePath())) {
            result.add(file);
          }
        }
      }
    }

    return result;
  }

  /**
   * 
   * @param key
   * 
   * @return
   */
  private static File getFilePathFromDFile(File dfilePath) {
    File result = null;
    try {
      String srcFile = FileUtils.readFileToString(dfilePath);

      if (srcFile.startsWith(LuceneStorageSystem.FP_KEY)) {
        // starting with ngPlast 4.2+, we won't use anymore absolute path to
        // retrieve
        // path to data storage (see DBMirrorConfig.getMirrorPath()). So, here
        // we build
        // the full path from the index location of only the file name part
        // stored in
        // the "dX" file name.
        // backward compatibility when data file was written as "FP:aaa" where
        // aaa was an absolute file path
        result = new File(
            srcFile.substring(LuceneStorageSystem.FP_KEY.length()));
      } else {
        result = new File(dfilePath.getParentFile(), srcFile);
      }
    } catch (Exception e) {
      return null;
    }

    return result;
  }

  /**
   * Checks out whether a string can be used as a valid file name. Valid file
   * names are made of digits, letters, the underscore character or the minus
   * character in any order.
   */
  public static boolean isValidFileName(String fName) {
    int i, size;
    char ch;

    size = fName.length();
    if (fName.charAt(0) == '-')
      return false;
    for (i = 0; i < size; i++) {
      ch = fName.charAt(i);
      if (!(Character.isDigit(ch) || Character.isLetter(ch) || ch == '_' || ch == '-')) {
        return false;
      }
    }
    return true;
  }

  /**
   * Checks out whether a string representing a path contains space characters.
   */
  public static boolean isPathNameContainsSpaceChar(String path) {
    int i, size;
    char ch;

    size = path.length();
    for (i = 0; i < size; i++) {
      ch = path.charAt(i);
      if (ch == ' ')
        return true;
    }
    return false;
  }

  /**
   * Returns the file name (without extension) given a absolute path. If path
   * denotes a directory, then the last element of the absolute path is
   * returned. It path denotes a file, then the last element of the absolute
   * path is returned but without its extension (if any). The file extension is
   * recognized by looking for the last occurrence of a dot. Note that the
   * method may return null if no file name can be found.
   */
  public static String getFileName(File path) {
    String fName;
    int idx;

    fName = path.getName();
    if (fName.length() == 0)
      return null;
    idx = fName.lastIndexOf('.');
    if (idx < 0)
      return fName;
    return fName.substring(0, idx);
  }

  /**
   * Uncompress a gzipped file. A file is considered gzipped if its extension
   * matches '.gz'. If it is not true, then the zipFile param is returned as is.
   * 
   * Return the uncompressed file path or null if failure.
   */
  public static String gunzipFile(String zipFile) {

    String zipname, source;
    byte[] buffer;
    int length, bufSize = 8192;

    if (zipFile.endsWith(".gz")) {
      zipname = zipFile;
      source = zipFile.substring(0, zipFile.length() - 3);
    } else {
      return zipFile;
    }

    try (GZIPInputStream zipin = new GZIPInputStream(new FileInputStream(
        zipname)); FileOutputStream out = new FileOutputStream(source)) {
      buffer = new byte[bufSize];

      while ((length = zipin.read(buffer, 0, bufSize)) != -1) {
        out.write(buffer, 0, length);
        if (LoggerCentral.processAborted()) {
          throw new PTaskEngineAbortException();
        }
      }
      out.flush();
    } catch (IOException e) {
      LOGGER.warn("Couldn't open " + zipname + ".");
      return null;
    }

    return source;
  }

  public static boolean unzipFile(String zipFile, String outputFolder) {
    byte[] buffer = new byte[8192];
    boolean bRet = true;
    ZipEntry ze;
    String oFolder, fileName;
    ZipInputStream zis = null;
    File folder, newFile;
    FileOutputStream fos = null;

    oFolder = terminatePath(outputFolder);

    try {
      // create output directory is not exists
      folder = new File(outputFolder);
      if (!folder.exists()) {
        folder.mkdir();
      }
      // get the zip file content
      zis = new ZipInputStream(new FileInputStream(zipFile));
      // get the zipped file list entry
      ze = zis.getNextEntry();
      while (ze != null) {
        fileName = ze.getName();
        // create all non existing folder
        if (fileName.endsWith("/") || fileName.endsWith("\\")) {
          new File(oFolder + fileName).mkdirs();
        } else {
          // extract a file
          newFile = new File(oFolder + fileName);
          fos = new FileOutputStream(newFile);
          int len;
          while ((len = zis.read(buffer)) > 0) {
            fos.write(buffer, 0, len);
            if (LoggerCentral.processAborted()) {
              throw new PTaskEngineAbortException();
            }
          }
          fos.flush();
          fos.close();
          fos = null;
        }
        ze = zis.getNextEntry();
      }
    } catch (IOException ex) {
      LOGGER.warn(ex);
      bRet = false;
    } finally {
      // ensure to close streams
      if (zis != null) {
        try {
          zis.closeEntry();
        } catch (Exception ex) {
        }
        try {
          zis.close();
        } catch (Exception ex) {
        }
      }
      if (fos != null) {
        try {
          fos.close();
        } catch (Exception ex) {
        }
      }
    }
    return bRet;
  }

  /**
   * Returns a map of arguments.
   * 
   * @param argLine
   *          a semicolon separated of arguments. Only one argument is allowed.
   *          Each argument is either a single string or a key/value pair. In
   *          that latter case key and values are separated by the character =.
   * 
   * @return a map or arguments
   */
  public static Map<String, String> getTaskArguments(String argLine) {
    Hashtable<String, String> args;
    StringTokenizer tokenizer;
    String token, key, value;
    int idx;

    args = new Hashtable<String, String>();
    tokenizer = new StringTokenizer(argLine, ";");
    while (tokenizer.hasMoreTokens()) {
      token = tokenizer.nextToken().trim();
      idx = token.indexOf('=');
      if (idx != -1) {
        key = token.substring(0, idx).trim();
        value = token.substring(idx + 1).trim();
      } else {
        key = value = token;
      }
      args.put(key, value);
    }
    return args;
  }

  public static <T> T[] copyOf(T[] original, int newLength) {
    if (null == original) {
      throw new NullPointerException();
    }
    if (0 <= newLength) {
      return copyOfRange(original, 0, newLength);
    }
    throw new NegativeArraySizeException();
  }

  @SuppressWarnings("unchecked")
  public static <T> T[] copyOfRange(T[] original, int start, int end) {
    if (original.length >= start && 0 <= start) {
      if (start <= end) {
        int length = end - start;
        int copyLength = Math.min(length, original.length - start);
        T[] copy = (T[]) Array.newInstance(original.getClass()
            .getComponentType(), length);
        System.arraycopy(original, start, copy, 0, copyLength);
        return copy;
      }
      throw new IllegalArgumentException();
    }
    throw new ArrayIndexOutOfBoundsException();
  }

  public static void replaceInFile(File source, File dest, String from,
      String to) throws IOException {
    FileWriter fw = null;
    FileReader fr = null;
    BufferedReader br = null;
    BufferedWriter bw = null;
    char charBuff[];
    String str;
    int fileLength;

    try {
      fr = new FileReader(source);
      fw = new FileWriter(dest);
      br = new BufferedReader(fr);
      bw = new BufferedWriter(fw);

      /* Determine the size of the buffer to allocate */
      fileLength = (int) source.length();
      // test for empty files
      if (fileLength != 0) {
        fileLength = Math.min(2048, fileLength);
        charBuff = new char[fileLength];

        while (br.read(charBuff, 0, fileLength) != -1) {
          str = new String(charBuff);
          str = Utils.replaceAll(str, from, to);
          charBuff = str.toCharArray();
          bw.write(charBuff, 0, str.length());
        }
        bw.flush();
      }
    } catch (IOException e) {
      throw e;
    } finally {
      IOUtils.closeQuietly(bw);
      IOUtils.closeQuietly(br);
      IOUtils.closeQuietly(fw);
      IOUtils.closeQuietly(fr);
    }
  }

  /**
   * Reads the first line of the file 'f'
   * 
   * @param f
   *          the file to read
   * @return the first line or an empty String
   */
  public static String readFirstLine(File f) throws Exception {
    BufferedReader reader = null;
    String result = "";

    if (!f.exists()) {
      return result;
    }

    try {
      reader = new BufferedReader(new InputStreamReader(new FileInputStream(f),
          "UTF-8"));
      result = reader.readLine();
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
        }
      }
    }
    return result;
  }

  public static void writeInFile(String filepath, String line) throws Exception {
    PrintWriter writer = null;
    File f;

    // usually this part is done using the MyParserMonitor from KLTaskEngine
    // However, FormatDB task is not really a parsing task even if it's call
    // convertToFasta(): formatdb task convert several files at a time, and
    // ParserMonitor only handles one file at a time.
    f = new File(filepath);

    try {
      writer = new PrintWriter(new FileWriter(f));
      writer.write(line);
      writer.flush();
    } finally {
      IOUtils.closeQuietly(writer);
    }
  }
  
  /**
   * Return the oldest file of a directory. This method does not
   * dig into sub-directories if any.
   */
  public static Date getOldestFile(File directory){
    Date now = Calendar.getInstance().getTime();
    Date oldest = now;
    Iterator<File> files = FileUtils.iterateFiles(directory, null, false);

    while(files.hasNext()){
      File f = files.next();
      if (FileUtils.isFileOlder(f, oldest)){
        oldest = new Date(f.lastModified());
      }
    }
    return oldest;
  }
  /**
   * Return the oldest file of a directory. This method does not
   * dig into sub-directories if any.
   */
  public static Date getOldestFile(String directory){
    return getOldestFile(new File(directory));
  }
}
