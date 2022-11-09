#!/usr/bin/env bash

#*****************************************************************************************
# Basic API.
#
# See script to figure out how to use this API.
#
# Author: Patrick Durand, Ifremer
# Created: June 2022
#*****************************************************************************************

# Set Platform to use for particular tasks
# such as activating/deactivating execution environment.
# Default is set to "generic".
if [  ! "$BDM_PLATFORM"  ]; then
  BDM_PLATFORM="generic"  # Variable defined outside this script framework
  BDMC_PLATFORM="generic" # Variable exclusively used inside the framework
fi

# Turn on/off error messages. 
SILENT="off"

# --------
# FUNCTION: turn all messages ON
function silentOff(){
  SILENT="off"
}
# --------
# FUNCTION: turn all messages OFF
function silentOn(){
  SILENT="on"
}

# --------
# FUNCTION: print out a simple message on stderr (only if SILENT mode is off)
function errorMsg(){
  if [ "$SILENT" == "off" ]; then
    printf "ERROR: $* \n" >&2
  fi
}

# --------
# FUNCTION: print out a simple message on stdout (only if SILENT mode is off)
function infoMsg(){
  if [ "$SILENT" == "off" ]; then
    printf "$* \n"
  fi
}

# --------
# FUNCTION: figure out whether or not a command exists.
#  arg1: a command name.
#  return: 0 if success 
hasCommand () {
    command -v "$1" >/dev/null 2>&1
}

# --------
# FUNCTION: download a file from a remote server.
#           URL is downloaded using curl or wget, saved in
#           current directory and named using provided local
#           file name. Function is capable of resuming a
#           previous aborted job.
#  arg1: local file name
#  arg2: URL
#  return: 0 if success
function downloadFile(){
  filename=$1
  url=$2
  #Use verbose-less download mode to avoid having huge log.
  #Use curl first, since it's more easy to get only error 
  #messages if any
  if hasCommand curl; then
    CMD="curl -sSL -o $filename -C - $url"
  elif hasCommand wget; then
    CMD="wget -c -q $url -O $filename"
  else
    errorMsg "Could not find curl nor wget, please install one of them."
    return 1
  fi
  infoMsg $CMD
  eval $CMD && return 0 || return 1
}

# Setup variables that will be initialized with arguments 
# coming from BeeDeeM task engine. See handleBDMArgs()
# function, below.
BDMC_WK_DIR=
BDMC_INST_DIR=
BDMC_PROCESSED_FILE=
BDMC_BANK_NAME=
BDMC_BANK_TYPE=
BDMC_MORE_ARGS=
#Usually, scripts inherit env variables, except in a special case:
# job submission via ssh tunelling. In this case, calling scripts 
# pass BDM_PLATFORM variable in the qsub command-line using -p arg.
BDMC_PLATFORM=$BDM_PLATFORM

# --------
# FUNCTION: handle command-line arguments coming from BeeDeeM
#           task engine and passed to BeeDeeM external script.
#
#           Such a BeeDeeM script is always  called 
#           with these arguments: 
#           -w <path> -d <path> -f <path> -n <name> -t <type>
#
#  -w <path>: <path> is the working directory path.
#             provided for both unit and global tasks.
#  -d <path>: <path> is the bank installation path.
#             provided for both unit and global tasks.
#  -f <path>: <path> is the path to file under unit task processing
#             only provided with unit task.
#  -n <name>: <name> is the bank name.
#  -t <type>: <path> is the bank type. One of p, n or d.
#             p: protein
#             n: nucleotide
#             d: dictionary or ontology
#  -p <name>: platform name (i.e. a specific cluster configuration)
function handleBDMArgs(){
  infoMsg "Arguments coming from BeeDeeM are: [$@]"
  local OPTIND
  while getopts w:d:f:n:t:p: opt
  do
    case "$opt" in
      w)  BDMC_WK_DIR="$OPTARG";;
      d)  BDMC_INST_DIR="$OPTARG";;
      f)  BDMC_PROCESSED_FILE="$OPTARG";;
      n)  BDMC_BANK_NAME="$OPTARG";;
      t)  BDMC_BANK_TYPE="$OPTARG";;
      p)  BDMC_PLATFORM="$OPTARG";;
    esac
  done
  shift `expr $OPTIND - 1`
  BDMC_MORE_ARGS=$@

  infoMsg "Working dir: $BDMC_WK_DIR"
  infoMsg "Install dir: $BDMC_INST_DIR"
  infoMsg "Processed file: $BDMC_PROCESSED_FILE"
  infoMsg "Bank name: $BDMC_BANK_NAME"
  infoMsg "Bank type: $BDMC_BANK_TYPE"
  infoMsg "Platform: $BDMC_PLATFORM"
  infoMsg "Remaining args: $BDMC_MORE_ARGS"
}

