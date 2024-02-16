# Notes about module rework

The core issue is that the hub currently is not a proper map, in fact the only relationship it has to maps is that
it reads a map world from r2. This is problematic because it means implementing a feature for maps (such as custom
biomes) does not allow it to work for the hub.

The aim of this doc is to figure out what it would take to rework the hub into being a standalone map server, with
a bunch of extra logic to handle the hub-specific features.

## Current state

- `modules/common`: Stuff that should be extracted into a separate library, basically just Minestom utilities.
- `modules/core`: Mapmaker core, including api specs, common commands, etc
- `modules/hub`: The hub server, which loads the map world from r2 and has the hub features like train, motw counter,
  etc.
- `modules/map`: The map server including editing, playing, testing maps, MapWorld, etc.
- `bin/*`: Glue modules which are a ton of duplication and very gross.

## End state & notes & etc

- `modules/common`: Cleaned up minestom utilities, ready to be extracted to another repo

`modules/map-core`

- Minimum required to run a map
- Must not enforce multiple maps per world, could have some kind of map allocator or something
- Should have the code for main map features like biomes, animations, etc. It should NOT have parkour, etc
  basically anything stored in the world file should have the supporting logic in this module
- Should make no assumptions about the map like whether you can build or not, just the basic requirements for
  reading the world, adding + removing players, etc.
-

`bin/hub`

- The hub server runtime deployed in Kubernetes
- Depends on `modules/map-core` for the map loading logic, then adds in any hub specific commands/etc.
- CAN RUN STANDALONE WITH NOOP SERVICES

`bin/map`

- CAN RUN STANDALONE WITH NOOP SERVICES

## Open questions

- Can we keep DevServer? what is the flow to run locally outside of tilt? Running the debugger is a hard requirement
    - I think this can work. Basically the Hub should have `HubMapWorld` which implements the hub logic, it should be
      possible to instantiate this in a map manager like any other map.





