#!/usr/bin/env bash

# Test script for BeeDeeM singularity container
# How to?
# Step 1: build image with: singularity build -f beedeem-4.7.4.sif Singularity
#         (update version to match BDM_VERSION variable, below)
# Step 2: test with either
#         a. ./test_container.sh
#         b. qsub test_container.sh (PBS Pro)
#         c. srun test_container.sh (slurm, direct execution)
#
# P. Durand (SeBiMER, Ifremer), last update on Nov 2022

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
BDM_VERSION=4.7.4
# Default working directory to test BeeDeeM Singularity image.
# Il will be overriden below, given host platform
BDM_SCRATCH_DIR=/tmp
# What is the name of the BeeDeeM image
BDM_SING_IMG_NAME=beedeem-${BDM_VERSION}.sif
# Where to find the BeeDeeM image
BDM_SING_IMG_HOME=$HOME/devel/BeeDeeM/singularity
# If BeeDeeM image is not located in above path, try to get it from SeBiMER repo
BDM_SING_PUBLIC_REPO=https://data-dataref.ifremer.fr/bioinfo/ifremer/sebimer/tools/ORSON

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
    echo "ERROR: Could not find curl nor wget, please install one of them."
    return 1
  fi
  echo $CMD
  eval $CMD && return 0 || return 1
}

# Depending on host platform, load singularity env
if hasCommand qstat; then
  echo "running on DATARMOR using PBS Pro scheduler"
  source /etc/profile.d/modules.sh
  module purge
  module load singularity/3.4.1
  BDM_SCRATCH_DIR=$SCRATCH
elif hasCommand sbatch; then
  hname=$(hostname -A)
  if [[ $hname == *"roscoff"* ]]; then
    echo "running on ABiMS using SLURM scheduler"
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

# Check existence of the BeeDeeM image
if [ ! -e "$BDM_SING_IMG_HOME/$BDM_SING_IMG_NAME" ]; then
  echo "WARN: $BDM_SING_IMG_HOME/$BDM_SING_IMG_NAME not found."
  echo "      Trying to downlod from: $BDM_SING_PUBLIC_REPO/$BDM_SING_IMG_NAME"
  cd $BDM_WORK_DIR
  downloadFile "$BDM_SING_IMG_NAME" "$BDM_SING_PUBLIC_REPO/$BDM_SING_IMG_NAME"
  RET_CODE=$?
  cd -
  BDM_SING_IMG_HOME=$BDM_WORK_DIR
  if [ ! $RET_CODE -eq 0 ]; then
     echo "ERROR: unable fo get BeeDeeM image"
     exit 1
  fi 
fi

# Configure Singularity runner
BDM_BINDS="--bind ${BDM_WORK_DIR} --bind ${BDM_BANKS_DIR}"
BDM_SINGULITY_IMG="$BDM_SING_IMG_HOME/$BDM_SING_IMG_NAME"
# For debugging if neeeded: dump all BDM_XXX variables
( set -o posix ; set ) | grep "BDM_"

#rm -rf $BDM_BANKS_DIR
mkdir -p $BDM_BANKS_DIR
#rm -rf $BDM_WORK_DIR
mkdir -p $BDM_WORK_DIR

# Prepare env variables to be used by BeeDeeM inside the container (mandatory)
export KL_JRE_ARGS="-Xms128M -Xmx2048M -Djava.io.tmpdir=${BDM_WORK_DIR} -DKL_LOG_TYPE=console"
export KL_WORKING_DIR=${BDM_WORK_DIR}
export KL_mirror__path=${BDM_BANKS_DIR}

# Set the banks to install
# These are '.dsc' files located in BeeDeeM image at path /opt/beedeem/conf/descriptors
DESCRIPTOR="SwissProt_human,PDB_proteins,MEROPS_PepUnits"

# Now, let's start a simple installation
echo "Start simple bank installation"
echo "Look at processing with:"
echo "tail -f $BDM_WORK_DIR/log"

singularity run \
  ${BDM_BINDS} \
  ${BDM_SINGULITY_IMG} \
  install.sh -desc ${DESCRIPTOR} >& ${BDM_WORK_DIR}/log 2>&1

if [ $? -eq 0 ]; then
  echo "SUCCESS"
else
  echo "FAILED.   Review log file: $BDM_WORK_DIR/log"
  exit 1
fi

