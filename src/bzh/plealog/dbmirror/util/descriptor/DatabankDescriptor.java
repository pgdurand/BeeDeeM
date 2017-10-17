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
package bzh.plealog.dbmirror.util.descriptor;

import java.io.File;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Properties;

import org.apache.commons.io.FileUtils;

import bzh.plealog.dbmirror.util.Utils;
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
  private String            _dbPath;
  private String            _code;
  private String            _description;
  private String            _type;
  private String            _nbSequence;
  private String            _diskSize;
  private String            _timeStamp;
  private String            _releaseStamp;
  private long              _diskSizeL;
  private boolean           hasAnnotation = false;
  
  private NumberFormat      numFormatter     = DecimalFormat
                                                 .getInstance(Locale.ENGLISH);

  public DatabankDescriptor(IdxDescriptor descriptor) {
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

    String directory = new File(_dbPath).getParent();
    Properties props = DBStampProperties.readDBStamp(directory);

    _timeStamp = props.getProperty(DBStampProperties.TIME_STAMP);
    _releaseStamp = props.getProperty(DBStampProperties.RELEASE_TIME_STAMP);
    
    if (_type.equals("D"))
      _nbSequence = numFormatter.format(Long.valueOf(props
          .getProperty(DBStampProperties.NB_ENTRIES)));
    else
      _nbSequence = numFormatter.format(Long.valueOf(props
          .getProperty(DBStampProperties.NB_SEQUENCES)));
    _diskSizeL = FileUtils.sizeOfDirectory(new File(directory));
    _diskSize = Utils.getBytes(_diskSizeL);
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
  
}
