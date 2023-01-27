# WorldEdit Commands

A comprehensive list of worledit commands which will be supported.
https://worldedit.enginehub.org/en/latest/commands/

Commands are not supported for the following reasons (at least):

- Deprecated
- Unrelated to world building/manipulation
- Does vanilla-like world generation or related
- Uses CraftScript or whatever the expression syntax is 

Statuses are as follows:

- ✅: Supported
- 🚧: Incomplete/in progress
- ⚠️: Partially supported
- ❌: Not supported
- 📝: Dont really want to support, but might for completeness

### GENERAL

- ❌ /we help - Use //help ...
- ❌ /we version - Not WorldEdit (may have some command that shows the terraform version, idk)
- ❌ /we trace - Perf/limits are not configurable via commands
- ❌ /we reload - No config to reload
- ❌ /we cui - CUI will be autodetected (with a setting somewhere to toggle)
- ❌ /we tz - No snapshot system
- ❌ /we report - Either a separate in game report system or discord or something
- ✅ //undo
  - 🚧 //undo [player]
- ✅ //redo
  - 🚧 //redo [player]
- ✅ //clearhistory
  - 🚧 //clearhistory [player]
- ❌ //limit - Perf/limits are not configurable via commands
- ❌ //timeout - Perf/limits are not configurable via commands
- ❌ //perf - Perf/limits are not configurable via commands
- ❌ //reorder - Perf/limits are not configurable via commands
- ❌ //drawsel - CUI will be autodetected (with a setting somewhere to toggle)
- ❌ //world - Cross world operations are not supported
  - caveat: eventually clipboard may be global
- ❌ //watchdog - Watchdog not supported
- 🚧 //gmask
  - caveat: sets globally for a specific player
- ❌ //toggleplace - Will add if someone asks for it
- ❌ //searchitem - Unrelated to world building

### NAVIGATION

❌ Navigation commands may still be present, but not as part of the worldedit compatability module.

### SELECTION

- ✅ //pos1
- ✅ //pos2
- ✅ //hpos1
- ✅ //hpos2
- 🚧 //chunk - sets primary in min corner, secondary in max corner. Thats all.
- 🚧 //wand - Really just an alias for the wand tool
- ❌ //toggleeditwand - Terraform tools are locked to a specific instance of an item, not all items of the type.
- 🚧 //contract
- 🚧 //shift
- 🚧 //outset
- 🚧 //inset
- 🚧 //size
- 🚧 //count
- 🚧 //distr
- 🚧 //sel
- 🚧 //expand

### REGION

- ⚠️ //set
- 🚧 //line
- 🚧 //curve
- 🚧 //replace
- 🚧 //overlay
- 🚧 //center
- 📝 //naturalize
- 🚧 //walls
- 🚧 //faces
- 🚧 //smooth
- 🚧 //move
- 🚧 //stack
- ❌ //regen - World generation not supported
- ❌ //deform - Expression language not supported
- 🚧 //hollow
- ❌ //forest - World generation not supported
- ❌ //flora - World generation not supported

### GENERATION

- 🚧 //hcyl
- 🚧 //cyl
- 🚧 //hsphere
- 🚧 //sphere
- ❌ //forestgen - World generation not supported
- ❌ //pumpkins - World generation not supported
- 🚧 //hpyramid
- 🚧 //pyramid
- ❌ //generate - Expression language not supported
- ❌ //generatebiome - Expression language not supported

### SCHEMATIC

Schematic commands are a bit up in the air. It is unclear how we will handle saving and loading. Eg, do we save 
schematics so people can load them later, or are they locked to the current session? Or maybe both where most people
lost their saved schematics when they leave the world but if you pay for extra worldedit then you get to keep them.

- 🚧 //schem list
- 🚧 //schem formats
- 🚧 //schem load
- 🚧 //schem delete
- 🚧 //schem save
- 🚧 //copy
- 🚧 //cut
- 🚧 //paste
- 🚧 //rotate
- 🚧 //flip
- 🚧 //clearclipboard

### TOOL

- 🚧 //tool stacker
- 🚧 //tool selwand
- ❌ //tool tree - World generation not supported
- 🚧 //tool repl
- 🚧 //tool farwand
- 🚧 //tool none
- ❌ //tool deltree - World generation not supported
- 🚧 //tool lrbuild
- 🚧 //tool floodfill
- 🚧 //tool cycler - WHAT DOES THIS DO??
- ❌ //tool navwand - Unrelated to world building
- 🚧 //tool info
- 🚧 /mask
- 🚧 /material
- 🚧 /range
- 🚧 /size
- 🚧 /tracemask - WHAT DOES THIS DO??

### SUPER PICKAXE

❌ Super pickaxe is not supported in any way. What a dumb feature of WE

### BRUSH

- ❌ //brush forest - World generation not supported
- ❌ //brush butcher - Entities are never present
- 🚧 //brush paint
- 🚧 //brush none
- 🚧 //brush clipboard
- 🚧 //brush gravity
- 🚧 //brush heightmap
- ❌ //brush extinguish - Same comment as snow
- 🚧 //brush sphere
- 🚧 //brush raise
- 🚧 //brush smooth
- 🚧 //brush cylinder
- 🚧 //brush set
- 🚧 //brush apply
- ❌ //brush deform - Expression language not supported
- 🚧 //brush lower
- ❌ //brush snow - World generation not supported
  - Not sure this is really world generation. Not sure where the line is, because pumpkin patches for sure seems dumb but this may be fair enough.
- 🚧 //brush biome

### BIOME

- 🚧 //biomelist
- 🚧 //biomeinfo
- 🚧 //setbiome

### CHUNK

- 🚧 //chunkinfo
- 🚧 //listchunks
- 🚧 //delchunks

### SNAPSHOT

❌ WorldEdit snapshot system is not supported at all. Map maker might provide a similar system in the future.

### SCRIPTING

❌ Expression language is not supported in any way

### UTILITY

- 🚧 //fill
- 🚧 //fillr
- 🚧 //drain
- 🚧 //fixlava
- 🚧 //fixwater
- 🚧 //removeabove
- 🚧 //removebelow
- 🚧 //removenear
- 🚧 //replacenear
- ❌ //snow - Same comment as above
- ❌ //thaw - Same comment as above
- ❌ //green - Same comment as above
- ❌ //extinguish - Same comment as above
- ❌ //butcher - Unrelated to world building
- ❌ //remove - Unrelated to world building
- ❌ //calculate - Unrelated to world building
- 🚧 //help


