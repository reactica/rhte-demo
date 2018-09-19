#!/usr/bin/env bash

SCRIPT_DIR=$(dirname "$0")
ROOT_POM="${SCRIPT_DIR}/../pom.xml"

mvn -f "${ROOT_POM}" fabric8:undeploy -Popenshift && \
oc delete $(oc get pod -o name | grep eventstore-dg) && \
oc delete $(oc get pod -o name | grep eventstream-amq)
