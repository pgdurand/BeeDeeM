#!/usr/bin/env bash

#*****************************************************************************************
# Local Wrapper API for Bash.
#
# In case, no job scheduler is available (pbs, slurm, etc), then this will be
# the default job execution engine: run jobs direclty on local computer.
#
# See test script to figure out how to use this API.

# Author: Patrick Durand, Ifremer
# Created: November 2022
#*****************************************************************************************

# Turn on/off error messages.
SILENT="off"

# Log some informations
if [ "$SILENT" == "off" ]; then
  echo "Scheduler is local (job is executed on this computer)"
fi

# --------
# FUNCTION: print out an simple message on stderr (only if SILENT mode is off)
function errorMsg(){
  if [ "$SILENT" == "off" ]; then
    printf "$* \n" >&2
  fi
}

# Note: below function definitions remain exactly the same as
#       for SLURM and PBS wrappers, so that they can be used 
#       without any modification of caller scripts.

# --------
# FUNCTION: submit a script to local computer
#  arg1: path to script to execute on local machine. 
#  arg2: optional. When set, this argument sets up a Log
#        directory.
#  return: 0 if success. For local job execution, this method
#  hangs until job is done. Then, method echoes LOG_FILE name.
function submit(){
  if [ "$#" -eq 2 ]; then
    LCL_LOG_FILE=$(mktemp -p $2 bdm.XXXXXXXXXX)
  else
    LCL_LOG_FILE=$(mktemp bdm.XXXXXXXXXX)
  fi
  $1 >& $LCL_LOG_FILE 2>&1
  RET_CODE=$?
  #Put exit code at the end of the log file
  #It is then retrievable by waitForJobToFinish() method, see below
  echo "exit:$RET_CODE" >> $LCL_LOG_FILE
  #Dump log file path as the answer of this method
  echo $LCL_LOG_FILE
  if [ ! $RET_CODE -eq 0 ];then
    errorMsg "ERROR: Unable to execute $1 on local computer"
  fi
  return $RET_CODE
}

# --------
# FUNCTION: submit a script to a job scheduler. For local job
# execution, this method calls directly submit($7 $6).
#  arg1: queue name
#  arg2: memory
#  arg3: nb CPUs
#  arg4: walltime
#  arg5: name of process
#  arg6: directory to redirect error and message logs
#  arg7: path to script to execute
#  return: 0 if success. For local job execution, this method
#  hangs until job is done. Then, method echoes LOG_FILE name.
function submitEx(){
  #Note: quote $7 since it can be a script name followed by some arguments
  ANSWER=$(submit "$7" $6)
  RET_CODE=$?
  echo $ANSWER
  return $RET_CODE
}

# --------
# FUNCTION: get status code of a job
#  DEPRECATED for local job runner
#  return: always 0
function getStatus(){
  return 0
}

# --------
# FUNCTION: get Exit status of a job
#  DEPRECATED for local job runner
#  return: always 0
function getExitCode(){
  return 0
}
# --------
# FUNCTION: wait for a job to finish, i.e. until status of
#           job is COMPLETED
#  arg1: job ID 
#   return:
#     0: success, i.e. job finished with exit_status=0
#     1: failure, i.e. job finished with exit_status!=0
#     2 or 3: failure, i.e. unable to get job status
#     4: failure, i.e. unable to get Exit code
function waitForJobToFinish(){
  #for local job runner, job ID is actually the log file
  #which contains exit code of called script at the end of
  #that file. See submit() method, above.
  local EXIT_CODE=$(tail -n1 $1)
  if [[ $EXIT_CODE == exit* ]]; then
    EXIT_CODE=$(echo $EXIT_CODE | cut -d':' -f2)
    [[ $EXIT_CODE -eq 0 ]] && return 0 || return 1
  else
    return 4
  fi
}
# --------
# FUNCTION: dump job log file to stdout
#  arg1: Log directory
#  arg2: job ID
#  return: 0 if success
function dumpJobLog(){
  #For local runner, JOB ID is actually the log file
  LCL_LOG_FILE=$2
  RET_CODE=0
  if [ -e $LCL_LOG_FILE ]; then
    cat $LCL_LOG_FILE
    RET_CODE=$?
  else
    echo "Job log file not found: $LCL_LOG_FILE"
    RET_CODE=1
  fi
  return $RET_CODE
}
# --------
# FUNCTION: remove log file
#  arg1: Log directory
#  arg2: job ID
#  return: 0 if success
function removeJobLog(){
  #For local runner, JOB ID is actually the log file
  LCL_LOG_FILE=$2
  local RET_CODE=0
  if [ -e $LCL_LOG_FILE ]; then
    rm -f $LCL_LOG_FILE
    RET_CODE=$?
  fi
  return $RET_CODE
}

if [ "$SILENT" == "off" ]; then
  echo "Job execution method: lcl_wrapper loaded"
fi

