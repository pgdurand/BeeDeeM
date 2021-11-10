#!/usr/bin/env bash

# This is a BeeDeeM external task script aims at downloading
# Uniprot_Reference_Proteomes using HTTP protocol.
#
# This script is used in bank descriptor:
#   ../descriptors/Uniprot_Reference_Proteomes.dsc
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

echo "Getting Uniprot Proteomes"
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

WK_DIR=${WK_DIR}/Uniprot_Reference_Proteomes
echo "Creating $WK_DIR"
mkdir -p $WK_DIR
echo "Changing dir to $WK_DIR"
cd $WK_DIR

# Use wildcard to get a single file, actually (bypass timestamp)
url="ftp://ftp.ebi.ac.uk/pub/databases/uniprot/current_release/knowledgebase/reference_proteomes/Reference_Proteomes_*.tar.gz"
filename="reference_proteomes.tar.gz"

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

OUT=$?
if [ ! $OUT -eq 0 ];then
  printf "ERROR: unable to get data." >&2
  exit 3
fi

# Extract protein Fasta files per kingdom
TOC_FILE="archive_files.txt"
KINGDOMS=(Archaea Bacteria Eukaryota Viruses)

# Create a file with all files contained in the archive
#  Enable resume since this step requires a lot of time.
if [ -f $TOC_FILE ]; then
  echo "use existing $TOC_FILE"
else
  echo "creating $TOC_FILE ..."
  tar -ztf $filename > ${TOC_FILE}.tmp
  mv ${TOC_FILE}.tmp
fi

# Use that file of files to create a unique fasta file per Kingdom
for kd in ${KINGDOMS[*]}
do
  # Extract list of file for tham Kingdom (enable resume)
  if [ -f $kd ]; then
    echo "use existing ${kd}.list"
  else
    echo "creating $kd ..."
    cat $TOC_FILE | grep "^${kd}" | grep ".fasta.gz$" | grep -v "_DNA" > ${kd}.list.tmp
    mv ${kd}.list.tmp ${kd}.list
  fi
  # Given that list of files, extract fasta.gz from archive (enable resume)
  mkdir -p ${kd}
  NREFFILES=$(wc -l ${kd}.list | cut -d' ' -f 1)
  NFILES=$(ls ${kd} | wc -l)
  if [ "$NREFFILES" -eq "$NFILES" ]; then
    echo "skip creation of $kd data directory: already exist with valid files"
  else
   echo "retrieving files for $kd" 
   tar -zxf $filename -C $kd -T ${kd}.list --strip-components=2
  fi
  # Create unique fasta file
  find ${kd} -type f -name '*.gz' -exec cat {} \; > ${kd}_volume.fasta.gz
done

# Replace tr| by sp| in FASTA headers to enable BeeDeeM further processing
for kd in ${KINGDOMS[*]}
do
  kdf=${kd}_volume.fasta.gz
  kdf2=${kd}_volume.fasta.gz~
  kdf_b=${kd}_volume.fasta
  mv $kdf $kdf2
  zcat $kdf2 | sed -e 's/>tr|/>sp|/g' > $kdf_b
  gzip $kdf_b
  #pigz -p 4 $kdf_b
done

