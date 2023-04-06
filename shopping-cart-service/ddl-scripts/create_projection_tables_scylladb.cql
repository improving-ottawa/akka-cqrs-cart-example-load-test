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
)
WITH gc_grace_seconds =864000
 AND compaction = {
    'class' : 'IncrementalCompactionStrategy',
    'bucket_high' : 1.5,
    'bucket_low' : 0.5,
    'min_sstable_size' : 50,
    'min_threshold' : 4,
    'max_threshold' : 32,
    'sstable_size_in_mb' : 1000,
    'space_amplification_goal' : 1.25
    };

CREATE TABLE IF NOT EXISTS cart_service.projection_management
(
    projection_name text,
    partition int,
    projection_key text,
    paused boolean,
    last_updated timestamp,
    PRIMARY KEY ((projection_name, partition), projection_key)
)
WITH gc_grace_seconds =864000
 AND compaction = {
    'class' : 'IncrementalCompactionStrategy',
    'bucket_high' : 1.5,
    'bucket_low' : 0.5,
    'min_sstable_size' : 50,
    'min_threshold' : 4,
    'max_threshold' : 32,
    'sstable_size_in_mb' : 1000,
    'space_amplification_goal' : 1.25
    };

/* Read-side projection tables */
CREATE TABLE IF NOT EXISTS cart_service.item_popularity
(
    item_id text,
    count   counter,
    PRIMARY KEY (item_id)
)
WITH gc_grace_seconds =864000
 AND compaction = {
    'class' : 'IncrementalCompactionStrategy',
    'bucket_high' : 1.5,
    'bucket_low' : 0.5,
    'min_sstable_size' : 50,
    'min_threshold' : 4,
    'max_threshold' : 32,
    'sstable_size_in_mb' : 1000,
    'space_amplification_goal' : 1.25
    };
