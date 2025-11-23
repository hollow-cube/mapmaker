local player = require("@mapmaker/env").player
local task = require("@mapmaker/task")

local doBonkEffect = require("./bonk")

local function onHitPlayer(other: typeof(player)): ()
	task.spawn(doBonkEffect, player, other)
end

player.onHitPlayer:listen(onHitPlayer)
