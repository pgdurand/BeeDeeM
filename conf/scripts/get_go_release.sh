#!/usr/bin/env bash

# This is a BeeDeeM external task script aims at getting
# GeneOntology release number.
#
# This script is used in bank descriptor:
#   ../descriptors/GeneOntology_terms.dsc
#
# Such a BeeDeeM script is called by the task engine and
# with arguments as defined in: 
# ./scheduler/common.sh->handleBDMArgs() function

echo "Getting Gene Ontology release number"

# ========================================================================================
# Section: include API
S_NAME=$(realpath "$0")
[[ -z "$BDM_CONF_SCRIPTS" ]] && script_dir=$(dirname "$S_NAME") || script_dir=$BDM_CONF_SCRIPTS
. $script_dir/scheduler/common.sh

# ========================================================================================
# Section: handle arguemnts
# Function call setting BDMC_xxx variables from cmdline arguments
handleBDMArgs $@
RET_CODE=$?
[ ! $RET_CODE -eq 0 ] && errorMsg "Wrong or missing arguments" && exit $RET_CODE

# ========================================================================================
# Section: do business

#Get GO official release number
FNAME=${BDMC_INST_DIR}/go-basic.obo
RELEASE=$(head -n 10 $FNAME | grep "data-version:" | cut -d'/' -f 2)
echo "release.time.stamp=$RELEASE" > ${BDMC_INST_DIR}/release-date.txt

