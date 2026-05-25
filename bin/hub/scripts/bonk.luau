local task = require('@mapmaker/task')
local task = require("@mapmaker/task")
type Player = Player.Player

function doBonkEffect(player: Player, other: Player)
	local scale = other.deprecated_scale

	player:play_sound("item.toy.squeak", { pitch = math.random() * 0.2 + 0.9 })
	local prop = player.world:spawn_text_prop({
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
	prop.text = Text.new("<red>BONK!")
	prop:interpolate(3, {
		scale = vec(1.5 * scale, 1.5 * scale, 1),
	})

	task.wait(4);
	prop:interpolate(3, {
		scale = vec(1.2 * scale, 1.2 * scale, 1),
	})

	task.wait(3)
	prop:remove()
end

return doBonkEffect
