#!/usr/bin/env bash

# This is a BeeDeeM external task script aims at getting
# Refseq release number.
#
# This script is used in bank descriptors:
#   ../descriptors/Refseq_xxx.dsc
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

echo "Getting Refseq release number"
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

url="https://ftp.ncbi.nih.gov/refseq/release/RELEASE_NUMBER"
filename="ref_release.txt"

echo "Getting $filename"
if [ -x "$(which wget)" ] ; then
    CMD="wget --continue -q $url -O $filename"
elif [ -x "$(which curl)" ]; then
    CMD="curl -sL -o $filename -C - $url"
else
    echo "Could not find curl or wget, please install one." >&2
    exit 0
fi

echo $CMD
eval $CMD

if [ ! $? -eq 0 ];then
  echo "Unable to fetch Genbank release number"
  exit 0
fi

#Get Refseq official release number
FNAME=${INST_DIR}/$filename
RELEASE=$(cat $FNAME | xargs)
echo "release.time.stamp=$RELEASE" > ${INST_DIR}/release-date.txt

