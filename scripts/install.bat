@echo off

rem
rem DBMS program to install databanks ; for Mac/Linux
rem Copyright (c) - Patrick G. Durand, 2007-2017
rem -------------------------------------------------------------------
rem User manual:
rem   https://pgdurand.gitbooks.io/beedeem/
rem -------------------------------------------------------------------
rem
rem The program can be used to install some databanks. It takes a 
rem single argument which is a global descriptor file
rem that has to be located within the conf directory of the application. 
rem Pass in the descriptor file without its '.gd' extension. See manual 
rem for more information: 
rem
rem https://pgdurand.gitbooks.io/beedeem/
rem
rem In addition, some parameters can be passed to the JVM for special 
rem configuration purposes:<br>
rem -DKL_DEBUG=true ; if true, if set, log will be in debug mode<br>
rem -DKL_WORKING_DIR=an_absolute_path ; if not set, log and working 
rem  directories are set to java.io.tmp<br>
rem -DKL_LOG_FILE=a_file_name ; if set, creates a log file with that 
rem  name within KL_WORKING_DIR<br><br>
rem
rem Proxy configuration: update configuration file: ${beedeemHome}/conf/system/network.config.

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
for /F %%f in ('dir /b %KL_APP_HOME%\bin\*.jar') do set FILES=!FILES!;%KL_APP_HOME%\bin\%%f

rem *** Start application
"%KL_JAVA_VM%" %KL_JAVA_ARGS% -classpath "%FILES%" bzh.plealog.dbmirror.main.CmdLineInstaller %1
