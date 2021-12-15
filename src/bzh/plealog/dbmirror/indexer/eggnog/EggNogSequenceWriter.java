/* Copyright (C) 2007-2021 Ludovic Antin
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.mutable.MutableInt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import bzh.plealog.dbmirror.indexer.SwissProtParser;
import bzh.plealog.dbmirror.lucenedico.DicoTermQuerySystem;
import bzh.plealog.dbmirror.lucenedico.DicoUtils;
import bzh.plealog.dbmirror.lucenedico.Dicos;
import bzh.plealog.dbmirror.reader.DBUtils;
import bzh.plealog.dbmirror.util.Formatters;
import bzh.plealog.dbmirror.util.conf.DBMSAbstractConfig;
import bzh.plealog.dbmirror.util.descriptor.DBDescriptorUtils;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * Class used to convert a fasta eggnog sequence to the uniprot format (with
 * annotations)
 * 
 * @author Ludovic Antin
 * 
 */
public class EggNogSequenceWriter {

  private static final Logger           LOGGER              = LogManager.getLogger(DBMSAbstractConfig.KDMS_ROOTLOG_CATEGORY
                                                                 + ".EggNogSequenceWriter");

  // ID 1A1C_VIGRR Reviewed; 368 AA.
  private static final MessageFormat mfId                = new MessageFormat(
                                                             SwissProtParser.KEYWORD_ID
                                                                 + "   {0}              Reviewed;         {1} "
                                                                 + SwissProtParser.KEYWORD_NB_LETTERS
                                                                 + ".");
  // AC Q01912;
  private static final MessageFormat mfAccession         = new MessageFormat(
                                                             SwissProtParser.KEYWORD_ACCESSION
                                                                 + "   {0};");

  // OS Vigna radiata var. radiata (Mung bean) (Phaseolus aureus).
  private static final MessageFormat mfOrganism          = new MessageFormat(
                                                             SwissProtParser.KEYWORD_ORGANISM
                                                                 + "   {0}.");

  // OX NCBI_TaxID=3916;
  private static final MessageFormat mfOrganismId        = new MessageFormat(
                                                             SwissProtParser.KEYWORD_ORGANISM_ID
                                                                 + "   NCBI_TaxID={0};");

  // FT DOMAIN 1 61 db_xref=NOG:COG2220.
  private static final MessageFormat mfFeature           = new MessageFormat(
                                                             SwissProtParser.KEYWORD_FEATURE_TABLE
                                                                 + "   DOMAIN        {0}     {1}       "
                                                                 + DBUtils.DB_XREF_KEY
                                                                 + "="
                                                                 + Dicos.EGGNOG.xrefId
                                                                 + ":{2}");

  // SQ SEQUENCE 368 AA;
  private static final MessageFormat mfStartSequence     = new MessageFormat(
                                                             SwissProtParser.KEYWORD_START_SEQUENCE
                                                                 + "   SEQUENCE   {0} "
                                                                 + SwissProtParser.KEYWORD_NB_LETTERS
                                                                 + ";");
  // where will be write the sequences
  private File                       resultFile          = null;

  // the super kingdom accepted by this writer formatted as a tax path element
  private String                     superKingdomTaxonId = null;

  // to write into the result file
  private BufferedWriter             writer              = null;

  private DicoTermQuerySystem        dicoSearcher        = DicoTermQuerySystem
                                                             .getDicoTermQuerySystem(DBDescriptorUtils
                                                                 .getLocalDBMirrorConfig());

  public EggNogSequenceWriter(File resultFile, String superKingdomTaxonId) {
    this.resultFile = resultFile;
    this.superKingdomTaxonId = DicoUtils.TAX_ID_NAME_PREFIX
        + superKingdomTaxonId + ";";
    if (this.resultFile.exists()) {
      FileUtils.deleteQuietly(this.resultFile);
    }
  }

