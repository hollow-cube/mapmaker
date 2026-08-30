# Anticheat trace format (`.trace`, format version 2)

A trace is one connection's captured packet stream: everything needed to put a replay client into
the state the real client was in, then every kept frame in arrival order. Written by
`modules/anticheat` (`TraceWriter`), read by `TraceReader`; `./gradlew :modules:anticheat:dumpTrace
-Pfile=x.trace -Pframes` prints one.

All integers are big-endian. `varint` is the vanilla protocol varint (LEB128, max 5 bytes).

## Container

```
"HCTR" (u32 magic 0x48435452) | u16 formatVersion | u32 headerCapacity | u32 headerLength
headerCapacity bytes: headerLength bytes of UTF-8 JSON (the header), then zero padding
zstd stream (level 3): body
```

The header is written first into a reserved region (`json length + 1024` slack) and rewritten in
place at close with the fields only known then (`endedAt`, `closedBy`, flags, counters). Header
first — rather than a better-compressing trailer — so a file cut short by a crash still opens: it
keeps its opening header and yields every frame up to the writer's last flush (the zstd stream is
flushed at least every 1 MiB of frame bytes). Readers treat a truncated body as the end of the
trace, not an error (`Trace#truncated()`).

## Header (JSON)

Every field is optional as far as parsing goes: an older or newer writer's header still loads,
unknown fields dropped, missing ones null. Fields:

| field | meaning |
|---|---|
| `formatVersion` | container version, same value as the fixed head's |
| `clientPvn` | the client protocol version every frame is in (776 = 26.2) |
| `brand` | client brand (`vanilla`, `fabric`, …), the tap's payload or the proxy's fallback |
| `playerId` / `playerName` | who the connection belongs to |
| `connectionId` | random uuid per tapped connection, orders several traces of one login |
| `captureId` | what the backend asked to be filed under (the run id for competes); null for a flush |
| `reason` | `run` \| `sample` \| `flag` \| `manual` |
| `closedBy` | `stop` \| `disconnect` \| `shutdown` \| `superseded` \| `flush` |
| `cohort` | `trusted` \| `random`, a grader prior, never ground truth |
| `trim` | `{chunkRadius, entityRange}` actually applied; `chunkRadius: -1` = kept everything |
| `proxy` | hostname of the proxy that wrote it |
| `proxyVersion` | git commit the plugin was built from |
| `startedAt` / `endedAt` | ISO-8601 UTC; `tNs` frame times are relative to the start |
| `pingIds` | `{first, last}` injected ping sequence range, absent when none were injected |
| `flags` | `ringTruncated`, `spoolTruncated`, `installedMidSession`, `tailUnfenced` — every way the trace is less than the whole truth. `tailUnfenced`: the connection ended with this trace, so frames after the last answered ping have no client-side upper bound and never will |
| `counters` | `frames`, `bytes` (frame section only), `preludeFrames`, `chunks`, `droppedFrames` (queue overflow at the tap) |
| `extras` | string map for fields a later build wants before a version bump is worth it |

## Body (inside the zstd stream)

```
varint preludeCount | preludeCount frames
varint chunkCount   | chunkCount world chunks
frames until end of stream
```

### Frame

```
varlong tDeltaNs | u8 (state << 1 | direction) | varint packetId | varint pingId
varint length | length bytes
```

- `tDeltaNs` — nanoseconds since the previous frame in the stream (prelude included; the first
  frame's delta is from 0). Frames are appended in arrival order, so deltas are small and
  nondecreasing; absolute `tNs` is the running sum, relative to the trace start, not wall clock.
- packed byte — `state` (0 handshake, 1 login, 2 configuration, 3 play) in the high bits,
  `direction` (0 = C2S, 1 = S2C) in bit 0. Stable wire codes, not enum ordinals.
- `packetId` — the client-pvn packet id; `Protocol776` maps (state, direction, id) to a name and,
  where one exists, a decoder. Bytes are the packet body **after** its id varint, exactly as the
  client parsed them (C2S before Via rewrote them, S2C after).
- `pingId` — the last tap-injected ping before this frame, as the tap's sequence counted from 1
  (0 = none yet). On the wire the injected ping packet (and its pong) carry
  `0x8000_0000 | sequence` — the bit is wire encoding that keeps the id clear of the backend's
  own ping space, and readers mask it off when matching a recorded pong body to a sequence. This
  is the fence bracket: a state change fenced by ping *n* was seen by the client no later than
  the C2S pong frame answering *n*.

### Prelude

Synthesized frames that replay the client into the snapshot state, all with `tNs = 0`: the state
cache's kept frames in original arrival order, then one `entity_position_sync` per tracked entity
(zero velocity on purpose — the model does not track it) and an absolute `player_position` for the
own player. Dropped display entities are absent entirely.

### World chunk

```
i32 chunkX | i32 chunkZ | varint sectionCount | sectionCount entries
entry: u8 kind (0 = inline, 1 = by-hash)
  inline:  varint length | length bytes: one Section
  by-hash: 32 bytes SHA-256 (reserved for a content-addressed store; never written today)
```

Sections are bottom-to-top over the whole dimension height (index = section index). Sections the
model holds as empty are written as an all-air single-value section, since the chunk packet cannot
express a missing one.

### Section

`ClientboundLevelChunkPacketData`'s section shape, byte-compatible with the 26.2 client
(`LevelChunkSection#read`):

```
i16 nonEmptyBlockCount | i16 fluidCount
u8 bitsPerEntry
  0:    varint singleValue (no data array entries)
  1..8: varint paletteLength | palette varints (1..4 bits stored at 4, as the client does)
  >8:   global palette, stored at 15 bits (26.2's registry width)
long[] data (fixed count derived from bits; no length prefix in v2's inline encoding — the Section
             blob is length-prefixed as a whole by the entry)
biomes: kept verbatim as the bytes they arrived as (nothing replays them; verbatim keeps encode
        byte-identical to decode)
```

Block state ids are global client-pvn state ids; quantisation to any other palette is read-time
work.

## Versioning

`formatVersion` bumps on any incompatible change to the container, header semantics or body.
Readers keep every version they ever wrote (v1 is the one allowed exception: its world section also
carried heightmaps and a block-entity/light tail nothing replays). Additive header fields go in
without a bump (unknown fields drop); a JSON→binary or field-meaning change bumps.

## Naming

Files are `{id}.trace`; the id is the proxy's (uuid), must match `[A-Za-z0-9][A-Za-z0-9._-]{0,127}`
(a path component). The store lays blobs out as `{yyyy}/{mm}/{dd}/{id}.trace` under its root, dated
by `startedAt`, and indexes them in `anticheat_traces` — the blob is the record, the row is only
the way in.
