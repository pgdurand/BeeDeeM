<?xml version="1.0" encoding="utf-8"?>
<!-- This is the Ant configuration file used to deploy, install and configure
     the DataBank Manager System (BeeDeeM) within a Docker container.
     
     Adapted from the original deploy.xml file used for standalone install.
     
     Copyright (c) 2017, Patrick G. Durand
-->

<project name="DBMS_installer_docker" default="help" basedir=".">

	<property environment="env"/>
	<property file="config.properties"/>

	<condition property="unixOS">
		<os family="unix"/>
	</condition>

  <!-- ============================================================= -->
  <target name="help">
    <echo>This is the Ant project file to install DBMS for Docker container.</echo>
    <echo/>
    <echo>Available target is: install</echo>
    <echo/>
  </target>

	<!-- ============================================================= -->
	<target name="install">
		<echo>Current configuration is:</echo>
		<echo>Installation dir: ${installDir}</echo>
		<echo>Working dir     : ${workingDir}</echo>
		<echo>BioBase dir     : ${biobaseRootDir}</echo>
		<echo>JVM dir         : ${javaDir}</echo>
		<echo>Java args       : ${javaArgs}</echo>

		<!-- Removed old installation if any -->
		<delete includeemptydirs="true" failonerror="false">
			<fileset dir="${installDir}">
				<exclude name="**/*.lic"/>
				<exclude name="**/*.dsc"/>
				<exclude name="**/*.gd"/>
			</fileset>
		</delete>
		<!-- Prepare directory structure on the target system -->
		<mkdir dir="${installDir}" />
		<!-- Gunzip the KDBMS distribution -->
		<gunzip src="${basedir}/beedeem-${env.BDM_VERSION}.tar.gz" dest="${installDir}/beedeem-${env.BDM_VERSION}.tar"/>
		<!-- Untar the KDBMS distribution -->
		<untar src="${installDir}/beedeem-${env.BDM_VERSION}.tar" dest="${installDir}"/>
		<!-- Remove useless file -->
		<delete file="${installDir}/beedeem-${env.BDM_VERSION}.tar" />
		<antcall target="copyScripts" />
		<delete dir="${installDir}/scripts" />
		<mkdir dir="${installDir}/license" />
		<!-- Configure file/dir permissions-->
		<antcall target="setperms" />
	</target>

  <!-- ============================================================= -->
	<target name="setperms" if="unixOS">
		<chmod dir="${installDir}"                perm="ugo+r" type="file" includes="**/*" />
		<chmod dir="${installDir}/external"       perm="ugo+x" type="file" includes="**/*" />
		<chmod dir="${installDir}"                perm="ugo+x" type="dir"  includes="**/*" />
		<chmod file="${installDir}/UiInstall.sh"  perm="ugo+x" />
		<chmod file="${installDir}/install.sh"    perm="ugo+x" />
		<chmod file="${installDir}/query.sh"      perm="ugo+x" />
    <chmod file="${installDir}/annotate.sh"   perm="ugo+x" />
    <chmod file="${installDir}/info.sh"   perm="ugo+x" />
	</target>

  <!-- ============================================================= -->
	<target name="copyScripts">
		<antcall target="copyScriptsUnix" />
	</target>

  <!-- ============================================================= -->
	<target name="prepareScript">
		<copy file="${installDir}/scripts/${kscript}.${kext}" tofile="${installDir}/${kscript}.tmp">
			<filterset>
				<filter token="KL_INSTALL_DIR" value="${installDir}"/>
				<filter token="KL_WORKING_DIR" value="${workingDir}"/>
				<filter token="BIOBASE_ROOTDIR" value="${biobaseRootDir}"/>
				<filter token="JAVA_ROOT_DIR" value="${javaDir}"/>
				<filter token="JAVA_ARGS" value="${javaArgs}"/>

			</filterset>
		</copy>
		<antcall target="updateUnixPath">
			<param name="sourceF" value="${installDir}/${kscript}.tmp"/>
			<param name="targetF" value="${installDir}/${kscript}.${kext}"/>
		</antcall>
		<delete file="${installDir}/${kscript}.tmp"/>
	</target>

  <!-- ============================================================= -->
	<target name="copyScriptsUnix" if="unixOS">
		<!-- Copy and configure the KDBMS starter script (Unix) -->
		<antcall target="prepareScript">
			<param name="kscript" value="UiInstall"/>
			<param name="kext" value="sh"/>
		</antcall>
		<antcall target="prepareScript">
			<param name="kscript" value="install"/>
			<param name="kext" value="sh"/>
		</antcall>
		<antcall target="prepareScript">
			<param name="kscript" value="query"/>
			<param name="kext" value="sh"/>
		</antcall>
    <antcall target="prepareScript">
      <param name="kscript" value="annotate"/>
      <param name="kext" value="sh"/>
    </antcall>
    <antcall target="prepareScript">
      <param name="kscript" value="info"/>
      <param name="kext" value="sh"/>
    </antcall>
		<!-- Copy and configure the KDBMS main config file-->
		<copy file="${installDir}/scripts/dbms.config" todir="${installDir}/conf">
			<filterset>
				<filter token="BIOBASE_ROOTDIR" value="${biobaseRootDir}"/>
			</filterset>
		</copy>
	</target>

  <!-- ============================================================= -->
	<target name="updateUnixPath" if="unixOS">
		<copy file="${sourceF}" tofile="${targetF}"/>
	</target>

</project>
