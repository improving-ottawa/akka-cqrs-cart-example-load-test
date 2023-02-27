## Housekeeping

Tests are currently broken because they were written strictly against the postgres write/read side and we have swapped in 
Cassandra as the primary/only persistence layer but have not yet written tests.  Cassandra read/write side were tested ad-hoc by running
the sample code as described below.

## Running the sample code

1. Depending on which persistence layers are configured, start required local infrastructure. `docker-compose.yml` should start everything required for running locally.
2. (Optional) Upon running the application for the first time, database schemas must be bootstrapped, as per below

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

2. Start a first node:

    ```shell
    sbt 'set run/javaOptions += "-Dconfig.resource=local1.conf"; run'
    ```

3. (Optional) Start another node with different ports:

    ```shell
    sbt 'set run/javaOptions += "-Dconfig.resource=local2.conf"; run'
    ```

4. (Optional) More can be started:

    ```shell
    sbt 'set run/javaOptions += "-Dconfig.resource=local3.conf"; run'
    ```

5. Check for service readiness

    ```shell
    curl http://localhost:9101/ready
    ```

6. Try it with [grpcurl](https://github.com/fullstorydev/grpcurl):

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

    or same `grpcurl` commands to port 8102 to reach node 2.
