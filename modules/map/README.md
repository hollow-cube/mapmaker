# Map
Manages map instances for editing and playing. Designed to be used either as a single instance or scale with itself.

## Roles
- Open a map (on a specific version) for playing
- Open a map for editing

## Notes
Opening a map loads the latest version no matter what for now, updating will directly overwrite that version.

Playing a map is a read only operation. Editing requires a lock on the map (only one editing instance at a time)