  /**
   * Write a eggnog sequence into the result file in uniprot format
   * 
   * @param eggNogSequenceHeader
   * @param sequenceLetters
   * @throws IOException
   */
  public void write(String eggNogSequenceHeader, String sequenceLetters)
      throws Exception {
    if (this.writer == null) {
      this.writer = new BufferedWriter(new FileWriter(this.resultFile));
    }

    // get taxon data
    this.extractTaxonData(eggNogSequenceHeader);

    MutableInt nbLetters = new MutableInt();
    // first format the sequence to get the number of letters
    String sequenceFormatted = Formatters.formatInNbColumns(60,
        sequenceLetters, "     ", nbLetters);

    writer.write(mfId.format(new Object[] { eggNogSequenceHeader,
        nbLetters.toString() }));
    writer.write("\n");
    writer.write(mfAccession.format(new Object[] { eggNogSequenceHeader }));
    writer.write("\n");
    writer.write(mfOrganism
        .format(new Object[] { EggNogSequenceWriter.currentTaxonName }));
    writer.write("\n");
    writer.write(mfOrganismId
        .format(new Object[] { EggNogSequenceWriter.currentTaxonId }));
    writer.write("\n");

    // write the feature table
    int size = EggNogSequenceWriter.currentNogs.size();
    // create an arraylist of EggNogEntry in order to sort it
    ArrayList<EggNogEntry> entries = new ArrayList<EggNogEntry>();
    for (int i = size - 1; i >= 0; i--) {
      entries.add(new EggNogEntry(EggNogSequenceWriter.currentNogs.get(i)));
    }
    Collections.sort(entries);

    for (EggNogEntry entry : entries) {
      writer.write(mfFeature.format(new Object[] {
          entry.getStartPos().toString(), entry.getEndPos().toString(),
          entry.getNogName() }));
      writer.write("\n");
    }

    writer.write(mfStartSequence.format(new Object[] { nbLetters.toString() }));
    writer.write("\n");
    writer.write(sequenceFormatted);
    writer.write("\n");
    writer.write(SwissProtParser.KEYWORD_END_ENTRY);
    writer.write("\n");
  }

  /**
   * @param taxonPath
   * 
   * @return true if the taxon path contains the super kingdom taxon id handled
   *         by this instance
   */
  public boolean accept(String taxonPath) {
    return taxonPath.contains(this.superKingdomTaxonId);
  }

  public void close() {
    IOUtils.closeQuietly(this.writer);
  }

  // Static data used by other classes but filled by one instance of this class
  private static String       currentSequenceHeader = "";
  private static List<String> currentNogs           = new ArrayList<String>();
  private static String       currentTaxonId        = "00000";
  private static String       currentTaxonName      = "";
  private static int          nbSequenceForTaxon    = 0;
  private static int          totalSequences        = 0;

  /**
   * Because the egg nog fasta sequences file contains sequences grouped by
   * taxon id, it is possible to retrieve taxon data once for a sequences group
   * 
   * @param eggNogSequenceHeader
   * @throws Exception
   */
  private void extractTaxonData(String eggNogSequenceHeader) throws Exception {

    synchronized (EggNogSequenceWriter.currentNogs) {

      if (!EggNogSequenceWriter.currentSequenceHeader
          .equals(eggNogSequenceHeader)) {
        nbSequenceForTaxon++;
        totalSequences++;
        EggNogSequenceWriter.currentSequenceHeader = eggNogSequenceHeader;

        EggNogSequenceWriter.currentNogs = EggNogPreparator.reverseIndex
            .getById(eggNogSequenceHeader);

        if (!eggNogSequenceHeader
            .startsWith(EggNogSequenceWriter.currentTaxonId)) {
          LoggerCentral.info(LOGGER, "Taxon '"
              + EggNogSequenceWriter.currentTaxonId + "' parsed : "
              + nbSequenceForTaxon + " sequences. ("
              + NumberFormat.getInstance().format(totalSequences) + ")");
          nbSequenceForTaxon = 0;
          EggNogSequenceWriter.currentTaxonId = eggNogSequenceHeader.substring(
              0, eggNogSequenceHeader.indexOf('_'));
          EggNogSequenceWriter.currentTaxonName = Dicos.NCBI_TAXONOMY
              .getData(this.dicoSearcher.getTerm(Dicos.NCBI_TAXONOMY,
                  EggNogSequenceWriter.currentTaxonId));
        }
      }
    }
  }

}
