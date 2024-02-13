Statuses are as follows:

- ✅: Supported
- 🚧: Incomplete/in progress
- ⚠️: Partially supported
- ❌: Not supported

### Masks

- ✅ Block
    - 🚧 Parsing
- ✅ Not
    - 🚧 Parsing
- 🚧 #existing
- ✅ #solid
    - 🚧 Parsing
- ✅ > (overlay)
    - 🚧 Parsing
- ✅ < (underlay)
    - 🚧 Parsing
- 🚧 Region
- 🚧 Tags ("block categories")
- ✅ Random noise
    - 🚧 Parsing
- ✅ Block state
    - 🚧 Parsing
- ❌ Expression - Expression language not supported
- 🚧 Biome
- ✅ #surface
    - 🚧 Parsing

### Patterns

- ✅ Single block
    - ✅ Legacy block IDs (ie `0` for `air`)
- ✅ Random (multiple patterns, ie `stone,stone_stair,dirt`)
- ✅ Random state (ie `*stone_stairs`)
- 🚧 Clipboard
    - TODO: Patterns currently have no way to access the clipboard of the Player at any phase
- ✅ Type/state preserving (ie `^oak_stairs`)
- ✅ Tags (ie `##stairs`)
- ❌ Special blocks - Not supported until requested, it seems really gross (will have args variant in tf patterns)
