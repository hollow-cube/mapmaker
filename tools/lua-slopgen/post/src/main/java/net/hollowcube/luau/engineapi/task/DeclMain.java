package net.hollowcube.luau.engineapi.task;

import net.hollowcube.luau.engineapi.emit.GlobalDeclEmitter;
import net.hollowcube.luau.engineapi.emit.LibraryModuleEmitter;
import net.hollowcube.luau.engineapi.emit.ModuleLayout;
import net.hollowcube.luau.engineapi.resolve.Aggregator;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.slopgen.Model;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;

/// CLI entrypoint for the `buildLuauDeclarations` Gradle task. Reads the aggregated engine-api
/// schema and emits a self-contained Luau type bundle into the output directory:
///
///  - `global.d.luau` — all GLOBAL-scope libraries, ambient luau-lsp `declare` form.
///  - `<group>/<name>.luau` — one real, `require`-able module per REQUIRE-scope library, with
///    relative `require`s between modules (see [ModuleLayout]).
///
/// Usage:
/// ```
/// DeclMain --schema path/to/engine-api.json --output path/to/output-dir
/// ```
public final class DeclMain {

    public static void main(String[] args) {
        Path schemaPath = null;
        Path outputDir = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--schema" -> schemaPath = Path.of(args[++i]);
                case "--output" -> outputDir = Path.of(args[++i]);
                default -> {
                    System.err.println("Unknown arg: " + args[i]);
                    System.exit(2);
                }
            }
        }
        if (schemaPath == null || outputDir == null) {
            System.err.println("--schema and --output are both required");
            System.exit(2);
        }

        var schema = Aggregator.readSchema(schemaPath);

        var globals = new ArrayList<Model.Library>();
        var requires = new ArrayList<Model.Library>();
        var exportsByJavaType = new HashMap<String, LibraryModuleEmitter.ExportRef>();
        for (var lib : schema.libraries().values()) {
            (lib.scope() == LuaLibrary.Scope.GLOBAL ? globals : requires).add(lib);
            for (var ex : lib.exports())
                exportsByJavaType.put(ex.javaType().toString(),
                    new LibraryModuleEmitter.ExportRef(lib.moduleName(), ex.luaName()));
        }
        requires.sort(java.util.Comparator.comparing(Model.Library::moduleName));

        int files = 0;
        try {
            Files.createDirectories(outputDir);

            if (!globals.isEmpty()) {
                String text;
                try {
                    text = new GlobalDeclEmitter(exportsByJavaType).emit(globals);
                } catch (GlobalDeclEmitter.UnrepresentableGlobalException e) {
                    System.err.println("error: " + e.getMessage());
                    System.exit(2);
                    return;
                }
                write(outputDir.resolve(ModuleLayout.globalFile()), text);
                files++;
            }

            var libEmitter = new LibraryModuleEmitter(exportsByJavaType);
            for (var lib : requires) {
                Path rel = ModuleLayout.fileFor(lib.moduleName());
                write(outputDir.resolve(rel), libEmitter.emit(lib));
                files++;
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write declarations under " + outputDir, e);
        }

        System.out.println("Wrote " + files + " Luau definition file(s) to " + outputDir
                           + " (" + globals.size() + " global, " + requires.size() + " require)");
    }

    private static void write(Path file, String text) throws IOException {
        if (file.getParent() != null) Files.createDirectories(file.getParent());
        Files.writeString(file, text, StandardCharsets.UTF_8);
    }

    private DeclMain() {
    }
}
