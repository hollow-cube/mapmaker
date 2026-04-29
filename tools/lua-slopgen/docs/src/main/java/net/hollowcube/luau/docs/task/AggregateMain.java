package net.hollowcube.luau.docs.task;

import net.hollowcube.luau.docs.compat.CompatFinding;
import net.hollowcube.luau.docs.resolve.ResolveDiagnostic;
import net.hollowcube.luau.docs.resolve.ResolveException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/// CLI entry point used by the Gradle `aggregateLuauApi` task. Reads `META-INF/luau-slopgen/*.json`
/// from a list of jars, validates them through [EngineApiBuild], writes the aggregate JSON, and
/// either runs the compat checker against a lockfile or refreshes the lockfile in `--update-lock`
/// mode.
///
/// Usage:
/// ```
/// AggregateMain --output build/luau-api/engine-api.json
///               --lock engine-api.lock.json
///               [--update-lock]
///               --jars path1.jar path2.jar …
/// ```
public final class AggregateMain {

    public static void main(String[] args) {
        var ctx = parse(args);

        var libs = new ArrayList<Path>();
        libs.addAll(ctx.jars);

        var api = EngineApiBuild.aggregateFromJars(libs);
        EngineApiBuild.writeEngineApi(api, ctx.output);
        System.out.println("Wrote engine API: " + ctx.output);

        if (ctx.updateLock) {
            EngineApiBuild.writeEngineApi(api, ctx.lock);
            System.out.println("Refreshed lockfile: " + ctx.lock);
            return;
        }

        if (ctx.lock == null || !Files.exists(ctx.lock)) {
            System.out.println("No lockfile present at " + ctx.lock + " — skipping compat check");
            return;
        }

        var report = EngineApiBuild.diffAgainstLockfile(api, ctx.lock);
        if (!report.findings().isEmpty()) {
            System.out.println("Compat findings:");
            for (var f : report.findings()) {
                System.out.println("  [" + f.category() + "] " + f.path() + " — " + f.message());
            }
        }
        if (report.hasBreakingChanges()) {
            System.err.println("BREAKING engine API changes detected. Run :tools:lua-slopgen:docs:updateLock if intentional.");
            System.exit(2);
        }
    }

    private static Ctx parse(String[] args) {
        var ctx = new Ctx();
        for (int i = 0; i < args.length; i++) {
            var a = args[i];
            switch (a) {
                case "--output" -> ctx.output = Path.of(args[++i]);
                case "--lock" -> ctx.lock = Path.of(args[++i]);
                case "--update-lock" -> ctx.updateLock = true;
                case "--jars" -> {
                    while (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                        ctx.jars.add(Path.of(args[++i]));
                    }
                }
                default -> {
                    System.err.println("Unknown arg: " + a);
                    System.exit(2);
                }
            }
        }
        if (ctx.output == null) {
            System.err.println("--output is required");
            System.exit(2);
        }
        return ctx;
    }

    private static final class Ctx {
        Path output;
        Path lock;
        boolean updateLock;
        List<Path> jars = new ArrayList<>();
    }

    /// Helper used by tests / programmatic callers — formats a [CompatFinding] like the CLI does.
    public static String formatFinding(CompatFinding f) {
        return "[" + f.category() + "] " + f.path() + " — " + f.message();
    }

    public static String formatDiagnostic(ResolveDiagnostic d) {
        return d.location() + " — " + d.message();
    }

    /// Format a [ResolveException]'s diagnostics for display.
    public static String formatResolveErrors(ResolveException ex) {
        var sb = new StringBuilder("Resolve errors:\n");
        for (var d : ex.diagnostics()) sb.append("  ").append(formatDiagnostic(d)).append('\n');
        return sb.toString();
    }
}
