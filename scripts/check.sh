#!/bin/sh
#
# -------------------------------------------------------------------
# DBMS program to check DB descriptors ; for Mac/Linux
# Copyright (c) - Patrick G. Durand, 2017
# -------------------------------------------------------------------
# User manual:
#   https://pgdurand.gitbooks.io/beedeem/
# -------------------------------------------------------------------
# The program can be used to check DSC files. It takes one argument:
# -dsc <bank-code>: descriptor name(s) to check (comma separated). 
#                   Use 'all' to check all descriptors.
#
# See manual for more information: 
# https://pgdurand.gitbooks.io/beedeem/
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
#

# *** Application home
KL_APP_HOME=@KL_INSTALL_DIR@

# *** Working directory
KL_WORKING_DIR=@KL_WORKING_DIR@

# *** Java VM 
JAVA_HOME=@JAVA_ROOT_DIR@
KL_JAVA_VM=$JAVA_HOME/bin/java
KL_JAVA_ARGS="@JAVA_ARGS@ -DKL_HOME=$KL_APP_HOME -DKL_WORKING_DIR=$KL_WORKING_DIR"

# *** JARs section
KL_JAR_LIST_TMP=`\ls $KL_APP_HOME/bin/*.jar`
KL_JAR_LIST=`echo $KL_JAR_LIST_TMP | sed 's/ /:/g'`

# *** start application
KL_APP_MAIN_CLASS=bzh.plealog.dbmirror.main.AutoCheckDescriptors
$KL_JAVA_VM $KL_JAVA_ARGS -classpath $KL_JAR_LIST $KL_APP_MAIN_CLASS $@

