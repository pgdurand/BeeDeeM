#!/bin/sh
#
# DBMS program to query the repository ; for Mac/Linux
# Copyright (c) - Patrick G. Durand, 2007-2020
# -------------------------------------------------------------------
# User manual:
#   https://pgdurand.gitbooks.io/beedeem/
# -------------------------------------------------------------------
# This is the program to use to query the databanks managed with 
# DBMS. The program takes 3 arguments, in this order:
#
#    <database> <seqid> <format>
#
# Accepted values for 'database' is: dico, nucleotide or protein.
# 
# Accepted value for 'seqid' is a sequence identifier. 
#
# Accepted value for 'format' is: txt (default), html, 
# insd, fas or finsd. 
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
#  KL_WORKING_DIR, KL_CONF_DIR and KL_LOG_FILE can be defined using
#  env variables before calling this script. Additional JRE arguments
#  can also be passed in to this script using env variable KL_JRE_ARGS.

# *** Application home
KL_APP_HOME=$( cd -P -- "$(dirname -- "$(command -v -- "$0")")" && pwd -P )

# *** Working directory
if [  ! "$KL_WORKING_DIR"  ]; then
  KL_WORKING_DIR=@KL_WORKING_DIR@
fi

# *** Configuration directory
if [  ! "$KL_CONF_DIR"  ]; then
  KL_CONF_DIR=$KL_APP_HOME/conf
fi

# *** Optional JRE arguments
if [  ! "$KL_JRE_ARGS"  ]; then
  KL_JRE_ARGS="@JAVA_ARGS@"
fi

# *** Java VM 
KL_JAVA_ARGS="$KL_JRE_ARGS -DKL_HOME=$KL_APP_HOME -DKL_WORKING_DIR=$KL_WORKING_DIR -DKL_CONF_DIR=$KL_CONF_DIR"

# *** Optional redefinition of log file
if [  ! -z "$KL_LOG_FILE"  ]; then
  KL_JAVA_ARGS+=" -DKL_LOG_FILE=$KL_LOG_FILE"
fi

# *** JARs section
KL_JAR_LIST_TMP=`\ls $KL_APP_HOME/bin/*.jar`
KL_JAR_LIST=`echo $KL_JAR_LIST_TMP | sed 's/ /:/g'`

# *** start application
KL_APP_MAIN_CLASS=bzh.plealog.dbmirror.main.CmdLineQuery
java $KL_JAVA_ARGS -classpath $KL_JAR_LIST $KL_APP_MAIN_CLASS -d $1 -i $2 -f $3
