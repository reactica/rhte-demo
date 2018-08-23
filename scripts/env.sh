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


function minishift_login {
  export IP=`minishift ip`
  oc login https://$IP:8443 -u $1 -u $2
  oc version
}

function create_project {
  OS_PROJECT_NAME=$1
  if oc new-project "${OS_PROJECT_NAME}"; then
    info "Project ${OS_PROJECT_NAME} created"
  else
    info "Reusing existing project ${OS_PROJECT_NAME}"
    oc project "${OS_PROJECT_NAME}"
  fi
}

function deploy_descriptor {
  info "Deploying $1"
  oc apply -f $1
}

function waitForPodState {
  info "Waiting for pod $1 to be in state $2"
  for (( i=0; i<120; ++i )); do
    local state=$(getPodState $1)

    if [ "$2" = "${state}" ] ; then {
      echo -e "✔️  Pod $1 is $2"
      return
    } else {
      echo -e "⚙️  Pod $1 is not in state $2, current state: ${state}"
      sleep 3
    }
    fi

  done
}

function getPodState {
  local res=`oc get pods | grep $1 | grep -v "deploy" | awk '{ print $3 }'`
  echo $res
}
