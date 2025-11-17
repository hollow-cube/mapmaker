local map = require("@mapmaker/map")
local World = require("@mapmaker/world")

local world = World.current

print(world.numberField)
-- world.numberField = 123

local entity = world:SpawnEntity("my-entity")


local z: map.MapType = "abc"


local other = require("./other")
other.sayHello()
other.explode()
