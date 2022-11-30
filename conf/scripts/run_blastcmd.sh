#!/usr/bin/env bash

set -euo pipefail 

# TO TEST:
# DATARMOR: ./run_blastcmd.sh -w /home/datawork-bioinfo-ss/beedeem/ -d /home/datawork-bioinfo-ss/beedeem/test-scheduler-scripts/p/PDB_proteins/current/PDB_proteins -n PDB_proteins -t p -p ifremer

# ABiMS: export BDM_CONF_SCRIPTS=/home/externe/ifremer/pdurand/beedeem/BeeDeeM/conf/scripts ; ./run_blastcmd.sh -w /shared/projects/metabarcoding2020/pdurand/beedeem-test-wk -d /shared/projects/metabarcoding2020/pdurand/beedeem-test/p/SwissProt_human/current/SwissProt_human -n SwissProt_human -t p -p abims

# This is a BeeDeeM external task script aims at running
# a Blastdbcmd job. 
#
# Such a BeeDeeM script is called by the task engine and
# with arguments as defined in: 
# ./scheduler/common.sh->handleBDMArgs() function

echo "Make a FASTA file out of a BLAST bank (blastdbcmd)"

# ========================================================================================
# Section: include API
S_NAME=$(realpath "$0")
[[ -z "$BDM_CONF_SCRIPTS" ]] && script_dir=$(dirname "$S_NAME") || script_dir=$BDM_CONF_SCRIPTS
. $script_dir/scheduler/common.sh 
JOB_SCHEDULER=$(getScheduler)
if [ $? -eq 0 ]; then
  . $script_dir/scheduler/${JOB_SCHEDULER}_wrapper.sh
else 
  errorMsg "No Job Scheduler found" 
  exit 1
fi

# ========================================================================================
# Section: handle arguemnts
# Function call sets BDMC_xxx variables from cmdline arguments
handleBDMArgs $@
RET_CODE=$?
[ ! $RET_CODE -eq 0 ] && errorMsg "Wrong or missing arguments" && exit $RET_CODE

# ========================================================================================
# Section: do business
# Get job scheduler resource file using canonical naming
CFG_SCHEDULER="$script_dir/scheduler/${JOB_SCHEDULER}_${BDMC_PLATFORM}.cfg"
CFG_SOFT="blastcmd"
# Get all resources at one as a string
CFG_RESOURCES=$(getResources $CFG_SCHEDULER $CFG_SOFT)
RET_CODE=$?
[ ! $RET_CODE -eq 0 ] && errorMsg "Unable to get resources to submit job" && exit $RET_CODE
# Convert that string as an array
CFG_RESOURCES=($CFG_RESOURCES)
# Go!
MY_SCRIPT="$script_dir/run_$CFG_SOFT.job -d $BDMC_INST_DIR -n $BDMC_BANK_NAME -p $BDMC_PLATFORM "
echo "> Submitting ${MY_SCRIPT}..."
ANSWER=$(submitEx "${CFG_RESOURCES[3]}" "${CFG_RESOURCES[1]}" "${CFG_RESOURCES[0]}" "${CFG_RESOURCES[2]}" "$CFG_SOFT-$BDMC_BANK_NAME" "$BDMC_WK_DIR" "${MY_SCRIPT}")
RCODE=$?
[ ! $RCODE -eq 0 ] && echo $ANSWER && exit $RCODE
JOB_ID=$ANSWER
echo "> Job ID is: $JOB_ID"
echo "> Waiting for job to complete..."
ANSWER=$(waitForJobToFinish $JOB_ID)
RCODE=$?
[ $RCODE -eq 0 ] && echo "SUCCESS" || echo "$ANSWER \nFAILED: $RCODE"
echo "> Begin job log:"
dumpJobLog $BDMC_WK_DIR $JOB_ID
echo "< End Job log"
removeJobLog $BDMC_WK_DIR $JOB_ID
exit $RCODE



