# *BeeDeeM*: the Bioinformatics Databank Manager System 

[![License AGPL](https://img.shields.io/badge/license-Affero%20GPL%203.0-blue.svg)](https://www.gnu.org/licenses/agpl-3.0.txt)  [![Build Status](https://travis-ci.org/pgdurand/BeeDeeM.svg?branch=master)](https://travis-ci.org/pgdurand/BeeDeeM)  [![](https://tokei.rs/b1/github/pgdurand/BeeDeeM?category=code)](https://github.com/pgdurand/BeeDeeM) [![](https://img.shields.io/badge/platform-Java--1.8+-yellow.svg)](http://www.oracle.com/technetwork/java/javase/downloads/index.html) [![](https://img.shields.io/badge/run_on-Linux--macOS--Windows-yellowgreen.svg)]() 


*BeeDeeM* is a general purpose **B**ioinformatics **D**atabank **M**anager. 

It provides a suite of command-line and UI softwares to install and use major sequence databanks and biological classifications. It is the ideal companion of the [NCBI BLAST suite of softwares](https://pgdurand.gitbooks.io/beedeem/test_install.html#run-a-blast-search).

## Main features

*BeeDeeM* automatically performs:

* the download of the database files from remote sites \(via FTP, Aspera\),
* the decompression of the files \(gzip files\),
* the un-archiving of the files \(tar files\),
* the conversion of native sequence banks \(e.g. Genbank\) to FASTA files,
* the preparation of databases in BLAST format from native sequence bank formats,
* the indexing of Genbank, Refseq, Embl, Genpept, Swissprot, TrEmbl, Fasta, Silva and BOLD files allowing their efficient querying by way of sequence identifiers,
* the indexing of sequence features and ontologies data (NCBI Taxonomy, Gene Ontology, Enzyme Commission and Intepro domains),
* the preparation of taxonomic subsets out of annotated sequence banks,
* the filtering of sequence banks with user-defined constraints.

[More](https://pgdurand.gitbooks.io/beedeem/).

## Main tools

*BeeDeeM* provides a toolchain made of:

* a **command-line tool to automate databanks installation**
* a **UI front-end to do the same in a more friendly way** (see below)
* a **command-line tool to annotate BLAST results**
* a **command-line to query databanks using sequence IDs**

[More](https://pgdurand.gitbooks.io/beedeem/).

### Use BeeDeeM from the command line

Here is an example of a script to start Genbank_CoreNucleotide installation on Ifremer's [DATARMOR supercomputer](https://www.top500.org/system/178981):

```
#!/usr/bin/env bash
#PBS -q web
#PBS -l mem=64gb
#PBS -l ncpus=8
#PBS -l walltime=72:00:00

# Release of BeeDeeM to use
BDM_HOME=$SOFT/bioinfo/beedeem
BDM_VER=latest

# Load BeeDeeM environment
module load java/1.8.0_121

# Configure BeeDeeM install tool
TASK_ARGS="-task download"
GLOBAL_ARGS="--ftp-delay 5000 --ftp-retry 3 --task-delay 1000"
MAILING_ARGS="--mail-smtp-host xxx --sender-mail yyy --recipient-mail zzz"

# prefix of '.dsc' file that must exist in $BDM_HOME/conf/descriptor
DESCRIPTOR="Genbank_CoreNucleotide"
export KL_LOG_FILE=${DESCRIPTOR}.log
$BDM_HOME/$BDM_VER/install.sh \
   ${TASK_ARGS} ${GLOBAL_ARGS} ${MAILING_ARGS} \
   -desc ${DESCRIPTOR} \
   >& "$HOME/beedeem/logs/${DESCRIPTOR}-pbs.out"
```

You can easily automate bank installation using such BeeDeeM based scripts with Unix cron scheduler.

### Use BeeDeeM UI

Here is a screenshot of BeeDeeM user friendly interface:

![UiManager](doc/dbms_ui.png)

## Practical use cases

Among others, these databanks can be used to:

* prepare and maintain up-to-date local copy of usefull data
* run BLAST sequence comparison jobs
* annotate BLAST results with sequence features and ontologies

## Companion tools

*BeeDeeM* features and data are accessible from:

* [BioDocument Viewer](https://github.com/pgdurand/BioDocumentViewer)
* [BLAST Viewer](https://github.com/pgdurand/BlastViewer)
* [BLAST Filter Tool](https://github.com/pgdurand/BLAST-Filter-Tool)
* [Plealog Bioinformatics Core API](https://github.com/pgdurand/Bioinformatics-Core-API)


[This manual](https://pgdurand.gitbooks.io/beedeem/) explains how to install, configure and use *BeeDeeM*.

## Requirements

Use a [Java Virtual Machine](http://www.oracle.com/technetwork/java/javase/downloads/index.html) 1.8 (or above) from Oracle. 

*Not tested with any other JVM providers but Oracle... so there is no guarantee that the software will work as expected if not using Oracle's JVM.* [More](https://pgdurand.gitbooks.io/beedeem/) about *BeeDeeM* requirements.

## Software installation, use and configuration

* [BeeDeeM manual on GitBook](https://pgdurand.gitbooks.io/beedeem/)
* BeeDeeM manual on GitHub: [Introduction](https://github.com/pgdurand/BeeDeeM-User-Manual/blob/master/README.md) and [Table of Content](https://github.com/pgdurand/BeeDeeM-User-Manual/blob/master/SUMMARY.md).

## License and dependencies

*BeeDeeM* itself is released under the GNU Affero General Public License, Version 3.0. [AGPL](https://www.gnu.org/licenses/agpl-3.0.txt)

It depends on several thrid-party libraries as stated in the NOTICE.txt file provided with this project.

--
(c) 2007-2020 - Patrick G. Durand and Ludovic Antin
