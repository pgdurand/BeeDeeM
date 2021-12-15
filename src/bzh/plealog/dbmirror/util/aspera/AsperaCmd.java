/* Copyright (C) 2019-2021 Patrick G. Durand
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
package bzh.plealog.dbmirror.util.aspera;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;
import bzh.plealog.dbmirror.util.runner.CommandArgument;
import bzh.plealog.dbmirror.util.runner.DBMSExecNativeCommand;
import bzh.plealog.dbmirror.util.runner.ExecMonitor;

/**
 * Java wrapper to the Aspera ascp application.
 * 
 * @author Patrick G. Durand
 */
public class AsperaCmd {
	// path to ascp binary
	private String bin_path;
	// path to openssh key to use to connect remote aspera server
	private String key_path;
	// URL to connect; format is: user@server
	private String remote_url;
	// directory use to save downloaded file
	private String target_dir;
	// arguments to be used with ascp tool
	Map<String, CommandArgument> args;

	private static final Logger LOGGER = LogManager.getLogger(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY + ".AsperaCmd");

	/**
	 * No private constructor available.
	 */
	private AsperaCmd() {
	}

	/**
	 * Constructor. This constructor sets up an ascp command line using these
	 * default arguments: -k 1 -T -l 640m. To understand these arguments:
	 * https://download.asperasoft.com/download/docs/cli/3.7.5/user_linux/pdf2/Aspera_CLI_Admin_3.7.5_Linux.pdf
	 *
	 * @param bin_path   absolute path Aspera ascp command.
	 * @param key_path   absolute path to openssh key to be used with Aspera ascp
	 *                   command.
	 * @param remote_url URL to be used to access Aspera remote server. Format is:
	 *                   user@server
	 * @param target_dir where to save files
	 */
	public AsperaCmd(String bin_path, String key_path, String remote_url, String target_dir) {
		this();
		this.bin_path = bin_path;
		this.key_path = key_path;
		this.remote_url = remote_url;
		this.target_dir = target_dir;
		// set default arguments for Aspera CLI
		args = new LinkedHashMap<String, CommandArgument>();
		args.put("-k", new CommandArgument("1", false));
		args.put("-T", new CommandArgument("", false));
		args.put("-l", new CommandArgument("640M", false));
	}

	/**
	 * Constructor.
	 * 
	 * @param bin_path   absolute path Aspera ascp command.
	 * @param key_path   absolute path to openssh key to be used with Aspera ascp
	 *                   command.
	 * @param remote_url URL to be used to access Aspera remote server. Format is:
	 *                   <user>@<server>
	 * @param target_dir where to save files
	 * @param args       set of arguments for Aspera CLI
	 */
	public AsperaCmd(String bin_path, String key_path, String remote_url, String target_dir,
			Map<String, CommandArgument> args) {
		this();
		this.bin_path = bin_path;
		this.key_path = key_path;
		this.remote_url = remote_url;
		this.target_dir = target_dir;
		this.args = args;
	}

	/**
	 * Add an argument for ascp command-line tool.
	 * 
	 * @param key cmdline argument
	 * @param arg corresponding value
	 */
	public void addArgument(String key, CommandArgument arg) {
		args.put(key, arg);
	}

	/**
	 * Set new arguments for ascp command-line tool.
	 * 
	 * @param args arguments
	 */
	public void setArguments(Map<String, CommandArgument> args) {
		this.args = args;
	}

	/**
	 * Returns arguments of ascp command-line.
	 * 
	 * @return arguments of ascp command-line.
	 */
	public Map<String, CommandArgument> getArguments() {
		return args;
	}

	/**
	 * Prepare ascp command-line.
	 * 
	 * @return arguments of ascp command-line.
	 */
	private Map<String, CommandArgument> prepareParams(String fileToRetrive) {
		LinkedHashMap<String, CommandArgument> params;
		params = new LinkedHashMap<String, CommandArgument>();
		params.putAll(args);
		params.put("-i", new CommandArgument(key_path, true));
		params.put(remote_url + ":" + fileToRetrive, new CommandArgument(target_dir, true));
		return params;
	}

	/**
	 * Retrieve a file using an Aspera server. Aspera ascp tool verbose
	 * messages are logged in BeeDeeM log file; useful if something went
	 * wrong.
	 * 
	 * @param fileToRetrive path to remote file to retrieve.
	 * 
	 * @return true if success
	 */
	public boolean getRemoteFile(String fileToRetrive) {
		DBMSExecNativeCommand runner;
		InfoMonitor monitor;

		monitor = new InfoMonitor();
		runner = new DBMSExecNativeCommand(monitor);
		Map<String, CommandArgument> param = prepareParams(fileToRetrive);
		runner.executeAndWait(bin_path, param);

		return runner.getExitCode() == 0;
	}

  public Process getRemoteFile_P(String fileToRetrive) {
    DBMSExecNativeCommand runner;
    InfoMonitor monitor;

    monitor = new InfoMonitor();
    runner = new DBMSExecNativeCommand(monitor);
    Map<String, CommandArgument> param = prepareParams(fileToRetrive);
    
    return runner.executeAndReturn(bin_path, param);
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