# --------
# FUNCTION: get a resource value for a job scheduler.
#  arg1: a path to a .cfg file
#  arg2: name of a tool.
#  arg3: one of: [c]pu, [m]em, [q]ueue or wall[t]ime.
#  return: 0 if success. Resource value is echoed.
declare -A cfg_keys=( ["c"]="cpu" ["m"]="mem" ["q"]="queue" ["t"]="time")
function getResource(){
  local file_cfg=$1
  local tool_cfg=$2
  local key_cfg=${cfg_keys[$3]}

  # Does resource key exist?
  [ -z $key_cfg ]&& errorMsg "Resource key \"$3\" is unknown. Use one of: [c]pu, [m]em, [q]ueue or wall[t]ime." && return 1
  # Does resource file exist?
  [ ! -f "$file_cfg" ] && errorMsg "File not found: $file_cfg" && return 1
  # Get default value first (maybe not provided)
  local value_cfg=$(grep "default\.${key_cfg}" $file_cfg | cut -d'=' -f2 | xargs)
  # Override default value if any other resource is provided for a tool
  # Providing non exisiting key is ok: default value is used, then. 
  local tmp_value=$(grep "${tool_cfg}\.${key_cfg}" $file_cfg)
  if [ ! -z $tmp_value ]; then
    value_cfg=$(echo ${tmp_value} | cut -d'=' -f2 | xargs)
  fi
  # No value at all?
  [ -z $value_cfg ] && errorMsg "Value not found for key \"${tool_cfg}\.${key_cfg}\" in file: $file_cfg" && return 1
  echo $value_cfg
}

# --------
# FUNCTION: get a resource value for a job scheduler.
#  arg1: a path to a .cfg file
#  arg2: name of a tool.
#  return: 0 if success. Resources for cpu, mem, walltime and queue 
#  are echoed as 4-token string (space separated).
function getResources(){
  local resources=()
  local res_value=$(getResource $1 $2 "c") #CPU
  [ $? -eq 0 ] && resources+=($res_value) || return 1
  res_value=$(getResource $1 $2 "m")   #MEM
  [ $? -eq 0 ] && resources+=($res_value) || return 1
  res_value=$(getResource $1 $2 "t") #WALLTIME
  [ $? -eq 0 ] && resources+=($res_value) || return 1
  res_value=$(getResource $1 $2 "q") #QUEUE
  [ $? -eq 0 ] && resources+=($res_value) || return 1
  echo ${resources[@]}
}
# --------
# FUNCTION: figure out which Job Scheduler is available on host system
# return: 0 if job scheduler found, 1 otherwise. Job scheduler name is echoed.
function getScheduler(){
  local ret_value=
  if hasCommand qstat; then
    ret_value=$(qstat --version)
    [[ "$ret_value" =~ "pbs" ]] && echo "pbs" && return 0
  elif hasCommand sbatch; then
    ret_value=$(sbatch --version)
    [[ "$ret_value" =~ "slurm" ]] && echo "slurm" && return 0
  else
    echo "lcl"
  fi
  return 1
}

