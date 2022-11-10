#!/usr/bin/env bash

# This is a BeeDeeM external task script aims at getting
# Uniprot reference Proteomes release number.
#
# This script is used in bank descriptors:
#   ../descriptors/Uniprot_Reference_Proteome.dsc
#
# Such a BeeDeeM script is called by the task engine and
# with these arguments: -w <path> -d <path> -f <path> -n <name> -t <type>
#
# Such a BeeDeeM script is called by the task engine and
# with arguments as defined in: 
# ./scheduler/common.sh->handleBDMArgs() function

echo "Getting Uniprot Proteomes release number"

# ========================================================================================
# Section: include API
:S_NAME=$(realpath "$0")
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

cd $BDMC_INST_DIR

url="ftp://ftp.ebi.ac.uk/pub/databases/uniprot/current_release/knowledgebase/reference_proteomes/README"
filename="README.txt"
ANSWER=$(downloadFile $filename $url)
echo $ANSWER
RET_CODE=$?
[ ! $RET_CODE -eq 0 ] && errorMsg "Unable to fetch Uniprot Reference Proteomes release number" && exit $RET_CODE

#Get Uniprot Reference Proteomes official release number
FNAME=${BDMC_INST_DIR}/$filename
RELEASE=$(grep -m 1 "^Release" $FNAME | cut -d' ' -f 2-)
echo "release.time.stamp=$RELEASE" > ${BDMC_INST_DIR}/release-date.txt

