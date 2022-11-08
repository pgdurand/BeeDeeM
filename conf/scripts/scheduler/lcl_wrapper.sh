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
  ANSWER=$(submit $7 $6)
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
#  DEPRECATED for local job runner
#  return: always 0
function waitForJobToFinish(){
  return 0
}
# --------
# FUNCTION: dump job log file to stdout
#  arg1: Log file
#  return: 0 if success
function dumpJobLog(){
  LCL_LOG_FILE=$1
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
#  arg1: Log file
#  return: 0 if success
function removeJobLog(){
  local RET_CODE=0
  if [ -e $LCL_LOG_FILE ]; then
    rm -f $LCL_LOG_FILE
    RET_CODE=$?
  fi
  return $RET_CODE
}
