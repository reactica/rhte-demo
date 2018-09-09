#!/usr/bin/env bash

export MINISHIFT_USERNAME="admin"
export MINISHIFT_PASSWORD="admin"
export OS_PROJECT_NAME="reactive-demo"


source openshift/env.sh

cd ..
mvn clean package fabric8:deploy -Popenshift

waitForPodState "event-generator" "Running"
waitForPodReadiness "event-generator" 1

waitForPodState "eventstore" "Running"
waitForPodReadiness "eventstore" 1

waitForPodState "billboard" "Running"
waitForPodReadiness "billboard" 1



