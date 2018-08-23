#!/usr/bin/env bash

export MINISHIFT_USERNAME="developer"
export MINISHIFT_PASSWORD="developer"
export OS_PROJECT_NAME="reactica"


source scripts/env.sh

minishift_login ${MINISHIFT_USERNAME} ${MINISHIFT_PASSWORD}
# Create the project, also switch to this project if required.
create_project ${OS_PROJECT_NAME}

# Deploy AMQP broker and wait for readiness
deploy_descriptor templates/amqp-broker.yaml
waitForPodState "reactica-broker" "Running"


