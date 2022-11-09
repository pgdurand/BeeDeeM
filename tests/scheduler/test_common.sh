#!/usr/bin/env bash

#*****************************************************************************************
# Test script for the common.sh API.
#
# Author: Patrick Durand, Ifremer
# Created: June 2022
#*****************************************************************************************

# ========================================================================================
# Section: include API
S_NAME=$(realpath "$0")
script_dir=$(dirname "$S_NAME")
. $script_dir/../../conf/scripts/scheduler/common.sh
. $script_dir/common_test.sh
. $script_dir/../../conf/scripts/scheduler/env_ifremer.sh

# ========================================================================================
# Section: a working directory
DATA_DIR="$SCRATCH"
if [ ! -e "$DATA_DIR" ]; then
  DATA_DIR="/tmp"
fi

echo "> Using DATA_DIR: $DATA_DIR"

# ========================================================================================
# Override number of tests to execute
MAX_TEST=13

# ========================================================================================
# Section: tests

# Test Msg ON
function test1(){
  echo "> Test 1: you should see some messages"
  silentOff
  errorMsg "This is an error message"
  infoMsg "This is a message"
  testOK
}

# Test Msg OFF
function test2(){
  echo "> Test 2: you should see no messages"
  silentOn
  errorMsg "This is an error message"
  infoMsg "This is a message"
  testOK
  silentOff
}

# Test download a simple file
function test3(){
  url="http://current.geneontology.org/ontology/go-basic.obo"
  filename="go-basic.obo"
  echo "> Test 3: download $url"
  cd $DATA_DIR
  ANSWER=$(downloadFile $filename $url)
  RET_CODE=$?
  echo $ANSWER
  if [ $RET_CODE -eq 0 ]; then testOK ; else testKO; fi
  rm -f $filename
  cd - >/dev/null 2>&1
}

# test activate/deactivate env with a valid software
function test4(){
  local soft_name="blast"
  local soft_ver="2.9.0"
  echo "> Test 4: activate ${soft_name}-${soft_ver} for platfom: $BDM_PLATFORM"
  activateEnv ${soft_name} ${soft_ver}
  if [ $? -eq 0 ]; then testOK ; else testKO; fi
  echo "> Test 5: execute ${soft_name}p -help"
  blastp -help > /dev/null 2>&1
  if [ $? -eq 0 ]; then testOK ; else testKO; fi
  echo "> Test 6: deactivate ${soft_name}"
  deActivateEnv ${soft_name} ${soft_ver}
  if [ $? -eq 0 ]; then testOK ; else testKO; fi
}

# test activate/deactivate env with an invalid software
function test5(){
  local soft_name="blast"
  local soft_ver="3.4.5"
  echo "> Test 7: activate a unknown software ${soft_name}-${soft_ver} for platfom: $BDM_PLATFORM"
  activateEnv ${soft_name} ${soft_ver}
  if [ ! $? -eq 0 ]; then testOK ; else testKO; fi
  echo "> Test 8: deactivate a unknown software ${soft_name}-${soft_ver} for platfom: $BDM_PLATFORM"
  deActivateEnv ${soft_name} ${soft_ver}
  if [ ! $? -eq 0 ]; then testOK ; else testKO; fi
}

# test activate/deactivate env with a valid software
function test6(){
  local fname="test_wrapper.xcfg"
  local soft_name="diamond"
  local cfg_key="c"
  echo "> Test 9: test invalid scheduler resource file name"
  getResource $fname $soft_name $cfg_key
  if [ ! $? -eq 0 ]; then testOK ; else testKO; fi
}

function test7(){
  local fname="test_wrapper.cfg"
  local soft_name="diamond"
  local cfg_key="x"
  echo "> Test 10: test invalid scheduler resource name"
  getResource $fname $soft_name $cfg_key
  if [ ! $? -eq 0 ]; then testOK ; else testKO; fi
}

function test8(){
  local fname="test_wrapper.cfg"
  local soft_name="diamo"
  local cfg_key="c"
  echo "> Test 11: test invalid software name for scheduler resource"
  local ANSWER=$(getResource $fname $soft_name $cfg_key)
  local ret_code=$?
  echo $ANSWER
  if [ $ret_code -eq 0 ]; then testOK ; else testKO; fi
}

function test9(){
  local fname="test_wrapper.cfg"
  local soft_name="diamond"
  echo "> Test 12: test all valid scheduler resource names"
  for key in c m q t; do
    ANSWER=$(getResource $fname $soft_name $key)
    if [ $? -eq 0 ]; then testOK ; else testKO; fi
  done
}

declare -A cfg_key_values=( ["c"]="8" ["m"]="64g" ["q"]="omp" ["t"]="04:00:00")
function test10(){
  local fname="test_wrapper.cfg"
  local soft_name="diamond"
  echo "> Test 13: test all valid scheduler resource names and values"
  for key in c m q t; do
    local ANSWER=$(getResource $fname $soft_name $key)
    local ret_code=$?
    echo " $ANSWER = ${cfg_key_values[$key]}"
    if [ $ret_code -eq 0 ] && [ "$ANSWER" = ${cfg_key_values[$key]} ]; then testOK ; else testKO; fi
  done
}

declare -A cfg_key_values2=( ["c"]="1" ["m"]="8g" ["q"]="sequentiel" ["t"]="12:00:00")
function test11(){
  local fname="test_wrapper.cfg"
  local soft_name="blastcmd"
  echo "> Test 14: test all valid scheduler resource names and values with override"
  for key in c m q t; do
    local ANSWER=$(getResource $fname $soft_name $key)
    local ret_code=$?
    echo "  $ANSWER = ${cfg_key_values2[$key]}"
    if [ $ret_code -eq 0 ] && [ "$ANSWER" = ${cfg_key_values2[$key]} ]; then testOK ; else testKO; fi
  done
}

declare -A cfg_key_values3=( ["c"]="1" ["m"]="115g" ["q"]="sequentiel" ["t"]="24:00:00")
function test12(){
  local fname="test_wrapper.cfg"
  local soft_name="blastcmd.big"
  echo "> Test 15: test all valid scheduler resource names and values with override"
  for key in c m q t; do
    local ANSWER=$(getResource $fname $soft_name $key)
    local ret_code=$?
    echo "  $ANSWER = ${cfg_key_values3[$key]}"
    if [ $ret_code -eq 0 ] && [ "$ANSWER" = ${cfg_key_values3[$key]} ]; then testOK ; else testKO; fi
  done
}

function test13(){
  local fname="test_wrapper.cfg"
  local soft_name="blastcmd.big"
  local expected_res="1 115g 24:00:00 sequentiel"
  echo "> Test 16: test getting all resources at once"
  local ANSWER=$(getResources $fname $soft_name)
  local ret_code=$?
  echo "  $ANSWER = $expected_res"
  if [ $ret_code -eq 0 ] && [ "$ANSWER" = "$expected_res" ]; then testOK ; else testKO; fi
}

# ========================================================================================
# Start tests
runTests


