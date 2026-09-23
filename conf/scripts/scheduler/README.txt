# Introduction

This directory contains bash script framework to execute any kind of processing jobs outside BeeDeeM Java engine.

The main idea relies upon using a job scheduler available on the host computer running BeeDeeM.

# Framework design

## The basics

To enable the use of a job sceduler, a basic bash API has been designed around a set of functions, namely:

* submit()
* submitEx()
* getStatus()
* getExitCode()
* waitForJobToFinish()
* dumpJobLog()
* removeJobLog()

Please refer to pbs_wrapper.sh script to review API documentation on the purpose of each of these functions.

Several environment variables can be used to control the behaviour of the API:

- BDM_PLATFORM: platform name on which the scripts are executed. Default is "generic". ABiMS (IFB cluster) and Ifremer (DATARMOR) settings are provided.
- BDM_SCHEDULER: job scheduler used to execute jobs. Defauklt is "local", i.e. the computer on which the script is started. PBS and SLURM settings are provided.
- BDM_QSUB_CMD: scheduler command to sublit a job. Default is non defined. Use "qsub" for PBS and "sbatch" for SLURM.
- BDM_QSTAT_CMD: scheduler command to get information about running job. Default is not defined. Use "qstat" for PBS and "sacct" for SLURM.
- BDM_CONF_SCRIPTS: path to "beedeem/conf/scripts" directory. Default is computed at runtime relative to running script. On slurm, this is not working, so set that variable using an absolute path to "beedeem/conf/scripts".

On cluster using a scheduler, it is very mush recommended to define ALL these variables.

## The computing resources

Each cluster has its own set of resources in terms of CPU, memory, walltime and queues. In addition, different set of values may have to be used depending on the job (Blast, BWA, etc.)

To setup all that information, one has to setup a dedicated "<scheduler>-<platform>.cfg" file, namely of job scheduler configuration file.

## The software environments

Finally, each cluster has its own policy to activate a software environement, e.g. using Linux modules.

To setup that part of the job submission system, one has to setup a dedicated "env-xxx.sh" script which define two functions:

* activateEnv()
* deActivateEnv()

Please refer to env-xxx.sh files to learn more about the use of such functions.

## The supported schedulers

For now, we propose an implementation of this API framework for the following job schedulers:

* PBS Pro: review pbs-xxx files for more information; should be compatible with former SGE system (however, never tested)
* SLURM: review slurm-xxx files for more information

In case, none of these schedulers is available on the host computer running BeeDeeM, the API will automatically select a local (lcl) scheduler: jobs will be executed directly on that host.

## The supported software environments

At Ifremer, all bioinformatics softwares are installed using Conda, so you can review the env-ifremer.sh script to learn how to properly activate and deactivate tools programmatically.

Then, it is up to you to setup a dedicated env-xxx.sh script to enable activation of tools matching your system.

# Practical use of the API

Simply review the run-makeblastdb.sh script to learn how you can implement this framework to execte any kind of pre- and post-processing jobs from a BeeDeeM bank installation descriptor.

You can also review the content of the test folder, and more precisely the text-xxx.sh scripts.

