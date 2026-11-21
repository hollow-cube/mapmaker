create index if not exists replays_legacy_format_idx on replays (id)
    where substring(current_preamble from 5 for 2) = '\x0004'::bytea;
