local lib = require("@mapmaker")
type EventSource<T...> = lib.EventSource<T...>

export type Init = {
	position: vector,
	yaw: number?,
	pitch: number?,
}

export type Entity<Self = Entity> = {
	read uuid: string,

	read position: vector,
	read yaw: number,
	read pitch: number,

	read health: number,

	read remove: (self: Self) -> (),
}

return (nil :: any) :: {
	-- general entity lib here
}
