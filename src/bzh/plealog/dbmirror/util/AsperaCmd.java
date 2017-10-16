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

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.conf.DBMSConfigurator;
import bzh.plealog.dbmirror.util.log.LoggerCentral;
import bzh.plealog.dbmirror.util.runner.CommandArgument;
import bzh.plealog.dbmirror.util.runner.DBMSExecNativeCommand;
import bzh.plealog.dbmirror.util.runner.ExecMonitor;

/**
 * Java wrapper to the Aspera ascp application.
 * 
 * @author Patrick G. Durand
 * */
public class AsperaCmd {

	private static final String DEF_PRG_NAME = "ascp";

	private String key_path;
	private String remote_url;
	private String target_dir;
	
	/**
	 * No private constructor available.
	 */
	private AsperaCmd(){}
	
	/**
	 * Constructor.
	 * 
	 * @param key_path absolute path to ssh key to be used with Apsera ascp command.
	 * @param remote_url URL to be used to access Aspera remote server
	 * @param target_dir where to save files
	 * */
	public AsperaCmd(String key_path, String remote_url, String target_dir){
	  this();
	  this.key_path = key_path;
	  this.remote_url = remote_url;
	  this.target_dir = target_dir;
	}
	
	private static final Log           LOGGER                   = LogFactory
			.getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
					+ ".AsperaCmd");
	
	/**
	 * Locate ascp executable.
	 */
	private String getASCPCmdPath() {
	  DBMSConfigurator conf;
		String ascpcmd, path;

		conf = DBMSAbstractConfig.getConfigurator();
		if (conf == null) {
			return null;
		}

		// For now: prg name is hard coded to enable easy software update:
		// indeed, this blastcmd feature has been added in BeeDeeM 4.1.1
		// release.
		ascpcmd = conf.getProperty(DBMSConfigurator.ASPERA_BIN);
		if (ascpcmd==null || ascpcmd.length()==0){
			ascpcmd = DEF_PRG_NAME;
		}
		path = conf.getProperty(DBMSConfigurator.ASPERA_PATH);
		if (path == null) {
			return null;
		}

		path = DBMSExecNativeCommand.formatNativePath(path, (path
				.indexOf(DBMSExecNativeCommand.APPDIR_VAR_NAME) >= 0) ? true : false,
						false);
		path = Utils.terminatePath(path) + ascpcmd;
		if (DBMSExecNativeCommand.getOSType() == DBMSExecNativeCommand.WINDOWS_OS) {
			path += ".exe";
		}
		if (new File(path).exists() == false) {
			return null;
		}
		return path;
	}

	/**
	 * Prepare ascp command-line.
	 */
	private Map<String, CommandArgument> prepareParams(String fileToRetrive) {
	  LinkedHashMap<String, CommandArgument> params = new LinkedHashMap<String, CommandArgument>();
	  
	  // for now, these arguments are hard-coded... adapt if needed
	  params.put("-k", new CommandArgument("1", false));
	  params.put("-T", new CommandArgument("", false));
	  params.put("-l640M", new CommandArgument("", false));
	  
	  // open-ssh key
    params.put("-i", new CommandArgument(key_path, true));
    
    // the file to download. 
    params.put(remote_url+":"+fileToRetrive, new CommandArgument(target_dir, true));
	  return params;
	}
	
	/**
	 * Retrieve a file using an Aspera server.
	 * 
	 * @param fileToRetrive path to remote file to retrieve. 
	 * For now, we use ascp on a "single file download"
   * basis to enable BeeDeeM to monitor file download 
   * processing as it does already using FTP or local 
   * file transfer.
	 * 
	 * @return true if success
	 */
	public boolean getRemoteFile(String fileToRetrive){
		DBMSExecNativeCommand runner;
		InfoMonitor           monitor;
		
		monitor = new InfoMonitor();
		runner = new DBMSExecNativeCommand(monitor);
		Map<String, CommandArgument> param = prepareParams(fileToRetrive);
		runner.executeAndWait(getASCPCmdPath(), param);
		
		return true;
	}

	/**
	 * A monitor used to process output of ascp cmd.
	 */
	private class InfoMonitor implements ExecMonitor {
		@Override
		public void warn(String msg) {
			LoggerCentral.warn(LOGGER, msg);
		}

		@Override
		public void info(String msg) {
		  LoggerCentral.info(LOGGER, msg);
		}
	}
}
