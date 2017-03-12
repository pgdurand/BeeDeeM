/* Copyright (C) 2007-2017 Ludovic Antin
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
package bzh.plealog.dbmirror.util.lucene;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.commons.io.IOUtils;

/**
 * Default implementation of the IObjectSerializer : use the standard java
 * serialization system
 * 
 * @author Ludovic Antin
 */
public class DefaultSerializer implements IObjectSerializer {

  @SuppressWarnings("unchecked")
  @Override
  public <T> T readObject(byte[] result, Class<T> objectClass) throws Exception {
    InputStream in = null;
    ObjectInputStream ois = null;
    try {
      in = new ByteArrayInputStream(result);
      ois = new ObjectInputStream(in);
      return (T) ois.readObject();
    } finally {
      IOUtils.closeQuietly(ois);
      IOUtils.closeQuietly(in);
    }
  }

  @Override
  public <T> byte[] writeObject(Object result, Class<T> objectClass)
      throws IOException {
    ByteArrayOutputStream out = null;
    ObjectOutputStream oos = null;
    try {
      out = new ByteArrayOutputStream();
      oos = new ObjectOutputStream(out);
      oos.writeObject(result);
      oos.flush();
      return out.toByteArray();
    } finally {
      IOUtils.closeQuietly(oos);
      IOUtils.closeQuietly(out);
    }
  }

}
