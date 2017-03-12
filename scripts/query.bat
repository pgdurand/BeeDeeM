@echo off

rem DBMS program to query the repository ; for Mac/Windows
rem Copyright (c) - Patrick G. Durand, 2007-2017
rem -------------------------------------------------------------------
rem User manual:
rem   https://pgdurand.gitbooks.io/beedeem/
rem -------------------------------------------------------------------
rem This is the program to use to query the databanks managed with 
rem DBMS. The program takes 3 argument, in this order:
rem
rem     <database> <seqid> <format>
rem
rem Accepted values for 'database' is: dico, nucleotide or protein.
rem 
rem Accepted value for 'seqid' is a sequence identifier. 
rem
rem Accepted value for 'format' is: txt (default), html, 
rem insd, fas or finsd. 
rem
rem 
rem In addition, some parameters can be passed to the JVM for special 
rem configuration purposes:<br>
rem -DKL_DEBUG=true ; if true, if set, log will be in debug mode<br>
rem -DKL_WORKING_DIR=an_absolute_path ; if not set, log and working 
rem  directories are set to java.io.tmp<br>
rem -DKL_LOG_FILE=a_file_name ; if set, creates a log file with that 
rem  name within KL_WORKING_DIR<br><br>

set CUR_DIR=%cd%

rem *** Application home
set KL_APP_HOME=@KL_INSTALL_DIR@

rem *** Working directory
set KL_WORKING_DIR=@KL_WORKING_DIR@

rem *** Java VM 
set JAVA_HOME=@JAVA_ROOT_DIR@
set KL_JAVA_VM=%JAVA_HOME%\bin\java
set KL_JAVA_ARGS=@JAVA_ARGS@ -DKL_HOME="%KL_APP_HOME%" -DKL_WORKING_DIR="%KL_WORKING_DIR%"

rem *** Create classpath
SETLOCAL ENABLEDELAYEDEXPANSION
set FILES=
set BIN_HOME=%KL_APP_HOME%\\bin
cd %BIN_HOME%
for /F %%f in ('dir /b *.jar') do set FILES=!FILES!;%KL_APP_HOME%\\bin\\%%f

rem *** Prepare HTML output 
rem echo "Content-type: text/html"
rem echo ""

rem *** Start application
cd %CUR_DIR%
"%KL_JAVA_VM%" %KL_JAVA_ARGS% -classpath "%FILES%" bzh.plealog.dbmirror.main.CmdLineQuery -d %1 -i %2 -f %3
