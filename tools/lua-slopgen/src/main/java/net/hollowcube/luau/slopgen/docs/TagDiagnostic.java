package net.hollowcube.luau.slopgen.docs;

/// Carries a malformed-tag complaint produced by the [JavadocTagParser]. The validator surfaces
/// these via the processor `Messager` so they appear inline in the IDE.
public record TagDiagnostic(String message) {}
