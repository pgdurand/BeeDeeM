#!/usr/bin/env bash

# =================================================
# Test script for BeeDeeM legacy installer or Conda.
# To be used directly on the command-line.
# DO NOT use for Singularity/Docker installations.
#
# How to?
#
# Step 1: install BeeDeeM as stated at:
#         https://pgdurand.gitbook.io/beedeem/installation/installation#legacy-installation 
#
# Step 2: run test as follows:
#         cd /path/to/beedeem 
#         export PATH=$PWD:$PATH
#         ./test_bdm
# --
# P. Durand (SeBiMER, Ifremer), last updated on Jan 2023
# =================================================


# == TEST 1 =========================================== 
echo "*** TEST 1: Start simple bank installation"

# set BeeDeeM log mode to console (otherwise default is a log file located in KL_WORKING_DIR)
export KL_LOG_TYPE=console

# Set the bank to install
# This is a '.dsc' file located in BeeDeeM installation path at ${beedeem-home}/conf/descriptors
DESCRIPTOR="SwissProt_human"

# start BeeDeeM with 'install' command
bdm install -desc ${DESCRIPTOR} 

# check whether command succeeded or not
if [ $? -eq 0 ]; then
  echo "TEST 1: SUCCESS"
else
  echo "TEST 1: FAILED."
  exit 1
fi


# == TEST 2 =========================================== 
echo "*** TEST 2: list installed bank"

# reset BeeDeeM log mode to file (default)
export KL_LOG_TYPE=file

# start BeeDeeM with 'info' command
bdm info -d all -f txt

if [ $? -eq 0 ]; then
  echo "TEST 2: SUCCESS"
else
  echo "TEST 2: FAILED. Review log file in: @KL_WORKING_DIR@"
  exit 1
fi


# == TEST 3 =========================================== 
# Change default BeeDeeM log file name to something else
export KL_LOG_FILE=query.log

SW_ENTRY="ZZZ3_HUMAN"
echo "*** TEST 3: query bank for entry: $SW_ENTRY"

# start BeeDeeM with 'query' command
bdm query -d protein -f txt -i $SW_ENTRY

if [ $? -eq 0 ]; then
  echo "TEST 3: SUCCESS"
else
  echo "TEST 3: FAILED. Review log file: @KL_WORKING_DIR@"
  exit 1
fi

