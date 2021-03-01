@echo off

rem
rem -------------------------------------------------------------------
rem DBMS program to delete databanks ; for Windows
rem Copyright (c) - Patrick G. Durand, 2017
rem -------------------------------------------------------------------
rem User manual:
rem   https://pgdurand.gitbooks.io/beedeem/
rem -------------------------------------------------------------------
rem In addition, some parameters can be passed to the JVM for special 
rem configuration purposes:<br>
rem -DKL_DEBUG=true ; if true, if set, log will be in debug mode<br>
rem -DKL_WORKING_DIR=an_absolute_path ; if not set, log and working 
rem  directories are set to java.io.tmp<br>
rem -DKL_LOG_FILE=a_file_name ; if set, creates a log file with that 
rem  name within KL_WORKING_DIR<br><br>
rem -DKL_CONF_DIR=an_absolute_path ; the absolute path to a home-made  
rem  conf directory. If not set, use ${user.dir}/conf.
rem -DKL_LOG_TYPE=none|console|file(default)
rem

rem *** Application home
set KL_APP_HOME=@KL_INSTALL_DIR@

rem *** Working directory
set KL_WORKING_DIR=@KL_WORKING_DIR@

rem *** Java VM 
set KL_JAVA_ARGS=-Xms128M -Xmx1024M -DKL_HOME="%KL_APP_HOME%" -DKL_WORKING_DIR="%KL_WORKING_DIR%"

rem *** Create classpath
SETLOCAL ENABLEDELAYEDEXPANSION
set FILES=
for /F %%f in ('dir /b %KL_APP_HOME%\bin\*.jar') do set FILES=!FILES!;%KL_APP_HOME%\bin\%%f

rem *** Start application
java.exe %KL_JAVA_ARGS% -classpath "%FILES%" bzh.plealog.dbmirror.main.DeleteBank %*
