
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

### Set a region to a pattern with masking (//set ...)
```
ActionBuilder
    .from(region)
    .pushMask(mask)
    .set(pattern) 
    .popMask()
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
    .rotate(Transform.rotate(90, 0, 0))
    .paste(world, pos)
```

### Offset (move)
Not a fan
```
var schem = session.action()
    .from(region, instance)
    .set(Block.AIR)
    .toSchematic()
    
session.action()
    .from(schem)
    .set(Block.AIR)
    .offset(10, 0, 0)
    .paste()

```




## Assorted notes
- A built action does _not_ execute, it just holds a list of steps (copying when relevant to preserve state) and can be evaluated/executed.
- Actions should create a snapshot of the instance (or affected chunks), then generate the changes in an AbsoluteBlockBatch all in a separate thread.
- Actions should respect world border
- Changes should load chunks as needed, but must unload the chunks if they still do not have any players inside them after the action is complete.
- Would like some api to estimate cost of a job. Could be as simple as "affected blocks * some operation cost"
  - a direct set operation could have a cost of 1, eg `//set air` with a 10x10x10 region would have a cost of 1000

## General notes, nothing to do with actions
- Line should be a region type/selection mode
- //sel <type> <params>, eg //sel line 5(width)