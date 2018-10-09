#!/bin/bash
SCRIPT_DIR=$(dirname "$0")
source ${SCRIPT_DIR}/openshift/env.sh

## Scales down and back up the demo

oc project reactive-demo

echo "Scaling application and DG and AMQ down"
for i in eventstore-dg eventstream-amq billboard current-line-updater event-generator event-store queue-length-calculator ; do
oc scale dc/$i --replicas=0
done

echo "Waiting for shutdown..."
sleep 10

echo "Scaling DG and AMQ up"
for i in eventstore-dg eventstream-amq ; do
oc scale dc/$i --replicas=1
done

# wait for amq and dg
waitForPodState "eventstore-dg" "Running"
waitForPodReadiness "eventstore-dg" 1
waitForPodState "eventstream" "Running"
waitForPodReadiness "eventstream" 1

echo "Scaling Application up"
for i in billboard current-line-updater event-generator event-store queue-length-calculator ; do
oc scale dc/$i --replicas=1
done


