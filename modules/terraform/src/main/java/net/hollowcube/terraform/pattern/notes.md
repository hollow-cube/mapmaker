

# Patterns

Requirements for WE patterns:
- x, y, z coordinates
- current block
- Surrounding blocks are required for #blend (can just give the WorldView i guess)

`func getBlock(x, y, z, currentBlock, ...) -> Block`

# Masks

A block matching a mask _can_ be modified, eg /mask stone would allow you to modify only stone blocks

Masks can be AND-ed together, that is to say if both match, the block can be modified

Requirements for WE masks
- current block
- surrounding blocks (just give the WorldView)
- some access to region, either by giving the player or the session (probably the session)

maybe need to have some context in the current execution to set some state, eg for Y gradient in arceon

`func matches(x, y, z, currentBlock, ...) -> boolean`

# Other

Actions should be prompted by the region

eg if you do //set, it iterates over the entire region and calls the pattern for each block.

a mask is applied to each block in the iterator

every action needs a source and a region, and actions modify either the source or the region.
- when reading from a schematic (eg clipboard, history maybe), the source AND region are the schematic.
- when doing a basic replacement, the source is the world, the region is the selection
- a region may need to read from the world to decide what it is.

regions and sources are stacked, some operations push changes to one or both
- a transformation (eg rotate) pushes a new region and a new source (both offset by the transformation)
- a mask pushes a new region (filtered by the mask), but not a new source
