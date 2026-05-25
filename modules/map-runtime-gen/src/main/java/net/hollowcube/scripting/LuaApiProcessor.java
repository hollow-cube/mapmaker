package net.hollowcube.scripting;

import net.hollowcube.scripting.emit.*;
import net.hollowcube.scripting.gen.LuaLibrary;
import net.hollowcube.scripting.types.CrossModuleResolver;
import net.hollowcube.scripting.types.MetaTypeResolver;
import net.hollowcube.scripting.types.ResolveDiagnostic;
import net.hollowcube.scripting.types.SymbolTable;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/// Single aggregating annotation processor for the Luau scripting API.
///
/// In one round it: builds [Model.Library] for every `@LuaLibrary` source, runs cross-module
/// resolution over the full set, emits per-library `<Lib>$luau.java` glue + the cross-cutting
/// `LuaStringAtoms` / `GeneratedStringAtoms` files via `Filer`, and writes the aggregate
/// `engine-api.json` plus the bundled `.luau` type files to the directory pointed at by the
/// required `-Aluau.outputDir=...` processor option.
///
/// The output dir is required — there's no scenario where running the AP without committing the
/// non-class outputs is meaningful, so no conditional codepath is justified.
@SupportedOptions(LuaApiProcessor.OUTPUT_DIR_OPTION)
public final class LuaApiProcessor extends AbstractProcessor {

    public static final String OUTPUT_DIR_OPTION = "luau.outputDir";

    /// Run-once latch. Aggregating APs are invoked again in later rounds (and in the cleanup
    /// round with `processingOver() == true`); re-processing yields stale `Elements`/`Trees`
    /// whose source positions and `getDocComment` results may be null, producing phantom
    /// validator errors that disappear on rerun. We do all work in the first non-empty round
    /// and become a no-op for the rest.
    private boolean processed = false;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // The cleanup round (`processingOver`) carries no new source — never act in it.
        if (roundEnv.processingOver() || processed) return false;

        var libraryElements = collectLibraryElements(roundEnv);
        var topLevelEnumElements = collectTopLevelEnumElements(roundEnv);
        if (libraryElements.isEmpty() && topLevelEnumElements.isEmpty()) return false;

        // From here on we commit to one full processing pass — even if subsequent rounds
        // expose new annotated elements (which shouldn't happen for slopgen's surface, but
        // be defensive), we leave them alone rather than risk a stale-Element re-validation.
        processed = true;

        var outputDir = requireOutputDir();
        var messager = processingEnv.getMessager();
        var filer = processingEnv.getFiler();
        var elementUtils = processingEnv.getElementUtils();

        var idents = new Idents();
        var modelBuilder = new LibraryModelBuilder(processingEnv, idents);
        var libraries = new LinkedHashMap<TypeElement, Model.Library>();

