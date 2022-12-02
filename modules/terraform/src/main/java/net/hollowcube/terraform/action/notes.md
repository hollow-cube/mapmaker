
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
