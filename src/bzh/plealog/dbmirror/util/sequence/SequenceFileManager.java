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
package bzh.plealog.dbmirror.util.sequence;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JProgressBar;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;

import bzh.plealog.dbmirror.util.Utils;
import bzh.plealog.dbmirror.util.conf.DBMSConfigurator;
import bzh.plealog.dbmirror.util.descriptor.DatabankFormat;
import bzh.plealog.dbmirror.util.log.LoggerCentral;

/**
 * A SequenceFileManager reads a sequence file and write in the same file (after
 * renamed the input file) only sequences validated by the SequenceValidators <br>
 * 
 * See the execute() method
 * 
 * @author Ludovic Antin
 * 
 */
public class SequenceFileManager {

  public static final long         MAX_BUFFER_SIZE         = 5l * Utils.MEGA;

  private File                     sequenceFile;
  private File                     filteredFile;
  private DatabankFormat           databankFormat;
  private Log                      logger;
  private List<ISequenceValidator> validators;

  private long                     nbSequencesFound        = -1;
  private long                     nbSequencesDiscarded    = 0;
  private JProgressBar             progressBar             = null;
  private int                      ratioValueProgress      = 100;

  // the buffer where an entry is stored before writing in the ouput file
  private StringBuffer             entryBuffer             = new StringBuffer();
  // the tmp file where a huge entry is stored
  private File                     tmpEntryFile;
  private FileWriter               fwTmpEntryFile;
  // the boolean indicating that the end of an entry is stored in a file
  private boolean                  storeEntryInTmpFile     = false;

  // read and write
  private BufferedReader           reader                  = null;
  private BufferedWriter           writer                  = null;

  private boolean                  mustCreateAFilteredFile = true;

  // special validator because the result of execute() can be a file list
  // instead of a single filtered file
  private SequenceValidatorSubset  validatorSubset;

  boolean                          abortProcess            = false;

  private String                   tmpFileDirectory;
  
  /**
   * Tell the SequenceFileManager to abort the process even if the execute
   * method has not yet finished his job
   */
  public void abortProcess() {
    this.abortProcess = true;
  }

  /**
   * Creates a new SequenceFileManager
   * 
   * @param sequenceFilepath
   *          the sequence file must exists
   * @param databankFormat
   *          SeqIOUtils.UNKNOWN => SeqIOUtils.FASTQ
   * @param logger
   * @param progressBar
   * 
   * @throws IOException
   */
  public SequenceFileManager(String sequenceFilepath,
      DatabankFormat databankFormat, Log logger, JProgressBar progressBar)
      throws IOException {
    setTmpFileDirectory(DBMSConfigurator.TMP_FILTER_DIRECTORY);
    this.sequenceFile = new File(sequenceFilepath);
    this.databankFormat = databankFormat;
    this.progressBar = progressBar;

    if (!this.getSequenceFile().exists()) {
      throw new FileNotFoundException(sequenceFilepath + " does not exists.");
    }

    if (this.getDatabankFormat() == null) {
      throw new FileNotFoundException(sequenceFilepath
          + " does not correspond to databank format '"
          + databankFormat.getType() + "'.");
    }

    this.logger = logger;
    this.validators = new ArrayList<ISequenceValidator>();
  }

  /**
   * Creates a new SequenceFileManager
   * 
   * @param reader
   *          the buffered reader
   * @param writer
   *          the buffered writer
   * @param databankFormat
   *          SeqIOUtils.UNKNOWN => SeqIOUtils.FASTQ
   * @param logger
   * 
   * @throws IOException
   */
  public SequenceFileManager(BufferedReader reader, BufferedWriter writer,
      DatabankFormat databankFormat, Log logger) {
    setTmpFileDirectory(DBMSConfigurator.TMP_FILTER_DIRECTORY);
    this.databankFormat = databankFormat;
    this.logger = logger;
    this.validators = new ArrayList<ISequenceValidator>();

    // init reader
    this.reader = reader;

    // init writer
    this.writer = writer;
  }

  /**
   * Set a working directory. Default is DBMSConfigurator.TMP_FILTER_DIRECTORY.
   */
  public void setTmpFileDirectory(String tmpDir){
    tmpFileDirectory = tmpDir;
  }
  
  /**
   * Method who gives a number of sequences. USE only if this objects contains a
   * SequenceValidatorSubset
   * 
   * @param subset
   * 
   * @return the number of sequences found in the subset
   */
  public int getNbSequenceInSubset(File subset) {
    if (this.validatorSubset != null) {
      Integer result = this.validatorSubset.getNbSequences(subset);
      if (result == null) {
        return (int) this.nbSequencesFound;
      }
      return result;
    } else {
      return (int) this.nbSequencesFound;
    }
  }

  /**
	 *
	 */
  public boolean willReturnMultipleFiles() {
    return this.validatorSubset != null;
  }

  /**
   * is this object use to really create a tmp filtered file ? if not : don"t
   * need to store entries in the buffer before writing and set
   * mustCreateAFilteredFile to false <br>
   * default is true
   * 
   * @param mustCreateAFilteredFile
   */
  public void setMustCreateAFilteredFile(boolean mustCreateAFilteredFile) {
    this.mustCreateAFilteredFile = mustCreateAFilteredFile;
  }

