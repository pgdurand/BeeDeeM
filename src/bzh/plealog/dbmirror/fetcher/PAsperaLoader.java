/* Copyright (C) 2019 Patrick G. Durand
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
package bzh.plealog.dbmirror.fetcher;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.util.aspera.AsperaCmd;
import bzh.plealog.dbmirror.util.aspera.AsperaUtils;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.conf.DBMSConfigurator;
import bzh.plealog.dbmirror.util.log.LoggerCentral;
import bzh.plealog.dbmirror.util.runner.DBMSExecNativeCommand;

/**
 * This class is a file loader using Aspera.
 * 
 * @author Patrick G. Durand
 */
public class PAsperaLoader extends PFTPLoader {

	private String bin_path;
  private String key_path;
  private String remote_url;
	private AsperaCmd aspera_cmd;
	
	public static final String ASPC_WORKER = "AsperaLoader";

	private static final Log      LOGGER       = LogFactory
      .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
          + ".PAsperaLoader");
  private static String ERR_1=" is not defined in dbms config file";
  private static String ERR_2=" Aspera server address not defined in bank descriptor";
  
	public PAsperaLoader(int id) {
		super(id);
	}

  @Override
	public String getWorkerBaseName() {
		return ASPC_WORKER;
	}

  @Override
	public void closeLoader() {
	  //nothing to do
	}

  @Override
  public boolean readyToDownload() {
    return true;
  }
  
  @Override
  public boolean prepareLoader(DBServerConfig fsc) {
	  boolean bRet=true;
	  //check we have Aspera configuration
	  //Aspera bin and ssh certificate are located in global config file
	  bin_path = AsperaUtils.getASCPCmdPath();
    if (bin_path==null) {
      LoggerCentral.warn(LOGGER, DBMSConfigurator.ASPERA_BIN+ERR_1);
      bRet = false;
    }
    key_path = AsperaUtils.getASCPKeyPath();
    if (key_path==null) {
      LoggerCentral.warn(LOGGER, DBMSConfigurator.ASPERA_KEY+ERR_1);
      bRet = false;
    }
    // Aspera server address in bank specific
    remote_url = fsc.getAsperaAddress();
    if (StringUtils.isBlank(remote_url)) {
      LoggerCentral.warn(LOGGER, ERR_2);
      bRet = false;
    }
    
    //setup Aspera 'ascp' command-line tool wrapper
    if (bRet) {
      aspera_cmd = new AsperaCmd(bin_path, key_path, remote_url, fsc.getLocalTmpFolder());
      String args=fsc.getAsperaArguments();
      if (StringUtils.isNotBlank(args)) {
        aspera_cmd.setArguments(AsperaUtils.handleArguments(args));
      }
    }
    return bRet;
	}

	@Override
	protected int downloadFile(DBServerConfig fsc, DBMSFtpFile rFile, File file, long lclFSize) {
		int iRet = 0;
		
		//no apsera cmd object: may happen if wrong configuration
	  if (aspera_cmd==null) {
		  return iRet;
		}
		
		//prepare the server side file to retrieve
		String fileToRetrive = rFile.getRemoteDir()+rFile.getFtpFile().getName();

    LoggerCentral.info(LOGGER, "  " + getLoaderId() + ": download: " + fileToRetrive);
		
    //prepare a stream monitor; also used to detect user interruption
    ControlFileLoadingTask monitor = new ControlFileLoadingTask(file, fsc, rFile);

    //timer set using 10 seconds rate; adapt to file size and/or transfer rate?
    Timer timer = new Timer(true);
    timer.scheduleAtFixedRate(monitor, 0, 10*1000);
    
    //to enable the feature "stop download" from UI, we have to take control
    //on Aspera process in coordination with a monitor
    
    //start download remote file
    Process proc = aspera_cmd.getRemoteFile_P(fileToRetrive);
    int exitCode=0;
    boolean apseraRunning = true;
    
    //monitor process to detect interruption
    while (apseraRunning && (!monitor.interrupted())) {
      apseraRunning = false;
      try {
        exitCode = proc.exitValue();
      } catch (IllegalThreadStateException ex) {
        apseraRunning = true;
      }
      try {
        Thread.sleep(DBMSExecNativeCommand.DEFAULT_TIME_SLICE);
      } catch (InterruptedException e) {
      }
    }
    
    //properly close process (I/O streams)
    DBMSExecNativeCommand.terminateProcess(proc);

    // return(3): see method documentation
    if (monitor.interrupted()) {
      return(3);
    }
    
    //normal job termination, we have to stop timer
    timer.cancel();
    
    if (exitCode!=0) {
      //simple error msg (real error msg displayed by Aspera CLI)
		  LoggerCentral.warn(LOGGER, "failed");
		}
		else {
		  iRet = 1;
		}
    
		return iRet;
	}
	/**
	 * Use a monitor to track file loading. Since we use ascp commend-line tool,
	 * we do not have much choice to give user a feedback on what's going on...
	 */
	private class ControlFileLoadingTask extends TimerTask {
	  File fileToMonitor;
	  MyCopyStreamListener monitor;
	  long streamSize;
	  boolean bInterrupted=false;

	  private ControlFileLoadingTask(File f, DBServerConfig fsc, DBMSFtpFile rFile){
	    fileToMonitor = f;
	    streamSize = rFile.getFtpFile().getSize();
	    monitor = new MyCopyStreamListener(getLoaderId(), _userMonitor, fsc.getName(), 
	        rFile.getFtpFile().getName(), streamSize);
	  }
	  
	  private boolean interrupted() {
	    return bInterrupted;
	  }
    
	  @Override
    public void run() {
      if (fileToMonitor.exists()==false) {
        return;
      }
      
      try{
        monitor.bytesTransferred(fileToMonitor.length(), 0, streamSize);
      }
      catch (MyCopyInteruptException ex) {
        //when catching this event, stop downloading
        bInterrupted=true;
        this.cancel();
      }
    }
	}
}
