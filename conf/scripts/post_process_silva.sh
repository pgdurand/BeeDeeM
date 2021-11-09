#!/usr/bin/env bash

# This is a BeeDeeM external task script aims at making
# Bowtie index for Silva.
#
# This script is used in bank descriptor:
#   ../descriptors/Silva_XXX.dsc
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

echo "Making Bowtie2 index for Silva"
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

# Make Bowtie2 index
cd $INST_DIR
mkdir -p bowtie.idx
cd bowtie.idx
BOW_VER=2.4.1
echo "key=BOWTIE2" > index.properties
echo "version=$BOW_VER" >> index.properties
. /appli/bioinfo/bowtie2/$BOW_VER/env.sh
#DO NOT modify value for threads... or remember to update 
#<galaxy-home>/beedeem/bdm-silva.pbs script too!
CMD="bowtie2-build --threads 4 --verbose ../${BANK_NAME}00 $BANK_NAME"
echo $CMD
eval $CMD
RET_CODE=$?
. /appli/bioinfo/bowtie2/$BOW_VER/delenv.sh
exit $RET_CODE

