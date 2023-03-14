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
FROM ubuntu:18.04

# Maintainer of BeeDeeM
MAINTAINER Patrick G. Durand

# ###
#    Configuring release of tools to package.
#
ENV BDM_VERSION=5.0.0
ENV BDMT_VERSION=2.1.1

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
RUN apt-get -y install software-properties-common bzip2 libidn11 bash wget libxext6 libxrender1 libxtst6 libxi6 unzip
RUN apt-get -y install openssh-server

# ###
#    Java JRE 1.8
RUN add-apt-repository ppa:ts.sch.gr/ppa \
          && echo debconf shared/accepted-oracle-license-v1-1 select true | debconf-set-selections \
          && echo debconf shared/accepted-oracle-license-v1-1 seen true | debconf-set-selections \
          && apt-get update \
          && apt-get -y install oracle-java8-installer \
          && apt-get -y install oracle-java8-set-default \
          && apt-get clean all \
          && rm -rf /var/lib/apt/lists/* \
          && cd /usr/lib/jvm/java-8-oracle\
          && rm -rf bin include lib man *.zip

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

RUN \
  cd /opt/beedeem && \
  cp scripts/*.sh . && \
  cp scripts/dbms.config conf && \
  sed -i 's/@BIOBASE_ROOTDIR@/\/beedeem-db/g' conf/dbms.config && \
  chmod +x *.sh && \
  chmod +x /opt/beedeem/external/bin/linux/* && \
  chmod +x /opt/beedeem/conf/scripts/*.sh && \
  cd /opt/beedeem/external/bin && rm -rf macos windows


# ###
#    Add BeeDeeM-tools. Always the latest one.
#
RUN \
  cd /opt && \
  mkdir beedeem-tools && \
  cd beedeem-tools && \
  wget https://github.com/ifremer-bioinformatics/BeeDeeM-Tools/releases/download/v${BDMT_VERSION}/beedeem-tools-${BDMT_VERSION}.tar.gz && \
  gunzip beedeem-tools-${BDMT_VERSION}.tar.gz && \
  tar -xf beedeem-tools-${BDMT_VERSION}.tar && \
  rm beedeem-tools-${BDMT_VERSION}.tar && \
  chmod +x *.sh

# ###
#     BeeDeeM runtime environment variables.
#
ENV PATH=/opt/beedeem:/opt/beedeem-tools:$PATH

