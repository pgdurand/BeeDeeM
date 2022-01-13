/* Copyright (C) 2007-2022 Patrick G. Durand
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

import java.io.File;
import java.io.IOException;

public abstract class PAbstractTask implements PTask {
	private String dbConfName;
	
	public String getDbConfName() {
		return dbConfName;
	}

	public void setDbConfName(String name) {
		dbConfName = name;
	}
	
	public static void setTaskOkForFile(String fPath) {
	  File f = new File(fPath+PTask.TASK_OK_FEXT);
	  try {
      f.createNewFile();
    } catch (IOException e) {
      //hide this exception; in the worst case, calling Taxk will have to
      //redo its execution on bank installation resume... not so bad, just
      //potentially time consuming
    }
	}
	
	public static boolean testTaskOkForFileExists(String fPath) {
	  File f = new File(fPath+PTask.TASK_OK_FEXT);
	  return f.exists();
	}
}
