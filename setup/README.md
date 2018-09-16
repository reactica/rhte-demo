## How to setup the demo

### Install and setup required services

```bash
./deploy.sh
```

This script requires `minishift`, `curl`, and `python` to be available in your `$PATH` and Internet access.

It:

* starts minishift with the right configuration is not yet started
* deploys the image streams and template
* adjusts permissions
* instantiates AMQ broker and the data grid server
* waits for the readiness of the datagrid and AMQ broker.

The script _should be idempotent_.


### Install the application

```bash
./create-application-config.sh
./deploy-application.sh
```

This script requires `mvn` and `minishift` to be available in your `$PATH` and Internet access.

It:

* Builds the application and its services
* deploys the application
* waits for the readiness of the components

### Edit the user and ride simulators config

```bash
oc edit configmap reactica-config
```

It takes a few seconds to be applied (due to minishift configuration).
