#!/usr/bin/env bash

# This is a BeeDeeM external task script aims at getting
# Genbank release number.
#
# This script is used in bank descriptors:
#   ../descriptors/Genbank_xxx.dsc
#
# Such a BeeDeeM script is called by the task engine and
# with with these arguments: -w <path> -d <path> -f <path> -n <name> -t <type>
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

echo "Getting Genbank release number"
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

cd $INST_DIR

url="https://ftp.ncbi.nih.gov/genbank/GB_Release_Number"
filename="gb_release.txt"

echo "Getting $filename"
if [ -x "$(which wget)" ] ; then
    CMD="wget -c -q $url -O $filename"
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

#Get GB official release number
FNAME=${INST_DIR}/$filename
RELEASE=$(cat $FNAME | xargs)
echo "release.time.stamp=$RELEASE" > ${INST_DIR}/release-date.txt

