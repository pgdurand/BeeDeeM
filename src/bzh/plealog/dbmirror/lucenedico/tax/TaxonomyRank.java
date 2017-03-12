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
package bzh.plealog.dbmirror.lucenedico.tax;

import org.apache.commons.lang.StringUtils;

public enum TaxonomyRank {
	//rank number, simplified name, rank code, list of synonyms
	LIFE(    1, "life"    , "", "life", "root"),
	DOMAIN(  2, "domain"  , "d__", "domain", "superkingdom"),
	KINGDOM( 3, "kingdom" , "k__", "kingdom"),
	PHYLUM(  4, "phylum"  , "p__", "phylum", "superphylum", "subphylum"),
	CLASS(   5, "class"   , "c__", "class", "superclass", "subclass", "infraclass"),
	ORDER(   6, "order"   , "o__", "order", "superorder", "suborder", "parvorder", "infraorder"),
	FAMILY(  7, "family"  , "f__", "family", "superfamily", "subfamily"),
	GENUS(   8, "genus"   , "g__", "genus", "subgenus"),
	SPECIES( 9, "species" , "s__", "species", "species group", "species subgroup", "subspecies", "varietas", "forma");

	private int         level;
	private String  	levelName;
	private String  	levelCode;
	private String[]	names;

	TaxonomyRank(int level, String levelName, String levelCode, String... name) {
		this.level = level;
		this.levelName = levelName;
		this.levelCode = levelCode;
		this.names = name;
	}

	public String getName() {
		return this.levelName;
	}

	public static boolean contains(String s) {
		return contains(s, false);
	}

	public static boolean contains(String s, boolean onlyLevelName) {

		for (TaxonomyRank c : TaxonomyRank.values()) {
			if (c.equals(s, onlyLevelName)) {
				return true;
			}
		}

		return false;
	}

	public boolean equals(String rank) {
		return equals(rank, false);
	}

	public boolean equals(String rank, boolean onlySimplifiedLevelName) {

		if (this.levelName.toLowerCase().equals(rank.toLowerCase())) {
			return true;
		}

		if (!onlySimplifiedLevelName) {
			for (String name : this.names)
				if (name.toLowerCase().equals(rank.toLowerCase())) {
					return true;
				}
		}

		return false;
	}

	public int getLevel(){
		return level;
	}

	public String getLevelCode(){
		return levelCode;
	}
	public static TaxonomyRank getTaxonomyRank(String rank) {
		return getTaxonomyRank(rank, false);
	}

	public static TaxonomyRank getTaxonomyRank(String rank, boolean onlyLevelName) {
		for (TaxonomyRank c : TaxonomyRank.values()) {
			if (c.equals(rank, onlyLevelName)) {
				return c;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return StringUtils.capitalize(getName());
	}
}
