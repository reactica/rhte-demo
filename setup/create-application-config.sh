#!/usr/bin/env bash
echo "Creating reactive-config config map"
oc create configmap reactica-config --from-file=application.yaml
