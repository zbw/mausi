#!/bin/bash

#
# mausi
#
# example call
#
# configuration:
#
GIT_PROJECT=..
TMP_DIR=${GIT_PROJECT}/target
DIR_MODELS=${GIT_PROJECT}/target # <- tmp output
DIR_DOCUMENTS=${GIT_PROJECT}/src/test/resources

LOG_DIR=${GIT_PROJECT}/target # ~/.mausi_logs
LOG_NAME=log.mausi.txt

STW_RDF=D:/tmp/stw_9.04.rdf
F_CSV_TRAIN=${DIR_DOCUMENTS}/training-data.csv
F_CSV_TEST=${DIR_DOCUMENTS}/test-data-eval.csv
F_CSV_PRED=${TMP_DIR}/test-data-predicted.csv

MAUSI_JAR=${GIT_PROJECT}/target/zaptain-mausi-0.0.1-SNAPSHOT.jar
MAUSI_MAIN=eu.zbw.a1.mausi.MausiWrapperApp

#
# call with joint training and testing:
java -cp ${MAUSI_JAR} ${MAUSI_MAIN} 2>&1 \
  -v "${STW_RDF}" \
  -f skos \
  -dir_models ${DIR_MODELS}\
  -csv_train ${F_CSV_TRAIN} \
  -csv_test ${F_CSV_TEST} \
  -csv_predicted ${F_CSV_PRED} \
  -score \
  | tee "${LOG_DIR}/${LOG_NAME}"


#
# call, load existing model, apply on example:
echo "~~~~~~~~~~~~~~~~~~~~~~~~~"
echo "~~~~~~~~~~~~~~~~~~~~~~~~~"
java -cp ${MAUSI_JAR} ${MAUSI_MAIN} \
  -load ${DIR_MODELS}/$(basename -- "$F_CSV_TRAIN").maui_model \
  -v "${STW_RDF}" \
  -f skos \
  -pairs \
  -csv_test ${F_CSV_TEST} \
  -csv_predicted ${F_CSV_PRED}_basic \

#
# call, load existing model, apply on custom data with extended settings:
echo "~~~~~~~~~~~~~~~~~~~~~~~~~"
echo "~~~~~~~~~~~~~~~~~~~~~~~~~"
java -cp ${MAUSI_JAR} ${MAUSI_MAIN} \
  -load ${DIR_MODELS}/$(basename -- "$F_CSV_TRAIN").maui_model \
  -v "${STW_RDF}" \
  -f skos \
  -csv_test ${F_CSV_TEST} \
  -csv_predicted ${F_CSV_PRED}_probs \
  -pairs \
  -score \
  -c 0.1 \
  -n 15