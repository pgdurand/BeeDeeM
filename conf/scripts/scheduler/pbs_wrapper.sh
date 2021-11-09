#!/usr/bin/env bash

#*****************************************************************************************
# PBS Wrapper API for Bash.
#
# See test script to figure out how to use this API.

# Author: Patrick Durand, Ifremer
# Created: October 2021
#*****************************************************************************************

# Turn on/off error messages. 
SILENT="off"

# Keep in mind that querying PBS to get job status too frequently is 
# a bad practice! (unit is seconds)
WAIT_TIME=60

# Set job commands to use. Enable "export" of theses variables
# outside this script and before calling it.
QSTAT_CMD=$BDM_QSTAT_CMD
if [ ! "$QSTAT_CMD" ]; then
  QSTAT_CMD="qstat"
fi
QSUB_CMD=$BDM_QSUB_CMD
if [ ! "$QSUB_CMD" ]; then
  QSUB_CMD="qsub"
fi

# Log some informations
if [ "$SILENT" == "off" ]; then
  echo "Scheduler is PBS Pro"
  echo "   job submission command is: $QSUB_CMD"
  echo "       job status command is: $QSTAT_CMD"
fi

# --------
# FUNCTION: print out an simple message on stderr (only if SILENT mode is off)
function errorMsg(){
  if [ "$SILENT" == "off" ]; then
    printf "$* \n" >&2
  fi
}

# --------
# FUNCTION: submit a script to PBS 
#  arg1: path to script to submit to PBS. This script is 
#        required to have PBS directives within its header.
#  arg2: optional. When set, this argument sets up a Log
#        directory.
#  return: 0 if success
function submit(){
  if [ "$#" -eq 2 ]; then
    CMD="$QSUB_CMD -V -S \"/bin/bash\" -j oe -o $2 -e $2 $1"
  else
    CMD="$QSUB_CMD -V -S \"/bin/bash\" $1"
  fi
  ANSWER=$(eval $CMD)
  RET_CODE=$?
  if [ $RET_CODE -eq 0 ];then
    JOB_ID=${ANSWER##*( )}
    echo "$JOB_ID"
    return 0
  else
    errorMsg "ERROR: Unable to submit $7 to PBS" 
    return 1
  fi  
}

# --------
# FUNCTION: submit a script to PBS 
#  arg1: queue name
#  arg2: memory
#  arg3: nb CPUs
#  arg4: walltime
#  arg5: name of process
#  arg6: directory to redirect error and message logs
#  arg7: path to script to submit to PBS
#  return: 0 if success
function submitEx(){
  CMD="$QSUB_CMD -V -q $1 -l mem=$2 -l ncpus=$3 -l walltime=$4 -N $5 -m n -j oe -S \"/bin/bash\" -o $6 -e $6 $7"
  ANSWER=$(eval $CMD)
  RET_CODE=$?
  if [ $RET_CODE -eq 0 ];then
    JOB_ID=${ANSWER##*( )}
    echo "$JOB_ID"
    return 0
  else
    errorMsg "ERROR: Unable to submit $7 to PBS"
    return 1
  fi
}

# --------
# FUNCTION: get status code of a job
#  arg1: job ID
#  return: 0 if success
function getStatus(){
  CMD="$QSTAT_CMD -fx $1 | grep \"job_state\""
  ANSWER=$(eval $CMD)
  RET_CODE=$?
  if [ $RET_CODE -eq 0 ];then
    STATUS=$(echo $ANSWER | cut -d'=' -f2 | sed -e 's/^[[:space:]]*//')
    echo $STATUS
    return 0
  else
    errorMsg "ERROR: Unable to get status for job $1"
    return 1
  fi
}

# --------
# FUNCTION: get Exit status of a job
#  arg1: job ID
#  return: 0 if success
function getExitCode(){
  CMD="$QSTAT_CMD -fx $1 | grep \"Exit_status\""
  ANSWER=$(eval $CMD)
  RET_CODE=$?
  if [ $RET_CODE -eq 0 ];then
    STATUS=$(echo $ANSWER | cut -d'=' -f2 | sed -e 's/^[[:space:]]*//')
    echo $STATUS
    return 0
  else
    errorMsg "ERROR: Unable to get Exit status for job $1"
    return 1
  fi
}
# --------
# FUNCTION: wait for a job to finish, i.e. until status of
#           job is F
#   arg1: job ID 
#   arg2: wait time to schedule PBS (qstat). Optional, 
#         default is 60 seconds. Remember that using
#         qstat too frequently is a very bad practice.
#   return:
#     0: success, i.e. job finished with exit_status=0
#     1: failure, i.e. job finished with exit_status!=0
#     2 or 3: failure, i.e. unable to get job status
#     4: failure, i.e. unable to get Exit code
function waitForJobToFinish(){
  JID=$1
  if [ "$#" -eq 2 ]; then
    WTIME=$2
  else
    WTIME=$WAIT_TIME
  fi
  STATUS=$(getStatus $JID)
  RET_CODE=$?
  if [ ! $RET_CODE -eq 0 ];then
    return 2
  fi
  while [ $RET_CODE -eq 0 -a ! "$STATUS" == "F" ]; do
    sleep $WTIME
    STATUS=$(getStatus $JID)
    RET_CODE=$?
    if [ ! $RET_CODE -eq 0 ];then
      return 3
    fi
  done
  EXIT_CODE=$(getExitCode $JID)
  RET_CODE=$?
  if [ ! $RET_CODE -eq 0 ];then
    return 4
  fi
  return $EXIT_CODE
}
# --------
# FUNCTION: dump job log file to stdout
#  arg1: job ID
#  return: 0 if success
function dumpJobLog(){
  LOG_FILE=$1/$2.OU
  RET_CODE=255
  # PBS may take some time to create log file
  # so we wait for it a little amount of time
  GET_LOG_COUNT=0
  while [ $GET_LOG_COUNT -lt 5 ]; do
    if [ -e $LOG_FILE ]; then
      cat $LOG_FILE
      RET_CODE=$?
      break
    fi
    echo "Wait for log file to be ready. $GET_LOG_COUNT"
    sleep 15 
    ((GET_LOG_COUNT++))
  done
  if [  $RET_CODE -eq 255 ]; then
    if [ ! -e $LOG_FILE ]; then
      echo "Job log file not found: $LOG_FILE"
    fi
  fi
  return $RET_CODE
}
# --------
# FUNCTION: remove log file
#  arg1: job ID
#  return: 0 if success
function removeJobLog(){
  LOG_FILE=$1/$2.OU
  RET_CODE=0
  if [ -e $LOG_FILE ]; then
    rm -f $LOG_FILE
    RET_CODE=$?
  fi
  return $RET_CODE
}

