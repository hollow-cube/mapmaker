create index if not exists replays_compacted_object_idx on replays (compacted_object)
    where compacted_object is not null;
