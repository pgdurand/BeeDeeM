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
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Hashtable;
import java.util.Locale;
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
 * Java wrapper to the blastdbcmd application.
 * 
 * @author Patrick G. Durand
 * */
public class BlastCmd {

	private static final String DEF_PRG_NAME = "blastdbcmd";

	private static final Log           LOGGER                   = LogFactory
			.getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
					+ ".BlastCmd");

	/**
	 * Locate Blastdbcmd executable in the software distribution.
	 */
	private String getBlastCmdPath() {
		DBMSConfigurator conf;
		String blastcmd, path;

		conf = DBMSAbstractConfig.getConfigurator();
		if (conf == null) {
			return null;
		}

		// For now: prg name is hard coded to enable easy software update:
		// indeed, this blastcmd feature has been added in BeeDeeM 4.1.1
		// release.
		blastcmd = conf.getProperty(DBMSConfigurator.BCMD_PRG_NAME);
		if (blastcmd==null || blastcmd.length()==0){
			blastcmd = DEF_PRG_NAME;
		}
		path = conf.getProperty(DBMSConfigurator.FDB_PATH_NAME);
		if (blastcmd == null || path == null) {
			return null;
		}

		path = DBMSExecNativeCommand.formatNativePath(path, (path
				.indexOf(DBMSExecNativeCommand.APPDIR_VAR_NAME) >= 0) ? true : false,
						false);
		path = Utils.terminatePath(path) + blastcmd;
		if (DBMSExecNativeCommand.getOSType() == DBMSExecNativeCommand.WINDOWS_OS) {
			path += ".exe";
		}
		if (new File(path).exists() == false) {
			return null;
		}
		return path;
	}

	/**
	 * Prepare blastdbcmd command-line.
	 */
	private static Map<String, CommandArgument> prepareParams(String dbAliasPath) {
	  Hashtable<String, CommandArgument> params;
	  params = new Hashtable<String, CommandArgument>();
	  params.put("-db", new CommandArgument(dbAliasPath, true));
	  params.put("-dbtype", new CommandArgument("guess", false));
	  params.put("-info", new CommandArgument("", false));
	  return params;
	}
	
	/**
	 * Returns the number of sequences contained in a Blast databank.
	 * 
	 * @param dbAliasPath path to Blast bank.
	 * 
	 * @return nb of sequences
	 */
	public int getNbSequences(String dbAliasPath){
		DBMSExecNativeCommand runner;
		InfoMonitor           monitor;
		
		monitor = new InfoMonitor();
		runner = new DBMSExecNativeCommand(monitor);
		Map<String, CommandArgument> param = prepareParams(dbAliasPath);
		LOGGER.debug("Parameters are: "+param.toString());
		runner.executeAndWait(getBlastCmdPath(), param);
		
		return monitor.nbSequences;
	}

	/**
	 * A monitor used to process output of blastdbcmd.
	 */
	private class InfoMonitor implements ExecMonitor {
		/* ./blastdbcmd -db /beedeem-path-to/PDB_proteins/PDB_proteinsM -dbtype guess -info
		 * 
			Database: PDB_proteins
				93,500 sequences; 23,509,168 total residues

			Date: Sep 7, 2017  1:18 PM	Longest sequence: 5,037 residues

			Volumes:
				/beedeem-path-to/PDB_proteins/PDB_proteins
		 */
		private int nbSequences=0;
		
		private String getFirstWord(String text) {
			String tText = text.trim();
			if (tText.indexOf(' ') > -1) {
				return tText.substring(0, text.indexOf(' '));
			} else {
				return tText;
			}
		}
		@Override
		public void warn(String msg) {
			LoggerCentral.warn(LOGGER, msg);
		}

		@Override
		public void info(String msg) {
		  NumberFormat nf = NumberFormat.getInstance(Locale.US);
			if (  msg.contains("sequences;") && 
			      (msg.contains("total residues")||msg.contains("total bases")) 
			   ){
				try {
					nbSequences = nf.parse(getFirstWord(msg)).intValue();
				} catch (ParseException e) {
					// should not happen. However, if it does happen, do not fail
					// and simply set nbSequences to -1.
					nbSequences=-1;
				}
			}
		}
	}
}
