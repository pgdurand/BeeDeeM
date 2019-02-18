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
package bzh.plealog.dbmirror.util.aspera;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.conf.DBMSConfigurator;
import bzh.plealog.dbmirror.util.runner.CommandArgument;
import bzh.plealog.dbmirror.util.runner.DBMSExecNativeCommand;

/**
 * Set of utility methods for the Aspera Java wrapper.
 */
public class AsperaUtils {
	/**
	 * Locate ascp executable. Relies on DBMSConfigurator object, i.e. dbms.config file. 
	 * 
	 * @return either a path or null if aspera.bin.path is not defined in config file.
	 */
	public static String getASCPCmdPath() {
	  DBMSConfigurator conf;
		String path;

		conf = DBMSAbstractConfig.getConfigurator();
		if (conf == null) {
			return null;
		}

		path = conf.getProperty(DBMSConfigurator.ASPERA_BIN);
		if (path == null || path.length()==0) {
			return null;
		}

		path = DBMSExecNativeCommand.formatNativePath(path, (path
				.indexOf(DBMSExecNativeCommand.APPDIR_VAR_NAME) >= 0) ? true : false,
						true);
		if (DBMSExecNativeCommand.getOSType() == DBMSExecNativeCommand.WINDOWS_OS) {
			path += ".exe";
		}
		if (new File(path).exists() == false) {
			return null;
		}
		return path;
	}

	/**
	 * Locate ascp openssh key. Relies on DBMSConfigurator object, i.e. dbms.config file. 
	 * 
	 * @return either a path or null if aspera.key.path is not defined in config file.
	 */
	public static String getASCPKeyPath() {
	  DBMSConfigurator conf;
		String aspkey;

		conf = DBMSAbstractConfig.getConfigurator();
		if (conf == null) {
			return null;
		}

		aspkey = conf.getProperty(DBMSConfigurator.ASPERA_KEY);
		if (aspkey==null || aspkey.length()==0){
			aspkey = null;
		}
		return aspkey;
	}

	/**
	 * Converts command-line arguments to a convenient object.
	 * */
	public static Map<String, CommandArgument> handleArguments(String argsLine) {
		LinkedHashMap<String, CommandArgument> args = new LinkedHashMap<>();
		
		String[] tokens = argsLine.split(" ");
		for(int i=0; i < tokens.length; i++) {
			String token = tokens[i];
			// a cmdline argument starts with '-'
			if (token.charAt(0)=='-') {
				//argument with value
				if (i+1 < tokens.length && tokens[i+1].charAt(0)!='-') {
					args.put(token, new CommandArgument(tokens[i+1], 
							tokens[i+1].contains(File.separator)));
					i++;
				}
				else {
					//argument with no value
					args.put(token, new CommandArgument("", false));
				}
			}
		}
		return args;
	}
}
