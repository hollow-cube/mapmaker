# WorldEdit Patterns

* Block: `stone`, `stone_stair[shape=straight]`. Any state not specified uses default.
* Random State: `*stone_stair`. Uses any random states of the block. Cannot specify any properties.
* Pattern list: `stone,stone_stair,dirt`, `30%stone,10%sand`. Format is optional change then % then pattern. If no % is
  specified, it is 1.
* Tags: `##wool`, `##minecraft:wool`. Chooses from the vanilla tag
  Valid to combine with random state, ie `##*stairs` which would use any random stair with any random state.
* Type/State Pattern: `^oak_stairs` would apply all the properties to the new blocks (if they have the property).
  Also it is valid to do `^[waterlogged=false]` which will keep the type but always set waterlogged to false

* "Named": `#name[arg1][arg2][argn]`

Named Functions:

* `#clipboard`