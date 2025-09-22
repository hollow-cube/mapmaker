## Imports

`require('/path/to/script.luau')` for absolute from module root
`require('path/to/script.luau')` for relative from current script
`require('./path/to/script.luau')` alt for relative

`require('@some/module')` for module imports

`mapmaker`, `hollowcube`, `hc` are reserved.
eg `@mapmaker/gui` maybe for gui related things.

(in theory we could have shared/external modules at some point, not any time soon)

## Script

## Globals

### Runtime global

Globally accessible in all scripts as `runtime`. Returns some info about the current runtime.

#### API

* `Version` - Mapmaker deployment version
* `Build` - Build number (first 6 of commit hash)
* `Size` - Runtime size name (eg `micro`)
* `Age` - Time (in seconds with fraction) since the runtime started.
    * World age can say ticks since world started.
* `CPU` - Object for CPU inspection
    * `TickTime` - Last tick time in seconds with fraction. (maybe should be µs no fraction)
* `Memory` - Object for memory inspection
    * `Used/Max/Free` - Script memory space
        * Probably should be allocated in an arena known to the jvm so we can track it better.
    * `VMUsed/Max/Free` - JVM Memory (including script)

### Map global

Globally accessible in all scripts as `map`.

> Should it be available in scripts not attached to the world/entities?

#### API

* `ID` - Map ID
* `Name` - Map name
* `Owner` - Map owner UUID
* `Size` - Map size name
* `Players` - Player manager
* `World` - Returns root world view (ie not a player view even for a player script)

### Luau Libaries

* Globals: Partially supported -> https://luau.org/library#global-functions
    * Wont allow the following until good reason: `gcinfo`, `get/setfenv`, `newproxy`, `rawget`, `rawset`
* `math`: Fully supported -> https://luau.org/library#math-library
* `table`: Fully supported -> https://luau.org/library#table-library
* `string`: Fully supported -> https://luau.org/library#string-library
* `coroutine`: Not sure, probably partial -> https://luau.org/library#coroutine-library
* `bit32`: Fully supported -> https://luau.org/library#bit32-library
* `utf8`: Fully supported -> https://luau.org/library#utf8-library
* `os`: Fully supported -> https://luau.org/library#os-library
* `debug`: Fully supported -> https://luau.org/library#debug-library
* `buffer`: Fully supported -> https://luau.org/library#buffer-library
* `vector`: Fully supported -> https://luau.org/library#vector-library

### Extra Global Libraries

All of these are globally accessible. Might be worth making some of them imported.
Currently not sure what the distinction is between global libraries and imported ones.

#### `task`

```luau
task.spawn(function()
end) -- returns handle
task.wait(1) -- in ticks
task.cancel(handle)
```

#### `json`

* `json.parse(string): unknown`
* `json.stringify(value, { indent?: integer }?): string`

## Basic Types

* luau primitives (incl. vector & buffer)
* `Quaternion` - quaternion
* `Color` - Any color (named, rgb, etc), may have alpha component (which is sometimes ignored)
* `Text` - Styled text component
    * `AnyText` - type alias for anything that can convert to text, eg Text, string, possibly number/etc
    * `Text.new` parses a minimessage string
    * ? `Text.parseLegacy` to parse legacy color codes (eg `&c`).
    * `Text.sanitize` to sanitize minimessage text (eg from player input)
    * Operators
        * `..` to join (styling inherited probably)
        * `#` to get length
        * `==` to compare
        * `~=` to compare
        * `tostring` to serialize to plain text
    * Instance methods
* `Direction` - `Direction.North`, `.South`, etc
* `Slot` - Special slot constants, eg `Slot.MainHand`, `.Saddle`, etc
    * Could probably be tagged light userdata constants

## Content Types

### `Item`

An item (stack)

* A custom item predefined in module
* A vanilla item?
* A custom item created dynamically?
* What is an air item
    * `nil`? a constant (eg `items.Air`)? `.IsAir` property?
    * Below examples assume nil but not sure.
