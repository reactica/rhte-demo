## How to setup the demo

### Install and setup OpenShift

These instructions are written for Red Hat CDK 3.5, but are valid for any minishift installation using OpenShift 3.9+.

1. Download and install [Red Hat CDK 3.5](https://developers.redhat.com/products/cdk/overview/).
2. Create a profile for the demo using the following settings
    ```
    minishift profile set rhte-vertx-demo
    minishift config set memory 8GB
    minishift config set cpus 3
    minishift config set image-caching true
    minishift addon enable admin-user
    minishift addon enable anyuid
    ```
3. Start minishift

    ```
    minishift start
    ```
4. Run the following commands to make sure we have the correct image streams of Red Hat Data Grid and Red Hat AMQ.

    ```
    sh setup/openshift/install-is-and-templates.sh
    ```
    **NOTE:** The script uses `--as=system:admin` so if you are installing to a cluster or user that doesn't have **anyuid** enabled you need to run the commands in the script manually.

5. Create a project and add view to the default service account.
    
    ```
    oc new-project reactive-demo
    ```

6. Give the default service account right to view the project (required for AMQ and DG)
    
    ```
    oc policy add-role-to-user view system:serviceaccount:$(oc project -q):default -n $(oc project -q)
    ```

7. Create the **EventStore** using Red Hat Data Grid
   ```
   oc new-app --template=datagrid72-basic -p APPLICATION_NAME=eventstore-dg -p CACHE_NAMES=userevents,rideevents
   ```

8. Create the **EventStream** using Red Hat AMQ
   ```
   oc new-app --template=amq63-basic -p APPLICATION_NAME=eventstream -p MQ_QUEUES=ENTER_EVENT_QUEUE,RIDE_EVENT_QUEUE,QLC_QUEUE,CL_QUEUE -p MQ_USERNAME=user -p MQ_PASSWORD=user123
   ```
9. DONE