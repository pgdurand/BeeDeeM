#!/usr/bin/env bash

# This is a BeeDeeM external task script aims at downloading
# Uniprot_TrEMBLGeneOntology using HTTP protocol.
#
# This script is used in bank descriptor:
#   ../descriptors/Uniprot_TrEMBL.dsc
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

echo "Getting TrEMBL"
echo "Arguments coming from BeeDeeM are:"
echo $@
echo "----"

# Prepare arguments for processing
WK_DIR=
INST_DIR=
PROCESSED_FILE=
while getopts w:d:f: opt
do
    case "$opt" in
      w)  WK_DIR="$OPTARG";;
      d)  INST_DIR="$OPTARG";;
      f)  PROCESSED_FILE="$OPTARG";;
    esac
done
shift `expr $OPTIND - 1`
# remaining arguments, if any, are stored here
MORE_ARGS=$@

echo "Working dir: $WK_DIR"
echo "Install dir: $INST_DIR"
echo "Processed file: $PROCESSED_FILE"
echo "----"

WK_DIR=${WK_DIR}/Uniprot_TrEMBL
echo "Creating $WK_DIR"
mkdir -p $WK_DIR
echo "Changing dir to $WK_DIR"
cd $WK_DIR

url="ftp://ftp.ebi.ac.uk/pub/databases/uniprot/current_release/knowledgebase/complete/uniprot_trembl.dat.gz"
filename="uniprot_trembl.dat.gz"

echo "Getting $filename"
if [ -x "$(which wget)" ] ; then
    CMD="wget -c -q $url -O $filename"
elif [ -x "$(which curl)" ]; then
    CMD="curl -sL -o $filename -C - $url"
else
    echo "Could not find curl or wget, please install one." >&2
    exit 1
fi

echo $CMD
eval $CMD

