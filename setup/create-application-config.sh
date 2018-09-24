#!/usr/bin/env bash
SCRIPT_DIR=$(dirname "$0")


echo "Creating reactive-config config map"
oc create configmap reactica-config --from-file=${SCRIPT_DIR}/application.yaml
