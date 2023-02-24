CREATE TABLE IF NOT EXISTS cart_service.item_popularity
(
    item_id text,
    count   counter,
    PRIMARY KEY (item_id)
)
