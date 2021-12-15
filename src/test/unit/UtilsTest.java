/* Copyright (C) 2007-2017 Ludovic Antin
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
package test.unit;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.junit.Test;

import bzh.plealog.dbmirror.indexer.LuceneUtils;
import bzh.plealog.dbmirror.lucenedico.DicoTermQuerySystem;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.conf.DBMirrorConfig;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptorUtils;
import bzh.plealog.dbmirror.util.descriptor.IdxDescriptor;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

public class UtilsTest {

  public static long                     start     = 0;
  public static HashMap<String, Long>    starts    = new HashMap<String, Long>();
  private static HashMap<String, long[]> durations = new HashMap<String, long[]>();

  public static boolean cleanInstalledDatabanks(String... except) {
    LuceneUtils.closeStorages();
    LoggerCentral.reset();

    boolean result = true;
    String excepts = ArrayUtils.toString(except);
    if ((StringUtils.isNotBlank(excepts)) && (!excepts.contains("d"))) {
      DicoTermQuerySystem.closeDicoTermQuerySystem();
      try {
        FileUtils.deleteDirectory(new File(DBMSAbstractConfig
            .getLocalMirrorPath(), "d"));
      } catch (Exception e) {
        result = false;
      }
    }
    if ((StringUtils.isNotBlank(excepts)) && (!excepts.contains("p"))) {
      try {
        FileUtils.deleteDirectory(new File(DBMSAbstractConfig
            .getLocalMirrorPath(), "p"));
      } catch (Exception e) {
        result = false;
      }
    }
    if ((StringUtils.isNotBlank(excepts)) && (!excepts.contains("n"))) {
      try {
        FileUtils.deleteDirectory(new File(DBMSAbstractConfig
            .getLocalMirrorPath(), "n"));
      } catch (Exception e) {
        result = false;
      }
    }
    try {
      FileUtils.deleteDirectory(new File(DBMSAbstractConfig
          .getLocalMirrorPath(), "tmp"));
    } catch (Exception e) {
      result = false;
    }
    if (StringUtils.isBlank(excepts)) {
      File mirrorFile = new File(DBMSAbstractConfig.getLocalMirrorPath(),
          "dbmirror.config");
      if (!FileUtils.deleteQuietly(mirrorFile)) {
        try (PrintWriter writer = new PrintWriter(mirrorFile)) {
          writer.print("");
          writer.close();
        } catch (Exception e) {
          result = false;
        }
      }
    }

    // reload config

    ArrayList<IdxDescriptor> data = new ArrayList<IdxDescriptor>();
    ArrayList<String> deletedCodes = new ArrayList<>();
    List<IdxDescriptor> all = DBDescriptorUtils
        .prepareIndexDBList(DBDescriptorUtils.getLocalDBMirrorConfig());
    for (IdxDescriptor idx : all) {
      if (new File(idx.getCode()).exists()) {
        data.add(idx);
      } else {
        deletedCodes.add(idx.getKbCode());
      }
    }
    // save the config with the deleted one
    DBMirrorConfig newConfig = DBDescriptorUtils.getMirrorConfig(data, null);
    if (deletedCodes.size() > 0) {
      newConfig.removeMirrorCode(deletedCodes);
      DBDescriptorUtils.saveDBMirrorConfig(
          DBMSAbstractConfig.getLocalMirrorConfFile(), newConfig);
    }
    return result;
  }

  public static String getMainTestFilepaths() {
    String result = FilenameUtils.concat(System.getProperty("user.dir"),
        "tests");
    result = FilenameUtils.concat(result, "junit");
    return result;
  }

  /**
   * Each test file needed for a test method must be stored in this repository
   * [current working directory]\tests\junit\
   * 
   * @return the concatenate file path
   */
  public static String getTestFilePath(String... fileNames) {
    String result = UtilsTest.getMainTestFilepaths();
    for (int i = 0; i < fileNames.length; i++) {
      result = FilenameUtils.concat(result, fileNames[i]);
    }
    return result;
  }

  private static String getConfPath() {
    String path;

    path = DBMSAbstractConfig.getInstallAppPath();
    if (path != null)
      path = Utils.terminatePath(path);
    else
      path = Utils.terminatePath(System.getProperty("user.dir"));
    path += ("conf" + File.separator);
    return path;
  }

  private static boolean _logConfigured = false;

  public static void configureApp() {
    if (!_logConfigured) {
      ConfigurationBuilder<BuiltConfiguration> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
      builder.setStatusLevel(Level.INFO);
      builder.setConfigurationName("BeeDeeMTest-Suite");
      builder.add(builder.newFilter("ThresholdFilter", Filter.Result.ACCEPT, Filter.Result.NEUTRAL)
          .addAttribute("level", Level.INFO));
      AppenderComponentBuilder appenderBuilder = builder.newAppender("Stdout", "CONSOLE").addAttribute("target",
          ConsoleAppender.Target.SYSTEM_OUT);
      appenderBuilder.add(builder.newLayout("PatternLayout")
          .addAttribute("pattern", "%d{dd-MM-yyyy HH:mm:ss} [%t] %-5p %c %x | %m%n"));
      appenderBuilder.add(builder.newFilter("MarkerFilter", Filter.Result.DENY, Filter.Result.NEUTRAL)
          .addAttribute("marker", "FLOW"));
      builder.add(appenderBuilder);
      
      builder.add(builder.newLogger("org.apache.logging.log4j", Level.INFO)
          .add(builder.newAppenderRef("Stdout")).addAttribute("additivity", false));
      builder.add(builder.newRootLogger(Level.ERROR).add(builder.newAppenderRef("Stdout")));
      Configurator.initialize(builder.build());
      _logConfigured = true;
    }

    String confPath = getConfPath();
    DBMSAbstractConfig.setConfPath(confPath);
    DBMSAbstractConfig.forceInitializeConfigurator(confPath
        + "dbms_test.config");
    DBMSAbstractConfig.testMode = true;
    LoggerCentral.reset();
  }

  /**
   * The default repository to store the test files for a test class is
   * repository [current working directory]\tests\junit\[class name]
   * 
   * @return the concatenate file path
   */
  private String getDefaultTestFilepaths() {
    return FilenameUtils.concat(UtilsTest.getMainTestFilepaths(), "Utils");
  }

  public static void start() {
    start = System.nanoTime();
  }

  public static void start(String key) {
    starts.put(key, System.nanoTime());
  }

  public static void stop(String key) {
    long stop = System.nanoTime();
    long duration = stop;
    if (starts.containsKey(key)) {
      duration -= starts.get(key);
    } else {
      duration -= start;
    }

    if (!durations.containsKey(key)) {
      durations.put(key, new long[2]);
    }
    durations.get(key)[0] += duration;
    durations.get(key)[1]++;
  }

  public static void displayDurations(String title) {
    File logFile = new File(FilenameUtils.concat(
        System.getProperty("user.dir"), "tests"), "logs.txt");
    List<String> results = new ArrayList<String>();
    long bigTotal = 0;
    for (String key : durations.keySet()) {
      long total = durations.get(key)[0];
      long nbTests = durations.get(key)[1];
      results.add(key + " => "
          + NumberFormat.getNumberInstance().format(total / nbTests) + " ns. ("
          + NumberFormat.getNumberInstance().format(total) + "/" + nbTests
          + ")");
      bigTotal += total;
    }
    java.util.Collections.sort(results);
    if (StringUtils.isNotEmpty(title)) {
      results.add(0, title);
    }
    results.add("Done in " + NumberFormat.getNumberInstance().format(bigTotal)
        + " ns.\n\n");
    for (String result : results) {
      System.out.println(result);
    }

    try {
      FileUtils.writeLines(logFile, results, true);
    } catch (IOException e) {
      e.printStackTrace();
    }

    durations.clear();
  }

  @Test
  public void test() {
    File dirTest = new File(getDefaultTestFilepaths());
    List<String> volumes = Utils.getFileVolumes(dirTest.getAbsolutePath(),
        "volume");
    assertEquals(3, volumes.size());
  }

}
