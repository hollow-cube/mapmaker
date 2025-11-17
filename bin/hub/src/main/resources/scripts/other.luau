function sayHello()
	print('hello from other!')
end

function explode()
	error("uh oh kaboom")
end

print('log from other.lua, should only happen once if module cache is working')

return {
	sayHello = sayHello,
	explode = explode,
}
