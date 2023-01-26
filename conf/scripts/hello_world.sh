#!/usr/bin/env bash

# This is a BeeDeeM external task script example.
#
# This script illustrates the use of external tasks.
# Such a task is called from a bank descriptor; e.g. see
# for instance ../descriptors/PDB_proteins_task.dsc
#
# Such a BeeDeeM script is called by the task engine with some
# BeeDeeM specific arguments, see 
#    conf/scripts/scheduler/common.sh#handleBDMArgs()
# for more information.

# If you setup a new script, simply copy this one, keep in it 
# lines 1 and 20-31 (sections "include API" and "handle arguments"), 
# then do whatever you have to do!

set -eo pipefail

# ========================================================================================
# Section: include API
S_NAME=$(realpath "$0")
[[ -z "$BDM_CONF_SCRIPTS" ]] && script_dir=$(dirname "$S_NAME") || script_dir=$BDM_CONF_SCRIPTS
. $script_dir/scheduler/common.sh

# ========================================================================================
# Section: handle arguments
# Function call setting BDMC_xxx variables from cmdline arguments
handleBDMArgs $@
RET_CODE=$?
[ ! $RET_CODE -eq 0 ] && errorMsg "Wrong or missing arguments" && exit $RET_CODE

# ========================================================================================
# Section: do business

echo "Working directory of BeeDeeM: $BDMC_WK_DIR"
echo "Bank installation path: $BDMC_INST_DIR"
echo "Current bank file processed: $BDMC_PROCESSED_FILE"
echo "Bank name: $BDMC_BANK_NAME"
echo "Bank type: $BDMC_BANK_TYPE"
echo "Additional args: $BDMC_MORE_ARGS"
echo "----"
 
