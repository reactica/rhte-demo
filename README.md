![Coderland logo](setup/images/Coderland_logo.png)

# `Reactica`

## Overview 

This is a demo of a ride queueing system for _Coderland_, an amusement park built with the latest and greatest technologies
to provide and innovative and compelling guest experience.

The name of the ride is _Reactica_, a thrilling roller coaster ride through another dimension.

The demo showcases the new Red Hat Application Runtimes (Red Hat [OpenShift Container Platform](https://openshift.com) with [Red Hat Application Services](https://developers.redhat.com/products/rhoar))
in the context of reactive systems development, showing how components work together to form truly distributed/reactive systems
(not just reactive programming). It utilizes several components from Red Hat,
including [Eclipse Vert.x](https://vertx.io), [Red Hat Data Grid](https://developers.redhat.com/products/datagrid/overview), and [Red Hat AMQ](https://developers.redhat.com/products/amq/overview).

## Architecture
--------
The architecture of the Reactica system looks like this: 

![Architecture Screenshot](setup/images/arch.png?raw=true "Architecture")

There are several individual microservices and infrastructure components that make up this app:

1. Billboard UI - A frontend based on [JQuery](https://jquery.com) using a [Vert.x](https://vertx.io) based runtime
2. Event Generator (Users and Rides in the diagram) - This service generates different events such as: users entering the queue, users getting on the ride etc, these events are sent out as [AMQ messages](https://developers.redhat.com/products/amq/overview) using [Vert.x](https://vertx.io) as its runtime
3. Event Store - This service retrieves events from AMQ and stores them to the [Data Grid](https://www.redhat.com/en/technologies/jboss-middleware/data-grid).
4. Current Line Updater - This service registers a [continuous query with Red Hat Data Grid](https://access.redhat.com/documentation/en-us/red_hat_jboss_data_grid/7.1/html/developer_guide/querying#continuous_queries) that triggers for events that are related to the line for the ride. When the line is updated, this service sends an updated version of the current line to the billboard service via AMQ.
5. Queue Length Estimate - This service calculates the approximate waiting time for a person entering the line at this point based on the number of persons in line and how many people the ride can carry. This service triggers every 10 seconds and sends an updated wait time to the billboard.

## User interface
The main UI looks like this: 

![Demo Screenshot](setup/images/billboard.png?raw=true "Demo Screenshot")

This panel shows several users waiting in line (in blue), several users currently on the ride (in yellow), and one person who has completed the ride (in green).  

Prerequisites
================
To deploy this demo, you need [Minishift](https://github.com/minishift/minishift/releases) v1.33.0 or later installed and the `minishift` CLI available on your `$PATH`. You also need `curl` and [Maven v3.5.3](https://maven.apache.org/) or later. 

Deploying the Demo
=============================
See the [setup instructions](setup/README.md).

# Resources

Coderland's [Reactica roller coaster](https://developers.redhat.com/coderland/reactive)
features a complete, sophisticated example of a system 
of reactive Vert.x microservices that work together. 

:gift: REPO: The Reactica roller coaster source code (this page)

:page_facing_up: [Reactica: reactive programming and Vert.x tutorial](https://developers.redhat.com/coderland/reactive/)

:clapper: [VIDEO: An overview of the Reactica roller coaster](https://youtu.be/FgqbSNdR2AQ)

## Part 1: An introduction to reactive programming and Vert.x 

:page_facing_up: [ARTICLE: An introduction to reactive programming and Vert.x](https://developers.redhat.com/coderland/reactive/reactive-intro)

:clapper: [VIDEO: Reactive programming and Vert.x](https://youtu.be/o-cBfanMJ8A)

## Part 2: Building a reactive system

:page_facing_up: [ARTICLE: Building a reactive system](https://developers.redhat.com/coderland/reactive/building-a-reactive-system/)

:clapper: [VIDEO: Reactica architecture](https://youtu.be/ajlY_qiIunA)

## Part 3: A reactive system in action

:page_facing_up: [ARTICLE: A reactive system in action](https://developers.redhat.com/coderland/reactive/reactive-system-in-action/)

:clapper: [VIDEO: Deploying Reactica](https://youtu.be/6EivZoILNFg)

## Other useful links

:book: Clement Escoffier's excellent book [Building Reactive Microservices in Java](https://developers.redhat.com/books/building-reactive-microservices-java/old/), available for free from [the Red Hat Developer Program](https://developers.redhat.com/)

:page_facing_up: Andre Staltz's [The introduction to Reactive Programming you've been missing](https://gist.github.com/staltz/868e7e9bc2a7b8c1f754)

:page_facing_up: [The Reactive Manifesto](https://www.reactivemanifesto.org/)

:page_facing_up: [The Vert.x home page](https://vertx.io)

:gift: [REPO: The `vertx-starter` source code](https://github.com/redhat-developer-demos/vertx-starter), a simple Vert.x verticle that shows the basics of how Vert.x works

***

Coderland :roller_coaster::rocket::ferris_wheel: is an imaginary theme park for learning, developer training, and Red Hat software. See [the Red Hat Developer Program](https://developers.redhat.com/) for more great stuff.