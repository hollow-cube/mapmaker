package net.hollowcube.scripting.types;

/// One resolver-level diagnostic attributed to a specific symbol path. `location` reads as a
/// slash-separated trail (e.g. `@mapmaker/players:Player.find:param[1]`) so a flat list of
/// these in build output is enough for an author to find the offending tag.
///
/// `severity` controls how [net.hollowcube.scripting.LuaApiProcessor] reports it to javac.
/// Use [Severity#ERROR] for anything that produces incorrect generated output (codegen would
/// emit `lib.X` where no `X` exists, dispatch tables would point at nothing, etc.);
/// [Severity#WARNING] for soft signals — most commonly bare unresolved names that may be
/// legitimate globals declared in `.d.luau` outside slopgen's view.
public record ResolveDiagnostic(Severity severity, String location, String message) {

    public enum Severity { WARNING, ERROR }

    public static ResolveDiagnostic error(String location, String message) {
        return new ResolveDiagnostic(Severity.ERROR, location, message);
    }

    public static ResolveDiagnostic warning(String location, String message) {
        return new ResolveDiagnostic(Severity.WARNING, location, message);
    }
}
