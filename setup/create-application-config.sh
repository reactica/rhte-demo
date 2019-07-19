#!/usr/bin/env bash
SCRIPT_DIR=$(dirname "$0")


echo "Creating reactiva-config config map"
eval $(minishift oc-env)
oc create configmap reactica-config --from-file=${SCRIPT_DIR}/application.yaml
