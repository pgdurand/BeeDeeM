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
package bzh.plealog.dbmirror.util.conf;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.plealog.genericapp.api.EZApplicationBranding;

import bzh.plealog.dbmirror.fetcher.DBServerConfig;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * Serialization class for JSON bank descriptor.
 * 
 * @author Patrick G. Durand
 */
public class BankJsonDescriptor {
  private BankJsonMainSectionDescriptor _main;

  private static final Log        LOGGER = LogFactory
      .getLog(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
          + ".BankJsonDescriptor");

  public static final String BLAST_INDEX = "blast";
  public static final String LUCENE_INDEX = "lucene";
  public static final String EDAM_GENOMICS = "genomics";
  public static final String EDAM_PROTEOMIS = "proteomics";
  public static final String TYPE_NUCL = "nucl";
  public static final String TYPE_PROT = "prot";
  public static final String TYPE_ONTO = "ontology";

  public BankJsonDescriptor() {
    // TODO Auto-generated constructor stub
  }

  /**
   * Constructor.
   * 
   * @param name name of the bank
   * @param description free text to describe the bank
   * @param installDate use format: dd/MM/YYYY
   * @param release release of the bank
   * @param type use one of DBServerConfig.PROTEIN_TYPE, DBServerConfig.NUCLEIC_TYPE
   * or DBServerConfig.DICO_TYPE
   * @param provider provider of the bank
   * @param blastPath path to blast bank if any
   * @param lucenePath path to lucene index if any
   * @param bytes size of bank in bytes
   * @param entries size of bank in number of sequences or entries (ontology)
   * */
  @SuppressWarnings("serial")
  public BankJsonDescriptor(String name, String description,
      String installDate, String release,
      String type, String provider, String blastPath, String lucenePath,
      long bytes, int entries) {

    BankJsonMainSectionDescriptor mainJ = new BankJsonMainSectionDescriptor();
    BankJsonSizeSectionDescriptor sizeJ = new BankJsonSizeSectionDescriptor();

    mainJ.setName(name);
    mainJ.setDescription(description);
    mainJ.setInstallDate(installDate);
    mainJ.setRelease(release);
    switch(type) {
    case DBServerConfig.PROTEIN_TYPE:
      mainJ.setOmics(new ArrayList<String>() {{add(EDAM_PROTEOMIS);}});
      mainJ.setType(new ArrayList<String>() {{add(TYPE_PROT);}});
      break;
    case DBServerConfig.NUCLEIC_TYPE:
      mainJ.setOmics(new ArrayList<String>() {{add(EDAM_GENOMICS);}});
      mainJ.setType(new ArrayList<String>() {{add(TYPE_NUCL);}});
      break;
    case DBServerConfig.DICO_TYPE:
    default:
      mainJ.setOmics(new ArrayList<String>() {{add(EDAM_PROTEOMIS);add(EDAM_GENOMICS);}});
      mainJ.setType(new ArrayList<String>() {{add(TYPE_ONTO);}});
    }
    mainJ.setProvider(new ArrayList<String>() {{add(provider);}});
    mainJ.setOwner(EZApplicationBranding.getAppName());
    HashMap<String, String> index = new HashMap<>();
    if (blastPath!=null) {
      index.put(BLAST_INDEX, blastPath);
    }
    if (lucenePath!=null) {
      index.put(LUCENE_INDEX, lucenePath);
    }
    mainJ.setIndex(index);

    sizeJ.setBytes(bytes);
    sizeJ.setSequences(entries);

    mainJ.setSize(sizeJ);
    this.setMain(mainJ);
  }

  @JsonProperty("main")
  public BankJsonMainSectionDescriptor getMain() { 
    return _main; 
  }

  public void setMain(BankJsonMainSectionDescriptor n) { 
    _main = n; 
  }

  /**
   * Read a JSON formatted bank descriptor.
   * 
   * @param f a file
   * 
   * @return a BankJsonDescriptor or null if load failed. Error is logged.
   */
  public static BankJsonDescriptor read(File f) {
    ObjectMapper mapper = new ObjectMapper();
    BankJsonDescriptor data = null;
    try {
      data = mapper.readValue(f, BankJsonDescriptor.class);
    } catch (Exception e) {
      LoggerCentral.warn(LOGGER, e.toString());
    }
    return data;
  }

  /**
   * Write a JSON formatted bank descriptor.
   * 
   * @param f a file
   * @param data a BankJsonDescriptor instance
   * 
   * @return true if file writing is ok, false otherwise. Error is logged.
   */
  public static boolean write(File f, BankJsonDescriptor data) {
    ObjectMapper mapper = new ObjectMapper();
    boolean bRet = false;
    mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    try {
      mapper.writeValue(f, data);
      bRet = true;
    } catch (Exception e) {
      LoggerCentral.warn(LOGGER, e.toString());
    }
    return bRet;
  }
}
