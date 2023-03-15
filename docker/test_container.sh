#!/usr/bin/env bash

# Test script for BeeDeeM docker container
# How to?
# Step 1: build image with: docker build -f Dockerfile -t beedeem-$BDM_VERSION .
#         (update version to match BDM_VERSION variable, below)
# Step 2: test with either
#         a. ./test_container.sh
#         b. qsub test_container.sh (PBS Pro)
#         c. srun test_container.sh (slurm, direct execution)
#
# P. Durand (SeBiMER, Ifremer), last update on March 2023

# Sample config for Slurm; adapt partition to your cluster configuration
#SBATCH -p fast
#SBATCH --mem 4GB
#SBATCH -t 0-02:00                  #(D-HH:MM)
#SBATCH -o lftp.%N.%j.out           # STDOUT
#SBATCH -e lftp.%N.%j.err           # STDERR

# Sample config for PBS pro; adapt queue to you cluster configuration
#PBS -q ftp
#PBS -l walltime=02:00:00
#PBS -l mem=4g
#PBS -l ncpus=2

# Version of BeeDeeM to test
BDM_VERSION=5.0.0
# Default working directory to test BeeDeeM Docker image.
# Il will be overriden below, given host platform
BDM_SCRATCH_DIR=/tmp
# What is the name of the BeeDeeM image
BDM_DKR_IMG_NAME=beedeem-${BDM_VERSION}

# --------
# FUNCTION: figure out whether or not a command exists.
#  arg1: a command name.
#  return: 0 if success 
hasCommand () {
    command -v "$1" >/dev/null 2>&1
}

# Depending on host platform, load singularity env
if hasCommand qstat; then
  hname=$(hostname)
  if [[ $hname == *"data"* ]]; then
    echo "running on DATARMOR using PBS Pro scheduler"
    source /etc/profile.d/modules.sh
    module purge
    module load singularity/3.4.1
    BDM_SCRATCH_DIR=$SCRATCH
  fi
elif hasCommand sbatch; then
  hname=$(hostname -A)
  if [[ $hname == *"roscoff"* ]]; then
    echo "running on ABiMS using SLURM scheduler"
    BDM_PLATFORM="abims"
    BDM_SCRATCH_DIR=$HOME
  fi
else
  echo "Cannot figure out which job scheduler is available."
  echo "  Execute BeeDeeM directly on THIS computer"
fi

# Configure BeeDeeM banks and working directories
BDM_SCRATCH_DIR="$BDM_SCRATCH_DIR/test_beedeem"
BDM_BANKS_DIR="$BDM_SCRATCH_DIR/banks"
BDM_WORK_DIR="$BDM_SCRATCH_DIR/working"

# Configure Singularity runner
BDM_BINDS="-v ${BDM_WORK_DIR}:${BDM_WORK_DIR} -v ${BDM_BANKS_DIR}:${BDM_BANKS_DIR}"

# For debugging if neeeded: dump all BDM_XXX variables
( set -o posix ; set ) | grep "BDM_"

# Ensure working pathes exist
mkdir -p $BDM_BANKS_DIR
mkdir -p $BDM_WORK_DIR

# Prepare env variables to be used by BeeDeeM inside the container (mandatory)
KL_JRE_ARGS="-Xms128M -Xmx2048M -Djava.io.tmpdir=${BDM_WORK_DIR} -DKL_LOG_TYPE=console"
KL_WORKING_DIR=${BDM_WORK_DIR}
KL_mirror__path=${BDM_BANKS_DIR}

# Set the banks to install
# These are '.dsc' files located in BeeDeeM image at path /opt/beedeem/conf/descriptors
DESCRIPTOR="SwissProt_human,PDB_proteins"

# Now, let's start a simple installation
echo "1/2 - Start BeeDeeM test: run a bank installation"
CMD_BASE="docker run --name $BDM_DKR_IMG_NAME -i -t --rm -e \"KL_JRE_ARGS=$KL_JRE_ARGS\" -e \"KL_WORKING_DIR=$KL_WORKING_DIR\" -e \"KL_mirror__path=$KL_mirror__path\" $BDM_BINDS $BDM_DKR_IMG_NAME"
#CMD="$CMD_BASE bdm -h"
CMD="$CMD_BASE bdm install -desc $DESCRIPTOR"
echo $CMD
eval $CMD
if [ $? -eq 0 ]; then
  echo "SUCCESS"
else
  echo "FAILED.   Review logs, above"
  exit 1
fi

echo

echo "2/2 - Start BeeDeeM-Tools test suite"
mkdir -p $BDM_WORK_DIR/bdm-tools
CMD="$CMD_BASE /opt/beedeem-tools/test.sh -w $BDM_WORK_DIR/bdm-tools"
echo $CMD
eval $CMD
if [ $? -eq 0 ]; then
  echo "SUCCESS"
else
  echo "FAILED.   Review logs, above"
  exit 1
fi



