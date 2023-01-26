#!/usr/bin/env bash

# Setup a working directory to let BeeDeeM
# install two banks (see below)
[ ! -e $SCRATCH ] && SCRATCH=/tmp
TMP_DIR=$(mktemp bdm.XXXXXXXXXX)
[ ! $? -eq 0 ] && TMP_DIR="bdm-test"

# These are the two mandatory variables to use
# to override default BeeDeeM configuration
export KL_WORKING_DIR=$SCRATCH/$TMP_DIR
export KL_mirror__path=$KL_WORKING_DIR
export KL_LOG_TYPE=console

echo "BeeDeeM Conda test within: $KL_WORKING_DIR"
mkdir -p $KL_WORKING_DIR

# == TEST 1 =========================================== 
echo "*** TEST 1: Start simple bank installation"
# These are two ".dsc" files located in BeeDeem-home/conf/descriptors
# path
DESC_LIST="PDB_proteins,SwissProt_human"
# Start installation
bdm install -desc $DESC_LIST

if [ $? -eq 0 ]; then
  echo "TEST 1: SUCCESS"
else
  echo "TEST 1: FAILED."
  exit 1
fi

# == TEST 2 =========================================== 
echo "*** TEST 2: list installed bank"
export KL_LOG_TYPE=none
bdm info -d all -f txt

if [ $? -eq 0 ]; then
  echo "TEST 2: SUCCESS"
else
  echo "TEST 2: FAILED. Review log file in: $BDM_WORK_DIR"
  exit 1
fi


# == TEST 3 =========================================== 
# Change default BeeDeeM log file name to something else
export KL_LOG_TYPE=file
export KL_LOG_FILE=query.log

SW_ENTRY="ZZZ3_HUMAN"
echo "*** TEST 3: query bank for entry: $SW_ENTRY"
bdm query -d p -f txt -i $SW_ENTRY

if [ $? -eq 0 ]; then
  echo "TEST 3: SUCCESS"
else
  echo "TEST 3: FAILED. Review log file: $BDM_WORK_DIR"
  exit 1
fi

# Note: by design of this script, we DO NOT delete
# $KL_WORKING_DIR... please, do it yourself.

