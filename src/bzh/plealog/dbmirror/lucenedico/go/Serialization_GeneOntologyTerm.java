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
package bzh.plealog.dbmirror.lucenedico.go;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.commons.io.IOUtils;

/**
 * Serialize/Deserialize an object to/from a byte array
 * 
 * @author Patrick G. Durand
 */
public class Serialization_GeneOntologyTerm {

  /**
   * Create a byte array from an object which implement the class Serializable
   * 
   * @param ob
   *          the object to serialize
   * 
   * @return the object contained in a byte array
   */
  public static byte[] serialize(Object ob) {
    ByteArrayOutputStream out = null;
    ObjectOutputStream oos = null;
    byte[] data;
    try {
      out = new ByteArrayOutputStream();
      oos = new ObjectOutputStream(out);
      oos.writeObject(ob);
      data = out.toByteArray();
      out.flush();
      return data;
    } catch (IOException ioe) {
      ioe.printStackTrace();
    } finally {
      IOUtils.closeQuietly(oos);
      IOUtils.closeQuietly(out);
    }
    return null;
  }

  /**
   * Generate an object from a byte array passed in parameter
   * 
   * @param ob
   *          the bye array which contain the object.
   * 
   * @return the object rebuilt
   */
  public static Object deserialize(byte[] ob) {
    InputStream in = null;
    ObjectInputStream ois = null;
    try {
      in = new ByteArrayInputStream(ob);
      ois = new ObjectInputStream(in);
      Object o = ois.readObject();
      return o;
    } catch (IOException ioe) {
      ioe.printStackTrace();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } finally {
      IOUtils.closeQuietly(in);
      IOUtils.closeQuietly(ois);
    }

    return null;

  }
}