* Do we differentiate between item type (material) and itemstack (with count)?
* How do you create item instances (factoring in custom items)?

Most likely we treat vanilla and custom items the same,
and wrap the components API to introduce our own in addition
to whichever vanilla components we want to expose.

### `Block`

A block with properties. Note that blocks should never have a nil value. Air is a block and should
be used in any such case to represent a missing block.

* What to do with block entities?
* Custom blocks?

Vanilla blocks acessible via `Block` global, eg `Block.Stone`. Properties
can be set via object constructor, eg `Block.StoneStairs { facing = Direction.West }`.

#### API

* `IsAir` - True for the air variants, false otherwise.
* `[property: string]: string` - Block property indexers

### `Particle`

A single particle & its settings.

* Only vanilla particles for now.

Accessible via `Particle` global, eg `Particle.Flame`. Can be configured
via object constructor, eg `Particle.Dust { color = Color.Red }`.

Different from a ParticleSystem/Spawner we may introduce for fancier effects.

### `Entity`

An entity is any object added to the world, not just visible objects.

For example, you could add a script to the world conditionally by attaching
it to an otherwise empty entity. It wouldn't render to the players at all.

There will be some built-in entities in addition to player spawned ones.

* Item -> ground item
* Text -> text display
* All projectiles?

Entities have:

* Properties?
    * Used to control data on the entity (like some vanilla entity properties)
* AnimationController?
    * Only present for entities with a model (possibly with animation_controller component)

Entities can be described statically for modules:

```json5
{
  // other fields idk, anything that wouldnt be attached to instances of the 
  // entity like spawn conditions if we had them.
  "components": {
    // Script components attach the given script to the entity
    // A new instance of the script will be created/removed with each instance of the entity
    "mapmaker:script": {
      "script": "path/to/script.luau"
    },
    "mapmaker:model": {
      // vanilla would show a vanilla entity at this position
      // Not all vanilla entities may be used notably. 
      // This field is mutually exclusive with the rest below.
      "vanilla": "minecraft:enderman"
    }
  }
}
```

You can spawn an entity with `world:SpawnEntity(position, type, initializer)`.

For example:

```luau
world:SpawnEntity(vec(1, 2, 3), "text", {
	text = "<red>Hello, world!"
})
```

#### API

* `Uuid` - uuid of the entity
* `Remove()` - Removes the entity from the world

#### Built-in Entities

* Item
    * Set item stack
    * Set pick up delay (per player?)
    * Disable pick up entirely

### Player

#### API

* `Uuid` - uuid of the player
* `Name` - username of the player
* `Position` - vector xyz position of the player
* `Yaw`, `Pitch` - rotation of the player
* `Velocity` - vector xyz velocity of the player

Persistence

* `SaveData` - Arbitrary scratch table which is persisted
    * Saved as JSON, so may only contain tables/primitives.
    * Never modified by the server itself
    * Max size of <NOT SURE> bytes.
* `SaveDataSize(): integer` - Computes the size of the current save data, in bytes.

Movement

* `Teleport(position: vector, yaw: number?, pitch: number?, relativeFlags: 'xyzrw'?)`
* `TeleportWithVelocity(...)`
* `SetVelocity(velocity: vector)` - In blocks per second? Or blocks per tick?

Communication

* `SendMessage(message: AnyText)`
* `SendChatPrompt(message: AnyText, options: { [key: string]: AnyText }): string` -> sends the message and gives
  clickable response options in the response.
* `ShowTitle(title: AnyText, subtitle?: AnyText, { fadeIn?: number, stay?: number, fadeOut?: number }?)`
* `ShowActionBar(message: AnyText, duration?: number)`
* `Sidebar` - Sidebar object controls the sidebar
    * `Enabled` - Get or set whether the sidebar is shown
    * `Title` - Get or set the sidebar title
    * `Clear()` - Remove all lines and reset title
    * Not sure about below
        * Should we support scores at all?
        * Or just use fixed number format to show an arbitrary suffix and people can use it for score if they want?
    * `AddLine(line: AnyText, index: integer?)` - Appends a line at the index (or end)
    * `SetLine(line: AnyText, index: integer)` - Updates the line at the given index
    * `RemoveLine(index: integer)` - Removes the line at the given index
