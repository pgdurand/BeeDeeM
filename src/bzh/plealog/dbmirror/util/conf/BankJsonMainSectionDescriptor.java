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

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Serialization class for JSON bank descriptor.
 * 
 * @author Patrick G. Durand
 */
public class BankJsonMainSectionDescriptor {
  private String _name;
  private String _description;
  private BankJsonSizeSectionDescriptor _size;
  private List<String> _provider;
  private String _installDate;
  private String _release;
  private List<String> _type;
  private List<String> _omics;
  private String _owner;
  private Map<String, String> _index;
  
  public BankJsonMainSectionDescriptor() {
    
  }
  
  @JsonProperty("NAME")
  public String getName() {
    return _name;
  }
  public void setName(String name) {
    this._name = name;
  }

  @JsonProperty("DESCRIPTION")
  public String getDescription() {
    return _description;
  }
  public void setDescription(String description) {
    this._description = description;
  }

  @JsonProperty("SIZE")
  public BankJsonSizeSectionDescriptor getSize() {
    return _size;
  }
  public void setSize(BankJsonSizeSectionDescriptor size) {
    this._size = size;
  }

  @JsonProperty("PROVIDER")
  public List<String> getProvider() {
    return _provider;
  }
  public void setProvider(List<String> provider) {
    this._provider = provider;
  }

  @JsonProperty("INSTALL_DATE")
  public String getInstallDate() {
    return _installDate;
  }
  public void setInstallDate(String installDate) {
    this._installDate = installDate;
  }

  @JsonProperty("RELEASE")
  public String getRelease() {
    return _release;
  }
  public void setRelease(String release) {
    this._release = release;
  }

  @JsonProperty("TYPE")
  public List<String> getType() {
    return _type;
  }
  public void setType(List<String> type) {
    this._type = type;
  }

  @JsonProperty("OMICS")
  public List<String> getOmics() {
    return _omics;
  }
  public void setOmics(List<String> omics) {
    this._omics = omics;
  }
  
  @JsonProperty("OWNER")
  public String getOwner() {
    return _owner;
  }

  public void setOwner(String owner) {
    this._owner = owner;
  }

  @JsonProperty("INDEX")
  public Map<String, String> getIndex() {
    return _index;
  }
  public void setIndex(Map<String, String> index) {
    this._index = index;
  }

}
