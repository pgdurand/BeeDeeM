#!/usr/bin/env bash

# This is a BeeDeeM external task script aims at downloading
# Silva LSURef Fasta file using HTTP protocol.
#
# This script is used in bank descriptor:
#   ../descriptors/Silva_LSU.dsc
#
# Such a BeeDeeM script is called by the task engine and
# with arguments as defined in: 
# ./scheduler/common.sh->handleBDMArgs() function

echo "Getting Silva LSU"

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

WK_DIR=${BDMC_WK_DIR}/Silva_LSU
echo "Creating $WK_DIR"
mkdir -p $WK_DIR
echo "Changing dir to $WK_DIR"
cd $WK_DIR

# See https://www.arb-silva.de/no_cache/download/archive/current/Exports
filename="SILVA_138.1_LSURef_tax_silva.fasta.gz"
url="https://www.arb-silva.de/fileadmin/silva_databases/current/Exports/$filename"
ANSWER=$(downloadFile $filename $url)
RET_CODE=$?
echo $ANSWER
exit $RET_CODE

