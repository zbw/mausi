#!/bin/bash

#
# mausi
#
# example simple dictionary matching server
#
GIT_PROJECT=..
TMP_DIR=${GIT_PROJECT}/target
DIR_MODELS=${GIT_PROJECT}/target # <- tmp output

LOG_DIR=${GIT_PROJECT}/target # ~/.mausi_logs
LOG_NAME=log.mausi.txt

# export STW_PTH=D:/tmp/stw_9.04.rdf

MAUSI_JAR=${GIT_PROJECT}/target/zaptain-mausi-0.0.1-SNAPSHOT.jar
MAUSI_SERVER_CLASS=eu.zbw.a1.mausi.MausiDictMatchingServer

java -cp ${MAUSI_JAR} ${MAUSI_SERVER_CLASS} \
  -port 4568