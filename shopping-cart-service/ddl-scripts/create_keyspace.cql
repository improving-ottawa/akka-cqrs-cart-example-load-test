-- CREATE ROLE cart_service with PASSWORD='cart_service';

CREATE KEYSPACE IF NOT EXISTS cart_service
            WITH REPLICATION = { 'class' : 'SimpleStrategy','replication_factor':1 };
CREATE KEYSPACE IF NOT EXISTS cart_service
            WITH REPLICATION = { 'class' : 'NetworkTopologyStrategy','improving':3 };


-- GRANT ALL PRIVILEGES on cart_service to cart_service;
