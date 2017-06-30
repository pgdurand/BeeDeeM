#######################################################################################
#
# Dockerfile to use BeeDeem through a Docker container.
#
# Copyright (c) 2017, Patrick G. Durand
#
#######################################################################################

# ###
#     Base commands.
#
#     We use Alpine-Oracle JDK 8 pre-build Docker Image.
#     See https://hub.docker.com/r/frolvlad/alpine-oraclejdk8/
#
FROM frolvlad/alpine-oraclejdk8:slim

# Maintainer of BeeDeeM
MAINTAINER Patrick G. Durand

# Install bash: BeeDeeM scripts dependency
# Install curl: to download BeeDeeM from Github
# Install libbz2, libidn: NCBI's makeblastdb dependencies
# mkdir: where to install BeeDeeM tmp stuff
RUN apk add --no-cache curl bash libbz2 libidn && \
    mkdir -p /tmp/bdm

# ###
#    Configuring BeeDeeM release. Always the latest one.
#
ENV BDM_VERSION=4.1.0

# ###
#     BeeDeeM configuration.
#
COPY config.properties.d deploy.xml.d /tmp/bdm/
COPY run_bdm.sh /usr/local/bin/

# ###
#     BeeDeeM download, installation and cleaning.
#     We get an official release bundle from Github.
#
RUN \
  cd /tmp/bdm && \
  export BDM_FILE=beedeem-${BDM_VERSION}-distrib.zip && \
  export BDM_URL=https://github.com/pgdurand/BeeDeeM/releases/download/v${BDM_VERSION}/${BDM_FILE} && \
  curl -ksL ${BDM_URL} -o ${BDM_FILE} && unzip ${BDM_FILE} && \
  cd ant && unzip ant-1.9.4.zip && cd .. && \
  export JAVA_HOME=/usr/lib/jvm/java-8-oracle/jre && \
  export ANT_HOME=/tmp/bdm/ant && \
  export PATH=$JAVA_HOME/bin:$ANT_HOME/bin:$PATH && \
  rm -f config.properties deploy.xml && mv config.properties.d config.properties && mv deploy.xml.d deploy.xml && \
  ant -f deploy.xml install  && \
  cd /tmp && rm -rf bdm && cd /opt/beedeem/external/bin && rm -rf macos windows

# ###
#     Add/update bank descriptors.
#
#     If you want to update existing bank descriptors, or to add new
#     ones, simply update the following COPY line accordingly, then
#     uncomment it and re-run the 'docker build'.
#
#     DO NOT modify '/opt/beedeem/conf/descriptors/': this is the place
#     where bank descriptors MUST BE located within the container to
#     enable BeeDeeM to use them.
#
#     Using COPY: https://docs.docker.com/engine/reference/builder/#copy
#
#COPY mytest.gd mybank.dsc /opt/beedeem/conf/descriptors/

# ###
#     Start BeeDeeM. 
#
ENTRYPOINT ["/usr/local/bin/run_bdm.sh"]
