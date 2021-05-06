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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Serialization class for JSON bank descriptor.
 * 
 * @author Patrick G. Durand
 */
public class BankJsonSizeSectionDescriptor {
  private long _bytes;
  private int _sequences;

  public BankJsonSizeSectionDescriptor() {
    
  }
  @JsonProperty("ON_DISK")
  public long getBytes() {
    return _bytes;
  }
  public void setBytes(long bytes) {
    this._bytes = bytes;
  }

  @JsonProperty("SEQUENCES")
  public int getSequences() {
    return _sequences;
  }
  public void setSequences(int sequences) {
    this._sequences = sequences;
  }
}
