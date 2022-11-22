#!/usr/bin/env bash

# Setup a working directory to let BeeDeeM
# install two banks (see below)
[ ! -e $SCRATCH ] && SCRATCH=/tmp
TMP_DIR=$(mktemp bdm.XXXXXXXXXX)
[ ! $? -eq 0 ] && TMP_DIR="bdm-test"

# These are the two mandatory variables to use
# to override default BeeDeeM configuration
export KL_WORKING_DIR=$SCRATCH/$TMP_DIR
export KL_mirror__path=$KL_WORKING_DIR

echo "BeeDeeM Conda test within: $KL_WORKING_DIR"
mkdir -p $KL_WORKING_DIR
# These are two ".dsc" files located in BeeDeem-home/conf/descriptors
# path
DESC_LIST="PDB_proteins,SwissProt_human"
# Start installation
install.sh -desc $DESC_LIST

# Note: by design of this script, we DO NOT delete
# $KL_WORKING_DIR... please, do it yourself.

