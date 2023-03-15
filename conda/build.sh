#!/usr/bin/env bash

# Unarchive the BeeDeeM distrib package
tar -zxf beedeem-${PKG_VERSION}.tar.gz

# Prepare correct BeeDeeM directory structure for Conda installation
cp -R ${SRC_DIR}/bin $PREFIX
cp -R ${SRC_DIR}/doc $PREFIX
cp -R ${SRC_DIR}/conf $PREFIX
cp -R ${SRC_DIR}/external $PREFIX
cp -R ${SRC_DIR}/license $PREFIX

# Copy BeeDeeM scripts and update them with default config
cp ${SRC_DIR}/scripts/bdm.sh $PREFIX/bin/bdm
sed -i 's/@KL_WORKING_DIR@/\/beedeem-wk/g' $PREFIX/bin/bdm
sed -i 's/@JAVA_ARGS@/-Xms128M -Xmx4G -Djava.io.tmpdir=\$KL_WORKING_DIR -DKL_LOG_TYPE=console/g' $PREFIX/bin/bdm

# Copy the BeeDeeM master configuration file and update it
cp ${SRC_DIR}/scripts/dbms.config $PREFIX/conf
sed -i 's/@BIOBASE_ROOTDIR@/\/beedeem-db/g' $PREFIX/conf/dbms.config

# Ensure executables have valid exec permission
chmod +x $PREFIX/bin/bdm
chmod +x $PREFIX/external/bin/linux/*
chmod +x $PREFIX/external/bin/macos/*
chmod +x $PREFIX/conf/scripts/*.sh
chmod +x $PREFIX/conf/scripts/scheduler/*.sh

# Discard Windows stuff
cd $PREFIX/external/bin && rm -rf windows
