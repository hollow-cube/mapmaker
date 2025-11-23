local Player = require("@mapmaker/player")

return ({} :: any) :: {
	--[=[
		The player object for the currently running script.

		Only available for player-bound scripts, otherwise will throw.
	]=]
	read player: Player.Player,
}