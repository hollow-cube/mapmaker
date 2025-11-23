local player = require("@mapmaker/env").player
local task = require("@mapmaker/task")

local doBonkEffect = require("./bonk")

local function onHitPlayer(other: typeof(player)): ()
	print(`Triggering for {player.name}`)
	task.spawn(doBonkEffect, player, other)
end

print(`Registering onHitPlayer for {player.name}`)
player.onHitPlayer:listen(onHitPlayer)
