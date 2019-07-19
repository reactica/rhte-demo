#!/usr/bin/env bash
set -e

# Colors are important
export RED='\033[0;31m'
export NC='\033[0m' # No Color
export YELLOW='\033[0;33m'
export BLUE='\033[0;34m'


function warning {
    echo -e "  ${RED} $1 ${NC}"
}

function info {
    echo -e "  ${BLUE} $1 ${NC}"
}

function minishift_start {
  # Retrieve minishift state
  local STATUS=`minishift status | head -1 | awk '{ print $2 }'`
  if [ "Running" = ${STATUS} ]; then
    info "Minishift already running"
  else
    info "Configuring minishift..."
    minishift profile set rhte-vertx-demo
    minishift config set memory 8GB
    minishift config set cpus 3
    minishift config set image-caching true
    minishift addon enable admin-user
    minishift addon enable anyuid
    minishift addon disable xpaas

    local _minishift_params=""

    #If CDK version of minishift set the exact version of openshift to use
    if ! (minishift version | grep -q "CDK"); then

      #Unless the user has registration environment parameters set skip the registration to avoid prompting for it
      if ! [ -z "${MINISHIFT_USERNAME}" -o -z "${MINISHIFT_PASSWORD}" ] ; then
        _minishift_params="--openshift-version=$1 --skip-registration"
      else
        _minishift_params="--openshift-version=$1"
      fi
    fi

    #If the user has set the registry env parameters enable the redhat-registry-login addon
    if ! [ -z "${REGISTRY_USERNAME}" -o -z "${REGISTRY_PASSWORD}" ] ; then
        minishift addon enable redhat-registry-login
    fi

    #Start minishift
    info "Starting minishift..."
    minishift start ${_minishift_params}

    #If the user has set the registry env parameters apply the redhat-registry-login addon
    if ! [ -z "${REGISTRY_USERNAME}" -o -z "${REGISTRY_PASSWORD}" ] ; then
      minishift addon apply redhat-registry-login -a REGISTRY_USERNAME="${REGISTRY_USERNAME}" -a REGISTRY_PASSWORD="${REGISTRY_PASSWORD}"
    fi
    info "NOTE: You may need to set the timezone in minishift using 'minishift ssh sudo timedatectl set-timezone [ID]; minishift openshift restart' if your app calculates timestamps"
  fi
}


function minishift_login {
    export IP=`minishift ip`
    eval $(minishift oc-env)
  if [ -z ${2} ]; then
    oc login --insecure-skip-tls-verify=true https://$IP:8443 -u $1
  else
    oc login --insecure-skip-tls-verify=true https://$IP:8443 -u $1 -p $2
  fi
  oc version
}

function create_project {
    OS_PROJECT_NAME=$1
    eval $(minishift oc-env)
  if oc new-project "${OS_PROJECT_NAME}"; then
    info "Project ${OS_PROJECT_NAME} created"
  else
    info "Reusing existing project ${OS_PROJECT_NAME}"
    oc project "${OS_PROJECT_NAME}"
  fi
  oc policy add-role-to-user edit developer -n reactive-demo
}

function deploy_descriptor {
    info "Deploying $1"
    eval $(minishift oc-env)
  oc apply -f $1
}

function waitForPodState {
  for i in {1..120}
   do
     state=$(getPodState $1)

     if [ "$2" = "${state}" ] ; then {
        echo -e "✔️  Pod $1 is $2"
        return
      } else {
        echo -e "⚙️  Pod $1 is not in state $2, current state: ${state}"
        sleep 3
      }
      fi
   done
  warning "Timeout reached while waiting for pod $1 to be in state $2."
  exit 408
}

function waitForPodReadiness {
  for i in {1..120}
   do
     state=$(getPodReadinessState $1)

     if [ "$2/$2" = "${state}" ] ; then {
        echo -e "✔️  Pod $1 is ready"
        return
      } else {
        echo -e "⚙️  Pod $1 is not ready"
        sleep 3
      }
      fi
   done
  warning "Timeout reached while waiting for pod $1 to be ready."
  exit 408
}

function getPodState {
    eval $(minishift oc-env)
  local res=`oc get pods | grep $1 | grep -v "deploy" |  grep -v "build" | awk '{ print $3 }'`
  echo ${res}
}

function getPodReadinessState {
    eval $(minishift oc-env)
  local res=`oc get pods | grep $1 | grep -v "deploy" |  grep -v "build"  | awk '{ print $2 }'`
  echo ${res}
}

function setSystemAccountRoleToUser {
    eval $(minishift oc-env)
    oc policy add-role-to-user view system:serviceaccount:$1:default -n $1
}
