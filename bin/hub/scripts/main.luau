local players = require("@mapmaker/players")
local task = require("@mapmaker/task")

local doBonkEffect = require("./bonk")

players.on_attack_player:connect(function(attacker, victim)
	local item = attacker:get_slot(Slot.MainHand)
	if not item or item.id ~= "mapmaker:create_maps" then
		return
	end

	task.spawn(doBonkEffect, attacker, victim)
end)
