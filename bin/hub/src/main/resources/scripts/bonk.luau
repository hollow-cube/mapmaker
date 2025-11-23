--!strict

local task = require("@mapmaker/task")
local Player = require("@mapmaker/player")

local BONK_SOUND = "item.toy.squeak"

function doBonkEffect(player: Player.Player, other: Player.Player)
	local scale = 1 -- todo get other scale

	player:playSound(BONK_SOUND, { pitch = math.random() * 0.2 + 0.9 })
	local entity = player.world:spawnEntity("text", {
		position = other.position + vec(
			math.random(-1, 1),
			math.random(2, 2.5),
			math.random(-1, 1)
		) * scale,
		scale = vec(.75 * scale, .75 * scale, 1),
		billboard = "center",
		shadow = true,
		background = 0,
	})

	task.wait(3)
	entity.text = Text.new("<red>BONK!")
	entity:interpolate(3, {
		scale = vec(1.5 * scale, 1.5 * scale, 1),
	})

	task.wait(4);
	entity:interpolate(3, {
		scale = vec(1.2 * scale, 1.2 * scale, 1),
	})

	task.wait(3)
	entity:remove()
end

return doBonkEffect
