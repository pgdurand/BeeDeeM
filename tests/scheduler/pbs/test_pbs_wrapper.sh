#!/usr/bin/env bash

#*****************************************************************************************
# PBS Wrapper for bash - Test script.
#
# Author: Patrick Durand, Ifremer
# Created: October 2021
#*****************************************************************************************


# ========================================================================================
# Section: include API
script_dir=$( cd -P -- "$(dirname -- "$(command -v -- "$0")")" && pwd -P )
script_dir=$script_dir/../../../conf/scripts/scheduler
. $script_dir/pbs_wrapper.sh

# ========================================================================================
# Section: a working directory
LOG_DIR="$SCRATCH"
if [ ! -e "$LOG_DIR" ]; then
  LOG_DIR="/tmp"
fi
echo "Log directory is: $LOG_DIR"
# a queue
if [ ! -e "$MQUEUE" ]; then
  MQUEUE="omp"
fi
echo "Queue is: $MQUEUE"
# a test script on that queue
sed -e "s|LOG_DIR|${LOG_DIR}|g" -e "s|QUEUE|${MQUEUE}|g" hello_worldEx.template > hello_worldEx.sh

# ========================================================================================
# Section: how many tests do we have to execute?
MAX_TEST=6

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
function getPbsScript(){
  if [  ! "$TEST_SCRIPT_DIR"  ]; then
    echo "hello_world"
  else
    echo "$TEST_SCRIPT_DIR/hello_world"
  fi
}
# ========================================================================================
# Section: tests to execute
function test1(){
  # Not working tests
  # Test 1: bad job ID, API should propery failed
  # Expected to succeed
  echo "$1/ Test bad Job ID"
  ANSWER=$(getStatus "P12256")
  RET_CODE=$?
  if [ ! $RET_CODE -eq 0 ]; then testOK ; else testKO; fi
}

function test2(){
  #Test wrong PBS parameters (here, wrong queue)
  # Expected to succeed
  echo "$1/ Test bad queue"
  MY_SCRIPT=$(getPbsScript)
  ANSWER=$(submit "sequenti" "1g" "1" "01:00:00" "Hello" "$LOG_DIR" "${MY_SCRIPT}.sh")
  RET_CODE=$?
  if [ ! $RET_CODE -eq 0 ]; then testOK ; else testKO; fi
}

function test3(){
  #Test wrong PBS parameters (here, wrong script)
  # Expected to succeed
  echo "$1/ Test bad script"
  MY_SCRIPT=$(getPbsScript)
  ANSWER=$(submit "sequentiel" "1g" "1" "01:00:00" "Hello" "$LOG_DIR" "${MY_SCRIPT}.sh.x")
  RET_CODE=$?
  if [ ! $RET_CODE -eq 0 ]; then testOK ; else testKO; fi
}
function test4(){
  #Test job status with wrong job ID 
  # Expected to fail
  echo "$1/ Test job status with invalid job ID"
  waitForJobToFinish "P12265" 5
  RET_CODE=$?
  if [ $RET_CODE -eq 2 ]; then testOK ; else testKO; fi
}

function test5(){
  #Test valid job submission
  # Expected to succeed
  echo "$1/ Test job submission"
  MY_SCRIPT=$(getPbsScript)
  echo "    submit ${MY_SCRIPT}Ex.sh ..."
  ANSWER=$(submit "${MY_SCRIPT}Ex.sh")
  RET_CODE=$?
  if [ $RET_CODE -eq 0 ]; then testOK ; else testKO; fi
  echo "    wait for job completion ..."
  JOB_ID=$ANSWER
  echo "    job ID is: $JOB_ID"
  waitForJobToFinish $JOB_ID 5
  RET_CODE=$?
  if [ $RET_CODE -eq 0 ]; then testOK ; else testKO; fi
  echo "    dump job log: [ "
  dumpJobLog $LOG_DIR $JOB_ID
  RET_CODE=$?
  echo "]"
  if [ $RET_CODE -eq 0 ]; then testOK ; else testKO; fi
  echo "    remove log file: $LOG_DIR/$JOB_ID.OU ..."
  removeJobLog $LOG_DIR $JOB_ID
  if [ $RET_CODE -eq 0 ]; then testOK ; else testKO; fi
}

function test6(){
  #Test valid job submission
  # Expected to succeed
  echo "$1/ Test job submission"
  MY_SCRIPT=$(getPbsScript)
  if [  "$TEST_SCRIPT_ARGS"  ]; then
    MY_SCRIPT="-- ${MY_SCRIPT}.sh \"$TEST_SCRIPT_ARGS\""
  else
    MY_SCRIPT="${MY_SCRIPT}.sh"
  fi
  echo "    submit $MY_SCRIPT ..."
  ANSWER=$(submitEx "sequentiel" "1g" "1" "01:00:00" "Hello" "$LOG_DIR" "$MY_SCRIPT")
  RET_CODE=$?
  if [ $RET_CODE -eq 0 ]; then testOK ; else testKO; fi
  echo "    wait for job completion ..."
  JOB_ID=$ANSWER
  echo "    job ID is: $JOB_ID"
  waitForJobToFinish $JOB_ID 5
  RET_CODE=$?
  if [ $RET_CODE -eq 0 ]; then testOK ; else testKO; fi
  echo "    dump job log: [ "
  dumpJobLog $LOG_DIR $JOB_ID
  RET_CODE=$?
  echo "]"
  if [ $RET_CODE -eq 0 ]; then testOK ; else testKO; fi
  echo "    remove log file: $LOG_DIR/$JOB_ID.OU ..."
  removeJobLog $LOG_DIR $JOB_ID
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