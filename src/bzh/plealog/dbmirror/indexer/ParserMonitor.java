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
package bzh.plealog.dbmirror.indexer;

import bzh.plealog.dbmirror.util.runner.DBMSUniqueSeqIdRedundantException;

/**
 * This interface defines a parser monitor. It aims at following the parsing process of databse sequence files.
 */
public interface ParserMonitor {
	/**
	 * This method is called for each new sequence discovered in a database sequence file.
	 * 
	 * @param id
	 *        an entry identifier.
	 * @param name
	 *        an entry name.
	 * @param fName
	 *        the name of the database file containing this entry.
	 * @param start
	 *        absolute position of the beginning of the entry within the file. Unit is bytes.
	 * @param stop
	 *        absolute position of the ending of the entry within the file. Unit is bytes.
	 */
	public void seqFound(String id, String name, String fName, long start, long stop, boolean checkRedundancy)
			throws DBMSUniqueSeqIdRedundantException;

	/**
	 * This method is called when the processing of a file is about to start.
	 * 
	 * @param fName
	 *        fName the name of the database file (full path)
	 * @param fSize
	 *        file size
	 **/
	public void startProcessingFile(String fName, long fSize);

	/**
	 * This method is called when the processing of a file is finished.
	 */
	public void stopProcessingFile(String fName, int entries);

	/**
	 * @return true if the redundant check was done and if a redundant sequence was found during the parse process
	 */
	public boolean redundantSequenceFound();

}
