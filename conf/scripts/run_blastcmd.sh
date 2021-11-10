#!/usr/bin/env bash

# This is a BeeDeeM external task script aims at running
# a Blastdbcmd job. 
#
# Such a BeeDeeM script is called by the task engine and
# with these arguments: -w <path> -d <path> -f <path> -n <name> -t <type>
#
#  -w <path>: <path> is the working directory path.
#             provided for both unit and global tasks.
#  -d <path>: <path> is the bank installation path.
#             provided for both unit and global tasks.
#  -f <path>: <path> is the path to file under unit task processing
#             only provided with unit task.
#  -n <name>: <name> is the bank name.
#  -t <type>: <path> is the bank type. One of p, n or d.
#             p: protein
#             n: nucleotide
#             d: dictionary or ontology

echo "----"
echo "Make a FASTA file out of a BLAST bank (blastdbcmd)"
echo "Arguments coming from BeeDeeM are:"
echo $@
echo "----"
# Prepare arguments for processing
WK_DIR=
INST_DIR=
PROCESSED_FILE=
BANK_NAME=
BANK_TYPE=
while getopts w:d:f:n:t: opt
do
    case "$opt" in
      w)  WK_DIR="$OPTARG";;
      d)  INST_DIR="$OPTARG";;
      f)  PROCESSED_FILE="$OPTARG";;
      n)  BANK_NAME="$OPTARG";;
      t)  BANK_TYPE="$OPTARG";;
    esac
done
shift `expr $OPTIND - 1`
# remaining arguments, if any, are stored here
MORE_ARGS=$@

echo "Working dir: $WK_DIR"
echo "Install dir: $INST_DIR"
echo "Processed file: $PROCESSED_FILE"
echo "Bank name: $BANK_NAME"
echo "Bank type: $BANK_TYPE"
echo "----"

# ========================================================================================
# Section: include API
script_dir=$( cd -P -- "$(dirname -- "$(command -v -- "$0")")" && pwd -P )
. $script_dir/scheduler/pbs_wrapper.sh

# ========================================================================================
# Section: submit job to scheduler
MY_SCRIPT="$script_dir/run_blastcmd.pbs -d $INST_DIR -n $BANK_NAME "
echo "Submitting ${MY_SCRIPT}..."
ANSWER=$(submitEx "sequentiel" "8g" "1" "12:00:00" "blastcmd-$BANK_NAME" "$WK_DIR" "-- ${MY_SCRIPT}")
RET_CODE=$?
if [ ! $RET_CODE -eq 0 ]; then 
  echo $ANSWER
  exit 1
fi
JOB_ID=$ANSWER
echo "Job ID is: $JOB_ID"
echo "Waiting for job to complete..."
waitForJobToFinish $JOB_ID 
if [ ! $RET_CODE -eq 0 ]; then
  echo "FAILED"
  RET_CODE=1
else
  echo "SUCCESS"
  RET_CODE=0
fi
echo "Job log is:"
dumpJobLog $WK_DIR $JOB_ID

exit $RET_CODE

