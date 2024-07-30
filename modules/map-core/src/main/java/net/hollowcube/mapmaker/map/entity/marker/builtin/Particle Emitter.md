# Particle Emitter

Particle Emitters are special marker types which emit particles in the world.

To create a new marker, use the `/addmarker` command, which will spawn a new marker at your location. You can then move
and edit the marker properties using the Axiom marker editor. To turn the marker into a particle emitter, set the type
to `mapmaker:particle_emitter`.

> Note that /addmarker is currently behind a feature flag, send the map id you want to use it to seth or matt and we
> can enable it for your map.

## Properties

> Note that due to a limitation of the NBT format, if one component of a vector (eg position) is a script, all must
> be scripts. It is valid to simply quote a number to make it a script. e.g. `"12"` is a valid script. It would not
> be valid to write something like `position: ["math.random()", 12, 12]`.

* `particle`: The minecraft particle type. The available types can be found
  here: https://minecraft.wiki/w/Particles_(Java_Edition)
* `lifetime (number)`: The total lifetime of the emitter, in ticks (it will loop).
* `rate (number)`: The number of particles to emit per tick, can be fractional.
* `position (3 numbers or scripts)`: A position offset from the marker position to spawn the particle.
* `velocity (3 numbers or scripts)`: The initial velocity of the particle. Mutually exclusive with `speed`, `count`,
  and `offset`.
* `speed (number or script)`: The speed of the particles. Mutually exclusive with `velocity`.
* `count (number or script)`: The number of particles to spawn per tick. Mutually exclusive with `velocity`.
* `offset (3 numbers or scripts)`: The offset of the particles from the marker position. Mutually exclusive
  with `velocity`.
* `active (number or script)`: If unset or 1, the emitter will emit particles. Otherwise it will not.

### Velocity vs Speed/Count/Offset

This is a bit of a weird behavior of the Java Edition particle system, but oh well we work with what we get. There are
two modes you can spawn particles, either a single particle in a fixed position with the given velocity, or multiple
particles in a "cloud", with a random direction. The `velocity` property is used for the former, and `speed`, `count`,
and `offset` are used for the latter.

Note however that each behavior is not necessary supported on a per-particle basis. For example, the `minecraft:dust`
particle does not support a velocity, and will ignore it if set.

### Particle Data

Some particles have extra data which can be set, those are documented below.

* `minecraft:dust`
    * `color (3 numbers or scripts)`: The RGB color of the particle. Each component is a float from 0-1.
    * `scale (number or script)`: The scale of the particle. This also affects the lifetime of the individial particle.
      The bigger the scale, the longer it will last (with some randomness included by the client).
* `minecraft:dust_color_transition`
    * `color (3 numbers or scripts)`: The initial RGB color of the particle. Each component is a float from 0-1.
    * `transition (3 numbers or scripts)`: The final RGB color of the particle. Each component is a float from 0-1.
    * `scale (number or script)`: The scale of the particle. This also affects the lifetime of the individial particle.
      The bigger the scale, the longer it will last (with some randomness included by the client).
* `minecraft:block`, `minecraft:block_marker`, `minecraft:dust_pillar`, `minecraft:falling_dust`
    * `block (string)`: The block state to use for the particle in the form `minecraft:stone_stairs[facing=north]` (can
      omit `[]` if empty), although the particular state is often not relevant.
* `minecraft:item`
    * `item (string)`: An item name, eg `minecraft:apple`.

## MoLang Environment

Expressions are written in MoLang. In addition to simple math, there are two environments provided for scripts,
documented below.

MoLang docs can be found here:
https://learn.microsoft.com/en-us/minecraft/creator/reference/content/molangreference/examples/molangconcepts/molangintroduction?view=minecraft-bedrock-stable

### Variable (`variable` or `v`)

| Property | Type           | Description                                                      |
|----------|----------------|------------------------------------------------------------------|
| age      | number (ticks) | The current age of the emitter                                   |
| lifetime | number (ticks) | The total lifetime of the emitter, or 0 for non-looping emitters |
| random_1 | number (0-1)   | A consistent random number for this loop of the emitter          |
| random_2 | number (0-1)   | A consistent random number for this loop of the emitter          |
| random_3 | number (0-1)   | A consistent random number for this loop of the emitter          |
| random_4 | number (0-1)   | A consistent random number for this loop of the emitter          |

### Query (`query` or `q`)

The query object is generally utility functions, it will probably be expanded in the future.

| Property     | Type                                 | Description                                                                             |
|--------------|--------------------------------------|-----------------------------------------------------------------------------------------|
| hsb_to_red   | (number, number, number) -> (number) | Converts an HSB color (all components 0-1) to the red component of an RGB color (0-1)   |
| hsb_to_green | (number, number, number) -> (number) | Converts an HSB color (all components 0-1) to the green component of an RGB color (0-1) |
| hsb_to_blue  | (number, number, number) -> (number) | Converts an HSB color (all components 0-1) to the blue component of an RGB color (0-1)  |

## Example

A full example emitter which creates a repeating half circle rainbow looks like the following:

```
{
    type: "mapmaker:particle_emitter",
    particle: "minecraft:dust",
    lifetime: 40,
    rate: 10,
    position: [
        "-math.cos((v.age/20)*90)*(2.5-m.random(0,1))",
        "math.sin((v.age/20)*90)*(2.5-m.random(0,1))",
        "0"
    ],
    velocity: [
        "0",
        "0.05",
        "0"
    ],
    color: [
        "q.hsb_to_red(v.age / 40, 1, 1)",
        "q.hsb_to_green(v.age / 40, 1, 1)",
        "q.hsb_to_blue(v.age / 40, 1, 1)"
    ]
}
```