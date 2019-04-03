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

1. Billboard - A frontend based on [JQuery](https://jquery.com) using a [Vert.x](https://vertx.io) based runtime
2. Event Generator - This service will generate different events such as: users entering the queue, users getting on the ride etc, these events are send out as [AMQ messages](https://www.redhat.com/en/technologies/jboss-middleware/amq) and it’s using [Vert.x](https://vertx.io) as runtime
3. Event Store - This service will retrieve events from AMQ and store them to the [Data Grid](https://www.redhat.com/en/technologies/jboss-middleware/data-grid).
4. Current Line Updater - This service will register a [continuous query with Red Hat Data Grid](https://access.redhat.com/documentation/en-us/red_hat_jboss_data_grid/7.1/html/developer_guide/querying#continuous_queries) that will trigger for events that are related to the queue (line). When the queue is updated this service will send a updated version of the current queue to the billboard service via AMQ.
5. Queue Length Calculator - This service will calculate the approximate waiting time for a person entering the queue at this point based on the number of persons in queue and how many people the ride can carry. This service will trigger every 10s and then sends an updated queue time to the billboard.


![Demo Screenshot](setup/images/billboard.png?raw=true "Demo Screenshot")

![Architecture Screenshot](setup/images/arch.png?raw=true "Architecture")

Prerequisites
================
In order to deploy this demo, you need [Minishift](https://github.com/minishift/minishift/releases) v1.33.0 or later installed and the `minishift` CLI available on your `$PATH`.

Deploying the Demo
=============================
See the [setup instructions](setup/README.md)
