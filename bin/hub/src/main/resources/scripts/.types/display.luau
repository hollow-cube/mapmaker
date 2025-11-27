local lib = require("@mapmaker")
type EventSource<T...> = lib.EventSource<T...>

export type DisplayInit = Init & DisplayInterpolationFields
export type DisplayInterpolationFields = {
	scale: vector?,
}

export type Display<Self = Display, InterpolationFields = DisplayInterpolationFields> = Entity<Self> & {
	scale: vector,

	read interpolate: (self: Self, duration: number, properties: InterpolationFields) -> (),
}

export type TextDisplayInit = DisplayInit & TextDisplayInterpolationFields
export type TextDisplayInterpolationFields = DisplayInterpolationFields & {
	text: string?,
}

export type TextDisplay<Self = TextDisplay, InterpolationFields = TextDisplayInterpolationFields> = Display<Self, InterpolationFields> & {
	text: string,
}

return (nil :: any) :: {
	-- general entity lib here
}
