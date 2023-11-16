# Network info

Scratch file for planning what needs to be persisted for players

## Persistent info

LOCAL SESSION

- Selections
    - Name
    - Type specific data (eg for cuboid, the two corners)
- History
    - Indexed history list
    - Current index
    - TODO: The server should be responsible for trimming this to a max size (both in bytes and in number of ops)
    - History size?

PLAYER SESSION

- Settings
    - Loaded CUIs (associated settings, each one can be enabled/disabled, and they have a fixed priority. For example,
      if you have both Axiom and Debug Renderer, axiom would be enabled, but when you make a selection it would still be
      rendered with debug renderer.)
        - Axiom
        - Debug Renderer
        - WorldEditCUI
        - Particles
- Capabilities (maybe merge with settings)
    - Max blocks/operation (estimated)
    - Max clipboards (size, number of clipboards)
    - Max selections
    - History limits (size, number of operations)
    - any other stuff from extra modules
- Clipboards
    - Name
    - Metadata
    - Buffer (or schematic?)

SCHEMATICS

- Name
- Metadata
- Schematic data
- Maybe a type (could have raw buffer schematics in the future)
  Server will read all "headers" but will only request the actual data when //schem load is used.

OPERATION LOG

- Type
- Tool Params
- Changed blocks (count)
- Result (success/fail)
