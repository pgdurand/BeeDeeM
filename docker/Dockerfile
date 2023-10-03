#######################################################################################
#
# Dockerfile to use BeeDeem through a Docker container.
# Package BeeDeeM and BeeDeeM-Tools by getting releases available on Github.
#
# Copyright (c) 2017-23, Patrick G. Durand
#
#######################################################################################

# ###
#     Base commands.
#
#     We use standard Ubuntu 18 Linux. 
#
FROM debian:11-slim

# Maintainer of BeeDeeM
MAINTAINER Patrick G. Durand

# ###
#    Configuring release of tools to package.
#
ENV BDM_VERSION=5.0.0
ENV BDMT_VERSION=2.1.2

# ###
# Install dependencies.
# software-properties-common required to install Java Runtime
# libbz2 libidn are required by blast tools included in BeeDeeM.
# libxext libxrender libxtst libxi are required by Bioinformatics-Core-API included 
#    in BeeDeeM and BeeDeeM-Tools.
# bash is required when running this image by Nextflow pipelines.
# wget is required to install some banks (e.g. GeneOntology).
# unzip is required to deploy BeeDeeM archive
# openssh-server is required to submit jobs on cluster through ssh connection
RUN apt-get -y update 
RUN apt-get -y install gawk openssh-client software-properties-common bzip2 libidn11 bash wget libxext6 libxrender1 libxtst6 libxi6 unzip

# ###
#    Java
#    See See https://www.oracle.com/java/technologies/downloads/#java17 
RUN cd /opt \
    && wget https://download.oracle.com/java/17/latest/jdk-17_linux-x64_bin.deb \
    && apt install -y /opt/jdk-17_linux-x64_bin.deb \
    && rm jdk-17_linux-x64_bin.deb


# ###
#     BeeDeeM download, installation and cleaning.
#     We get an official release bundle from Github.
#
RUN \
  mkdir -p /opt/beedeem/tmp-install && \
  cd /opt/beedeem/tmp-install && \
  wget https://github.com/pgdurand/BeeDeeM/releases/download/v${BDM_VERSION}/beedeem-${BDM_VERSION}-distrib.zip && \
  unzip beedeem-${BDM_VERSION}-distrib.zip && \
  tar -zxf beedeem-${BDM_VERSION}.tar.gz -C /opt/beedeem && \
  cd .. && \
  rm -rf tmp-install

# ###
#    Do a little configuration
#
RUN \
  cd /opt/beedeem \
  && cp scripts/bdm.sh ./bdm \
  && sed -i 's/@KL_WORKING_DIR@/\/beedeem-wk/g' bdm \
  && sed -i 's/@JAVA_ARGS@/-Xms128M -Xmx2048M -Djava.io.tmpdir=\$KL_WORKING_DIR -DKL_LOG_TYPE=console/g' bdm \
  && cp scripts/dbms.config conf \
  && sed -i 's/@BIOBASE_ROOTDIR@/\/beedeem-db/g' conf/dbms.config \
  && chmod +x bdm \
  && chmod +x /opt/beedeem/external/bin/linux/* \
  && chmod +x /opt/beedeem/conf/scripts/*.sh \
  && chmod +x /opt/beedeem/conf/scripts/scheduler/*.sh \
  && cd /opt/beedeem/external/bin && rm -rf macos windows

# ###
#    Add BeeDeeM-tools. Always the latest one.
#
RUN \
  cd /opt && \
  mkdir beedeem-tools && \
  cd beedeem-tools && \
  wget https://gitlab.ifremer.fr/api/v4/projects/91/packages/generic/BeeDeeM-Tools/${BDMT_VERSION}/beedeem-tools-${BDMT_VERSION}.tar.gz && \
  gunzip beedeem-tools-${BDMT_VERSION}.tar.gz && \
  tar -xf beedeem-tools-${BDMT_VERSION}.tar && \
  rm beedeem-tools-${BDMT_VERSION}.tar && \
  chmod +x *.sh

# ###
#     BeeDeeM runtime environment variables.
#
ENV PATH=/opt/beedeem:/opt/beedeem-tools:$PATH