  private void updateWithSequenceFile() throws IOException {
    // set the filtered file
    this.filteredFile = new File(tmpFileDirectory,
        "filtered_"+this.sequenceFile.getName());

    if (this.progressBar != null) {
      this.progressBar.setValue(0);
      while ((this.getSequenceFile().length() / this.ratioValueProgress) > Integer.MAX_VALUE) {
        this.ratioValueProgress = this.ratioValueProgress * 10;
      }
      // avoid too much setValue
      this.ratioValueProgress = this.ratioValueProgress * 10;
      this.progressBar
          .setMaximum((int) (this.getSequenceFile().length() / this.ratioValueProgress));
    }

    // init the tmp entry file
    String parent=this.sequenceFile.getParent();
    if (parent!=null) {
      tmpEntryFile = new File(parent, "tmp_entry_file.dat");
    }
    else {
      tmpEntryFile = new File("tmp_entry_file.dat");
    }
    tmpEntryFile.createNewFile();
    fwTmpEntryFile = new FileWriter(tmpEntryFile);

    // init reader
    reader = new BufferedReader(new InputStreamReader(new FileInputStream(
        this.sequenceFile), "UTF-8"));

    // init writer
    File parentFile=this.filteredFile.getParentFile();
    if (parentFile!=null && !parentFile.exists()) {
      parentFile.mkdirs();
    }
    if (this.filteredFile.exists()) {
      this.filteredFile.delete();
    }
    this.filteredFile.createNewFile();
    writer = new BufferedWriter(new FileWriter(this.filteredFile));
  }

  public File getSequenceFile() {
    return sequenceFile;
  }

  public DatabankFormat getDatabankFormat() {
    return databankFormat;
  }

  public void addValidator(ISequenceValidator validator) {
    validator.setDatabankFormat(this.databankFormat);
    this.validators.add(validator);
    validator.initialise(this.getSequenceFile());

    if ((validator instanceof SequenceValidatorSubset)
        && (((SequenceValidatorSubset) validator).isActive())) {
      this.validatorSubset = (SequenceValidatorSubset) validator;
    }
  }

  public List<ISequenceValidator> getValidators() {
    return validators;
  }

  /**
   * @return the number of sequences filtered by the previous execute() call
   */
  public long getNbSequencesFound() {
    return nbSequencesFound;
  }

  /**
   * @return the number of sequences discarded by the previous execute() call
   */
  public long getNbSequencesDiscarded() {
    return nbSequencesDiscarded;
  }

  /**
   * Write an entry in the fw.
   * 
   * @param fw
   * @throws IOException
   */
  private void writeEntry(BufferedWriter fw) throws IOException {

    if ((entryBuffer.length() > 0) || (!this.mustCreateAFilteredFile)) {
      // increment if there is something or if the filtered file must NOT be
      // created
      this.nbSequencesFound++;
    }

    if (entryBuffer.length() > 0) {
      fw.write(entryBuffer.toString());
    }

    if (storeEntryInTmpFile) // may be the end of the entry is in the tmp file
    {
      fwTmpEntryFile.close();
      fw.write(FileUtils.readFileToString(tmpEntryFile));
      tmpEntryFile.createNewFile();
      fwTmpEntryFile = new FileWriter(tmpEntryFile);
    }
  }

  /**
   * 
   * @return true if there is something to do : at least one validator is active
   */
  public boolean somethingToDo() {
    if (this.getDatabankFormat() == null) {
      if (this.logger != null) {
        LoggerCentral.warn(logger,
            "Databank format is null : filtering unavailable.");
      }
    }

    boolean result = false;
    for (int i = validators.size() - 1; i >= 0; i--) {
      if (validators.get(i).isActive()) {
        result = true;
      } else {
        validators.remove(i);
      }
    }
    return result;
  }

