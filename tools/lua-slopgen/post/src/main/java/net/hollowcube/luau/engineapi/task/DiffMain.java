package net.hollowcube.luau.engineapi.task;

import net.hollowcube.luau.engineapi.compat.EngineApiDiff;
import net.hollowcube.luau.engineapi.resolve.Aggregator;

import java.nio.file.Path;

/// CLI entrypoint for the `compareEngineApi` Gradle task. Diffs two `engine-api.json` snapshots
/// and prints findings; exits non-zero when breaking changes are present.
///
/// Usage:
/// ```
/// DiffMain --old path/to/old.json --new path/to/new.json
/// ```
public final class DiffMain {

    public static void main(String[] args) {
        Path oldPath = null;
        Path newPath = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--old" -> oldPath = Path.of(args[++i]);
                case "--new" -> newPath = Path.of(args[++i]);
                default -> {
                    System.err.println("Unknown arg: " + args[i]);
                    System.exit(2);
                }
            }
        }
        if (oldPath == null || newPath == null) {
            System.err.println("--old and --new are both required");
            System.exit(2);
        }

        var oldSchema = Aggregator.readSchema(oldPath);
        var newSchema = Aggregator.readSchema(newPath);
        var report = EngineApiDiff.diff(oldSchema, newSchema);

        if (report.findings().isEmpty()) {
            System.out.println("No differences found.");
            return;
        }
        System.out.println("Compat findings:");
        for (var f : report.findings()) {
            System.out.println("  [" + f.category() + "] " + f.path() + " — " + f.message());
        }
        if (report.hasBreakingChanges()) {
            System.err.println("BREAKING engine API changes detected.");
            System.exit(1);
        }
    }
}
