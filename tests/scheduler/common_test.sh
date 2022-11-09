#!/usr/bin/env bash

#*****************************************************************************************
# Common API for testing framework.
#
# See test_xxx.sh scripts to figure out how to use this API.
#
# Author: Patrick Durand, Ifremer
# Created: June 2022
#*****************************************************************************************

# ========================================================================================
# Section: manage test status
MAX_TEST=1 #Should be redefined in calling script
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
  echo ""
  echo "** Test: ${TEST_TOTAL}. Success: ${TEST_SUCCESS}. Failed: ${TEST_FAILED}. "
}

# ========================================================================================
# Start tests
function runTests(){
  CUR_TEST=1
  while [ "$CUR_TEST" -le "$MAX_TEST" ]; do
    eval test$CUR_TEST $CUR_TEST
    ((CUR_TEST++))
  done
  dumpTestStats
}
