#!/usr/bin/env bash

#*****************************************************************************************
# ABiMS platform specific environment API.
#
# On ABiMS' cluster (Station Biologique de Rocoff), all external tools are accessible from 
# linux module environments.
#
# Author: Patrick Durand, Ifremer
# Created: November 2022
#*****************************************************************************************

function activateEnv(){
  local soft_name=$1
  local soft_ver=$2
  module load ${soft_name}/${soft_ver}
  if [ $? -eq 0 ]; then
    infoMsg "activate ${soft_name} ${soft_ver} environment for $BDMC_PLATFORM platform"
  else
    errorMsg "Unknown software: ${soft_name}-${soft_ver}"
    return 1
  fi
}

function deActivateEnv(){
  local soft_name=$1
  local soft_ver=$2
  module unload ${soft_name}/${soft_ver}
  if [ $? -eq 0 ]; then
    infoMsg "deActivate ${soft_name} ${soft_ver} environment for $BDMC_PLATFORM platform"
  else
    errorMsg "Unknown software: ${soft_name}-${soft_ver}"
    return 1
  fi
}

