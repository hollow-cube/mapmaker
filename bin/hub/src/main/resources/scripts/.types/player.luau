local lib = require("@mapmaker")
type EventSource<T...> = lib.EventSource<T...>

export type Player = {
	read id: string,
	read name: string,

	read onHitPlayer: EventSource<()>,
}

return (nil :: any) :: {
	-- general player lib here
}
