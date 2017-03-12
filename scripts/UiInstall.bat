@echo off

rem
rem DBMS program to install databanks using the graphical interface ; for Windows
rem Copyright (c) - Patrick G. Durand, 2007-2017
rem -------------------------------------------------------------------
rem User manual:
rem   https://pgdurand.gitbooks.io/beedeem/
rem -------------------------------------------------------------------
rem The program can be used to install some databanks interactively.
rem
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
"%KL_JAVA_VM%" %KL_JAVA_ARGS% -classpath "%FILES%" bzh.plealog.dbmirror.main.UiInstaller

rem *** do not leave immediately (to check potential cmd line messages coming from the application)
pause
