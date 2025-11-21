--local player = require("@mapmaker/env").Player

local task = require("@mapmaker/task")

print("before task")

task.spawn(function(): ()
	local i = 1
	while i < 5 do
		print("hello, world " .. tostring(i))
		i = i + 1
		task.wait(20)
	end
end)

print("after task")







--local function onHitPlayer(): ()
----	print("Another player was hit!!")
--	error("hit the method!!")
--end
--
--player.onHitPlayer:listen(onHitPlayer)
