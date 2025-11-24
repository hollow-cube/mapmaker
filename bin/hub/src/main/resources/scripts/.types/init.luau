export type EventSource<T...> = {
	listen: (self: EventSource<T...>, callback: (T...) -> ()) -> (),
	wait: (self: EventSource<T...>) -> (T...),
}

return {}
