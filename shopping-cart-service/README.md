## Housekeeping

Tests are currently broken because they were written strictly against the postgres write/read side and we have swapped in 
Cassandra as the primary/only persistence layer but have not yet written tests.  Cassandra read/write side were tested ad-hoc by running
the sample code as described below.

## Running the sample code

1. Make `shopping-cart-service` directory as your current working directory. For example, 

    ```shell
    cd ~/akka-cqrs-cart-example-load-test/shopping-cart-service
    ```

2. Depending on which persistence layers are configured, start required local infrastructure. `docker-compose.yml` should start everything required for running locally.
3. (Optional) Upon running the application for the first time, database schemas must be bootstrapped, as per below

   For Postgres DB:
    ```shell
    docker-compose up -d

    # creates the tables needed for Akka Persistence
    # as well as the offset store table for Akka Projection
    docker exec -i shopping-cart-service-postgres-db-1 psql -U shopping-cart -t < ddl-scripts/create_tables.sql
    
    # creates the user defined projection table.
    docker exec -i shopping-cart-service-postgres-db-1 psql -U shopping-cart -t < ddl-scripts/create_user_tables.sql
    ```

   OR, for Cassandra as a journal & projection:
   ```shell
    docker-compose up -d

    
   # create keyspace
    docker exec -i shopping-cart-service-cassandra-db-1 cqlsh -t < ddl-scripts/create_keyspace.cql
    
    # creates the tables needed for Akka Persistence
    docker exec -i shopping-cart-service-cassandra-db-1 cqlsh -t < ddl-scripts/create_es_tables.cql
   
    # finally, the offset store table for Akka Projections and any projection tables
    docker exec -i shopping-cart-service-cassandra-db-1 cqlsh -t < ddl-scripts/create_projection_tables.cql
    ```

4. Compile the `shopping-cart-service` code

    ```shell
    sbt clean compile
    ```

5. Start a first node:

    ```shell
    sbt 'set run/javaOptions += "-Dconfig.resource=local1.conf"; run'
    ```

6. (Optional) Start another node with different ports:

    ```shell
    sbt 'set run/javaOptions += "-Dconfig.resource=local2.conf"; run'
    ```

7. (Optional) More can be started:

    ```shell
    sbt 'set run/javaOptions += "-Dconfig.resource=local3.conf"; run'
    ```

8. Check for service readiness

    ```shell
    curl http://localhost:9101/ready
    ```

9. Try it with [grpcurl](https://github.com/fullstorydev/grpcurl):

    ```shell
    # add item to cart
    grpcurl -d '{"cartId":"cart1", "itemId":"socks", "quantity":3}' -plaintext 127.0.0.1:8101 shoppingcart.ShoppingCartService.AddItem
    
    # get cart
    grpcurl -d '{"cartId":"cart1"}' -plaintext 127.0.0.1:8101 shoppingcart.ShoppingCartService.GetCart
    
    # update quantity of item
    grpcurl -d '{"cartId":"cart1", "itemId":"socks", "quantity":5}' -plaintext 127.0.0.1:8101 shoppingcart.ShoppingCartService.UpdateItem
    
    # check out cart
    grpcurl -d '{"cartId":"cart1"}' -plaintext 127.0.0.1:8101 shoppingcart.ShoppingCartService.Checkout
    
    # get item popularity
    grpcurl -d '{"itemId":"socks"}' -plaintext 127.0.0.1:8101 shoppingcart.ShoppingCartService.GetItemPopularity
    ```

    On successful execution of the above `grpccurl` commands you should have the following record counts in tables in the `cart_service` keyspace (assuming you are using Cassandra/ScyllaDB as your persistence layer):
    ```
   Table Name                Record Count
   ----------------------    ---------------
   all_persistence_ids       1
   item_popularity           1
   offset_store              1
   tag_scanning              1
   messages                  3
   tag_views                 3
   metadata                  0
   snapshots                 0
   projection_management     0
   ```
10. Optional, if you have launched additional `shopping-cart-service` instances, as mentioned above, then issue similar `grpcurl` commands for port 8102 and 8103 to reach to 2nd and 3rd instances of `shopping-cart-service`, respectively. 
