#!/usr/bin/env bash

# This is a BeeDeeM external task script aims at making
# Bowtie index for Silva.
#
# This script is used in bank descriptor:
#   ../descriptors/Silva_XXX.dsc
#
# Such a BeeDeeM script is called by the task engine and
# with arguments as defined in: 
# ./scheduler/common.sh->handleBDMArgs() function

echo "Making Bowtie2 index for Silva"

# ========================================================================================
# Section: include API
S_NAME=$(realpath "$0")
[[ -z "$BDM_CONF_SCRIPTS" ]] && script_dir=$(dirname "$S_NAME") || script_dir=$BDM_CONF_SCRIPTS
. $script_dir/scheduler/common.sh
. $script_dir/env_${BDM_PLATFORM}.sh

# ========================================================================================
# Section: handle arguemnts
# Function call setting BDMC_xxx variables from cmdline arguments
handleBDMArgs $@
RET_CODE=$?
[ ! $RET_CODE -eq 0 ] && errorMsg "Wrong or missing arguments" && exit $RET_CODE

# ========================================================================================
# Section: do business

# Make Bowtie2 index
cd $BDMC_INST_DIR
mkdir -p bowtie.idx
cd bowtie.idx
SOFT_NAME="bowtie2"
SOFT_VER=2.4.1
echo "key=$SOFT_NAME" > index.properties
echo "version=$SOFT_VER" >> index.properties
activateEnv "$SOFT_NAME" "$SOFT_VER"
#DO NOT modify value for threads... or remember to update 
#calling script too (e.g. bdm-silva.pbs)
CMD="bowtie2-build --threads 4 --verbose ../${BANK_NAME}00 $BANK_NAME"
echo $CMD
eval $CMD
RET_CODE=$?
deActivateEnv "$SOFT_NAME" "$SOFT_VER"
exit $RET_CODE

