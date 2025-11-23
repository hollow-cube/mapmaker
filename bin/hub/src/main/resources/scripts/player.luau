local player = require("@mapmaker/env").player
local task = require("@mapmaker/task")

local doBonkEffect = require("./bonk")

local function onHitPlayer(): ()
	task.spawn(doBonkEffect, player, player)
end

player.onHitPlayer:listen(onHitPlayer)
