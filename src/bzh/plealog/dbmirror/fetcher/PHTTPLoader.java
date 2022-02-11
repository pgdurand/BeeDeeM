/* Copyright (C) 2021 Patrick G. Durand
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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import bzh.plealog.bioinfo.io.http.HTTPBasicEngine;
import bzh.plealog.bioinfo.io.http.HTTPEngineException;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * This class is a file loader using HTTP or HTTPS protocol.
 * 
 * @author Patrick G. Durand
 */
public class PHTTPLoader extends PFTPLoader {

	private String url_base;
	
	public static final String HTTP_WORKER = "HTTPLoader";

	private static final Log      LOGGER       = LogFactory
      .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
          + ".PHTTPLoader");
  
	public PHTTPLoader(int id) {
		super(id);
	}

  @Override
	public String getWorkerBaseName() {
		return HTTP_WORKER;
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
  public Log getLogger() {
    return LOGGER;
  }
 
  @Override
  public boolean prepareLoader(DBServerConfig fsc) {
	  url_base = fsc.getFTPAternativeProtocol();
	  url_base+="://";
	  url_base+=fsc.getAddress();
    return HTTPBasicEngine.isServerAvailable(url_base);
	}

	@Override
	protected int downloadFile(DBServerConfig fsc, DBMSFile rFile, File file, long lclFSize) {
		int iRet = 0;
		Map<String, String> header_attrs=null;
		
		//prepare the server side file to retrieve
		String fileToRetrive = url_base + "/" + rFile.getRemoteDir()+rFile.getName();

    LoggerCentral.info(LOGGER, "  " + getLoaderId() + ": download: " + fileToRetrive);

    //resume downloading if needed
    if (lclFSize!=0) {
      LoggerCentral.info(LOGGER, "resume downloading at byte: "+lclFSize);
      header_attrs = new HashMap<>();
      header_attrs.put(
          HTTPBasicEngine.RANGE_HTTP, 
          String.format(HTTPBasicEngine.RANGE_HTTP_FORMAT, lclFSize));
    }
    //go!
    try {
      HTTPBasicEngine.doGet(fileToRetrive, header_attrs, file,
          new MyCopyStreamListener(getLoaderId(), _userMonitor, 
              fsc.getName(), rFile.getName(), rFile.getSize(), 0));
      iRet = 1;
    } catch (HTTPEngineException e1) {
      //do not raise warn or error here, handled by LoaderEngine
      LoggerCentral.info(LOGGER, e1.getMessage() + " (" + e1.getHttpCode() + ")");
    }
    if (_userMonitor!=null && _userMonitor.jobCancelled()) {
      iRet=3;
    }
		return iRet;
	}
}
