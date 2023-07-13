#!/usr/bin/env bash

# #############################################################################
# Test script for BeeDeeM singularity container.
#
# How to?
#
# Step 1: build image with: singularity build -f beedeem-5.0.0.sif Singularity
#         (update version to match BDM_VERSION variable, below)
#
# Step 2: test with either
#         a. ./test_container.sh
#         b. qsub test_container.sh (PBS Pro)
#         c. srun test_container.sh (slurm, direct execution)
#
# P. Durand (SeBiMER, Ifremer), last updated on March 2023
# #############################################################################

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

# #############################################################################

# ###
# Section 1: prepare BeeDeeM test suite

# Version of BeeDeeM to test
BDM_VERSION=5.0.1
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

# ###
# Section 2: prepare environement depending on host
#   BeeDeeM devel team used to work on two clusters, one from 
#   Ifremer (Brest, France), one from Station Biologique (Roscoff, France)
# Depending on host platform, load singularity env
if hasCommand qstat; then
  hname=$(hostname)
  if [[ $hname == *"data"* ]]; then
    echo "running on Ifremer using PBS Pro scheduler"
    source /etc/profile.d/modules.sh
    module purge
    module load singularity/3.4.1
    BDM_SCRATCH_DIR=$SCRATCH
  fi
elif hasCommand sbatch; then
  hname=$(hostname -A)
  if [[ $hname == *"roscoff"* ]]; then
    echo "running on SBR using SLURM scheduler"
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

# ###
# Section 3: prepare SIngularity container

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

# ###
# Section 4: prepare tests
# For debugging if neeeded: dump all BDM_XXX variables
( set -o posix ; set ) | grep "BDM_"
mkdir -p $BDM_BANKS_DIR
mkdir -p $BDM_WORK_DIR


# ###
# Section 5: prepare env variables to be used by BeeDeeM inside the container
# These 3 KL_ variables are mandatory to use BeeDeeM

# KL_JRE_ARGS sets Java Runtime variables (memory and tmpdir) and BeeDeeM log type (none|console|file)
export KL_JRE_ARGS="-Xms128M -Xmx2048M -Djava.io.tmpdir=${BDM_WORK_DIR} -DKL_LOG_TYPE=console"
# KL_WORKING_DIR sets the working directory of BeeDeeM (place to store tmp files at runtime)
export KL_WORKING_DIR=${BDM_WORK_DIR}
# KL_mirror__path sets the directory where BeeDeeM installs banks
export KL_mirror__path=${BDM_BANKS_DIR}

# ###
# Section 6: start tests

# Now, let's start a simple installation
echo "###############################################################################"
echo "# Start BeeDeeM test bank installation"
# These are '.dsc' files located in BeeDeeM image at path /opt/beedeem/conf/descriptors
DESCRIPTOR="SwissProt_human,PDB_proteins" 
CMD="singularity run ${BDM_BINDS} ${BDM_SINGULITY_IMG} bdm install -desc ${DESCRIPTOR}"
echo $CMD
eval $CMD
if [ $? -eq 0 ]; then
  echo "SUCCESS"
else
  echo "FAILED.   Review log file: $BDM_WORK_DIR/log"
  exit 1
fi

# Plast vs SwissProt DB (contains annotations)
echo "###############################################################################"
echo "# Run annotated PLAST using SwissProt human previously installed"
CMD="plast.sh -p plastp -i /opt/beedeem-tools/data/query.fa -d $BDM_BANKS_DIR/p/SwissProt_human/current/SwissProt_human/SwissProt_human00 -o $KL_WORKING_DIR/query_vs_SW.xml -a 4 -maxhits 10 -maxhsps 1 -e 1e-5 -F F"
CMD="singularity run ${BDM_BINDS} ${BDM_SINGULITY_IMG} $CMD"
echo $CMD
eval $CMD
if [ $? != 0  ]; then
  echo "FAILED"
  exit 1
else
  echo "SUCCESS"
fi

# Annotate PLAST result and prepare file for BlastViewer (zml format)
#  see https://github.com/pgdurand/BlastViewer
echo "###############################################################################"
echo "# Annotate results"
CMD="bdm annotate -i $KL_WORKING_DIR/query_vs_SW.xml -o $KL_WORKING_DIR/query_vs_SW.zml -type full -writer zml"
CMD="singularity run ${BDM_BINDS} ${BDM_SINGULITY_IMG} $CMD"
echo $CMD
eval $CMD
if [ $? != 0  ]; then
  echo "FAILED"
  exit 1
else
  echo "SUCCESS"
fi

# Dump previous annotated results as CSV file
#  you may use dumpcsh.h to get help on CSV format
echo "###############################################################################"
echo "# Dump annotated PLAST results as CSV file"
CMD="dumpcsv.sh -i $KL_WORKING_DIR/query_vs_SW.zml -f zml -o $KL_WORKING_DIR/query_vs_SW.csv"
CMD="singularity run ${BDM_BINDS} ${BDM_SINGULITY_IMG} $CMD"
echo $CMD
eval $CMD
if [ $? != 0  ]; then
  echo "FAILED"
  exit 1
else
  echo "SUCCESS"
fi

# BeeDeeM-Tools full test suite
echo "###############################################################################"
echo "# Start BeeDeeM-Tools test suite"
mkdir -p $BDM_WORK_DIR/bdm-tools
CMD="singularity run ${BDM_BINDS} ${BDM_SINGULITY_IMG} /opt/beedeem-tools/test.sh -w $BDM_WORK_DIR/bdm-tools"
echo $CMD
eval $CMD
if [ $? -eq 0 ]; then
  echo "SUCCESS"
else
  echo "FAILED.   Review log file: $BDM_WORK_DIR/log"
  exit 1
fi

