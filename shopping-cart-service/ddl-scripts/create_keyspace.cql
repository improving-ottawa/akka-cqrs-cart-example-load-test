-- CREATE ROLE cart_service with PASSWORD='cart_service';

-- Keyspace definition for single-node cluster
CREATE KEYSPACE IF NOT EXISTS cart_service
            WITH REPLICATION = { 'class' : 'SimpleStrategy','replication_factor':1 };

-- Keyspace definition for multi-node cluster
-- CREATE KEYSPACE IF NOT EXISTS cart_service
--            WITH REPLICATION = { 'class' : 'NetworkTopologyStrategy','improving':3 };

-- GRANT ALL PRIVILEGES on cart_service to cart_service;