  /**
   * Main method : parse the sequence file and execute all validators<br>
   * 
   * The result will be written in a new file which path is :
   * mirror.path\tmp\<sequenceFileName>
   * 
   * @return the filtered file or subsets filetered files if a
   *         SequenceValidatorSubset has been added and is valid
   * @throws IOException
   */
  public List<File> execute() throws IOException {

    this.abortProcess = false;

    List<File> result = new ArrayList<File>();

    // nothing to do ?
    if (!this.somethingToDo()) {
      result.add(this.getSequenceFile());
      return result;
    }

    // should we create a filtered file
    if ((this.validators.size() == 1)
        && (this.validators.get(0) instanceof SequenceValidatorSubset)) {
      this.setMustCreateAFilteredFile(false);
    }

    if ((this.writer == null) && (this.sequenceFile != null)) {
      this.updateWithSequenceFile();
    }

    // line read in the sequence file
    StringBuilder line = null;
    String lineRead = "";
    // is the current entry valid ?
    boolean entryValidated = false;
    // is the current line the one before the real sequence (not id, desc,
    // annot...)
    boolean isLineBeforeSequence = false;
    // progress value
    long nbBytesRead = 0;
    int progressStep = 0;
    int nextStep = 0;
    if (this.progressBar != null) {
      progressStep = this.progressBar.getMaximum() / 100;
      nextStep = progressStep;
    }

    // do the job : check each line with validators
    // if the line is valid : it is stored in a StringBuffer but if the buffer
    // increase too wuch (MAX_BUFFER_SIZE)
    // the end of the entry is stored in a tmp file.
    // At the end, the entire entry (if it is valid) is written in the ouput
    // file (buffer + tmp file are read)
    try {

      // reset the number of filtered sequences
      this.nbSequencesFound = 0;
      // -1 because of the first one
      this.nbSequencesDiscarded = -1;
      while ((lineRead = reader.readLine()) != null) {
        line = new StringBuilder(lineRead);
        if (this.abortProcess) {
          break;
        }

        if (this.progressBar != null) {
          // the counting is approximative, but this is only for a progress bar
          // no need to use String.getBytes() which causes lack of time
          // in UTF8 a char (but not all) is stored with 1 or 2 bytes
          nbBytesRead += line.length() * 1.2;
          // nbBytesRead += line.getBytes("UTF-8").length;
          if (nbBytesRead > (nextStep)) {
            this.progressBar
                .setValue((int) (nbBytesRead / this.ratioValueProgress));
            nextStep += progressStep;
          }
        }

        if (line.toString().startsWith(this.databankFormat.getIdString())) // new
                                                                           // entry
                                                                           // found
        {
          // stop the previous entry
          for (ISequenceValidator validator : validators) {
            if (!validator.stopEntry()) {
              entryValidated = false;
            }
          }

          if (entryValidated) {
            this.writeEntry(writer);
          } else {
            this.nbSequencesDiscarded++;
          }

          // start the new entry
          entryValidated = true; // first set this entry ok !
          isLineBeforeSequence = false; // is is not the real sequence
          entryBuffer = new StringBuffer(); // reset the buffer
          storeEntryInTmpFile = false; // store next entry in the buffer
          // check the validators for the startEntry
          for (ISequenceValidator validator : validators) {
            if (!validator.startEntry()) {
              // the new entry is nok
              entryValidated = false;
              break;
            }
          }

        }

        if (entryValidated) // inside the entry only if everything is still ok
                            // for the current sequence
        {
          // was the previous line the one before the real sequence ?
          if (isLineBeforeSequence) {
            isLineBeforeSequence = false;

            // start the sequence
            for (ISequenceValidator validator : validators) {
              if (!validator.startSequence()) {
                entryValidated = false;
              }
            }

            if (!entryValidated) {
              continue; // read next line from the sequence file
            }
          }
          // sequence after this line ?
          else if (line.toString().startsWith(
              this.databankFormat.getBeginSequenceString())) {
            isLineBeforeSequence = true;
          }

          // is the line can be written in the output file ?
          for (ISequenceValidator validator : validators) {
            if (!validator.analyseLine(line)) {
              entryValidated = false;
            }
          }
          if (this.mustCreateAFilteredFile) {
            if (!entryValidated) {
              continue;// read next line from the sequence file
            } else if (StringUtils.isNotBlank(line.toString())) {
              // write in the buffer or in the tmp file ?
              if (storeEntryInTmpFile) {
                fwTmpEntryFile.write(line.toString());
                fwTmpEntryFile.write("\n");
              } else {
                // the line can be written in the buffer
                entryBuffer.append(line);
                entryBuffer.append("\n");
                if (entryBuffer.length() > MAX_BUFFER_SIZE) {
                  storeEntryInTmpFile = true;
                }
              }
            }
          }

        } // end : if entryValidated

      } // end : readline

      // stop the last entry
      for (ISequenceValidator validator : validators) {
        if (!validator.stopEntry()) {
          entryValidated = false;
          break;
        }
      }
      if (entryValidated) {
        this.writeEntry(writer);
      } else {
        this.nbSequencesDiscarded++;
      }

    } catch (Exception ex) {
      if (this.logger != null) {
        LoggerCentral.warn(logger, "Filter failed : " + ex.getMessage());
      }
    } finally {
      IOUtils.closeQuietly(reader);
      IOUtils.closeQuietly(writer);
      IOUtils.closeQuietly(fwTmpEntryFile);
      if (tmpEntryFile != null) {
        try {
          tmpEntryFile.delete();
        } catch (Exception ex) {
          if (this.logger != null) {
            LoggerCentral.info(logger,
                "Unable to delete '" + tmpEntryFile.getAbsolutePath() + "' : "
                    + ex.getMessage());
          }
        }
      }

      for (ISequenceValidator validator : validators) {
        validator.finish();
      }
    }

    if (this.validatorSubset != null) {
      return this.validatorSubset.getCreatedSubsets();
    }
    result.add(this.filteredFile);
    return result;

  }

  /**
   * 
   * @return the string representing this object for a unit indexing task
   *         parameters
   */
  public String toParametersForUnitTask() {
    StringBuffer result = new StringBuffer();

    for (ISequenceValidator validator : this.validators) {
      result.append(validator.toParametersForUnitTask());
      result.append(";");
    }
    return result.toString();
  }

}
