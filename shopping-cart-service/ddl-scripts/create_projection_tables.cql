/* Projection management related tables */
CREATE TABLE IF NOT EXISTS cart_service.offset_store
(
    projection_name text,
    partition       int,
    projection_key  text,
    offset          text,
    manifest        text,
    last_updated    timestamp,
    PRIMARY KEY ((projection_name, partition), projection_key)
    );

CREATE TABLE IF NOT EXISTS cart_service.projection_management
(
    projection_name text,
    partition int,
    projection_key text,
    paused boolean,
    last_updated timestamp,
    PRIMARY KEY ((projection_name, partition), projection_key)
    );

/* Read-side projection tables */
CREATE TABLE IF NOT EXISTS cart_service.item_popularity
(
    item_id text,
    count   counter,
    PRIMARY KEY (item_id)
)
