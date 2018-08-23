#!/bin/bash

echo "### INSTALLING IS FOR AMQ AND RDG"
if $(oc get is/jboss-datagrid72-openshift -n openshift > /dev/null 2>&1); then
      oc replace -f https://raw.githubusercontent.com/jboss-container-images/jboss-datagrid-7-openshift-image/datagrid72/templates/datagrid72-image-stream.json -n openshift --as=system:admin
  else
      oc create -f https://raw.githubusercontent.com/jboss-container-images/jboss-datagrid-7-openshift-image/datagrid72/templates/datagrid72-image-stream.json -n openshift --as=system:admin
  fi

  oc -n openshift import-image jboss-datagrid72-openshift:1.1 --as=system:admin > /dev/null && echo "jboss-datagrid72-openshift:1.1 image successfully imported"

  if $(oc get is/jboss-amq-63 -n openshift > /dev/null 2>&1); then
      oc replace -f https://raw.githubusercontent.com/jboss-openshift/application-templates/ose-v1.4.15/amq/amq63-image-stream.json -n openshift --as=system:admin
  else
      oc create -f https://raw.githubusercontent.com/jboss-openshift/application-templates/ose-v1.4.15/amq/amq63-image-stream.json -n openshift --as=system:admin
  fi

  oc -n openshift import-image jboss-amq-63:1.4 --as=system:admin > /dev/null && echo "jboss-amq-63:1.4 image successfully imported"

echo "### INSTALLING TEMPLATES FOR AMQ AND RDG"

if $(oc get template/datagrid72-basic -n openshift > /dev/null 2>&1); then
    oc replace -f https://raw.githubusercontent.com/jboss-container-images/jboss-datagrid-7-openshift-image/datagrid72/templates/datagrid72-basic.json -n openshift --as=system:admin
else
    oc create -f https://raw.githubusercontent.com/jboss-container-images/jboss-datagrid-7-openshift-image/datagrid72/templates/datagrid72-basic.json -n openshift --as=system:admin
fi

if $(oc get template/amq63-basic -n openshift > /dev/null 2>&1); then
    oc replace -f https://raw.githubusercontent.com/jboss-openshift/application-templates/ose-v1.4.15/amq/amq63-basic.json -n openshift --as=system:admin
else
    oc create -f https://raw.githubusercontent.com/jboss-openshift/application-templates/ose-v1.4.15/amq/amq63-basic.json -n openshift --as=system:admin
fi

