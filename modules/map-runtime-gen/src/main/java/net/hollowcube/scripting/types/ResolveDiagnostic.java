package net.hollowcube.scripting.types;

/// One resolver-level error attributed to a specific symbol path. `location` reads as a slash-
/// separated trail (e.g. `@mapmaker/players:Player.find:param[1]`) so a flat list of these in
/// build output is enough for an author to find the offending tag.
public record ResolveDiagnostic(String location, String message) {}
