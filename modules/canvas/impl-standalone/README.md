# canvas-impl-standalone

Standalone serverside implementation of the Canvas API.

## Notes
- `RootContainer` which handles interaction with the actual player inventory
  - Placing items in the inventory
  - Proxying the player inventory
  - Tracking the current title of the inventory
- `ViewContainer` which holds a single user-defined View object
  - Acts as a barrier of IDs (eg when traversing to find an id, we stop at this point)
  - Alternatively, can we find ID/do reflection when building the gui and just update the actual view object?
- All elements will be clonable, which will allow us to parse them from xml a single time.
