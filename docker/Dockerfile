#######################################################################################
#
# Dockerfile to use BeeDeem through a Docker container.
#
# Copyright (c) 2017-21, Patrick G. Durand
#
#######################################################################################

# ###
#     Base commands.
#
#     We use Alpine-Oracle JRE 8 pre-build Docker Image.
#     See https://github.com/platten/alpine-oracle-jre8-docker
#
FROM platten/alpine-oracle-jre8-docker

# Maintainer of BeeDeeM
MAINTAINER Patrick G. Durand

# ###
# Install dependencies.
# libbz2 libidn are required by blast tools included in BeeDeeM.
# libxext libxrender libxtst libxi are required by Bioinformatics-Core-API included 
#    in BeeDeeM and BeeDeeM-Tools.
# bash is required when running this image by Nextflow pipelines.
# wget is required to install some banks (e.g. GeneOntology).
RUN apk update 
RUN apk add --no-cache libbz2 libidn bash wget libxext libxrender libxtst libxi

# ###
#    Configuring BeeDeeM release. Always the latest one.
#
ENV BDM_VERSION=4.7.0

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
ENV BDMT_VERSION=2.0.1
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
ENV KL_WORKING_DIR=/beedeem-wk
# shell variable name does not allowed use of '.'
# (mirror__path == mirror.path)
ENV KL_mirror__path=/beedeem-db
# You can override the following at runtime by using Docker "-e" argument.
ENV KL_JRE_ARGS="-Xms128M -Xmx2048M -Djava.io.tmpdir=/beedeem-wk"

