#!/usr/bin/env bash

SCRIPT_DIR=$(dirname "$0")

export MINISHIFT_USERNAME="admin"
export MINISHIFT_PASSWORD="admin"
export OS_PROJECT_NAME="reactive-demo"


source ${SCRIPT_DIR}/openshift/env.sh

pushd ${SCRIPT_DIR}/..
mvn clean package fabric8:deploy -Popenshift

waitForPodState "event-generator" "Running"
waitForPodReadiness "event-generator" 1

waitForPodState "event-store" "Running"
waitForPodReadiness "event-store" 1

waitForPodState "current-line-updater" "Running"
waitForPodReadiness "current-line-updater" 1

waitForPodState "queue-length-calculator" "Running"
waitForPodReadiness "queue-length-calculator" 1

waitForPodState "billboard" "Running"
waitForPodReadiness "billboard" 1

popd



