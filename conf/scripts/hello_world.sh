#!/usr/bin/env bash

# This is a BeeDeeM external task script template.
#
# This script illustrates the use of external tasks.
# Such a task is called from a bank descriptor; e.g. see
# for instance ../descriptors/PDB_proteins_task.dsc
#
# Such a BeeDeeM script is called by the task engine and
# with these arguments: -w <path> -d <path> -f <path> -n <name> -t <type>
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

echo "Executing an external script"
echo "Arguments coming from BeeDeeM are:"
echo $@

echo "----"
# Prepare arguments for processing
WK_DIR=
INST_DIR=
PROCESSED_FILE=
BANK_NAME=
BANK_TYPE=
while getopts w:d:f:n:t: opt
do
    case "$opt" in
      w)  WK_DIR="$OPTARG";;
      d)  INST_DIR="$OPTARG";;
      f)  PROCESSED_FILE="$OPTARG";;
      n)  BANK_NAME="$OPTARG";;
      t)  BANK_TYPE="$OPTARG";;
    esac
done
shift `expr $OPTIND - 1`
# remaining arguments, if any, are stored here
MORE_ARGS=$@

echo "Working dir: $WK_DIR"
echo "Install dir: $INST_DIR"
echo "Processed file: $PROCESSED_FILE"
echo "Bank name: $BANK_NAME"
echo "Bank type: $BANK_TYPE"
echo "----"
 
