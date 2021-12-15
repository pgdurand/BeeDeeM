/* Copyright (C) 2007-2017 Patrick Durand
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
package test.unit;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import bzh.plealog.dbmirror.lucenedico.DicoStorageSystem;
import bzh.plealog.dbmirror.lucenedico.DicoTerm;
import bzh.plealog.dbmirror.lucenedico.DicoTermQuerySystem;
import bzh.plealog.dbmirror.lucenedico.Dicos;
import bzh.plealog.dbmirror.lucenedico.tax.TaxonomyRank;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptorUtils;
import bzh.plealog.dbmirror.util.sequence.TaxonMatcherHelper;

public class NcbiTaxonomyIndexerTest {

	private static DicoStorageSystem		ncbiTaxonDico;
	private static DicoTermQuerySystem		dicoSystem;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		UtilsTest.configureApp();

		UtilsTest.cleanInstalledDatabanks("d");
		UtilsTest.start();
		if (!TaxonMatcherHelper.isNCBITaxonomyInstalled()) {
			RunningMirrorPanelTest.installLocalNCBITaxonomy();
		}
		UtilsTest.stop("install");
		UtilsTest.displayDurations("");

		dicoSystem = DicoTermQuerySystem.getDicoTermQuerySystem(DBDescriptorUtils.getDBMirrorConfig(DBMSAbstractConfig.getLocalMirrorConfFile()));
		ncbiTaxonDico = dicoSystem.getStorage(Dicos.NCBI_TAXONOMY);

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		DicoTermQuerySystem.closeDicoTermQuerySystem();
	}

	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
	}

	@Test
	public void testMatchingTerm() {
		String term_name = "Bacillus sp. HMB-6315";
		/*
		 * 654542,Vibrio sp. BBT01 667723,Influenza A virus
		 * (A/Washington/29/2009(H1N1)) 681283,Leucoagaricus sp. ECV-2009b
		 * 699400,Bacillus sp. HMB-6315 721777,Vibrio sp. SL-15
		 */
		String id = "";
		for (int i = 0; i < 20; i++) {
			UtilsTest.start();
			id = ncbiTaxonDico.getID(term_name);
			UtilsTest.stop("search");
		}
		UtilsTest.displayDurations("");
		Assert.assertEquals("n699400", id);
	}

	private String findPath(String id) {
		StringBuffer result = new StringBuffer();
		DicoTerm[] terms = ncbiTaxonDico.getTerms(id, "o1");
		for (DicoTerm term : terms) {
			result.append(term.getId() + ";");
		}
		return result.toString();
	}

	private String findTerms(String id) {
		StringBuffer result = new StringBuffer();
		DicoTerm[] terms = ncbiTaxonDico.getTerms(id, "o1");
		String[] ids = new String[terms.length];
		int i = 0;
		for (DicoTerm term : terms) {
			ids[i] = term.getId();
			i++;
		}
		terms = ncbiTaxonDico.getTerms(ids);
		for (DicoTerm term : terms) {
			result.append(term.getDataField() + ";");
		}
		return result.toString();
	}

	@Test
	public void testFindPath() {
		Assert.assertEquals(this.findPath("o2"), "o1;o131567;o2;");
		Assert.assertEquals(this.findPath("o6"), "o1;o131567;o2;o1224;o28211;o356;o335928;o6;");
		Assert.assertEquals(this.findPath("o7"), "o1;o131567;o2;o1224;o28211;o356;o335928;o6;o7;");
		Assert.assertEquals(this.findPath("o9"), "o1;o131567;o2;o1224;o1236;o91347;o543;o32199;o9;");
		Assert.assertEquals(this.findPath("o10"), "o1;o131567;o2;o1224;o1236;o72274;o135621;o10;");
		Assert.assertEquals(this.findPath("o11"), "o1;o131567;o2;o201174;o1760;o85006;o85016;o1707;o11;");
		Assert.assertEquals(this.findPath("o13"), "o1;o131567;o2;o68297;o203486;o203487;o203488;o13;");
		Assert.assertEquals(this.findPath("o14"), "o1;o131567;o2;o68297;o203486;o203487;o203488;o13;o14;");
		Assert.assertEquals(this.findPath("o16"), "o1;o131567;o2;o1224;o28216;o206350;o32011;o16;");
		Assert.assertEquals(this.findPath("o19"), "o1;o131567;o2;o1224;o68525;o28221;o69541;o213423;o18;o19;");
		Assert.assertEquals(
				this.findPath("o9606"),
				"o1;o131567;o2759;o33154;o33208;o6072;o33213;o33511;o7711;o89593;o7742;o7776;o117570;o117571;o8287;o1338369;o32523;o32524;o40674;o32525;o9347;o1437010;o314146;o9443;o376913;o314293;o9526;o314295;o9604;o207598;o9605;o9606;");
	}

	@Test
	public void testFindTerms() {

		Assert.assertEquals(this.findTerms("o2"), "1|root;o1	|	no rank;o131567	|	superkingdom;");
		Assert.assertEquals(this.findTerms("o6"), "1|root;o1	|	no rank;o131567	|	superkingdom;o2	|	phylum;o1224	|	class;o28211	|	order;o356	|	family;o335928	|	genus;");
		Assert.assertEquals(this.findTerms("o19"),
				"1|root;o1	|	no rank;o131567	|	superkingdom;o2	|	phylum;o1224	|	subphylum;o68525	|	class;o28221	|	order;o69541	|	family;o213423	|	genus;o18	|	species;");
		Assert.assertTrue(this.findTerms("o654542").endsWith("o662	|	species;"));
		Assert.assertTrue(this.findTerms("o667723").endsWith("o114727	|	no rank;"));
		Assert.assertTrue(this.findTerms("o681283").endsWith("o34433	|	species;"));
		Assert.assertTrue(this.findTerms("o699400").endsWith("o1386	|	species;"));
		Assert.assertTrue(this.findTerms("o721777").endsWith("o717610	|	species;"));

	}

	@Test
	public void testQuerySystem() {

		// full path (ids)
		String value = dicoSystem.getTaxPathIds("9606", false, false);
		Assert.assertEquals(
				"n1;n131567;n2759;n33154;n33208;n6072;n33213;n33511;n7711;n89593;n7742;n7776;n117570;n117571;n8287;n1338369;n32523;n32524;n40674;n32525;n9347;n1437010;n314146;n9443;n376913;n314293;n9526;n314295;n9604;n207598;n9605;",
				value);

		// simplify path (names)
		value = dicoSystem.getTaxPath("9606", true);
		Assert.assertEquals("root;d__Eukaryota;k__Metazoa;p__Chordata;c__Mammalia;o__Primates;f__Hominidae;g__Homo;", value);

		// simplify path (ids)
		value = dicoSystem.getTaxPathIds("9606", true);
		Assert.assertEquals("n1;n2759;n33208;n7711;n40674;n9443;n9604;n9605;", value);

		value += "n9606";
		ArrayList<String> expectedValues = new ArrayList<String>();
		expectedValues.add("1:root:root");
		expectedValues.add("2759:Eukaryota:superkingdom");
		expectedValues.add("33208:Metazoa:kingdom");
		expectedValues.add("7711:Chordata:phylum");
		expectedValues.add("40674:Mammalia:class");
		expectedValues.add("9443:Primates:order");
		expectedValues.add("9604:Hominidae:family");
		expectedValues.add("9605:Homo:genus");
		expectedValues.add("9606:Homo sapiens:species");
		int i = 0;

		StringTokenizer tokenizer = new StringTokenizer(value, ";");
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			value = token.substring(1);
			value += ":" + dicoSystem.getTerm(Dicos.NCBI_TAXONOMY, value).getDataField() + ":" + dicoSystem.getRealTaxRank(value);
			Assert.assertEquals(expectedValues.get(i), value);
			i++;
		}
		value = dicoSystem.getTaxPath("112040");
		Assert.assertEquals("root;d__Bacteria;p__Bacteroidetes;c__Flavobacteriia;o__Flavobacteriales;f__Flavobacteriaceae;", value);
		value = dicoSystem.getRealTaxRank("112040");
		Assert.assertEquals("genus", value);
		TaxonomyRank valueRank = dicoSystem.getSimplifiedTaxRank("112040");
		Assert.assertEquals(TaxonomyRank.GENUS, valueRank);

		value = dicoSystem.getTaxPathIds("6633");
		Assert.assertNull(value);

		value = dicoSystem.getTerm(Dicos.NCBI_TAXONOMY, "176279").getDataField();
		Assert.assertEquals("Staphylococcus epidermidis RP62A", value);

		value = dicoSystem.getRealTaxRank("176279");
		Assert.assertEquals("no rank", value);

		value = dicoSystem.getTaxPath("6633");
		Assert.assertNull(value);

		TaxonMatcherHelper matcher = new TaxonMatcherHelper();
		matcher.setDicoTermQuerySystem(dicoSystem);
		matcher.setTaxonomyFilter("112040", null);
		boolean results[] = new boolean[3];
		matcher.isSeqTaxonvalid(results, "taxon:63186");
		Assert.assertTrue(results[0]);
		Assert.assertTrue(results[1]);
	}

	@Test
	public void testApproxSearch() {

		List<DicoTerm> terms = ncbiTaxonDico.getApprochingTerms("melittangium", 10000);
		Assert.assertTrue(this.contains(terms, "Melittangium"));
		Assert.assertTrue(this.contains(terms, "44:"));
		Assert.assertTrue(this.contains(terms, "45:"));
		Assert.assertTrue(this.contains(terms, "Melittangium boletus"));
		Assert.assertTrue(this.contains(terms, "Rhododendron meliphagidum"));
		Assert.assertTrue(this.contains(terms, "83452:"));

		terms = ncbiTaxonDico.getApprochingTerms("molitt*ngium", 10000);
		Assert.assertTrue(this.contains(terms, "Melittangium"));
		Assert.assertTrue(this.contains(terms, "44:"));
		Assert.assertTrue(this.contains(terms, "45:"));
		Assert.assertTrue(this.contains(terms, "Melittangium boletus"));
		Assert.assertTrue(this.contains(terms, "Rhododendron meliphagidum"));
		Assert.assertTrue(this.contains(terms, "83452:"));

		terms = ncbiTaxonDico.getApprochingTerms("moli[\\tt*ngium", 10000);
		Assert.assertTrue(this.contains(terms, "Melittangium"));
		Assert.assertTrue(this.contains(terms, "44:"));
		Assert.assertTrue(this.contains(terms, "45:"));
		Assert.assertTrue(this.contains(terms, "Melittangium boletus"));
		Assert.assertTrue(this.contains(terms, "Rhododendron meliphagidum"));
		Assert.assertTrue(this.contains(terms, "83452:"));

		terms = ncbiTaxonDico.getApprochingTerms("*sapiens", 10000);
		Assert.assertTrue(this.contains(terms, "Homo sapiens"));
		Assert.assertTrue(this.contains(terms, "9606:"));
		Assert.assertTrue(this.contains(terms, "45:"));
		Assert.assertTrue(this.contains(terms, "Homo sapiens ssp. Denisova"));
		Assert.assertTrue(this.contains(terms, "1383439:"));
		Assert.assertTrue(this.contains(terms, "Homo sapiens neanderthalensis"));

		terms = ncbiTaxonDico.getApprochingTerms("*sapiens", 10000);
		Assert.assertTrue(this.contains(terms, "Homo sapiens"));
		Assert.assertTrue(this.contains(terms, "9606:"));
		Assert.assertTrue(this.contains(terms, "45:"));
		Assert.assertTrue(this.contains(terms, "Homo sapiens ssp. Denisova"));
		Assert.assertTrue(this.contains(terms, "1383439:"));
		Assert.assertTrue(this.contains(terms, "Homo sapiens neanderthalensis"));

		terms = ncbiTaxonDico.getApprochingTerms("Homo sapiens", 10000);
		Assert.assertTrue(this.contains(terms, "Homo sapiens"));
		Assert.assertTrue(this.contains(terms, "9606:"));
		Assert.assertTrue(this.contains(terms, "Homo"));
		Assert.assertTrue(this.contains(terms, "9605:"));
		Assert.assertTrue(this.contains(terms, "45:"));
		Assert.assertTrue(this.contains(terms, "Homo sapiens ssp. Denisova"));
		Assert.assertTrue(this.contains(terms, "1383439:"));
		Assert.assertTrue(this.contains(terms, "Homo sapiens neanderthalensis"));

		terms = ncbiTaxonDico.getApprochingTerms("10090", 50);
		Assert.assertFalse(this.contains(terms, "o10090"));
	}

	private boolean contains(List<DicoTerm> terms, String idOrName) {
		for (DicoTerm term : terms) {
			if (term.toString().toLowerCase().contains(idOrName.toLowerCase())) {
				return true;
			}
		}
		return false;
	}
}
