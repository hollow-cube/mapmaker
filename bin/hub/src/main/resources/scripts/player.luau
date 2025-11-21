local player = require("@mapmaker/env").Player

local function onHitPlayer(): ()
--	print("Another player was hit!!")
	error("hit the method!!")
end

player.onHitPlayer:listen(onHitPlayer)

--error("the player is: " .. tostring(player.Name))