* `PlaySound(sound: string, { volume?: number, pitch?: number, category?: string }?)`
* `PlaySoundAt(sound: string, position: vector, { volume?: number, pitch?: number, category?: string }?)`
* `PlaySoundFrom(sound: string, source: Entity | Player, { volume?: number, pitch?: number, category?: string }?)`
* `StopSound(sound: string?, category: string?)`
* <sup>1</sup>`PlayerList` - Player list object controls the player list
    * `Header` - Get or set the header
    * `Footer` - Get or set the footer
    * Possibly also functions to add fake players, choose who to show, etc
* <sup>1</sup>`AddBossBar(...)` - later problem
* <sup>1</sup>`RemoveBossBar(...)` - later problem

<sup>1</sup> Used for branding, so not sure if it should be exposed. Maybe its a paid feature to remove
all HC branding? Or for trusted people? Something else?

Item/Inventory

* `GetSlot(slot: Slot): Item | nil` - gets item in slot, returns nil if the player doesnt have that slot (eg saddle)
* `SetSlot(slot: Slot, item: Item | nil): boolean` - Sets the slot, returns whether the slot changed.
* `GetItem(index: integer): Item | nil` - gets item in player inventory at index. 1-9 is hotbar left to right, 10+ is
  main inventory starting from top left moving right then down
* `SetItem(index: integer, item: Item | nil): boolean` - sets item in slot, returns whether the slot changed
* `AddItem(item: Item, { options }?): (retval)` - adds an item to the inventory
    * Options: allowStacking (stack with same item), allowPartial (stack with same item)
    * Return: Not sure :)
        * Success is simplest but doesnt tell much and loses info if only part of the stack is added
        * Slot would say where, but what if its added to multiple slots
        * Possibly `(success bool, slot, remainder)` and accept losing info for multiple slots? Or make slot change to a
          list if allowed to split multiple times

#### Events

* UseItem - Right click with item
* BlockInteract - Right click on block
* EntityInteract - Right click on entity
* PlayerInteract - Right click on other player (if entities and players are split, otherwise just fold into entity
  interact)
* PickUpItem - Collect an item from the ground

### `World`

#### API

* `Age` - World age, in ticks.
    * See `runtime.Age` for runtime age in wall clock time.

* *insert all the 'communication' functions from Player, but would send to all players*

#### Events

* Tick

## Services/Managers/whatever

### `PlayerManager`

Accessible globally as `map.Players`

#### API

TODO: api to get entities by range/type/nearby/etc

* `PlayerCount -> integer`
* `Players -> { Player }`
* `GetPlayer(uuid) -> Player?`
* `GetPlayerByName(name) -> Player?`
* ? `Kick(uuid | name | player, reason?: Text)`
* ? `Ban(uuid | name | player, reason?: Text)`

#### Events

* PlayerJoined
* PlayerLeft

## Libraries

### `@mapmaker/parkour`?

Exposes access to parkour features, would allow programatically:

* starting/stopping parkour
    * Would add pk hotbar, start timer, etc.
* Resetting (soft and hard reset)
* Finishing
* Applying actions

### `@mapmaker/terraform`

Gives access to terraform operations like setting regions, loading/saving schematics, etc.

### `@mapmaker/http`?

HTTP request library? Probably a lot of issues here
that need to be thought through :|

At a minimum, extremely rate limited.

### `@mapmaker/store`?

If we ever allowed people to sell things in maps for cubits.

# Other Notes

* Should allow people to buy custom map names
    * `/play mycoolgame`
    * Via `mycoolgame.hollowcu.be`
    * Custom domain at extra cost maybe