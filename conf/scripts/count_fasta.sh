#!/usr/bin/env bash

# This is a BeeDeeM external task script aims at fastly
# sequences in a FASTA file without using the Lucene indexer 
# task (idxfas).
#
# Such a BeeDeeM script is called by the task engine and
# with up to three arguments: -w <path> -d <path> -f <path>
#
#  -w <path>: <path> is the working directory path.
#             provided for both unit and global tasks.
#  -d <path>: <path> is the bank installation path.
#             provided for both unit and global tasks.
#  -f <path>: <path> is the path to file under unit task processing
#             only provided with unit task.
#  -n <name>: <name> is the bank name.
#  -t <type>: <path> is the bank type. One of p, n or d.
#             p: protein
#             n: nucleotide
#             d: dictionary or ontology
#  -p <name>: platform name (i.e. a specific cluster configuration)

echo "Counting sequences in FASTA file"
echo "Arguments coming from BeeDeeM are:"
echo $@
echo "----"

# Prepare arguments for processing
BDMC_WK_DIR=
BDMC_INST_DIR=
BDMC_PROCESSED_FILE=
BDMC_BANK_NAME=
BDMC_BANK_TYPE=
BDMC_PLATFORM=
while getopts w:d:f:n:t:p: opt
do
    case "$opt" in
      w)  BDMC_WK_DIR="$OPTARG";;
      d)  BDMC_INST_DIR="$OPTARG";;
      f)  BDMC_PROCESSED_FILE="$OPTARG";;
      n)  BDMC_BANK_NAME="$OPTARG";;
      t)  BDMC_BANK_TYPE="$OPTARG";;
      p)  BDMC_PLATFORM="$OPTARG";;
    esac
done
shift `expr $OPTIND - 1`
# remaining arguments, if any, are stored here
MORE_ARGS=$@

echo "Working dir: $BDMC_WK_DIR"
echo "Install dir: $BDMC_INST_DIR"
echo "Processed file: $BDMC_PROCESSED_FILE"
echo "Bank name: $BDMC_BANK_NAME"
echo "----"

cd $BDMC_INST_DIR
NB_SEQS=$(grep -c "^>" $BDMC_PROCESSED_FILE) >&2
if [ $? -eq 0 ] ; then
    echo "Content of ${BDMC_PROCESSED_FILE}: $NB_SEQS sequences"
    echo "$NB_SEQS" > ${BDMC_BANK_NAME}.num
else
    echo "Could not count sequences" >&2
    exit 1
fi
