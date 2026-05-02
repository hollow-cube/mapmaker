package net.hollowcube.luau.engineapi.compat;

/// One finding from comparing an old engine API to a new one. `path` is a slash-separated
/// trail like `@mapmaker/players:Player.find:return[0]` so build output can be scanned for
/// the right place to look.
public record CompatFinding(DiffCategory category, String path, String message) {}
