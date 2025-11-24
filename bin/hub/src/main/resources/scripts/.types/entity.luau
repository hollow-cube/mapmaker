local lib = require("@mapmaker")
type EventSource<T...> = lib.EventSource<T...>

export type Init = {
	position: vector,
	yaw: number?,
	pitch: number?,
}

export type Entity<Self = Entity> = {
	read uuid: string,

	read position: vector,
	read yaw: number,
	read pitch: number,

	read remove: (self: Self) -> (),
}

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
	text: AnyText?,
}

export type TextDisplay<Self = TextDisplay, InterpolationFields = TextDisplayInterpolationFields> = Display<Self, InterpolationFields> & {
	read text: Text,
	write text: AnyText,
}

return (nil :: any) :: {
	-- general entity lib here
}
