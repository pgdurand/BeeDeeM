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
package bzh.plealog.dbmirror.indexer.eggnog;

/**
 * All super kingdoms managed by eggnog
 * 
 * @author Ludovic Antin
 * 
 */
public enum EggNogSuperKingdoms {

  all("1"), bacteria("2"), archea("2157"), eukaryota("2759");

  public String taxonId = "";

  EggNogSuperKingdoms(String taxonId) {
    this.taxonId = taxonId;
  }
}
