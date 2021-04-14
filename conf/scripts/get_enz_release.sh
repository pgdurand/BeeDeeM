#!/usr/bin/env bash

# This is a BeeDeeM external task script aims at getting
# Enzyme release number.
#
# This script is used in bank descriptors:
#   ../descriptors/Enzyme.dsc
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

echo "Getting Gene Ontology"
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

cd $INST_DIR

#Get Enzyme official release number
FNAME=${INST_DIR}/enzclass.txt
RELEASE=$(head -n 10 $FNAME | grep "Release:" | cut -d':' -f 2 | xargs)
echo "release.time.stamp=$RELEASE" > ${INST_DIR}/release-date.txt

