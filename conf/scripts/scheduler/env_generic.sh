#!/usr/bin/env bash

#*****************************************************************************************
# Generic platform specific environment API.
#
# Utility methods to access external tools (Blast, Diamond, etc).
# Generic platform means these tools are available directly from $PATH.
#
# Author: Patrick Durand, Ifremer
# Created: June 2022
#*****************************************************************************************

function activateEnv(){
  local soft_name=$1
  local soft_ver=$2
  local ret_code=0
  if hasCommand $soft_name; then
    infoMsg "Found ${soft_name} in PATH for $BDMC_PLATFORM platform"
  else
    errorMsg "Unknown software: ${soft_name} on $BDMC_PLATFORM platform"
    ret_code=1
  fi
  return $ret_code
}

function deActivateEnv(){
  return 0
}

