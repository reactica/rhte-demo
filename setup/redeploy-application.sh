#!/usr/bin/env bash

SCRIPT_DIR=$(dirname "$0")
ROOT_POM="${SCRIPT_DIR}/../pom.xml"

sh ${SCRIPT_DIR}/undeploy-application.sh
sh ${SCRIPT_DIR}/deploy-application.sh
#oc delete $(oc get pod -o name | grep eventstream-amq) && \
#mvn -f "${ROOT_POM}" clean install fabric8:deploy -Popenshift
