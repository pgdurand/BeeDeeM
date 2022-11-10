#!/usr/bin/env bash

# BeeDeeM post-processing script for PFAM-HMM.
#
# Such a BeeDeeM script is called by the task engine and
# with arguments as defined in: 
# ./scheduler/common.sh->handleBDMArgs() function

echo "Running script: $0"

# ========================================================================================
# Section: include API
S_NAME=$(realpath "$0")
[[ -z "$BDM_CONF_SCRIPTS" ]] && script_dir=$(dirname "$S_NAME") || script_dir=$BDM_CONF_SCRIPTS
. $script_dir/scheduler/common.sh
. $script_dir/env_${BDM_PLATFORM}.sh

# ========================================================================================
# Section: handle arguemnts
# Function call setting BDMC_xxx variables from cmdline arguments
handleBDMArgs $@
RET_CODE=$?
[ ! $RET_CODE -eq 0 ] && errorMsg "Wrong or missing arguments" && exit $RET_CODE

# ========================================================================================
# Section: do business

# To handle Pfam hmmer file, we must have INST_DIR not empty
if [ -z "${BDMC_INST_DIR}" ]; then
  printf "/!\ Missing mandatory argument: -d <path-to-db-install-dir>\n" >&2
  exit 1
fi

echo "Entering PFAM install directory: ${BDMC_INST_DIR}"
cd ${BDMC_INST_DIR}
SOFT_NAME="hmmer"
SOFT_VER="3.3"
activateEnv "$SOFT_NAME" "$SOFT_VER"
echo "Running hmmpress -f Pfam-A.hmm"
hmmpress -f Pfam-A.hmm > hmm.log 2>&1
RET_CODE=$?
if [ $RET_CODE -eq 0 ]; then
  grep "and indexed" hmm.log | cut -d' ' -f4 > ${BANK_NAME}.num
fi
cat hmm.log
rm hmm.log
deActivateEnv "$SOFT_NAME" "$SOFT_VER"
exit $RET_CODE

