## WIP: Akka persistent entity Load Test project

This is a collection of projects that act as a simplified test harness for designing and running a simple load
test scenario through a typical Akka persistent entity workload.

## Goals

To provide a test harness to experiment with:

1.  Providing a controlled environment in which to test the performance of various journal/snapshot/projection backends (Cassandra vs Scylla vs Postgres)
2.  Different tunings of persistent entities and the JVM operating them, to gain insight into the performance of the JVM under various conditions

## Scenario

Currently the scenario only involves the shopping-cart-service (taken from Akka's CQRS/microservices developer guide example), to keep the moving parts to a minimum

### To run the load test

Navigate to shipping-cart-service, and start necessary infrastructure (Kafka - for now, Postgres - projections, Cassandra - journal/snapshots):

```
$> shopping-cart-service/docker-compose up
```

If this is the first time starting the application, you must also bootstrap the journal.

```
To use Cassandra database as a journal:

# create keyspace
docker exec -i shopping-cart-service-cassandra-db-1 cqlsh -t < ddl-scripts/create_keyspace.cql

# creates the tables needed for Akka Persistence
# as well as the offset store table for Akka Projection
docker exec -i shopping-cart-service-cassandra-db-1 cqlsh -t < ddl-scripts/create_es_tables.cql
```



Once all infra containers are ready, start a at least one node of the shopping cart service:

```
$> cd shopping-cart-service; sbt -Dconfig.resource=local1.conf run
```

Once the shopping cart service is active, the load test can be started:

```
$> cd load-testing; sbt gatling:test
```

Look to the READMEs of each subproject for more details on tuning parameters, etc
