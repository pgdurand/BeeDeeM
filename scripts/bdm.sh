#!/usr/bin/env bash
#
# -------------------------------------------------------------------
# BeeDeeM starter command for macOS/Linux
# Copyright (c) - Patrick G. Durand, 2007-2023
# -------------------------------------------------------------------
# User manual:
#   https://pgdurand.gitbooks.io/beedeem/
# -------------------------------------------------------------------
# Command use: 
#    bdm -h: to get help
#    bdm <command> [options]: to start a command
#
# In addition, some parameters can be passed to the JVM for special 
# configuration purposes:<br>
# -DKL_DEBUG=true ; if true, if set, log will be in debug mode<br>
# -DKL_WORKING_DIR=an_absolute_path ; if not set, log and working 
#  directories are set to java.io.tmp<br>
# -DKL_LOG_FILE=a_file_name ; if set, creates a log file with that 
#  name within KL_WORKING_DIR<br><br>
# -DKL_CONF_DIR=an_absolute_path ; the absolute path to a home-made  
#  conf directory. If not set, use ${user.dir}/conf.
# -DKL_LOG_TYPE=none|console|file(default)
#
#  Alternatively, you can set these special variables as environment 
#  variables BEFORE calling this script. Additional JRE arguments
#  can also be passed in to this script using env variable KL_JRE_ARGS.
#
# Proxy configuration: update configuration file: 
#      ${beedeemHome}/conf/system/network.config.

# *** Bank installation scripts of BeeDeeM (conf/scripts) requires realpath:
#     available in BASH 5 for macOS
#     or available from Linux:coreutils
which realpath
if [ ! $? -eq 0 ]; then
  echo "/!\ ERROR: realpath command not found"
  echo "           macOS: install bash 5"
  echo "           Linux: install coreutils"
  exit 1
fi

# *** Application home
KL_APP_HOME=$( cd -P -- "$(dirname -- "$(command -v -- "$0")")" && pwd -P )
# For Conda installation, scripts are in the bin directory, so get correct home
[[ "$KL_APP_HOME" == *bin ]] && KL_APP_HOME=$(dirname "$KL_APP_HOME")

# *** Working directory
if [  ! "$KL_WORKING_DIR"  ]; then
  export KL_WORKING_DIR=@KL_WORKING_DIR@
fi

# *** Configuration directory
if [  ! "$KL_CONF_DIR"  ]; then
  export KL_CONF_DIR=$KL_APP_HOME/conf
fi

# *** Optional JRE arguments (at least RAM specs)
if [  ! "$KL_JRE_ARGS"  ]; then
  KL_JRE_ARGS="@JAVA_ARGS@"
fi

# *** Java VM 
KL_JAVA_ARGS="$KL_JRE_ARGS -DKL_HOME=$KL_APP_HOME"

# *** JARs section
KL_JAR_LIST_TMP=`\ls $KL_APP_HOME/bin/*.jar`
KL_JAR_LIST=`echo $KL_JAR_LIST_TMP | sed 's/ /:/g'`

# *** start application
KL_APP_MAIN_CLASS=bzh.plealog.dbmirror.main.BeeDeeMain
java $KL_JAVA_ARGS -classpath $KL_JAR_LIST $KL_APP_MAIN_CLASS $@

