Coderland Reactica Demo
=======================
This is a demo of a ride queueing system for _Coderland_, an amusement park built with the latest and greatest technologies
to provide and innovative and compelling guest experience.

The name of the ride is _Reactica_, a thrilling roller coaster ride through another dimension.

The demo showcases the new Red Hat Application Runtimes (Red Hat [OpenShift Container Platform](https://openshift.com) with [Red Hat Application Services](https://developers.redhat.com/products/rhoar))
in the context of reactive systems development, showing how components work together to form truly distributed/reactive systems
(not just reactive programming). It utilizes several components from Red Hat,
including [Eclipse Vert.x](https://vertx.io), [Red Hat Data Grid](https://www.redhat.com/en/technologies/jboss-middleware/data-grid), and [Red Hat AMQ](https://www.redhat.com/en/technologies/jboss-middleware/amq).

Services
--------
There are several individual microservices and infrastructure components that make up this app:

1. Billboard UI - A frontend based on [JQuery](https://angularjs.org)

![Demo Screenshot](setup/images/billboard.png?raw=true "Demo Screenshot")

![Architecture Screenshot](setup/images/arch.png?raw=true "Architecture")

Prerequisites
================
In order to deploy this demo, you need Minishift installed and the `minishift` CLI available on your `$PATH`.

Deploying the Demo
=============================
See the [setup instructions](setup/README.md)
