package net.hollowcube.sqlgen;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/// The generator itself, with no filesystem output and no `System.exit`: SQL in, Java source out,
/// keyed by the path each file belongs at.
///
/// [Main] wraps this to write or diff the result; tests call it directly.
public final class SqlGen {

    public static Map<String, String> generate(Path migrations, Path queries, String packageName, String databaseName) {
        var files = new ArrayList<QueryFile>();
        for (var path : sqlFiles(queries, "queries")) files.add(QueryFileParser.parse(path));

        try (var db = AnalysisDb.boot(migrations)) {
            Catalog catalog;
            try {
                catalog = Catalog.read(db.conn());
            } catch (SQLException e) {
                throw new GenException("failed to read the schema catalog: " + e.getMessage(), e);
            }

            var types = new TypeMap(catalog, packageName);
            var model = new Resolver(db.conn(), catalog, types, packageName).resolve(databaseName, files);
            return new Emitter(model, types).emit();
        }
    }

    static List<Path> sqlFiles(Path dir, String what) {
        if (!Files.isDirectory(dir)) throw new GenException("no " + what + " directory at " + dir);
        try (var files = Files.list(dir)) {
            var paths = files.filter(path -> path.getFileName().toString().endsWith(".sql"))
                .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                .toList();
            if (paths.isEmpty()) throw new GenException("no .sql files in " + dir);
            return paths;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private SqlGen() {
    }
}
