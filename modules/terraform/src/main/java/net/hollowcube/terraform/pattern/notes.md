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

## Mask "scripting"

I THINK I WILL SWITCH THIS TO USE | AND & AS EXPECTED

`/` can be used to OR masks together
`|` is a pipe operator, eg takes the input from left and feeds it to right (aka AND)
`!` is a not operator, eg inverts the following mask
`()` can be used to group masks
space is allowed but not required, and it is ignored.

Basic masks:

- Single block: `stone`, `minecraft:stone`, `stone_stairs[facing=east]`
    - block states are fuzzy matched, eg they do not care about unspecified states.
      If the given state does not exist, the mask will match nothing.
- Any matching state: TODO
- Random noise: `%50`
- Over/underlay: `>`, `<`
- named: `#name[args]`
    - `#existing`
    - `#solid`
    - `#surface`
    - Arguments can be either given in order or by name, eg `#arcangle[0,90,10]`
      or `#arcangle[minAngle=0,maxAngle=90,range=10]`
        - Named and ordered may not be mixed
        - In both cases it is valid to leave out optional args

Examples:

- `stone/grass_block` - matches stone or grass
- `!stone` - matches anything but stone
- `!(stone/grass_block)` - matches anything but stone or grass
    - May change the precedence here so that `!` is higher than `/`, meaning `!stone/grass_block` would match anything
      but stone or grass
- `%50|>grass_block` - matches 50% of blocks above grass

Parsing notes:

- LHS can be...
    - a prefix operator (!, >, <)
    - a block (with state)
    - a named mask (with args)
    - %50 is syntax sugar for #rand(.5)
- Infix
    - `/` or `|`

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
