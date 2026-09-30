#!/usr/bin/env bash

#*****************************************************************************************
# Ifremer platform specific environment API.
#
# On Ifremer's DATARMOR cluster, all external tools are accessible from 
# CONDA environements.
#
# Author: Patrick Durand, Ifremer
# Created: June 2022
# Updated: Sept 2026 (module load)
#*****************************************************************************************

function activateEnv(){
  local soft_name=$1
  local soft_ver=$2
  local ret_code=0
  module load ${soft_name}/${soft_ver}
  if [ $? -eq 0 ]; then
    infoMsg "activate ${soft_name} ${soft_ver} environment for $BDMC_PLATFORM platform"
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
  module unload ${soft_name}/${soft_ver}
  if [ $? -eq 0 ]; then
    infoMsg "deActivate ${soft_name} ${soft_ver} environment for $BDMC_PLATFORM platform"
  else
    errorMsg "Unknown software: ${soft_name}-${soft_ver}"
    ret_code=1
  fi
  return $ret_code
}
