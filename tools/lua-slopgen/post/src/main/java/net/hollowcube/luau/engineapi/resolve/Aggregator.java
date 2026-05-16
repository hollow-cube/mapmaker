package net.hollowcube.luau.engineapi.resolve;

import net.hollowcube.luau.engineapi.emit.EditorJson;
import net.hollowcube.luau.slopgen.Model;
import net.hollowcube.luau.slopgen.Schema;
import net.hollowcube.luau.slopgen.SchemaJson;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Stream;

/// Reads per-library [Schema] fragments produced by the annotation processor (one file per
/// `@LuaLibrary`, each being a [Schema] with one entry in `libraries`), merges them into a
/// single aggregate [Schema], and runs cross-module reference resolution against it.
///
/// Throws [ResolveException] when any reference is unresolved.
public final class Aggregator {

    private Aggregator() {
    }

    public record Result(Schema schema, List<ResolveDiagnostic> diagnostics) {}

    /// Aggregate from a list of fragment directories. Each directory should contain
    /// `<fqcn>.json` files written by [net.hollowcube.luau.slopgen.LuaLibraryProcessor] when its
    /// `luau.modelOut` option is set. Returns the schema together with any cross-module resolve
    /// diagnostics; the caller decides whether they're fatal.
    public static Result aggregateFromDirs(List<Path> fragmentDirs) {
        var libs = new ArrayList<Model.Library>();
        for (var dir : fragmentDirs) {
            if (!Files.isDirectory(dir)) continue;
            try (Stream<Path> walk = Files.walk(dir)) {
                walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .forEach(p -> libs.addAll(readFragment(p).libraries().values()));
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to scan fragment dir: " + dir, e);
            }
        }
        return aggregate(libs);
    }

    /// Aggregate a list of [Model.Library] entries into a single [Schema] and run cross-module
    /// resolution. Diagnostics are returned in [Result#diagnostics()]; the schema is always
    /// produced (with whatever made it through).
    public static Result aggregate(List<Model.Library> libraries) {
        var symbols = SymbolTable.build(libraries);
        var diagnostics = new ArrayList<ResolveDiagnostic>();
        for (var lib : libraries) CrossModuleResolver.resolve(lib, symbols, diagnostics);
        var byModule = new LinkedHashMap<String, Model.Library>();
        for (var lib : libraries) byModule.put(lib.moduleName(), lib);
        var schema = new Schema(SchemaJson.CURRENT_SCHEMA_VERSION, SchemaJson.KIND, byModule);
        return new Result(schema, List.copyOf(diagnostics));
    }

    public static Schema readFragment(Path path) {
        try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return SchemaJson.read(r);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read fragment: " + path, e);
        }
    }

    public static void writeSchema(Schema schema, Path output) {
        try {
            if (output.getParent() != null) Files.createDirectories(output.getParent());
            Files.writeString(output, SchemaJson.toJson(schema), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write schema: " + output, e);
        }
    }

    /// Write the editor document — the slim, Java-free, minified editor JSON with the generated
    /// global + per-module Luau type text embedded under `types`. See [EditorJson].
    public static void writeEditorSchema(Schema schema, Path output) {
        try {
            if (output.getParent() != null) Files.createDirectories(output.getParent());
            Files.writeString(output, EditorJson.build(schema), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write editor schema: " + output, e);
        }
    }

    public static Schema readSchema(Path path) {
        try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return SchemaJson.read(r);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read schema: " + path, e);
        }
    }
}
