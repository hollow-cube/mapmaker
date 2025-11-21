local task = {}

function task.spawn<T...>(functionOrThread: thread | (T...) -> ...any, ...: T...): thread
	return nil :: any
end

function task.defer<T...>(functionOrThread: thread | (T...) -> ...any, ...: T...): thread
	return nil :: any
end

function task.delay<T...>(ticks: number, functionOrThread: thread | (T...) -> ...any, ...: T...): thread
	return nil :: any
end

function task.wait(ticks: number?): number
	return nil :: any
end

function task.cancel(thread: thread)
end

return task