        for (var typeElement : libraryElements) {
            if (!typeElement.getModifiers().contains(Modifier.FINAL))
                messager.printError("LuaLibrary classes must be final", typeElement);

            var luaLibrary = typeElement.getAnnotation(LuaLibrary.class);
            assert luaLibrary != null;
            var libName = luaLibrary.name();
            boolean isGlobal = luaLibrary.scope() == LuaLibrary.Scope.GLOBAL;
            if (isGlobal && !libName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$"))
                messager.printError(
                    "Global-scoped LuaLibrary name must be a valid global identifier", typeElement);
            if (!isGlobal && !libName.startsWith("@"))
                messager.printError("Require-scoped LuaLibrary name must start with @", typeElement);

            var packageName = elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
            var glueQualifiedName = packageName + "." + typeElement.getSimpleName() + "$luau";
            // Subsequent rounds see the generated glue as a TypeElement — skip so we don't try
            // to re-emit a file we already wrote.
            if (elementUtils.getTypeElement(glueQualifiedName) != null) continue;

            var library = modelBuilder.build(typeElement);
            if (library != null) libraries.put(typeElement, library);
        }

        // ----- Top-level `@LuaEnum` classes: stand-alone, own glue file, ambient declaration. -----
        var topLevelEnums = new LinkedHashMap<TypeElement, Model.EnumDecl>();
        for (var enumElement : topLevelEnumElements) {
            var ann = enumElement.getAnnotation(net.hollowcube.scripting.gen.LuaEnum.class);
            // Spec: a top-level `@LuaEnum` MUST be `scope = GLOBAL` (the default). REQUIRE has
            // no representation today — there's no library to attach it to.
            if (ann != null && ann.scope() != LuaLibrary.Scope.GLOBAL) {
                messager.printError(
                    "Top-level @LuaEnum must use scope = GLOBAL. Place this enum inside an "
                    + "@LuaLibrary to export it as a module field instead.",
                    enumElement);
                continue;
            }

            var packageName = elementUtils.getPackageOf(enumElement).getQualifiedName().toString();
            var glueSimpleName = enumElement.getSimpleName() + "$luau";
            var glueQualifiedName = packageName + "." + glueSimpleName;
            if (elementUtils.getTypeElement(glueQualifiedName) != null) continue;
            var glueType = com.palantir.javapoet.ClassName.get(packageName, glueSimpleName);

            var enumDecl = EnumModelBuilder.build(processingEnv, idents, enumElement, glueType,
                LuaLibrary.Scope.GLOBAL);
            if (enumDecl != null) topLevelEnums.put(enumElement, enumDecl);
        }

        if (libraries.isEmpty() && topLevelEnums.isEmpty()) return false;

        // ----- Cross-module resolution against the full in-memory set. -----
        var symbols = SymbolTable.build(libraries.values());
        var resolveDiagnostics = new ArrayList<ResolveDiagnostic>();
        for (var lib : libraries.values())
            CrossModuleResolver.resolve(lib, symbols, resolveDiagnostics);
        reportDiagnostics(messager, libraries, resolveDiagnostics);

        // ----- Meta-type expansion. After this pass, no `$`-prefixed Named survives. -----
        var metaDiagnostics = new ArrayList<ResolveDiagnostic>();
        var metaResolver = new MetaTypeResolver(symbols, libraries.values(), metaDiagnostics);
        var expanded = new LinkedHashMap<TypeElement, Model.Library>();
        for (var entry : libraries.entrySet())
            expanded.put(entry.getKey(), metaResolver.rewrite(entry.getValue()));
        libraries = expanded;
        reportDiagnostics(messager, libraries, metaDiagnostics);

        // ----- Generated Java glue per library. -----
        var libraryEmitter = new LibraryEmitter(idents, AtomTableEmitter.LUA_STRING_ATOMS);
        for (var entry : libraries.entrySet()) {
            try {
                libraryEmitter.emit(entry.getValue(), entry.getKey()).writeTo(filer);
            } catch (IOException e) {
                messager.printError(
                    "Failed to write generated file: " + e.getMessage(), entry.getKey());
            }
        }

        // ----- Generated Java glue per top-level enum. -----
        var enumEmitter = new EnumEmitter();
        for (var entry : topLevelEnums.entrySet()) {
            try {
                enumEmitter.emit(entry.getValue(), entry.getKey()).writeTo(filer);
            } catch (IOException e) {
                messager.printError(
                    "Failed to write generated enum file: " + e.getMessage(), entry.getKey());
            }
        }

        // ----- Cross-cutting atom tables + API registry. Aggregating contracts. -----
        // Originating set: every library plus every top-level enum. Either alone is enough to
        // trigger emission, so we combine for the originating-elements argument that Gradle
        // uses to track incremental sources.
        var originatingSet = new ArrayList<Element>(libraries.keySet());
        originatingSet.addAll(topLevelEnums.keySet());
        var originating = originatingSet.toArray(new Element[0]);
        var atomEmitter = new AtomTableEmitter();
        try {
            if (elementUtils.getTypeElement(AtomTableEmitter.LUA_STRING_ATOMS.canonicalName()) == null)
                atomEmitter.emitConstants(idents, originating).writeTo(filer);
            if (elementUtils.getTypeElement(AtomTableEmitter.GENERATED_STRING_ATOMS.canonicalName()) == null)
                atomEmitter.emit(idents, originating).writeTo(filer);
            // Single-entrypoint registry — host calls LuaApiRegistry.register(state) once
            // instead of listing every <Lib>$luau / <Enum>$luau by hand.
            if (elementUtils.getTypeElement(ApiRegistryEmitter.LUA_API_REGISTRY.canonicalName()) == null)
                new ApiRegistryEmitter().emit(libraries.values(), topLevelEnums.values(), originating).writeTo(filer);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write atom-table files", e);
        }

        // ----- Aggregate JSON + .luau type bundle to the committed output dir. -----
        writeEngineApiOutputs(outputDir, libraries.values(), topLevelEnums.values());

        return false; // Don't claim @LuaLibrary; nobody else processes it either, but be polite.
    }

    /// Drain a diagnostic list to javac, mapping each entry's severity onto the corresponding
    /// `Diagnostic.Kind`. Diagnostics aren't bound to a specific source element, so each is
    /// reported on the first library — the message itself carries the slash-separated location
    /// trail an author needs to find the tag.
    private static void reportDiagnostics(
        javax.annotation.processing.Messager messager,
        java.util.Map<TypeElement, Model.Library> libraries,
        java.util.List<ResolveDiagnostic> diagnostics
    ) {
        if (diagnostics.isEmpty()) return;
        var anchor = libraries.keySet().iterator().next();
        for (var d : diagnostics) {
            var kind = switch (d.severity()) {
                case ERROR -> Diagnostic.Kind.ERROR;
                case WARNING -> Diagnostic.Kind.WARNING;
            };
            messager.printMessage(kind, d.location() + " — " + d.message(), anchor);
        }
    }

    /// Walk the round's `@LuaLibrary` elements in a deterministic order (qualified name).
    private static java.util.List<TypeElement> collectLibraryElements(RoundEnvironment roundEnv) {
        var elements = new ArrayList<Element>(roundEnv.getElementsAnnotatedWith(LuaLibrary.class));
        elements.sort(Comparator.comparing(e -> {
            if (e instanceof TypeElement t) return t.getQualifiedName().toString();
            return e.getSimpleName().toString();
        }));
        var out = new ArrayList<TypeElement>(elements.size());
        for (var e : elements) if (e instanceof TypeElement t) out.add(t);
        return out;
    }

