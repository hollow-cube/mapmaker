local player = require("@mapmaker/env").player
local item = require("@mapmaker/item")

player:setItem(3, item.deprecated_makeItemFromMaterial("minecraft:diamond"))

player:sendMessage(Text.new("<green>Welcome!"))

player.onUseItem:listen(function()
	local item = player:getSlot(Slot.MAIN_HAND)
	if not item or item.id ~= "minecraft:diamond" then
		return
	end

	player:sendMessage(Text.new("<red>You used an item!!!"))

	player.world:setBlock(player.position, "minecraft:diamond_block")

	player:teleport(vec(0, 5, 0), 0, 0, "xyzrw")
end)
