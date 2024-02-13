# Terraform

Terraform is a set of world manipulation tools. It is designed as a Minestom-native alternative to WorldEdit,
VoxelSniper, etc.

maybe we should just make commands, masks, patterns each an option eg you can say you want
// - WorldEdit commands
// - Terraform masks
// - Terraform patterns

## Concepts

### Selection

In Terraform, each player may have multiple different selections, each which can be configured independently (eg shape,
masking, etc).
This will allow compatibility layers such as Loft in Arceon to continue functioning as if they have their own selection,
while still
using the core terraform selection implementation to maintain compatibility with other settings.

For every player, there is a default selection. This is used in WorldEdit compatibility when executing commands such
as `//set`, `//replace`, etc.
Other compatibility layers which use their own selection

Tools may be assigned to specific selections, ... todo these details when i get to them

### World vs Global

Clipboard _MIGHT_ eventually be global, but can be per world right now

### Misc notes

On join hub

- do nothing (no commands in hub cmd mgr)

On join map

```
var terraform = Terraform
    .build();

var playerSession = terraform.createSession(player);
var playerSession = terraform.createSession(player, sessionData);
```

###### Operations

- split operations by section
- when a section is done, it should be applied
- collect all sections and save as history
    - History needs to be stored not always in memory when it becomes big
        - Memory -> Disk (cache) -> Database
        - Terraform should be responsible for cleaning up old history/maintaining a max size

Could there be a "collection" phase to collect relevant sections and split the to-be-changed blocks into chunks which
can be executed in parallel?

- axiom: not really, it is streamed
- //set: yes
- //replace: yes

NOT REALLY TO THE ABOVE
What about instead, just apply blocks until the batch reaches a certain size, and then apply to the world and repeat.

realistically, it doesnt matter for now, just need to set a max size

Can i do these like axiom batches?

- compute the entire update in memory, then apply it
- always stored as a series of section palettes
- lets me do fancy direct palette updates
- can be parallelized and streamed very effectively

Would have a BatchApplyOperation which would have the following:

- metadata: size, etc
- sections: a list of sections + palettes
- block entities

The BatchApplyOperation needs to be built, which can be done asynchronously in a queue. Both compute and apply could
happen asynchronously.

- The compute phase would have an INIT, QUEUED, IN_PROGRESS, COMPLETE, FAILED state
- The apply phase would have the above states also.
    - The apply phase is the only relevant ones for undo/redo since they are precomputed.

Operation lifecycle:

- Task is queued
- Task runs, emits a batch apply operation
- operation is queued
- operation runs, updates blocks in world

Players should only be able to have a single job running, possibly they will be able to queue operations

- /tf ops -> show info about the current operation
- operations should happen in the local session, however the queues should be global to the server
- ActionBuilder should be able to handle simple things, but there should be a lower level API for creating batch apply
  ops directly, which should handle application and generate history
- for now axiom would not generate history
- terraform should provide the ability to have an audit log of operations (queue, begin, end times)
- terraform should provide a report of how much data any given player is using.

History should essentially be stored as two batch operations (undo + redo)

Structure:

- Task: A multiphase operation (compute -> apply typically)
- Compute Phase: A task which computes the changes for the next step, not always present (eg for axiom, undo/redo)
    - The compute phase typically will be executed on another thread using a snapshot, though this is not a requirement.
    - COMPUTE SHOULD BE CANCELLABLE, no blocks will have been placed at this point
- Apply Phase: Applies the changes to the world. This should be locking per world, even if the relevant thread pool is
  shared per server.
    - APPLY MAY NOT BE CANCELED, and terraform needs to provide some kind of "await quiescence" method to wait for all
      pending operations to complete before closing or saving.
    - Basically, externally it must be possible to lock applies to an instance to allow a save to occur at a safe point.
      Eg `terraform.requestSafePoint(instance)` or something.
    - Compute may continue during this period.

Want to replace Terraform.init() with a builder which should be called once per server.

- That should result in an instance which is passed around.
- The default options should configure it the following:
    - Globally enabled (eg in all instances)
    - State only ever saved in memory
    - Basically no limits, just sensible stuff like not enormous history, only build in world border, etc

Checkpointing tasks

- This is a future thing for sure, seems complicated
- A task compute phase could mark some point (due to memory, max time, etc) and apply those changes, then resume.
- In this case it would be partially applied and saved to history in parts. History would need to accomodate this and
  have some marker to indicate that the following entries are part of the same task and should be either merged or
  always applied together

FAIRNESS/SCHEDULING
It is possible that I will need compute tasks to be resumable so that I can schedule them more appropriately if they are
taking a very long time. Wouldn't want everyone slowing down because someone queued some dumb ass change.

Task should have the following:

- Unique ID (`a5c8d2`)
- Non-unique tag (`axiom`, `set`, `replace`, `undo`, `redo`)
- Tool info (axiom tool info, set should include some details about the pattern, replace would include pattern + mask)
- State - INIT, QUEUED, COMPUTE, APPLY, COMPLETE, FAILED(?)
- Compute job optionally
- BlockBuffer if compute is done
- Apply job
- Has history? (true for all but undo/redo which do not write history entries)

PERFORMANCE NOTE FOR BLOCK BUFFER
Can have some specialized buffers:

- If an area is provided (eg min and max pos), we can simply use an array for the sections and index them with offsets
  from the min. This should be a huge performance boost
- If an area is provided and it is a very small area, potentially it makes sense to have a single bigger palette (though
  this might have worse iteration perf, need to test).

need to have tests and benchmarks for these.

###### Permissions

Use minecraft-like permissions, but have a permission provider for player sessions which is responsible for checking it.

###### Compat requirements

Axiom:

- Needs to be able to store arbitrary data on the local session