    /// Top-level `@LuaEnum` types only — enums nested inside a `@LuaLibrary` are picked up by
    /// [LibraryModelBuilder] during library traversal. "Top level" here means the enclosing
    /// element is a `PACKAGE`; an enum nested in a non-library class falls through both paths
    /// and is reported as an error.
    private java.util.List<TypeElement> collectTopLevelEnumElements(RoundEnvironment roundEnv) {
        var out = new ArrayList<TypeElement>();
        var elements = new ArrayList<Element>(roundEnv.getElementsAnnotatedWith(
            net.hollowcube.scripting.gen.LuaEnum.class));
        elements.sort(Comparator.comparing(e -> {
            if (e instanceof TypeElement t) return t.getQualifiedName().toString();
            return e.getSimpleName().toString();
        }));
        for (var e : elements) {
            if (!(e instanceof TypeElement t)) continue;
            var enclosing = t.getEnclosingElement();
            if (enclosing.getKind() == javax.lang.model.element.ElementKind.PACKAGE) {
                out.add(t);
                continue;
            }
            // Nested in another type. If that type is an @LuaLibrary, the library builder
            // handles it — skip silently. Otherwise the placement is unsupported.
            if (enclosing instanceof TypeElement parent
                && parent.getAnnotation(LuaLibrary.class) != null) {
                continue;
            }
            processingEnv.getMessager().printError(
                "@LuaEnum may be a top-level class or nested inside an @LuaLibrary — placing it "
                + "inside a non-library class is not supported", t);
        }
        return out;
    }

    private Path requireOutputDir() {
        var raw = processingEnv.getOptions().get(OUTPUT_DIR_OPTION);
        if (raw == null || raw.isBlank()) {
            throw new IllegalStateException(
                "Required annotation processor option `-A" + OUTPUT_DIR_OPTION
                + "=<path>` is missing. Set it from the consuming Gradle build, e.g. "
                + "`tasks.compileJava { options.compilerArgs.add(\"-A" + OUTPUT_DIR_OPTION
                + "=${" + "projectDir}/luau-api\") }`.");
        }
        return Path.of(raw);
    }

    private void writeEngineApiOutputs(Path outputDir, Iterable<Model.Library> libraries,
                                       Iterable<Model.EnumDecl> topLevelEnums) {
        try {
            Files.createDirectories(outputDir);

            // engine-api.json — pretty-printed for diff readability.
            var byModule = new LinkedHashMap<String, Model.Library>();
            for (var lib : libraries) byModule.put(lib.moduleName(), lib);
            var schema = new Schema(SchemaJson.CURRENT_SCHEMA_VERSION, SchemaJson.KIND, byModule);
            var jsonPath = outputDir.resolve("engine-api.json");
            try (var w = new BufferedWriter(Files.newBufferedWriter(jsonPath, StandardCharsets.UTF_8))) {
                w.write(SchemaJson.toJson(schema));
            }

            // types/global.d.luau + types/<group>/<name>.luau
            var typesDir = outputDir.resolve("types");
            Files.createDirectories(typesDir);

            var globals = new ArrayList<Model.Library>();
            var requires = new ArrayList<Model.Library>();
            var exportsByJavaType = new HashMap<String, LibraryModuleEmitter.ExportRef>();
            for (var lib : libraries) {
                (lib.scope() == LuaLibrary.Scope.GLOBAL ? globals : requires).add(lib);
                for (var ex : lib.exports())
                    exportsByJavaType.put(ex.javaType().toString(),
                        new LibraryModuleEmitter.ExportRef(lib.moduleName(), ex.luaName()));
            }
            requires.sort(Comparator.comparing(Model.Library::moduleName));

            var topLevelEnumList = new ArrayList<Model.EnumDecl>();
            for (var en : topLevelEnums) topLevelEnumList.add(en);

            if (!globals.isEmpty() || !topLevelEnumList.isEmpty()) {
                var text = new GlobalDeclEmitter(exportsByJavaType).emit(globals, topLevelEnumList);
                writeFile(typesDir.resolve(ModuleLayout.globalFile()), text);
            }
            var libModuleEmitter = new LibraryModuleEmitter(exportsByJavaType);
            for (var lib : requires) {
                var rel = ModuleLayout.fileFor(lib.moduleName());
                writeFile(typesDir.resolve(rel), libModuleEmitter.emit(lib));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write engine-api outputs to " + outputDir, e);
        }
    }

    private static void writeFile(Path file, String text) throws IOException {
        if (file.getParent() != null) Files.createDirectories(file.getParent());
        Files.writeString(file, text, StandardCharsets.UTF_8);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        // Top-level `@LuaEnum` doesn't sit under a `@LuaLibrary`, so it has to be a triggering
        // annotation in its own right — otherwise the processor never runs for a file that only
        // contains a standalone enum.
        return Set.of(LuaLibrary.class.getName(), net.hollowcube.scripting.gen.LuaEnum.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_25;
    }
}
