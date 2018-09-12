## How to setup the demo

### Install and setup required services

```bash
./deploy.sh
```

This script requires `minishift` top be available in your `$PATH` and Internet access.

It:

* starts minishift with the right configuration is not yet started
* deploys the image streams and template
* adjusts permissions
* instantiates AMQ broker and the data grid server
* waits for the readiness of the datagrid and AMQ broker.

The script _should be idempotent_.

### Install the application

```bash
./deploy-application.sh
```

This script requires `minishift` top be available in your `$PATH` and Internet access.

It:

* Builds the application and its services
* deploys the application
* waits for the readiness of the components

The script _should be idempotent_.

