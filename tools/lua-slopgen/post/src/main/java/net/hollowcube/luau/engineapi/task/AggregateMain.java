package net.hollowcube.luau.engineapi.task;

import net.hollowcube.luau.engineapi.resolve.Aggregator;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/// CLI entrypoint for the `aggregateEngineApi` Gradle task. Reads per-library [Schema] fragments
/// from one or more directories, validates cross-module references, and writes a single
/// aggregate `engine-api.json`.
///
/// With `--editor-output` it additionally writes the editor variant: the same Luau surface,
/// stripped of all Java/codegen detail and minified, for shipping to the editor app.
///
/// Usage:
/// ```
/// AggregateMain --output build/luau-api/engine-api.json
///               [--editor-output build/luau-api/engine-api.editor.json]
///               [--strict]
///               [--fragments dir1 dir2 …]
/// ```
///
/// `--strict` fails the build on any cross-module resolve diagnostic. Default behavior prints
/// diagnostics as warnings and writes the schema regardless — this matches the in-flight
/// `LuaDocsValidator.STRICT=false` migration posture in the annotation processor.
public final class AggregateMain {

    public static void main(String[] args) {
        var ctx = parse(args);
        var result = Aggregator.aggregateFromDirs(ctx.fragmentDirs);
        Aggregator.writeSchema(result.schema(), ctx.output);
        System.out.println("Wrote engine API: " + ctx.output);

        if (ctx.editorOutput != null) {
            try {
                Aggregator.writeEditorSchema(result.schema(), ctx.editorOutput);
            } catch (net.hollowcube.luau.engineapi.emit.GlobalDeclEmitter.UnrepresentableGlobalException e) {
                System.err.println("error: " + e.getMessage());
                System.exit(2);
            }
            System.out.println("Wrote editor engine API: " + ctx.editorOutput);
        }

        if (!result.diagnostics().isEmpty()) {
            var stream = ctx.strict ? System.err : System.out;
            stream.println((ctx.strict ? "Resolve errors" : "Resolve warnings") + ":");
            for (var d : result.diagnostics()) {
                stream.println("  " + d.location() + " — " + d.message());
            }
            if (ctx.strict) System.exit(2);
        }
    }

    private static Ctx parse(String[] args) {
        var ctx = new Ctx();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--output" -> ctx.output = Path.of(args[++i]);
                case "--editor-output" -> ctx.editorOutput = Path.of(args[++i]);
                case "--strict" -> ctx.strict = true;
                case "--fragments" -> {
                    while (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                        ctx.fragmentDirs.add(Path.of(args[++i]));
                    }
                }
                default -> {
                    System.err.println("Unknown arg: " + args[i]);
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
        Path editorOutput;
        boolean strict;
        List<Path> fragmentDirs = new ArrayList<>();
    }
}
