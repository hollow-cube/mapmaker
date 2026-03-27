declare function vec(x: number, y: number, z: number): vector

declare class Slot end
declare Slot: {
	MAIN_HAND: Slot,
}

export type AnyText = Text | string
declare class Text
	function __len(self): number
	function __concat(self, other: AnyText): Text

	function __eq(self, other: Text): boolean

	function __tostring(self): string
end

declare Text: {
	new: (raw: string) -> Text,

	sanitize: (raw: string) -> string,
}
