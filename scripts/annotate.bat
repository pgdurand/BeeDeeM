@echo off

rem DBMS program to annotate Blast results ; for Mac/Linux
rem Copyright (c) - Patrick G. Durand, 2007-2017
rem -------------------------------------------------------------------
rem User manual:
rem   https://pgdurand.gitbooks.io/beedeem/
rem -------------------------------------------------------------------
rem This class can be used to annotate Blast results. 
rem Command line takes three arguments, in this order:
rem
rem   <blast_result> <output_file> <type>
rem
rem <blast_result>: an existing Blast file, legacy XML format (absolute path)
rem <output_file>:  output file that will contain the annotated 
rem                 Blast result (absolute path)
rem <type>:         type of annotation to retrieve. 
rem                 Options: bco or full. Use bco to only retrieve
rem                 biological classifications information. Use full 
rem                 to retrieve full feature tables.
rem In addition, some parameters can be passed to the JVM for special 
rem configuration purposes:
rem -DKL_DEBUG=true ; if true, if set, log will be in debug mode
rem -DKL_WORKING_DIR=an_absolute_path ; if not set, log and working 
rem  directories are set to java.io.tmp
rem -DKL_LOG_FILE=a_file_name ; if set, creates a log file with that 
rem  name within KL_WORKING_DIR

set CUR_DIR=%cd%

rem *** Application home
set KL_APP_HOME=@KL_INSTALL_DIR@

rem *** Working directory
set KL_WORKING_DIR=@KL_WORKING_DIR@

rem *** Java VM 
set JAVA_HOME=@JAVA_ROOT_DIR@
set KL_JAVA_VM=%JAVA_HOME%\bin\java
set KL_JAVA_ARGS=-Xms128M -Xmx1024M -DKL_HOME="%KL_APP_HOME%" -DKL_WORKING_DIR="%KL_WORKING_DIR%"

rem *** Create classpath
SETLOCAL ENABLEDELAYEDEXPANSION
set FILES=
set BIN_HOME=%KL_APP_HOME%\\bin
cd %BIN_HOME%
for /F %%f in ('dir /b *.jar') do set FILES=!FILES!;%KL_APP_HOME%\\bin\\%%f

rem *** Start application
cd %CUR_DIR%
"%KL_JAVA_VM%" %KL_JAVA_ARGS% -classpath "%FILES%" bzh.plealog.dbmirror.main.Annotate -i %1 -o %2 -type %3 -writer zml

