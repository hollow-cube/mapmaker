package net.hollowcube.luau.engineapi.resolve;

import java.util.List;

/// Thrown by [Aggregator] when the resolver produces at least one diagnostic. The full list is
/// accessible via [#diagnostics()] so callers can format and print them in build output.
public final class ResolveException extends RuntimeException {

    private final List<ResolveDiagnostic> diagnostics;

    public ResolveException(List<ResolveDiagnostic> diagnostics) {
        super(diagnostics.size() + " unresolved type reference"
              + (diagnostics.size() == 1 ? "" : "s"));
        this.diagnostics = List.copyOf(diagnostics);
    }

    public List<ResolveDiagnostic> diagnostics() {
        return diagnostics;
    }
}
