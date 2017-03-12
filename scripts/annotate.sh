#!/bin/sh
#
# DBMS program to annotate Blast results ; for Mac/Linux
# Copyright (c) - Patrick G. Durand, 2007-2017
# -------------------------------------------------------------------
# User manual:
#   https://pgdurand.gitbooks.io/beedeem/
# -------------------------------------------------------------------
# This class can be used to annotate Blast results. 
# Command line takes three arguments, in this order:
#
#   <blast_result> <output_file> <type>
#
# <blast_result>: an existing Blast file, legacy XML format (absolute path)
# <output_file>:  output file that will contain the annotated 
#                 Blast result (absolute path)
# <type>:         type of annotation to retrieve. 
#                 Options: bco or full. Use bco to only retrieve
#                 biological classifications information. Use full 
#                 to retrieve full feature tables.
# In addition, some parameters can be passed to the JVM for special 
# configuration purposes:
# -DKL_DEBUG=true ; if true, if set, log will be in debug mode
# -DKL_WORKING_DIR=an_absolute_path ; if not set, log and working 
#  directories are set to java.io.tmp
# -DKL_LOG_FILE=a_file_name ; if set, creates a log file with that 
#  name within KL_WORKING_DIR
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
KL_APP_MAIN_CLASS=bzh.plealog.dbmirror.main.Annotate
$KL_JAVA_VM $KL_JAVA_ARGS -classpath $KL_JAR_LIST $KL_APP_MAIN_CLASS -i $1 -o $2 -type $3 -writer zml

