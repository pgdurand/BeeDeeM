#!/usr/bin/env bash

#*****************************************************************************************
# Ifremer platform specific environment API.
#
# On Ifremer's DATARMOR cluster, all external tools are accessible from 
# CONDA environements.
#
# Author: Patrick Durand, Ifremer
# Created: June 2022
#*****************************************************************************************

function activateEnv(){
  local soft_name=$1
  local soft_ver=$2
  local ret_code=0
  local file_path=/appli/bioinfo/${soft_name}/${soft_ver}/env.sh
  if [ -e "$file_path" ]; then
    infoMsg "activate ${soft_name} ${soft_ver} environment for $BDMC_PLATFORM platform"
    source $file_path
  else
    errorMsg "Unknown software: ${soft_name}-${soft_ver}"
    ret_code=1
  fi
  return $ret_code
}

function deActivateEnv(){
  local soft_name=$1
  local soft_ver=$2
  local ret_code=0
  local file_path=/appli/bioinfo/${soft_name}/${soft_ver}/delenv.sh
  if [ -e "$file_path" ]; then
    infoMsg "deActivate ${soft_name} ${soft_ver} environment for $BDMC_PLATFORM platform"
    source $file_path
  else
    errorMsg "Unknown software: ${soft_name}-${soft_ver}"
    ret_code=1
  fi
  return $ret_code
}

