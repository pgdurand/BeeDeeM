Script to test PBS wrapper code

You may have to adapt queue before running the test script:
export MQUEUE=<your-queue>

It is also possible to run an additional test by settings these
variables before running the test script:

export BDM_SCHEDULER="pbs"      # Optional since we force use of pbs
export BDM_PLATFORM="ifremer"   # To load a specific conf file located in ../../../conf/scripts/scheduler/
export BDM_QSTAT_CMD="ssh $DM_BDM_PBS_USER qstat"  # to change calling command for qstat
export BDM_QSUB_CMD="ssh $DM_BDM_PBS_USER qsub"    # same for qsub

These variables are loaded by ../../../conf/scripts/scheduler/pbs_wrapper.sh
