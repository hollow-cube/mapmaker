
Actions represent a pipeline of modifications to the world, with support automagically for history and whatever else

Use cases:

### Set region to block (//set ...)
```
ActionBuilder
        .from(region)
        .set(block)
```

### Set a region to a pattern (//set ...)
```
ActionBuilder
    .from(region)
    .set(pattern) 
```

### Copy all but air
```
ActionBuilder
    .from(region)
    .mask(Mask.NO_AIR))
    .toClipboard()
```

### Rotation
```
session.action()
    .from(clipboard)
    .rotate(rotation)
    .paste(world, pos)
```

## Assorted notes
- A built action does _not_ execute, it just holds a list of steps (copying when relevant to preserve state) and can be evaluated/executed.
- Actions should create a snapshot of the instance (or affected chunks), then generate the changes in an AbsoluteBlockBatch all in a separate thread.
  - Maybe just big actions for this? not sure.
- Actions should respect world border
- Changes should load chunks as needed, but must unload the chunks if they still do not have any players inside them after the action is complete.
- Would like some api to estimate cost of a job. Could be as simple as "affected blocks * some operation cost"
  - a direct set operation could have a cost of 1, eg `//set air` with a 10x10x10 region would have a cost of 1000

## General notes, nothing to do with actions
- Would like to have some abstract way to draw selections on the client to support WE CUI, debug renderer, particles, etc