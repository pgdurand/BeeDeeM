#!/usr/bin/env bash

# Config for Slurm
#SBATCH -p fast
#SBATCH --mem 4GB
#SBATCH -t 0-02:00                  #(D-HH:MM)
#SBATCH -o lftp.%N.%j.out           # STDOUT
#SBATCH -e lftp.%N.%j.err           # STDERR

# Config for PBS pro
#PBS -q ftp
#PBS -l walltime=02:00:00
#PBS -l mem=4g
#PBS -l ncpus=2


# Test script for BeeDeeM singularity container
# How to?
# Step 1: build image with: singularity build -f beedeem-4.7.2.sif Singularity
#         (update version to match BDM_VERSION variable, below)
# Step 2: test with: ./test_container.sh


# Configure BeeDeeM banks and working directories
SCRATCH="$HOME/test_beedeem"
BDM_BANKS_DIR=$SCRATCH/banks
BDM_WORK_DIR=$SCRATCH/working
#rm -rf $BDM_BANKS_DIR
mkdir -p $BDM_BANKS_DIR
#rm -rf $BDM_WORK_DIR
mkdir -p $BDM_WORK_DIR

# Configure Singularity runner
BDM_VERSION=4.7.2
BDM_BINDS="--bind ${BDM_WORK_DIR} --bind ${BDM_BANKS_DIR}"
BDM_SINGULITY_IMG="beedeem-${BDM_VERSION}.sif"

# Prepare env variables to be used by BeeDeeM inside the container (mandatory)
export KL_JRE_ARGS="-Xms128M -Xmx2048M -Djava.io.tmpdir=${BDM_WORK_DIR} -DKL_LOG_TYPE=console"
export KL_WORKING_DIR=${BDM_WORK_DIR}
export KL_mirror__path=${BDM_BANKS_DIR}

# Set the banks to install
DESCRIPTOR="SwissProt_human,PDB_proteins,MEROPS_PepUnits"

# Now, let's start a simple installation
echo "Start simple bank installation"
echo "Look at processing with:"
echo "tail -f $BDM_WORK_DIR/log"

singularity run \
  ${BDM_BINDS} \
  ${BDM_SINGULITY_IMG} \
  install.sh -desc ${DESCRIPTOR} >& ${BDM_WORK_DIR}/log

if [ ! $? -eq 0 ]; then
  echo "FAILED.   Review log file:"
  exit 1
fi

