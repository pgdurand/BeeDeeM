#!/usr/bin/env bash

# This is a BeeDeeM external task script aims at getting
# Interpro release number.
#
# This script is used in bank descriptors:
#   ../descriptors/Interpro_terms.dsc
#
# Such a BeeDeeM script is called by the task engine and
# with arguments as defined in: 
# ./scheduler/common.sh->handleBDMArgs() function

echo "Getting Interpro release number"

# ========================================================================================
# Section: include API
S_NAME=$( cd -P -- "$(dirname -- "$(command -v -- "$0")")" && pwd -P )
[[ -z "$BDM_CONF_SCRIPTS" ]] && script_dir=$S_NAME || script_dir=$BDM_CONF_SCRIPTS
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

url="http://ftp.ebi.ac.uk/pub/databases/interpro/current_release/release_notes.txt"
filename="ipr_relnotes.txt"
ANSWER=$(downloadFile $filename $url)
RET_CODE=$?
[ ! $RET_CODE -eq 0 ] && errorMsg "Unable to fetch Interpro release number" && exit $RET_CODE

#Get IPR official release number
FNAME=${BDMC_INST_DIR}/$filename
RELEASE=$(head -n 10 $FNAME | grep -v "Release Notes" | grep "Release " | cut -d',' -f 1 | cut -d' ' -f 2)
echo "release.time.stamp=$RELEASE" > ${BDMC_INST_DIR}/release-date.txt

