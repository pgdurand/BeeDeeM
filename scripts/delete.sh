#!/bin/sh
#
# -------------------------------------------------------------------
# DBMS program to delete databanks ; for Mac/Linux
# Copyright (c) - Patrick G. Durand, 2017
# -------------------------------------------------------------------
# User manual:
#   https://pgdurand.gitbooks.io/beedeem/
# -------------------------------------------------------------------
# In addition, some parameters can be passed to the JVM for special 
# configuration purposes:<br>
# -DKL_DEBUG=true ; if true, if set, log will be in debug mode<br>
# -DKL_WORKING_DIR=an_absolute_path ; if not set, log and working 
#  directories are set to java.io.tmp<br>
# -DKL_LOG_FILE=a_file_name ; if set, creates a log file with that 
#  name within KL_WORKING_DIR<br><br>
# -DKL_CONF_DIR=an_absolute_path ; the absolute path to a home-made  
#  conf directory. If not set, use ${user.dir}/conf.
#

# *** Application home
KL_APP_HOME=$( cd -P -- "$(dirname -- "$(command -v -- "$0")")" && pwd -P )

# *** Working directory
if [  ! "$KL_WORKING_DIR"  ]; then
  KL_WORKING_DIR=@KL_WORKING_DIR@
fi

# *** DATARMOR: configuration directory
if [  ! "$KL_CONF_DIR"  ]; then
  KL_CONF_DIR=$KL_APP_HOME/conf
fi

# *** Java VM 
KL_JAVA_ARGS="@JAVA_ARGS@ -DKL_HOME=$KL_APP_HOME -DKL_WORKING_DIR=$KL_WORKING_DIR -DKL_CONF_DIR=$KL_CONF_DIR"

# *** JARs section
KL_JAR_LIST_TMP=`\ls $KL_APP_HOME/bin/*.jar`
KL_JAR_LIST=`echo $KL_JAR_LIST_TMP | sed 's/ /:/g'`

# *** start application
KL_APP_MAIN_CLASS=bzh.plealog.dbmirror.main.DeleteBank
java $KL_JAVA_ARGS -classpath $KL_JAR_LIST $KL_APP_MAIN_CLASS $@

