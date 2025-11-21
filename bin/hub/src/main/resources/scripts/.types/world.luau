--[=[
	World type description blah blah blah
]=]
export type World = {

	--[=[
		Number of milliseconds passed since the UNIX epoch.
	]=]
	read numberField: number,

	--[=[
		Spawn an entity blah blah blah

		#### Example usage

		```luau
		local world = require("@mapmaker/world").current

		world:SpawnEntity("my-entity")
		```

		---

		@param entityType The type of entity to spawn
		@return Nothing ever :)
	]=]
	SpawnEntity: (self: World, entityType: string) -> nil,
}


--[=[
	Built-in library for date & time

	### Example usage

	```lua
	local DateTime = require("@lune/datetime")
	```
]=]
return (nil :: any) :: {
	read current: World,
}
