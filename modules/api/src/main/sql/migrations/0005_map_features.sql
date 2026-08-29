-- What the api-worker's index-map job learns about a map by opening its world: one row per map,
-- replaced whole whenever the map is indexed again.
--
-- Deliberately holds nothing that already lives in maps: no rating, no play count, no author.
-- Those are joinable at query time, and copying them here would mean reindexing every map
-- whenever someone rates it. What is left is exactly the set of things you can only learn from
-- the world bytes, which keeps a row a pure function of them: an unchanged map always produces
-- an identical row. Ratios of these — fill, aspect, verticality — are one expression away and
-- are not stored.
--
-- feature_version is the version of the code that wrote the row; a backfill finds the rows
-- behind it. Sizes are in blocks, from the extent trimmed of the outermost 1% of occupied 8^3
-- cells at each end, since a single stray block a thousand blocks out is common and would
-- otherwise dominate.
create table if not exists map_features
(
    map_id              uuid             not null primary key,
    feature_version     int              not null,
    indexed_at          timestamptz      not null default now(),

    -- The mapmaker data version the world was saved with, null when it predates one. Triggers in
    -- a world that old are not scanned, so the counts and sets below are empty for it.
    data_version        int,

    -- Geometry
    block_count         bigint           not null, -- non-air blocks
    extent_x            int              not null,
    extent_y            int              not null,
    extent_z            int              not null,
    occupied_cells      int              not null, -- 8^3 cells containing at least one block
    distinct_blocks     int              not null, -- block types, ignoring state
    dominant_block_frac double precision not null, -- share of blocks that are the most common type

    -- Everything in the world that is not a marker: displays, item frames, armour stands. Good
    -- maps tend to be decorated, so this is a quality signal, and text displays in particular
    -- are how a map explains itself.
    entity_count        int              not null,
    text_display_count  int              not null,

    -- Structure, from checkpoint, finish and status triggers, with pads of plates merged into
    -- one trigger. The spawn is not a checkpoint.
    checkpoint_count    int              not null,
    checkpoint_spacing  double precision,          -- median nearest-neighbour distance; null under two checkpoints
    finish_count        int              not null,
    status_count        int              not null,

    -- Mechanics, from the action lists on every trigger. Presence only: items persist once
    -- granted, so a count would measure map length more than the mechanic.
    mechanics           text[]           not null, -- blocks, ender_pearl, elytra, teleport, timer, ...
    attributes          text[]           not null, -- attributes the map edits: gravity, scale, ...
    potion_effects      text[]           not null, -- speed, jump_boost, ...
    settings            text[]           not null, -- settings something turns on: only_sprint, no_jump, ...
    action_count        int              not null, -- actions across every trigger, a complexity signal

    -- Triggers that did not decode and were left out of everything above, plus one for each
    -- chunk whose data did not read at all, entities included. Anything but zero means the
    -- structure, mechanics and entity counts of this row are missing something.
    decode_failures     int              not null
);

create index if not exists map_features_mechanics_idx on map_features using gin (mechanics);
create index if not exists map_features_settings_idx on map_features using gin (settings);
