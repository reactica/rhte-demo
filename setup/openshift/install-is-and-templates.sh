#!/bin/bash

DG_IS='https://raw.githubusercontent.com/jboss-container-images/jboss-datagrid-7-openshift-image/datagrid73/templates/datagrid73-image-stream.json'
AMQ_IS='https://raw.githubusercontent.com/jboss-container-images/jboss-amq-7-broker-openshift-image/amq-broker-72/amq-broker-7-image-streams.yaml'
JDK_IS='https://raw.githubusercontent.com/jboss-container-images/openjdk/openjdk18/templates/image-streams.json'

info "### INSTALLING IS FOR OPENJDK"
if $(oc get is/redhat-openjdk18-openshift -n openshift > /dev/null 2>&1); then
    curl -sL $JDK_IS | sed "s/registry.redhat.io/registry.access.redhat.com/g" | oc replace -n openshift --as=system:admin -f -
else
    curl -sL $JDK_IS | sed "s/registry.redhat.io/registry.access.redhat.com/g" | oc create -n openshift --as=system:admin -f -
fi

oc -n openshift import-image redhat-openjdk18-openshift:1.5 --as=system:admin > /dev/null && echo "redhat-openjdk18-openshift:1.5 image successfully imported"

info "### INSTALLING IS FOR AMQ AND RDG"
if $(oc get is/jboss-datagrid73-openshift -n openshift > /dev/null 2>&1); then
    curl -sL $DG_IS | sed "s/registry.redhat.io/registry.access.redhat.com/g" | oc replace -n openshift --as=system:admin -f -
else
    curl -sL $DG_IS | sed "s/registry.redhat.io/registry.access.redhat.com/g" | oc create -n openshift --as=system:admin -f -
fi

oc -n openshift import-image jboss-datagrid73-openshift:1.0 --as=system:admin > /dev/null && echo "jboss-datagrid73-openshift:1.0 image successfully imported"

if $(oc get is/amq-broker-72-openshift -n openshift > /dev/null 2>&1); then
    curl -sL $AMQ_IS | sed "s/registry.redhat.io/registry.access.redhat.com/g" | oc replace -n openshift --as=system:admin -f -
else
    curl -sL $AMQ_IS | sed "s/registry.redhat.io/registry.access.redhat.com/g" | oc create -n openshift --as=system:admin -f -
fi

  oc -n openshift import-image amq-broker-72-openshift:1.3 --as=system:admin > /dev/null && echo "amq-broker-72-openshift:1.3 image successfully imported"

info "### INSTALLING TEMPLATES FOR AMQ AND RDG"

if $(oc get template/datagrid73-basic -n openshift > /dev/null 2>&1); then
    oc replace -f https://raw.githubusercontent.com/jboss-container-images/jboss-datagrid-7-openshift-image/datagrid73/templates/datagrid73-basic.json -n openshift --as=system:admin
else
    oc create -f https://raw.githubusercontent.com/jboss-container-images/jboss-datagrid-7-openshift-image/datagrid73/templates/datagrid73-basic.json -n openshift --as=system:admin
fi

if $(oc get template/amq-broker-72-basic -n openshift > /dev/null 2>&1); then
    oc replace -f https://github.com/jboss-container-images/jboss-amq-7-broker-openshift-image/raw/amq-broker-72/templates/amq-broker-72-basic.yaml -n openshift --as=system:admin
else
    oc create -f https://github.com/jboss-container-images/jboss-amq-7-broker-openshift-image/raw/amq-broker-72/templates/amq-broker-72-basic.yaml -n openshift --as=system:admin
fi

# If it does not work, try:
# $ eval $(minishift docker-env)
# $ docker pull registry.access.redhat.com/redhat-openjdk-18/openjdk18-openshift:1.6
# $ docker pull registry.access.redhat.com/jboss-datagrid-7/datagrid73-openshift:1.0
# $ docker pull registry.access.redhat.com/amq-broker-7-tech-preview/amq-broker-71-openshift:1.3

