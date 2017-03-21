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
#     We use Ubuntu-Xenial Linux.
#     See https://hub.docker.com/_/ubuntu/
#
FROM ubuntu:xenial
MAINTAINER Patrick G. Durand

# ###
#     Package installation and configuration.
#
#     install latest packages of the base system
#     as well as packages required to compile GATB-Core
#
RUN apt-get update && apt-get -y dist-upgrade \
    && apt-get install -y --no-install-recommends vim wget unzip \
    && apt-get clean

# ###
#     Java installation.
#
#     We need an Oracle official JRE.
#     Taken from: https://github.com/101dev/docker-oracle_java8/blob/master/Dockerfile
#        Not installed: oracle-java8-unlimited-jce-policy
#
RUN \
  echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | debconf-set-selections && \
  echo "deb http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" | tee /etc/apt/sources.list.d/webupd8team-java.list && \
  echo "deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" | tee -a /etc/apt/sources.list.d/webupd8team-java.list && \
  apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys EEA14886 && \
  apt-get update && \
  apt-get install -y oracle-java8-installer && \
  rm -rf /var/lib/apt/lists/* && \
  rm -rf /var/cache/oracle-jdk8-installer

# ###
#    Configuring BeeDeeM release.
#
ENV BDM_VERSION=4.0.0
    
# ###
#     BeeDeeM download. 
#     We get an official release bundle from Github.
#
RUN \
  export BDM_FILE=beedeem-${BDM_VERSION}-distrib.zip && \
  export BDM_URL=https://github.com/pgdurand/BeeDeeM/releases/download/v${BDM_VERSION}/${BDM_FILE} && \
  cd /tmp && \
  wget --no-check-certificate ${BDM_URL} && \
  unzip ${BDM_FILE} && \
  cd ant && unzip ant-1.9.4.zip && cd ..
  
# ###
#     BeeDeeM configuration.
#
COPY config.properties deploy.xml /tmp/

# ###
#     BeeDeeM installation.
#
#     Note: you may have to update JAVA_HOME only if you change the Java installation
#           process (see above).
#
RUN \ 
  cd /tmp && \
  export JAVA_HOME=/usr/lib/jvm/java-8-oracle/jre && \
  export ANT_HOME=/tmp/ant && \
  export PATH=$JAVA_HOME/bin:$ANT_HOME/bin:$PATH && \
  ant -f deploy.xml install  

COPY run_bdm.sh /usr/local/bin

# ###
#     Start BeeDeeM. 
#
ENTRYPOINT ["/usr/local/bin/run_bdm.sh"]
