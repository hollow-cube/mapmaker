local lib = require("@mapmaker")
type EventSource<T...> = lib.EventSource<T...>
local Entity = require("@mapmaker/entity")

export type Player = {
	read uuid: string,
	read name: string,

	read position: vector,
	read yaw: number,
	read pitch: number,

	read world: WorldView,

	read onHitPlayer: EventSource<()>,
}


export type WorldView = {

	read spawnEntity: ((self: WorldView, type: 'text', init: Entity.TextDisplayInit) -> Entity.TextDisplay),

}

return (nil :: any) :: {
	-- general player lib here
}
