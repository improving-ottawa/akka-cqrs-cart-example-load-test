## Deploying in K8S

This directory contains a few resource definitions demonstrating how this application might be deployed and operated in k8s.

Note:  this guide was developed and tested on Docker desktop embedded k8s.

The demo resources include:
- a single node cassandra cluster (`cassandra.yaml`)
- a 3 node shopping-cart-service deployment, with an exposing service (`resources.yaml`)
  - uses [Akka Kubernetes API Discovery](https://doc.akka.io/docs/akka-management/current/discovery/kubernetes.html) to find peers for cluster bootstrapping.
- a test-pod definition to use to test connections between the other pods involved, for troubleshooting, etc. (`test-pod.yaml`)

### Building image

The demonstration resources deploy the application as a container, and therefor require an image be constructed.  Ultimately this image will need to be published/available to the k8s pods.  Build the image by running this command in the root directory of `shopping-cart-service`:
```
$> sbt clean docker:publishLocal
```
Take note of the image path, name and tag.

If running in a cloud K8s environment, the image will need to be pushed/published to an image repository with the appropriate credentials.

### Configuration

Shopping cart service resources are configured in `resources.yaml`

##### Image

It references this image, which must be published somewhere that the k8s pod will be able to access it - the example relied on a locally published docker image
https://github.com/improving-ottawa/akka-cqrs-cart-example-load-test/blob/579f4251773c8c21d874d28794d0e5f9ca104894/shopping-cart-service/deployment/resources.yaml#L34-L38

If changes are made to the application or the version number changes, a new image must be published.

##### Cassandra
The shopping cart service deployment resources  accepts the DB seed entries and datacenter values as environment variable parameters: 

https://github.com/improving-ottawa/akka-cqrs-cart-example-load-test/blob/579f4251773c8c21d874d28794d0e5f9ca104894/shopping-cart-service/deployment/resources.yaml#L53-L56

In this example, the `cassandra-1.cassandra-single` hostname is provided by the service defined for the single node Cassandra cluster (see `cassandra.yaml`)

These parameters specifically must be modified to point to the seed nodes of a deployed persistence backend (Cassandra or Scylla cluster).

##### Application

It also references the `src/main/resources/deployed.conf` file to enable K8S API for discovery and configure the minimum number of peers to form a cluster.

https://github.com/improving-ottawa/akka-cqrs-cart-example-load-test/blob/579f4251773c8c21d874d28794d0e5f9ca104894/shopping-cart-service/deployment/resources.yaml#L43-L48

##### Resources

The nodes are given fairly modest resource ask & limits, and heapsizes:
https://github.com/improving-ottawa/akka-cqrs-cart-example-load-test/blob/579f4251773c8c21d874d28794d0e5f9ca104894/shopping-cart-service/deployment/resources.yaml#L84-L89


### Deploying

To deploy cassandra:
```
$> kubectl apply -f cassandra.yaml
```

If it's the first time starting this cluster, you must bootstrap the schema as well:

```
$> kubectl exec -t cassandra-single  -- cqlsh -e "$(cat ddl-scripts/create_keyspace.cql)"
$> kubectl exec -t cassandra-single  -- cqlsh -e "$(cat ddl-scripts/create_es_tables.cql)"
$> kubectl exec -t cassandra-single  -- cqlsh -e "$(cat ddl-scripts/create_projection_tables.cql)"
```


To deploy shopping-cart-service:
```
$> kubectl apply -f resources.yaml
```
To view the pods
```
$> kubectl get pods
```
Once all pods are running you should be able to confirm they have started successfully by inspecting the logs from one of the pods:
```
kubectl logs shopping-cart-service-76f848979b-2hk9d
```


#### Test it out

You can test whether the service is responding properly by opening a port-forward to the shopping-cart-service:
```
kubectl port-forward service/shopping-cart-service 8101:8101
```
and send a test command (from another terminal):
```
grpcurl -d '{"cartId":"cart1", "itemId":"socks", "quantity":3}' -plaintext 127.0.0.1:8101 shoppingcart.ShoppingCartService.AddItem
```
