#!/usr/bin/env bash

#*****************************************************************************************
# LOCAL Wrapper for bash - Test script.
#
# Author: Patrick Durand, Ifremer
# Created: November 2022
#*****************************************************************************************


# ========================================================================================
# Section: include API
script_dir=$( cd -P -- "$(dirname -- "$(command -v -- "$0")")" && pwd -P )
. $script_dir/../../../conf/scripts/scheduler/lcl_wrapper.sh

# ========================================================================================
# Section: a working directory
LOG_DIR="$SCRATCH"
if [ ! -e "$LOG_DIR" ]; then
  LOG_DIR="/tmp"
fi
echo "Log directory is: $LOG_DIR"

# ========================================================================================
# Section: how many tests do we have to execute?
MAX_TEST=3

# ========================================================================================
# Section: manage test status
TEST_TOTAL=0
TEST_SUCCESS=0
TEST_FAILED=0
function testOK(){
  echo "  Test: SUCCESS"
  ((TEST_TOTAL++))
  ((TEST_SUCCESS++))
}
function testKO(){
  echo "  Test: ERROR"
  ((TEST_TOTAL++))
  ((TEST_FAILED++))
}
function dumpTestStats(){
  echo "Test: ${TEST_TOTAL}. Success: ${TEST_SUCCESS}. Failed: ${TEST_FAILED}. "
}
# DO NOT add extension to script!!!
function getTestScript(){
  if [  ! "$TEST_SCRIPT_DIR"  ]; then
    echo "hello_world"
  else
    echo "$TEST_SCRIPT_DIR/hello_world"
  fi
}
# ========================================================================================
# Section: tests to execute

function test1(){
  #Test invalid job execution
  # Expected to fail
  echo "$1/ Test invalid job execution"
  MY_SCRIPT=$(getTestScript)
  echo "    execute ${MY_SCRIPT}Ex.sh ..."
  ANSWER=$(submit "${MY_SCRIPT}Ex.sh")
  RET_CODE=$?
  if [ ! $RET_CODE -eq 0 ]; then testOK ; else testKO; fi
  LOG_FILE=$ANSWER
  echo "    dump job log: $LOG_FILE [ "
  dumpJobLog $LOG_FILE
  RET_CODE=$?
  echo "]"
  if [ $RET_CODE -eq 0 ]; then testOK ; else testKO; fi
  echo "    remove log file: $LOG_FILE ..."
  removeJobLog $LOG_FILE
  if [ $RET_CODE -eq 0 ]; then testOK ; else testKO; fi
}
function test2(){
  #Test valid job execution
  # Expected to succeed
  echo "$1/ Test valid job execution"
  MY_SCRIPT=$(getTestScript)
  echo "    execute ${MY_SCRIPT}.sh ..."
  ANSWER=$(submit "$script_dir/${MY_SCRIPT}.sh")
  RET_CODE=$?
  if [ $RET_CODE -eq 0 ]; then testOK ; else testKO; fi
  LOG_FILE=$ANSWER
  echo "    dump job log: $LOG_FILE [ "
  dumpJobLog $LOG_FILE
  RET_CODE=$?
  echo "]"
  if [ $RET_CODE -eq 0 ]; then testOK ; else testKO; fi
  echo "    remove log file: $LOG_FILE ..."
  removeJobLog $LOG_FILE
  if [ $RET_CODE -eq 0 ]; then testOK ; else testKO; fi
}
function test3(){
  #Test valid job execution
  # Expected to succeed
  #Test compatibility API between real scheduler and this local job runner
  echo "$1/ Test valid job execution; API compatibility with real job scheduler"
  MY_SCRIPT=$(getTestScript)
  echo "    execute ${MY_SCRIPT}.sh ..."
  #ANSWER=$(submitEx "sequenti" "1g" "1" "01:00:00" "Hello" "$LOG_DIR" "$script_dir/${MY_SCRIPT}.sh")
  ANSWER=$(submitEx "sequenti" "1g" "1" "01:00:00" "Hello" "$LOG_DIR" "$script_dir/${MY_SCRIPT}.sh")
  RET_CODE=$?
  if [ $RET_CODE -eq 0 ]; then testOK ; else testKO; fi
  LOG_FILE=$ANSWER
  echo "    dump job log: $LOG_FILE [ "
  dumpJobLog $LOG_FILE
  RET_CODE=$?
  echo "]"
  if [ $RET_CODE -eq 0 ]; then testOK ; else testKO; fi
  echo "    remove log file: $LOG_FILE ..."
  removeJobLog $LOG_FILE
  if [ $RET_CODE -eq 0 ]; then testOK ; else testKO; fi
}
function dumpTestStats(){
  echo ""
  echo "** Test: ${TEST_TOTAL}. Success: ${TEST_SUCCESS}. Failed: ${TEST_FAILED}. "
}

# ========================================================================================
# Start tests
CUR_TEST=1
while [ "$CUR_TEST" -le "$MAX_TEST" ]; do
  eval test$CUR_TEST $CUR_TEST
  ((CUR_TEST++))
done
dumpTestStats

rm -f hello_worldEx.sh
