#!/usr/bin/env bash

# This is a BeeDeeM external task script aims at getting
# Genbank release number.
#
# This script is used in bank descriptors:
#   ../descriptors/Genbank_xxx.dsc
#
# Such a BeeDeeM script is called by the task engine and
# with arguments as defined in: 
# ./scheduler/common.sh->handleBDMArgs() function

echo "Getting Genbank release number"

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

cd $BDMC_INST_DIR
url="https://ftp.ncbi.nih.gov/genbank/GB_Release_Number"
filename="gb_release.txt"
ANSWER=$(downloadFile $filename $url)
RET_CODE=$?
[ ! $RET_CODE -eq 0 ] && errorMsg "Unable to fetch Genbank release number" && exit $RET_CODE

#Get GB official release number
FNAME=${BDMC_INST_DIR}/$filename
RELEASE=$(cat $FNAME | xargs)
echo "release.time.stamp=$RELEASE" > ${BDMC_INST_DIR}/release-date.txt

