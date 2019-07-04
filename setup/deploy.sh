#!/usr/bin/env bash
SCRIPT_DIR=$(dirname "$0")

export OPENSHIFT_USERNAME="admin"
export OPENSHIFT_PASSWORD="admin"
export OS_PROJECT_NAME="reactive-demo"

source ${SCRIPT_DIR}/openshift/env.sh

# if (minishift version | grep -q "CDK"); then
#   echo "Using CDK Registration"
# else
#   if [ -z "${REGISTRY_USERNAME}" -o -z "${REGISTRY_PASSWORD}" ] ; then
#     warning "Must set REGISTRY_USERNAME and REGISTRY_PASSWORD environment variables to access redhat.registry.io"
#     exit 1
#   fi
# fi

# Start minishift is needed
minishift_start "v3.11.0"

minishift_login "system:admin"

# Create the project, also switch to this project if required.
create_project ${OS_PROJECT_NAME}

# Give the default service account right to view the project (required for AMQ and DG)
setSystemAccountRoleToUser ${OS_PROJECT_NAME}

minishift_login ${OPENSHIFT_USERNAME} ${OPENSHIFT_PASSWORD}

# Install image streams
source ${SCRIPT_DIR}/openshift/install-is-and-templates.sh


if oc get dc | grep "eventstore-dg"; then
  info "Data grid already deployed"
else
  info "Instantiating data grid eventstore-dg"
  oc new-app --template=datagrid73-basic -p APPLICATION_NAME=eventstore-dg -p CACHE_NAMES=userevents,rideevents,users
fi

waitForPodState "eventstore-dg" "Running"
waitForPodReadiness "eventstore-dg" 1

if oc get dc | grep "eventstream"; then
  info "AMQ Broker already deployed"
else
  info "Instantiating AMQ Broker eventstream"
  oc new-app --template=amq-broker-72-basic -p APPLICATION_NAME=eventstream -p AMQ_NAME=eventstream -p AMQ_QUEUES=USER_QUEUE,ENTER_EVENT_QUEUE,RIDE_EVENT_QUEUE,QLC_QUEUE,CL_QUEUE -p AMQ_USER=user -p AMQ_PASSWORD=user123 -p AMQ_PROTOCOL=amqp
fi

waitForPodState "eventstream" "Running"
waitForPodReadiness "eventstream" 1
