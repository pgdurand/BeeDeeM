/* Copyright (C) 2007-2023 Patrick G. Durand
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
package bzh.plealog.dbmirror.util.descriptor;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.BankJsonDescriptor;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptor.TYPE;
import bzh.plealog.dbmirror.util.runner.DBStampProperties;

/**
 * This class is used to expose IdxDescriptor to Velocity Templates.
 * 
 *  @author Patrick G. Durand
 */
public class DatabankDescriptor implements Serializable {
  private static final long serialVersionUID = 5222950557207540653L;

  private String            _name;
  private String            _dbHome;//home dir of bank
  private String            _dbPath;//path to main index, e.g. blast bank alias
  private String            _code;
  private String            _description;
  private String            _type;
  private String            _nbSequence;
  private String            _diskSize;
  private String            _timeStamp;
  private String            _releaseStamp;
  private HashMap<String, String> _otherIndex;
  private long              _diskSizeL;
  private boolean           hasAnnotation = false;
  
  private NumberFormat      numFormatter     = DecimalFormat
                                                 .getInstance(Locale.ENGLISH);

  public DatabankDescriptor(IdxDescriptor descriptor) {
    _otherIndex = new HashMap<>();
    _name = descriptor.getName();
    _dbPath = descriptor.getCode();
    _description = descriptor.getDescription();
    _code = descriptor.getKbCode();

    if (descriptor.getType().equals(TYPE.blastn) || descriptor.getType().equals(TYPE.nucleic))
      _type = "N";
    else if (descriptor.getType().equals(TYPE.blastp) || descriptor.getType().equals(TYPE.proteic))
      _type = "P";
    else if (descriptor.getType().equals(TYPE.dico))
      _type = "D";

    _dbHome = new File(_dbPath).getParent();
    Properties props = DBStampProperties.readDBStamp(_dbHome);

    _timeStamp = props.getProperty(DBStampProperties.TIME_STAMP);
    _releaseStamp = props.getProperty(DBStampProperties.RELEASE_TIME_STAMP);
    
    if (_type.equals("D"))
      _nbSequence = numFormatter.format(Long.valueOf(props
          .getProperty(DBStampProperties.NB_ENTRIES)));
    else
      _nbSequence = numFormatter.format(Long.valueOf(props
          .getProperty(DBStampProperties.NB_SEQUENCES)));
    _diskSizeL = FileUtils.sizeOfDirectory(new File(_dbHome));
    _diskSize = Utils.getBytes(_diskSizeL);
    //_otherIndex.put("diamond:2.0.6", _dbHome);
    //_otherIndex.put("blast-v4:2.6.0", _dbHome);
    scanForOtherIndex(_dbHome);
  }

  /**
   * @return the _nbSequence
   */
  public String getNbSequence() {
    return _nbSequence;
  }

  /**
   * @return the _dbPath
   */
  public String getDbPath() {
    return _dbPath;
  }

  /**
   * @return the _dbHome
   */
  public String getDbHome() {
    return _dbHome;
  }

  /**
   * @return the _code
   */
  public String getCode() {
    return _code;
  }

  /**
   * @return the _type
   */
  public String getType() {
    return _type;
  }

  /**
   * @return the _name
   */
  public String getName() {
    return _name;
  }

  /**
   * @return the _description
   */
  public String getDescription() {
    return _description;
  }

  /**
   * @return the _diskSize
   */
  public String getDiskSize() {
    return _diskSize;
  }

  public long getDiskSizeL() {
    return _diskSizeL;
  }

  public boolean hasAnnotation() {
    return hasAnnotation;
  }

  public void setHasAnnotation(boolean hasAnnotation) {
    this.hasAnnotation = hasAnnotation;
  }

  public String getTimeStamp() {
    return _timeStamp;
  }
  
  public String getReleaseTimeStamp() {
    return _releaseStamp;
  }
  
  public Map<String, String> getAdditionalIndex(){
    return _otherIndex;
  }
  
  private void scanForOtherIndex(String directory) {
    List<File> files;
    //within installation directory, look for all sub-dir terminating with .idx
    try {
      files = Files.list(Paths.get(directory))
          .filter(Files::isDirectory)
          .filter(path -> path.toString().endsWith(BankJsonDescriptor.OTHER_INDEX_FEXT))
          .map(Path::toFile)
          .collect(Collectors.toList());
    } catch (IOException e) {
      //LoggerCentral.warn("Unable to list additional indexes: "+e.toString());
      return;
    }
    // then, process additional indexes if any (bowtie, diamond, etc)
    for(File idxDirectory : files) {
      //Do we have a dedicated an index.properties file?
      String idxPath = idxDirectory.getAbsolutePath();
      File propFile = new File(Utils.terminatePath(idxPath)
          +BankJsonDescriptor.OTHER_INDEX_PROPS);
      if (propFile.exists()){
        Properties props = new Properties();
        try (FileReader fr = new FileReader(propFile)){
          props.load(fr);
          String idxKey = 
              props.getProperty(BankJsonDescriptor.OTHER_INDEX_PROP_KEY) + 
              " (" + 
              props.getProperty(BankJsonDescriptor.OTHER_INDEX_PROP_VER + ")"); 
          _otherIndex.put(idxKey, idxPath);
        } catch (Exception e) {
          //LOGGER.warn("Unable to read property file: "+propFile+": "+e.toString());
        }
      }
      //otherwise, use directory name has index key
      else {
        String fName = idxDirectory.getName();
        int idx = fName.lastIndexOf('.');
        _otherIndex.put(fName.substring(0, idx), idxPath);
      }
      
    }
  }
}
