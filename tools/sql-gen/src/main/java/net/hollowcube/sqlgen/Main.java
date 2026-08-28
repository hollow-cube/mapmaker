package net.hollowcube.sqlgen;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/// The generator, as a command: read the SQL, describe it against a Postgres built from the
/// migrations, write the Java.
///
/// `--check` runs the whole thing and compares against what is on disk instead of writing, which is
/// what CI uses to catch generated sources that were not regenerated.
public final class Main {

    public static void main(String[] args) {
        Args parsed;
        try {
            parsed = Args.parse(args);
        } catch (GenException e) {
            System.err.println("sql-gen: " + e.getMessage());
            System.err.println("usage: sql-gen --migrations <dir> --queries <dir> --out <dir> "
                + "--package <name> --name <Database> [--check]");
            System.exit(2);
            return;
        }

        try {
            var generated = SqlGen.generate(parsed.migrations, parsed.queries, parsed.packageName, parsed.databaseName);
            if (parsed.check) {
                check(parsed, generated);
            } else {
                write(parsed, generated);
            }
        } catch (GenException e) {
            System.err.println("sql-gen: " + e.getMessage());
            System.exit(1);
        }
    }

    /// Writes the generated sources, removing any Java left behind by an earlier run. The output
    /// package belongs entirely to the generator, so anything in it that this run did not produce is
    /// stale by definition.
    private static void write(Args args, Map<String, String> generated) {
        try {
            for (var existing : existingFiles(args)) {
                if (!generated.containsKey(existing)) Files.delete(args.out.resolve(existing));
            }
            for (var entry : sorted(generated).entrySet()) {
                var path = args.out.resolve(entry.getKey());
                Files.createDirectories(path.getParent());
                Files.writeString(path, entry.getValue());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        System.out.println("sql-gen: wrote " + generated.size() + " files to " + args.out);
    }

    private static void check(Args args, Map<String, String> generated) {
        var problems = new ArrayList<String>();
        try {
            for (var existing : existingFiles(args)) {
                if (!generated.containsKey(existing)) problems.add("stale: " + existing);
            }
            for (var entry : sorted(generated).entrySet()) {
                var path = args.out.resolve(entry.getKey());
                if (!Files.exists(path)) {
                    problems.add("missing: " + entry.getKey());
                } else if (!Files.readString(path).equals(entry.getValue())) {
                    problems.add("out of date: " + entry.getKey());
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        if (problems.isEmpty()) {
            System.out.println("sql-gen: " + generated.size() + " generated files are up to date");
            return;
        }
        System.err.println("sql-gen: generated sources do not match the SQL; run the sqlGen task");
        for (var problem : problems) System.err.println("  " + problem);
        System.exit(1);
    }

    /// Every `.java` already under the output package, as paths relative to `--out`.
    private static List<String> existingFiles(Args args) throws IOException {
        var root = args.out.resolve(args.packageName.replace('.', '/'));
        if (!Files.isDirectory(root)) return List.of();
        try (var files = Files.walk(root)) {
            return files.filter(path -> path.getFileName().toString().endsWith(".java"))
                .map(path -> args.out.relativize(path).toString())
                .sorted()
                .toList();
        }
    }

    private static Map<String, String> sorted(Map<String, String> files) {
        var out = new LinkedHashMap<String, String>();
        for (var key : new TreeSet<>(files.keySet())) out.put(key, files.get(key));
        return out;
    }

    private record Args(Path migrations, Path queries, Path out, String packageName, String databaseName, boolean check) {

        static Args parse(String[] args) {
            var values = new LinkedHashMap<String, String>();
            boolean check = false;
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("--check")) {
                    check = true;
                    continue;
                }
                if (!args[i].startsWith("--") || i + 1 >= args.length) {
                    throw new GenException("unexpected argument '" + args[i] + "'");
                }
                values.put(args[i].substring(2), args[++i]);
            }
            return new Args(
                Path.of(required(values, "migrations")),
                Path.of(required(values, "queries")),
                Path.of(required(values, "out")),
                required(values, "package"),
                required(values, "name"),
                check);
        }

        private static String required(Map<String, String> values, String name) {
            var value = values.get(name);
            if (value == null) throw new GenException("missing required argument --" + name);
            return value;
        }
    }

    private Main() {
    }
}
