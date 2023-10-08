# Help command generation

WORLDEDIT EXAMPLE

```
------ Help for //replace ------
Replace all blocks in the selection with another
Usage: //replace [from] <to>
Arguments:
  [from] (defaults to none): The mask representing blocks to replace
  <to>: The pattern of blocks to replace with
```

MAP CASES

- Single syntax with maybe optional arguments, like worldedit.

// In the case of /c flip, the syntax is:

- axis opt[clipboard]
- opt[clipboard]

these can be merged into simply opt[axis] opt[clipboard]
TODO CAN I JUST DO THIS IN THE COMMAND?

- if yes, then i can add docs to the args and generate the help command from that
- What if there are multiple distinct syntaxes? like `/map alter 0 name my big long name here` `/map alter 0 size large`


* to mark as required

```
------ Help for /map alter ------
Alter a map's properties
Usage: /map alter <map>
  name <name>: Set the map's name
  size <size>: Set the map's size
Arguments:
  map*: The map to alter
  name*: The new name of the map
Examples:
  /map alter 0 name my big long name here
  /map alter 0 size large
```

Help should be per player, so for map list it should be something like the following

```
NO PERMS:
------ Help for /map list ------
Get info about all of your unpublished maps
Usage: /map list

WITH PERMS
------ Help for /map list ------
Get info about all of your unpublished maps (maybe this should be conditional and change)
Usage: /map list [target]
Arguments:
  target (default: you): The player whose maps to list
Examples:
  /map list
  /map list Ossipago1
```
