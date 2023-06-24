

TODO introduce a "map type" in map data, unrelated from parkour/building/adventur
Should be: DEFAULT, PERSONAL, ORG

COMMANDS IN HUB

- /map
  - create
    - ~ - create map in first available slot
    - [slot]
      - ~ - create map in given slot
      - [various args todo]
  - list - show info about your slots
  - info
    - ~ - IF IN MAP, show info about current map
    - [id|slot|publishedId] - show info about map
  - alter
    - name [name]
    - item|display|display_item [material]
    - type [parkour|building|adventure]
MISSING  - edit
    - [id|slot|publishedId|personal|pw] - (personal world can also be slot 6)
MISSING  - play
    - [id|publishedId] - play the map
  - delete
    - [id|slot|publishedId] - delete the map
MISSING  - publish
    - [id|slot] - publish the map
MISSING  - copy
    - [id|slot|publishedId] [slot] - copying requires admin permission on map
MISSING  - move
    - [slot] [slot]
MISSING  - trusted - all require admin perm
    - list
    - add [player]
    - remove [player]
    - accept [player]
MISSING  - verify [id|slot] - (requires admin perm on map) - during verification, its test mode only. if someone joins (eg trusted), they are put into a spectator mode. After verification, all are booted to lobby. Editing again requires confirmation FROM AN "ADMIN" OF MAP
MISSING  - legacy
    - list
      - [uuid] - requires elevated permission
    - import
      - [id] [slot] - imports an omega map locally
- /join [player] - joins the map another player is playing

- /accept - accept an invite to join a map

Aliases:
- /create       -> /map create
- /e|enter|play -> /map play

COMMANDS IN PLAYING MAPS

- /checkpoint|cp           - return to last checkpoint
- /hub|leave|l|spawn|lobby - return to hub
- /spectate|spec           - toggle spectator mode
- /reset                   - reset progress
- /test                    - go to test mode
- /fly                     - toggle flight ability
- /invite - invite into your current map
- /report [reason (enum)] - if reason not specified, open report gui
- /list


COMMANDS IN EDITING MAPS

- /fly - toggle flight ability
- /setspawn
- /invite - invite into your current map (requires "editing" perm on map)
- /remove - kick from map (requires admin perm on map)
- /tp - teleport to player in the same map

- /speed
- /flyspeed
- /walkspeed
- /give
- /clearinventory
- /list

COMMANDS IN TESTING MODE
- /build - return to building mode
- /list





ALL COMMANDS NOT FILTERED BELOW

COMMANDS IN HUB

- /map
  - create
    - ~ - create map in first available slot
    - [slot]
      - ~ - create map in given slot
      - [various args todo]
  - list - show info about your slots
  - info
    - ~ - IF IN MAP, show info about current map
    - [id|slot|publishedId] - show info about map
  - alter
    - name [name]
    - item|display|display_item [material]
    - type [parkour|building|adventure]
  - edit
    - [id|slot|publishedId|personal|pw] - (personal world can also be slot 6)
  - play
    - [id|publishedId] - play the map
  - delete
    - [id|slot|publishedId] - delete the map
  - publish
    - [id|slot] - publish the map
  - copy
    - [id|slot|publishedId] [slot] - copying requires admin permission on map
  - move
    - [slot] [slot]
  - download
    - [id|slot|publishedId] [anvil|polar(?)|schem(?)] - downloading requires admin permission on map (for now it will be a discord bot command)
  - trusted - all require admin perm
    - list
    - add [player]
    - remove [player]
  - verify [id|slot] - (requires admin perm on map) - during verification, its test mode only. if someone joins (eg trusted), they are put into a spectator mode. After verification, all are booted to lobby. Editing again requires confirmation FROM AN "ADMIN" OF MAP
  - legacy
    - list
      - [uuid] - requires elevated permission
    - import
      - [id] [slot] - imports an omega map locally
    - export
      - [id] - gives unconverted 1.8 world files (for now it will be a discord bot command)
- /join [player] - joins the map another player is playing

Aliases:
- /create       -> /map create
- /e|enter|play -> /map play
-


- /org
  - map
    - list

COMMANDS IN PLAYING MAPS

- /checkpoint|cp           - return to last checkpoint
- /hub|leave|l|spawn|lobby - return to hub
- /spectate|spec           - toggle spectator mode
- /reset                   - reset progress
- /test                    - go to test mode
- /fly                     - toggle flight ability
- /scp                     - set artificial checkpoint to current pos (only works in test/spectator mode)
- /invite - invite into your current map
- /report [reason (enum)] - if reason not specified, open report gui


COMMANDS IN EDITING MAPS

- /fly - toggle flight ability
- /setspawn
- /invite - invite into your current map (requires "editing" perm on map)
- /remove - kick from map (requires admin perm on map)
- /tp - teleport to player in the same map