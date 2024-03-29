CREATE TABLE IF NOT EXISTS cart_service.messages
(
    persistence_id    text,
    partition_nr      bigint,
    sequence_nr       bigint,
    timestamp         timeuuid,
    timebucket        text,
    writer_uuid       text,
    ser_id            int,
    ser_manifest      text,
    event_manifest    text,
    event             blob,
    meta_ser_id       int,
    meta_ser_manifest text,
    meta              blob,
    tags              set<text>,
    PRIMARY KEY ((persistence_id, partition_nr), sequence_nr, timestamp, timebucket)
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

CREATE TABLE IF NOT EXISTS cart_service.tag_views
(
    tag_name            text,
    persistence_id      text,
    sequence_nr         bigint,
    timebucket          bigint,
    timestamp           timeuuid,
    tag_pid_sequence_nr bigint,
    writer_uuid         text,
    ser_id              int,
    ser_manifest        text,
    event_manifest      text,
    event               blob,
    meta_ser_id         int,
    meta_ser_manifest   text,
    meta                blob,
    PRIMARY KEY ((tag_name, timebucket), timestamp, persistence_id, tag_pid_sequence_nr)
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

CREATE TABLE IF NOT EXISTS cart_service.tag_write_progress
(
    persistence_id      text,
    tag                 text,
    sequence_nr         bigint,
    tag_pid_sequence_nr bigint,
    offset              timeuuid,
    PRIMARY KEY ((persistence_id), tag)
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

CREATE TABLE IF NOT EXISTS cart_service.tag_scanning
(
    persistence_id text,
    sequence_nr    bigint,
    PRIMARY KEY (persistence_id)
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

CREATE TABLE IF NOT EXISTS cart_service.metadata
(
    persistence_id text PRIMARY KEY,
    deleted_to     bigint,
    properties     map<text,text>
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

CREATE TABLE IF NOT EXISTS cart_service.all_persistence_ids
(
    persistence_id text PRIMARY KEY
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


CREATE TABLE IF NOT EXISTS cart_service.snapshots
(
    persistence_id    text,
    sequence_nr       bigint,
    timestamp         bigint,
    ser_id            int,
    ser_manifest      text,
    snapshot_data     blob,
    snapshot          blob,
    meta_ser_id       int,
    meta_ser_manifest text,
    meta              blob,
    PRIMARY KEY ((persistence_id), sequence_nr)
)
    WITH CLUSTERING ORDER BY (sequence_nr DESC)
    AND gc_grace_seconds =864000
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
