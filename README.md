# *BeeDeeM*: the Bioinformatics Databank Manager System 

![build](https://github.com/pgdurand/BeeDeeM/actions/workflows/ant.yml/badge.svg) [![License AGPL](https://img.shields.io/badge/license-Affero%20GPL%203.0-blue.svg)](https://www.gnu.org/licenses/agpl-3.0.txt) [![](https://img.shields.io/badge/platform-Java--1.8+-yellow.svg)](http://www.oracle.com/technetwork/java/javase/downloads/index.html) [![](https://img.shields.io/badge/run_on-Linux--macOS--Windows-yellowgreen.svg)]() 

[![](https://img.shields.io/badge/bio.tools-BeeDeeM-orange.svg)](https://bio.tools/beedeem) [![Anaconda-Server Badge](https://anaconda.org/sebimer/beedeem/badges/version.svg)](https://anaconda.org/sebimer/beedeem) [![](https://img.shields.io/badge/hub.docker-BeeDeeM-blue.svg)](https://hub.docker.com/repository/docker/sebimer/beedeem/general) [![](https://img.shields.io/badge/singularity-BeeDeeM-blue.svg)](https://data-dataref.ifremer.fr/bioinfo/ifremer/sebimer/tools/ORSON/)

## About

*BeeDeeM* is a general purpose **B**ioinformatics **D**atabank **M**anager. 

It provides a suite of command-line and UI softwares to manage (download, unarchive, index, install) and enable the easy use of major sequence databanks and biological classifications. 

## Main features

*BeeDeeM* automatically performs:

* the download of the database files from remote sites \(via FTP, HTTP or Aspera\),
* the decompression of the files \(gzip files\),
* the un-archiving of the files \(tar files\),
* the conversion of native sequence banks \(e.g. Genbank\) to FASTA files,
* the preparation of databases in BLAST format from native sequence bank formats,
° the preparation of other indexes such as Diamond, Bowtie, Hisat, etc.
* the indexing of Genbank, Refseq, Embl, Genpept, Swissprot, TrEmbl and Fasta files allowing their efficient querying by way of sequence identifiers,
* the indexing of sequence features and ontologies data (NCBI Taxonomy, Gene Ontology, Enzyme Commission, Intepro domains and PFAM domains),
* the preparation of taxonomic subsets out of annotated sequence banks,
* the filtering of sequence banks with user-defined constraints.

_Task execution extension:_

* Any kind of pre- and post-processing of data can be done using external scripts
* Such scripts can be executed on the host computer (local mode) or though SGE, PBS or SLURM scheduler (cluster mode)
* Task executions are controlled by configuration files; _e.g._ to specify software ressources (RAM, CPU, walltime), access to softwares (direct execution or through Conda), _etc._

_Index creation extension:_

* Using the task execution engine, additional index can be quite easily created in a fully automated way (e.g. Diamond, Bowtie, _etc._)&#x20;

More: read the [user manual](https://pgdurand.gitbooks.io/beedeem/)!

It is the ideal companion of sequence comparison tools (e.g. [BLAST](https://pgdurand.gitbooks.io/beedeem/test_install.html#run-a-blast-search), [PLAST](https://plast.inria.fr/), [Diamond](https://github.com/bbuchfink/diamond/wiki)), as well as tools such as [ORSON](https://gitlab.ifremer.fr/bioinfo/workflows/orson) annotation pipeline, [BLAST Viewer](https://github.com/pgdurand/BlastViewer) platform and [Galaxy platform](https://galaxyproject.org/).

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
#PBS -l mem=4gb
#PBS -l ncpus=8
#PBS -l walltime=72:00:00

# Release of BeeDeeM to use
BDM_HOME="$SOFT/bioinfo/beedeem"
BDM_VER="5.0.0"

# Load BeeDeeM environment
module load java/1.8.0_121

# Tell BeeDeeM where is its working directory and where it has to install banks
# (adapt! This is for a test)
export KL_WORKING_DIR=$HOME/bdm-test ; mkdir -p $KL_WORKING_DIR
export KL_mirror__path=$HOME/bdm-banks ; mkdir -p $KL_mirror__path

# prefix of '.dsc' file that must exist in $BDM_HOME/conf/descriptor
DESCRIPTOR="PDB_protein"
export KL_LOG_FILE=${DESCRIPTOR}.log
$BDM_HOME/$BDM_VER/bdm install \
   -desc ${DESCRIPTOR} \
   >& "$HOME/beedeem/logs/${DESCRIPTOR}-pbs.out"
```

You can easily automate bank installation using such scripts. Above script relies on a standalone installation of the software, but you can also use either [Conda](conda), [Docker](docker) or [Singularity](singularity) installation of the software.

### Use BeeDeeM UI

In addition to use BeeDeeM from the command-line, the software also comes with a friendly interface:

![UiManager](doc/dbms_ui.png)

## Practical use cases

Among others, these databanks can be used to:

* prepare and maintain up-to-date local copy of usefull data
* run BLAST, Diamond or PLAST sequence comparison jobs
* annotate BLAST, Diamond or PLAST results with sequence features and ontologies

## Companion tools

*BeeDeeM* features and data are accessible from:

* [ORSON nextflow pipeline](https://github.com/ifremer-bioinformatics/orson)
* [BioDocument Viewer](https://github.com/pgdurand/BioDocumentViewer)
* [BLAST Viewer](https://github.com/pgdurand/BlastViewer)
* [BLAST Filter Tool](https://github.com/pgdurand/BLAST-Filter-Tool)
* [Plealog Bioinformatics Core API](https://github.com/pgdurand/Bioinformatics-Core-API)

It is worth noting that BeeDeeM is capable of [creating Galaxy Data Manager loc files](https://pgdurand.gitbook.io/beedeem/utils/list-banks#get-loc-files-for-galaxy), enabling a Galaxy web portal to use banks installed by BeeBeeM.

[This manual](https://pgdurand.gitbooks.io/beedeem/) explains how to install, configure and use *BeeDeeM*.

## Requirements

Use a [Java Virtual Machine](http://www.oracle.com/technetwork/java/javase/downloads/index.html) 1.8 (or above) from Oracle. 

*Not tested with any other JVM providers but Oracle... so there is no guarantee that the software will work as expected if not using Oracle's JVM.* [More](https://pgdurand.gitbooks.io/beedeem/) about *BeeDeeM* requirements.

## Software installation, use and configuration

* [BeeDeeM manual on GitBook](https://pgdurand.gitbooks.io/beedeem/)

## License and dependencies

*BeeDeeM* itself is released under the GNU Affero General Public License, Version 3.0. [AGPL](https://www.gnu.org/licenses/agpl-3.0.txt)

It depends on several thrid-party libraries as stated in the NOTICE.txt file provided with this project.

----
(c) 2003-2023 - Patrick G. Durand

BeeDeeM development started in early 2003 by the development of [Core API](https://github.com/pgdurand/Bioinformatics-Core-API) for [BLAST Viewer](https://github.com/pgdurand/BlastViewer). Firt release of BeeDeeM was out by mid 2007... a long, long story by now! ;-) 
