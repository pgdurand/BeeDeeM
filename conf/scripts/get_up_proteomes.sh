#!/usr/bin/env bash

# This is a BeeDeeM external task script aims at downloading
# Uniprot_Reference_Proteomes using HTTP protocol.
#
# This script is used in bank descriptor:
#   ../descriptors/Uniprot_Reference_Proteomes.dsc
#
# Such a BeeDeeM script is called by the task engine and
# with arguments as defined in: 
# ./scheduler/common.sh->handleBDMArgs() function

echo "Getting Uniprot Proteomes"

# ========================================================================================
# Section: include API
S_NAME=$(realpath "$0")
[[ -z "$BDM_CONF_SCRIPTS" ]] && script_dir=$(dirname "$S_NAME") || script_dir=$BDM_CONF_SCRIPTS
. $script_dir/scheduler/common.sh

# ========================================================================================
# Section: handle arguemnts
# Function call setting BDMC_xxx variables from cmdline arguments
handleBDMArgs $@
RET_CODE=$?
[ ! $RET_CODE -eq 0 ] && errorMsg "Wrong or missing arguments" && exit $RET_CODE

# ========================================================================================
# Section: do business

WK_DIR=${BDMC_WK_DIR}/Uniprot_Reference_Proteomes
echo "Creating $WK_DIR"
mkdir -p $WK_DIR
echo "Changing dir to $WK_DIR"
cd $WK_DIR

# Use wildcard to get a single file, actually (bypass timestamp)
url="ftp://ftp.ebi.ac.uk/pub/databases/uniprot/current_release/knowledgebase/reference_proteomes/Reference_Proteomes_*.tar.gz"
filename="reference_proteomes.tar.gz"
fOK="${filename}_OK"

if [ ! -f $fOK ]; then
  echo "Getting $filename"
  ANSWER=$(downloadFile $filename $url)
  echo $ANSWER
  RET_CODE=$?
  [ ! $RET_CODE -eq 0 ] && errorMsg "Unable to get data" && exit 3 || touch $fOK
else
  echo "Skip $filename downloading: already done"
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
  mv ${TOC_FILE}.tmp ${TOC_FILE}
fi

# Use that file of files to create a unique fasta file per Kingdom
for kd in ${KINGDOMS[*]}
do
  # Extract list of file for tham Kingdom (enable resume)
  if [ -f ${kd}.list ]; then
    echo "use existing ${kd}.list"
  else
    echo "creating ${kd}.list ..."
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

