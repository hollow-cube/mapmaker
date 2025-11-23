local lib = require("@mapmaker")
type EventSource<T...> = lib.EventSource<T...>
local Entity = require("@mapmaker/entity")

export type SoundCategory = "master" | "music" | "record" | "weather" | "block" | "hostile" | "neutral" | "player" | "ambient" | "voice" | "ui"
export type SoundOptions = { volume: number?, pitch: number?, category: SoundCategory? }

export type Player = {
	read uuid: string,
	read name: string,

	read position: vector,
	read yaw: number,
	read pitch: number,

	read world: WorldView,

	-- Events

	read onHitPlayer: EventSource<(Player)>,

	-- Methods

	read playSound: (self: Player, sound: string, options: SoundOptions?) -> (),
	read playSoundAt: (self: Player, sound: string, position: vector, options: SoundOptions?) -> (),
	-- todo: playSoundFrom(sound: string, source: Entity | Player, options: SoundOptions?)
	read stopSound: (self: Player, sound: string?, category: SoundCategory?) -> (),

}

export type WorldView = {

	-- Methods

	read spawnEntity: ((self: WorldView, type: 'text', init: Entity.TextDisplayInit) -> Entity.TextDisplay),

}

return (nil :: any) :: {
	-- general player lib here
}
