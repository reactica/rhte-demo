#!/usr/bin/env bash

export MINISHIFT_USERNAME="admin"
export MINISHIFT_PASSWORD="admin"
export OS_PROJECT_NAME="reactive-demo"


source openshift/env.sh

# Start minishift is needed
minishift_start "v3.9.0"

minishift_login "system:admin"

# Create the project, also switch to this project if required.
create_project ${OS_PROJECT_NAME}

# Give the default service account right to view the project (required for AMQ and DG)
setSystemAccountRoleToUser ${OS_PROJECT_NAME}

minishift_login ${MINISHIFT_USERNAME} ${MINISHIFT_PASSWORD}

# Install image streams
source openshift/install-is-and-templates.sh


if oc get dc | grep "eventstore-dg"; then
  info "Data grid already deployed"
else
  info "Instantiating data grid eventstore-dg"
  oc new-app --template=datagrid72-basic -p APPLICATION_NAME=eventstore-dg -p CACHE_NAMES=userevents,rideevents,users
fi

waitForPodState "eventstore-dg" "Running"
waitForPodReadiness "eventstore-dg" 1

if oc get dc | grep "eventstream"; then
  info "AMQ Broker already deployed"
else
  info "Instantiating AMQ Broker eventstream"
  oc new-app --template=amq-broker-71-basic -p APPLICATION_NAME=eventstream -p AMQ_QUEUES=USER_QUEUE,ENTER_EVENT_QUEUE,RIDE_EVENT_QUEUE,QLC_QUEUE,CL_QUEUE -p AMQ_USER=user -p AMQ_PASSWORD=user123 -p AMQ_PROTOCOL=amqp
fi

waitForPodState "eventstream" "Running"
waitForPodReadiness "eventstream" 1
