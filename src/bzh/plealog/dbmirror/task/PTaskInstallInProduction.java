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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.fetcher.DBServerConfig;
import bzh.plealog.dbmirror.indexer.LuceneUtils;
import bzh.plealog.dbmirror.lucenedico.Dicos;
import bzh.plealog.dbmirror.lucenedico.task.PTaskDicoIndexer;
import bzh.plealog.dbmirror.util.BlastCmd;
import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.ant.PAntTasks;
import bzh.plealog.dbmirror.util.conf.BankJsonDescriptor;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.conf.DBMirrorConfig;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptorUtils;
import bzh.plealog.dbmirror.util.descriptor.IdxDescriptor;
import bzh.plealog.dbmirror.util.log.LoggerCentral;
import bzh.plealog.dbmirror.util.runner.DBStampProperties;
import bzh.plealog.dbmirror.util.runner.FormatDBRunner;

/**
 * A task capable of installing into production a successfully downloaded and
 * processed database file.
 * 
 * @author Patrick G. Durand
 */
public class PTaskInstallInProduction extends PAbstractTask {

  private HashSet<DBServerConfig> _dbs;
  private String                  _errMsg;

  private static final Log        LOGGER = LogFactory
                                             .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                 + ".PTaskEngine");

  private static final String NOT_APPLICABLE = "n/a";
  
  public PTaskInstallInProduction(List<DBServerConfig> dbs) {
    _dbs = new HashSet<DBServerConfig>();
    for (DBServerConfig db : dbs) {
      _dbs.add(db);
    }
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getName() {
    return "InstallProdDir";
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getUserFriendlyName() {
    return "installing databank in production";
  }

  /**
   * Implementation of KLTask interface.
   */
  public String getErrorMsg() {
    return _errMsg;
  }

  private void updateMirror(List<IdxDescriptor> mirrorDescriptors,
      DBServerConfig db, String dbDownloadPath, String dbInstallPath) {
    String tasks, reader = null, idxPath;
    boolean hasIdx = true, hasFormatDB, hasMakeAlias;

    tasks = db.getUnitPostTasks() + "," + db.getGlobalPostTasks() + ","
        + db.getInstallInProdData();

    // Find the reader type
    if (tasks.indexOf(PTask.TASK_U_GB_IDX) != -1) {
      reader = DBMirrorConfig.GB_READER;
    } else if (tasks.indexOf(PTask.TASK_U_GP_IDX) != -1) {
      reader = DBMirrorConfig.GP_READER;
    } else if ((tasks.indexOf(PTask.TASK_U_SW_IDX) != -1)
        || (tasks.indexOf(PTask.TASK_U_NOG_IDX) != -1)) {
      reader = DBMirrorConfig.UP_READER;
    } else if (tasks.indexOf(PTask.TASK_U_EM_IDX) != -1) {
      reader = DBMirrorConfig.EM_READER;
    } else if (tasks.indexOf(PTask.TASK_U_DICO_IDX + "(") != -1) {
      for (Dicos dico : Dicos.values()) {
        if (tasks.indexOf(PTaskDicoIndexer.DICO_TYPE + "=" + dico.readerId) != -1) {
          reader = dico.readerId;
          break;
        }
        if ((StringUtils.isNotEmpty(dico.additionalReaderId))
            && (tasks.indexOf(PTaskDicoIndexer.DICO_TYPE + "="
                + dico.additionalReaderId) != -1)) {
          reader = dico.additionalReaderId;
          break;
        }
      }
    } else if (tasks.indexOf(PTask.TASK_U_FAS_IDX) != -1) {
      if ((tasks.indexOf(PTaskFormatDB.IS_TAXO + "=" + Boolean.TRUE) != -1)
          || (tasks.indexOf(PTaskFormatDB.IS_CDD + "=" + Boolean.TRUE) != -1)) {
        if (db.getTypeCode().equals(DBServerConfig.PROTEIN_TYPE)) {
          reader = DBMirrorConfig.FASTP_READER;
        } else if (db.getTypeCode().equals(DBServerConfig.NUCLEIC_TYPE)) {
          reader = DBMirrorConfig.FASTN_READER;
        } else {
          hasIdx = false;
        }
      } else {
        hasIdx = false;
      }
    } else {
      hasIdx = false;
    }

    // add the mirror to the central config
    dbInstallPath = dbInstallPath + File.separator + db.getName();
    dbDownloadPath = dbDownloadPath + File.separator + db.getName();

    // check for a Lucene index
    if (hasIdx) {
      idxPath = dbDownloadPath + LuceneUtils.IDX_OK_FEXT;
      if (!new File(idxPath).exists()) {
        throw new RuntimeException("index file not found: " + idxPath);
      }
      DBDescriptorUtils.addNewIndex(mirrorDescriptors, db.getName(),
          db.getDescription(),
          Utils.transformCode(dbInstallPath + LuceneUtils.IDX_OK_FEXT, false),
          reader, dbDownloadPath);
    }

    hasFormatDB = tasks.indexOf(PTask.TASK_G_FORMATDB) != -1;
    hasMakeAlias = tasks.indexOf(PTask.TASK_G_MAKEALIAS) != -1;
    // do we have a formatted blast mirror in addition to the data index ?
    if (hasFormatDB || hasMakeAlias) {
      if (db.isNucleic()) {
        if (hasFormatDB)
          dbInstallPath += FormatDBRunner.BLAST_ALIAS_TAG;
        dbInstallPath += FormatDBRunner.NUCLEIC_ALIAS_EXT;
        if (hasFormatDB)
          dbDownloadPath += FormatDBRunner.BLAST_ALIAS_TAG;
        dbDownloadPath += FormatDBRunner.NUCLEIC_ALIAS_EXT;

        reader = DBMirrorConfig.BLASTN_READER;
      } else if (db.isProteic()) {
        if (hasFormatDB)
          dbInstallPath += FormatDBRunner.BLAST_ALIAS_TAG;
        dbInstallPath += FormatDBRunner.PROTEIN_ALIAS_EXT;
        if (hasFormatDB)
          dbDownloadPath += FormatDBRunner.BLAST_ALIAS_TAG;
        dbDownloadPath += FormatDBRunner.PROTEIN_ALIAS_EXT;
        reader = DBMirrorConfig.BLASTP_READER;
      } else {
        dbInstallPath = null;
        dbDownloadPath = null;
        reader = null;
      }
      if (reader != null && dbDownloadPath != null
          && new File(dbDownloadPath).exists()) {
        DBDescriptorUtils.addNewIndex(mirrorDescriptors, db.getName(),
            db.getDescription(), Utils.transformCode(dbInstallPath, false),
            reader, dbDownloadPath);

      } else {
        throw new RuntimeException("unable to locate Blast databank for "
            + db.getName());
      }
    }
  }
  
  /**
   * Requested by users: provide correct path to indexes when possible.
   * */
  private String makeCorrectIndexPath(String dbPathInstalled, String indexDirName, String bankName, String bankType) {
    StringBuffer buf = new StringBuffer(Utils.terminatePath(dbPathInstalled)+indexDirName);
    String dirName = indexDirName.toLowerCase();
    
    if (dirName.startsWith("diamond")) {
      buf.append(File.separator);
      buf.append(bankName);
      buf.append(".dmnd");
    }
    else if (dirName.startsWith("blast")) {
      buf.append(File.separator);
      buf.append(bankName);
      buf.append(FormatDBRunner.BLAST_ALIAS_TAG);
      buf.append(".");
      buf.append(bankType);
      buf.append("al");
    }
    
    return buf.toString();
  }
  
  private void scanForOtherIndexes(DBServerConfig db, Map<String, String> index, 
      String dbPathDownload, String dbPathInstalled){
    List<File> files;
    //within installation directory, look for all sub-dir terminating with .idx
    try {
      files = Files.list(Paths.get(dbPathDownload))
          .filter(Files::isDirectory)
          .filter(path -> path.toString().endsWith(BankJsonDescriptor.OTHER_INDEX_FEXT))
          .map(Path::toFile)
          .collect(Collectors.toList());
    } catch (IOException e) {
      LOGGER.warn("Unable to list additional indexes: "+e.toString());
      return;
    }
    // then, process additional indexes if any (bowtie, diamond, etc)
    for(File idxDirectory : files) {
      //Do we have a dedicated an index.properties file?
      File propFile = new File(Utils.terminatePath(idxDirectory.getAbsolutePath())
          +BankJsonDescriptor.OTHER_INDEX_PROPS);
      if (propFile.exists()){
        Properties props = new Properties();
        try (FileReader fr = new FileReader(propFile)){
          props.load(fr);
          index.put(
              props.getProperty(BankJsonDescriptor.OTHER_INDEX_PROP_KEY),
              makeCorrectIndexPath(dbPathInstalled, idxDirectory.getName(), db.getName(), db.getTypeCode()));
        } catch (Exception e) {
          LOGGER.warn("Unable to read property file: "+propFile+": "+e.toString());
        }
      }
      //otherwise, use directory name has index key
      else {
        String fName = idxDirectory.getName();
        int idx = fName.lastIndexOf('.');
        index.put(
            fName.substring(0, idx),
            Utils.terminatePath(dbPathInstalled)+fName);
      }
      
    }
  }
  
  private String getLuceneIndexPath(DBServerConfig db, String dbDownloadPath, String dbInstallPath) {
    String luceneIndex=NOT_APPLICABLE;
    
    dbInstallPath = dbInstallPath + File.separator + db.getName() + LuceneUtils.IDX_OK_FEXT;
    dbDownloadPath = dbDownloadPath + File.separator + db.getName() + LuceneUtils.IDX_OK_FEXT;
    
    if (new File(dbDownloadPath).exists()) {
      luceneIndex = dbInstallPath;
    }
    return luceneIndex;
  }
  
  private String getBlastIndexPath(DBServerConfig db, String dbDownloadPath, String dbInstallPath) {
    boolean hasFormatDB, hasMakeAlias;
    String tasks, blastIndex=NOT_APPLICABLE;
    
    tasks = db.getUnitPostTasks() + "," + db.getGlobalPostTasks() + ","
        + db.getInstallInProdData();
    hasFormatDB = tasks.indexOf(PTask.TASK_G_FORMATDB) != -1;
    hasMakeAlias = tasks.indexOf(PTask.TASK_G_MAKEALIAS) != -1;
    
    dbInstallPath = dbInstallPath + File.separator + db.getName();
    dbDownloadPath = dbDownloadPath + File.separator + db.getName();
    
    // do we have a formatted blast mirror in addition to the data index ?
    if (hasFormatDB || hasMakeAlias) {
      if (db.isNucleic()) {
        if (hasFormatDB)
          dbInstallPath += FormatDBRunner.BLAST_ALIAS_TAG;
        dbInstallPath += FormatDBRunner.NUCLEIC_ALIAS_EXT;
        if (hasFormatDB)
          dbDownloadPath += FormatDBRunner.BLAST_ALIAS_TAG;
        dbDownloadPath += FormatDBRunner.NUCLEIC_ALIAS_EXT;
      } else if (db.isProteic()) {
        if (hasFormatDB)
          dbInstallPath += FormatDBRunner.BLAST_ALIAS_TAG;
        dbInstallPath += FormatDBRunner.PROTEIN_ALIAS_EXT;
        if (hasFormatDB)
          dbDownloadPath += FormatDBRunner.BLAST_ALIAS_TAG;
        dbDownloadPath += FormatDBRunner.PROTEIN_ALIAS_EXT;
      } else {
        dbInstallPath = null;
        dbDownloadPath = null;
      }
    }
    if (new File(dbDownloadPath).exists()) {
      blastIndex = dbInstallPath;
    }
    return blastIndex;
  }
  
  private int readEntries(File f) {
    BufferedReader reader = null;
    int nEntries = -1;
    try {
      reader = new BufferedReader(new InputStreamReader(new FileInputStream(f),
          "UTF-8"));
      nEntries = Integer.valueOf(reader.readLine());
    } catch (Exception e) {
      // do not log error with a warn : this is not a bad error,
      // so we do not want to stop the overall indexing process
      LoggerCentral.info(LOGGER, "unable to get nb. of entries from: " + f
          + ": " + e);
    } finally {
      IOUtils.closeQuietly(reader);
    }
    return nEntries;
  }

  private int[] getTotalEntries(String path) {
    int n, entries = -1, sequences = -1;
    File fPath;
    File[] filesNum;
    File[] filesFdbNum;
    fPath = new File(path);

    // read only .num files for entries
    filesNum = fPath.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return (name.toLowerCase().trim().endsWith(DBMSAbstractConfig.FEXT_NUM))
            && (!name.toLowerCase().trim()
                .endsWith(DBMSAbstractConfig.FDBEXT_NUM));
      }
    });
    for (File file : filesNum) {
      n = readEntries(file);
      if (n != -1) {
        if (entries == -1) {
          entries = 0;
        }
        entries += n;
      }
    }

    // read fdb.num files for sequences
    filesFdbNum = fPath.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return (name.toLowerCase().trim()
            .endsWith(DBMSAbstractConfig.FDBEXT_NUM));
      }
    });

    if (filesFdbNum.length == 1) {
      n = readEntries(filesFdbNum[0]);
      if (n != -1) {
        if (sequences == -1) {
          sequences = 0;
        }
        sequences += n;
      }
    } else if (filesFdbNum.length > 1) {
      // JIRA KDMS-39 : bad counting
      // 1. get nb sequences from tmp file
      for (File file : filesFdbNum) {
        if (file.getAbsolutePath().equalsIgnoreCase(
            PTaskFormatDB.getTmpEntriesCountFilepath(path))) {
          n = readEntries(file);
          if (n != -1) {
            if (sequences == -1) {
              sequences = 0;
            }
            sequences += n;
          }
        }
      }
      if (sequences <= 0) {
        // 2. get nb sequences from other files
        for (File file : filesFdbNum) {
          n = readEntries(file);
          if (n != -1) {
            if (sequences == -1) {
              sequences = 0;
            }
            sequences += n;
          }
        }
      }
    }
    if (sequences==-1 && entries==-1){
      String aliasName = DBDescriptorUtils.getBlastBankAliasName(path);
      if (aliasName!=null){
        BlastCmd bc = new BlastCmd();
        entries = sequences = bc.getNbSequences(aliasName);
      }
    }
    return new int[] { entries, sequences };
  }

  private boolean doJob() {
    List<IdxDescriptor> mirrorDescriptors = null;
    DBMirrorConfig mirrorConfig;
    String dbPath, dbPathCur, dbPathDStamp, str, dbPathInstalled, dbPathDownload;
    File fCur;
    int[] dbSizes;

    try {
      if (LoggerCentral.errorMsgEmitted()) {
        throw new Exception(
            "unable to install DBs into production: warn messages emitted.");
      } else if (LoggerCentral.processAborted()) {
        throw new Exception(
            "unable to install DBs into production: process aborted.");
      }
      mirrorConfig = DBDescriptorUtils.getDBMirrorConfig(DBMSAbstractConfig
          .getLocalMirrorConfFile());
      if (mirrorConfig != null) {
        mirrorDescriptors = DBDescriptorUtils.prepareIndexDBList(mirrorConfig);
      }
      for (DBServerConfig db : _dbs) {
        dbPath = db.getLocalFolder();
        // installation dir is always the mirror path
        dbPathCur = Paths.get(DBMSAbstractConfig.getLocalMirrorPath(),
            db.getTypeCode(), db.getName(), DBMSAbstractConfig.CURRENT_DIR)
            .toString();
        // the prepared databank
        dbPathDStamp = dbPath + DBMSAbstractConfig.DOWNLOADING_DIR;

        // check if we have some sequences in the db
        dbSizes = getTotalEntries(Utils.terminatePath(dbPathDStamp)
            + db.getName());
        if ((dbSizes[0] == 0) && (dbSizes[1] == 0)) {
          throw new Exception(
              "unable to install Blast databank in production: no sequences");
        }
        // restart install after stopped
        if (dbSizes[1] == 0) {
          dbSizes[1] = dbSizes[0];
        } else if (dbSizes[0] == 0) {
          dbSizes[0] = dbSizes[1];
        }
        // special case for NCBI Taxonomy only: the Lucene index contains
        // 2xentries (nodes + names)
        if (db.getName().toLowerCase().indexOf("ncbi_taxonomy") != -1) {
          dbSizes[0] = dbSizes[0] / 2;
        }
        LuceneUtils.closeStorages();
        fCur = new File(dbPathCur);
        // rename current Production dir to something else
        if (fCur.exists()) {
          str = dbPath + DBMSAbstractConfig.CURRENTON_DIR
              + DBMSAbstractConfig.getStarterDate();
          // when reloading a mirror, str may already exists : destroy it
          if (new File(str).exists()) {
            if (!PAntTasks.deleteDirectory(str)) {
              throw new Exception("unable to delete old index: " + str);
            }
          }
          if (!PAntTasks.movefile(dbPathCur, str)) {
            throw new Exception("unable to rename " + dbPathCur + " to " + str);
          }
        }

        dbPathDownload = Utils.terminatePath(dbPathDStamp) + db.getName();
        dbPathInstalled = Utils.terminatePath(dbPathCur) + db.getName();

        // create the time stamp
        long bankSize = FileUtils.sizeOfDirectory(new File(dbPathDownload));
        Date now = Calendar.getInstance().getTime();
        String installDate = DBStampProperties.BANK_DATE_FORMATTER.format(now); 
        String releaseDate = DBStampProperties.readReleaseDate(dbPathDownload);
        if (!DBStampProperties.writeDBStamp(dbPathDownload, installDate, releaseDate, dbSizes, bankSize)) {
          throw new Exception("unable to write time stamp file");
        }
        // save the new installed mirror within the central configuration
        if (mirrorConfig != null) {
          updateMirror(mirrorDescriptors, db, dbPathDownload, dbPathInstalled);
        }
        //added on June 2021: write DB stats in a JSON file
        HashMap<String, String> index = new HashMap<>();
        index.put(BankJsonDescriptor.BLAST_INDEX, getBlastIndexPath(db, dbPathDownload, dbPathInstalled));
        index.put(BankJsonDescriptor.LUCENE_INDEX, getLuceneIndexPath(db, dbPathDownload, dbPathInstalled));
        scanForOtherIndexes(db, index, dbPathDownload, dbPathInstalled);
        BankJsonDescriptor descriptor = new BankJsonDescriptor(
            db.getName(), 
            db.getDescription(), 
            installDate, 
            releaseDate, 
            db.getTypeCode(), 
            db.getProvider(), 
            index, 
            bankSize, 
            dbSizes[0]);
        descriptor.write(new File(Utils.terminatePath(dbPathDownload)+BankJsonDescriptor.DEFAULT_DESCRIPTOR_FNAME));
        // rename tmp dir to become the Production one
        if (!PAntTasks.movefile(dbPathDStamp, dbPathCur)) {
          throw new Exception("unable to rename " + dbPathDStamp + " to "
              + dbPathCur);
        }
        LoggerCentral.info(LOGGER, dbPathDStamp
            + " has been installed in production");
      }
      if (mirrorConfig != null) {

        LoggerCentral.info(LOGGER,
            "mirrorDescriptors = " + mirrorDescriptors.size());
        LoggerCentral.info(LOGGER,
            "KDMSAbstractConfig.getLocalMirrorConfFile() = "
                + DBMSAbstractConfig.getLocalMirrorConfFile());
        DBDescriptorUtils.saveDBMirrorConfig(
            DBMSAbstractConfig.getLocalMirrorConfFile(),
            DBDescriptorUtils.getMirrorConfig(mirrorDescriptors, mirrorConfig));
      }
    } catch (Exception e) {
      _errMsg = "unable to install Production directory: " + e.toString();
      return false;
    }

    return true;
  }
  /**
   * Implementation of KLTask interface.
   */
  private boolean executeImpl() {
    PrintWriter writer = null;
    String dbPathLock;
    File locker;
    boolean bRet;
    int counter = 0;

    // prepare a lock file
    dbPathLock = DBMSAbstractConfig.getLocalMirrorPath() + "lock";
    locker = new File(dbPathLock);

    while (true) {
      // if there is another lock file, we have to wait our turn
      if (locker.exists() == false) {
        break;
      }
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
      // do not wait until the end of the Universe
      counter++;
      if (counter == 120) {// two minutes
        _errMsg = "unable to install Production directory: system blocked by another lock file: "
            + dbPathLock;
        return false;
      }
    }

    // create lock file
    try {
      writer = new PrintWriter(locker);
      writer.println("lock");
      writer.flush();
    } catch (Exception e) {
      _errMsg = "unable to install Production directory: unable to create lock: "
          + dbPathLock;
      return false;
    } finally {
      IOUtils.closeQuietly(writer);
    }

    // install DB in production
    bRet = doJob();

    // release the lock file. We do not check if the delete fails: in the worst
    // case, another KDMS job will fail.
    // At least, the current installation job will succeed...
    locker.delete();

    return bRet;
  }

  /**
   * Implementation of KLTask interface.
   */
  public boolean execute() {
    boolean bRet = executeImpl();
    if (!bRet) {
      LoggerCentral.warn(LOGGER, _errMsg);
    }
    return bRet;
  }
  public void setParameters(String params) {
  }

}
