local player = script.Parent
local world = script.World

local BUTTON_POSITION = vec(0, 41, -5)

player.Sidebar.Enabled = true
player.Sidebar.Title = Text.new("<rainbow>Button Clicker")

function onButtonPress(blockPosition, block)
	if blockPosition ~= BUTTON_POSITION then
		return
	end

	local buttonCount = player.SaveData.buttonCount or 0
	player.SaveData.buttonCount = buttonCount + 1
	print("Pressed", player.SaveData.buttonCount, "times")

	local entity = world:SpawnEntity(BUTTON_POSITION, "text", {
		Text = Text.new("<rainbow>+" .. (buttonCount + 1)),
	})
	task.spawn(function()
		task.wait(10)
		entity:Remove()
	end)
end

player.OnBlockInteract:Listen(onButtonPress)
