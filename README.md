# *BeeDeeM*: the Bioinformatics Databank Manager System 

[![License AGPL](https://img.shields.io/badge/license-Affero%20GPL%203.0-blue.svg)](https://www.gnu.org/licenses/agpl-3.0.txt) [![](https://tokei.rs/b1/github/pgdurand/BeeDeeM?category=code)](https://github.com/pgdurand/BeeDeeM) [![](https://img.shields.io/badge/platform-Java--1.7+-yellow.svg)](http://www.oracle.com/technetwork/java/javase/downloads/index.html) [![](https://img.shields.io/badge/run_on-Linux--Mac_OSX--Windows-yellowgreen.svg)]()

*BeeDeeM* is a general purpose **B**ioinformatics **D**atabank **M**anager. 

It provides a suite of command-line and UI softwares to install and use major sequence databanks and biological classifications.

## Main features

*BeeDeeM* automatically performs:

* the download of the database files from remote sites \(via FTP\),
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

Use a [Java Virtual Machine](http://www.oracle.com/technetwork/java/javase/downloads/index.html) 1.7 (or above) from Oracle. 

*Not tested with any other JVM providers but Oracle... so there is no guarantee that the software will work as expected if not using Oracle's JVM.* [More](https://pgdurand.gitbooks.io/beedeem/) about *BeeDeeM* requirements.

## Software installation, use and configuration

See [*BeeDeeM* manual](https://pgdurand.gitbooks.io/beedeem/).

## License and dependencies

*BeeDeeM* itself is released under the GNU Affero General Public License, Version 3.0. [AGPL](https://www.gnu.org/licenses/agpl-3.0.txt)

It depends on several thrid-party libraries as stated in the NOTICE.txt file provided with this project.

--
(c) 2007-2017 - Patrick G. Durand and Ludovic Antin
