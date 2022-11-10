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
# Such a BeeDeeM script is called by the task engine and
# with arguments as defined in: 
# ./scheduler/common.sh->handleBDMArgs() function

echo "Getting TrEMBL"

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

WK_DIR=${BDMC_WK_DIR}/Uniprot_TrEMBL
echo "Creating $WK_DIR"
mkdir -p $WK_DIR
echo "Changing dir to $WK_DIR"
cd $WK_DIR

url="ftp://ftp.ebi.ac.uk/pub/databases/uniprot/current_release/knowledgebase/complete/uniprot_trembl.dat.gz"
filename="uniprot_trembl.dat.gz"

ANSWER=$(downloadFile $filename $url)
RET_CODE=$?
echo $ANSWER
exit $RET_CODE
