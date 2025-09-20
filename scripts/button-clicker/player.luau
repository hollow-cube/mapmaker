local player = script.Parent

local BUTTON_POSITION = vec(0, 41, -5)

function onButtonPress(blockPosition, block)
	if blockPosition ~= BUTTON_POSITION then
		return
	end

	local buttonCount = player.SaveData.buttonCount or 0
	player.SaveData.buttonCount = buttonCount + 1
	print("Pressed", player.SaveData.buttonCount, "times")
end

player.OnBlockInteract:Listen(onButtonPress)
