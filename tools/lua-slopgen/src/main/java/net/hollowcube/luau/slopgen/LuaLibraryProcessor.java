package net.hollowcube.luau.slopgen;

import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.slopgen.emit.AtomTableEmitter;
import net.hollowcube.luau.slopgen.emit.LibraryEmitter;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;

/// Per-library glue emitter. Marked **isolating** in `META-INF/gradle/incremental.annotation.processors`
/// — Gradle reruns this only for the `@LuaLibrary` files that actually change. Atom references in
/// the generated source are symbolic (`LuaStringAtoms.A_<name>`), so changing one library's method
/// body does not touch any other generated file.
///
/// When the `luau.modelOut` processor option is set, emits a per-library JSON fragment to
/// `<luau.modelOut>/<fqcn>.json` (a [Schema] document with a single entry in `libraries`). The
/// fragment lives outside `CLASS_OUTPUT` so it does not ship inside the production jar; the
/// `engine-api` module consumes it via a Gradle artifact variant. Without the option set, only
/// runtime glue is emitted.
@SupportedOptions(LuaLibraryProcessor.MODEL_OUT_OPTION)
public class LuaLibraryProcessor extends AbstractProcessor {

    public static final String MODEL_OUT_OPTION = "luau.modelOut";

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        var messager = processingEnv.getMessager();
        var elementUtils = processingEnv.getElementUtils();
        var filer = processingEnv.getFiler();

        // Per-library throwaway Idents — atom values assigned here are unused; the SymbolicResolver
        // emits names not values. The tags are also throwaway since per-library codegen doesn't
        // need globally-unique tags (the runtime issues real tags from a separate space, but the
        // current codepath bakes a constant per export — that's a pre-existing concern, not split-
        // related).
        var idents = new Idents();
        var modelBuilder = new LibraryModelBuilder(processingEnv, idents);
        var emitter = new LibraryEmitter(idents, AtomTableEmitter.LUA_STRING_ATOMS);

        Path fragmentDir = resolveFragmentDir();

        var sortedElements = new ArrayList<Element>(roundEnv.getElementsAnnotatedWith(LuaLibrary.class));
        sortedElements.sort(Comparator.comparing(e -> {
            if (e instanceof TypeElement t) return t.getQualifiedName().toString();
            return e.getSimpleName().toString();
        }));

        for (var annotatedElement : sortedElements) {
            if (!(annotatedElement instanceof TypeElement typeElement)) continue;

            if (!typeElement.getModifiers().contains(Modifier.FINAL))
                messager.printError("LuaLibrary classes must be final", annotatedElement);

            var luaLibrary = typeElement.getAnnotation(LuaLibrary.class);
            assert luaLibrary != null; // We're processing this annotation, its present.
            var libName = luaLibrary.name();
            boolean isGlobal = luaLibrary.scope() == LuaLibrary.Scope.GLOBAL;
            if (isGlobal && !libName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$"))
                messager.printError("Global-scoped LuaLibrary name must be a valid global identifier", annotatedElement);
            if (!isGlobal && !libName.startsWith("@"))
                messager.printError("Require-scoped LuaLibrary name must start with @", annotatedElement);

            var packageName = elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
            var glueQualifiedName = packageName + "." + typeElement.getSimpleName() + "$luau";
            if (elementUtils.getTypeElement(glueQualifiedName) != null) continue;

            var library = modelBuilder.build(typeElement);
            if (library == null) continue;

            try {
                emitter.emit(library, typeElement).writeTo(filer);
            } catch (IOException e) {
                messager.printError("Failed to write generated file for " + annotatedElement.getSimpleName(), annotatedElement);
            }

            if (fragmentDir != null) writeFragment(fragmentDir, library, typeElement);
        }

        // Return false so the annotation also reaches LuaAtomTableProcessor.
        return false;
    }

    private Path resolveFragmentDir() {
        var raw = processingEnv.getOptions().get(MODEL_OUT_OPTION);
        if (raw == null || raw.isBlank()) return null;
        return Path.of(raw);
    }

    /// Writes the per-library JSON fragment ([Schema] with one library populated) into the
    /// configured output dir as `<fqcn>.json`. Plain file I/O — `Filer` would route the file
    /// into `CLASS_OUTPUT` and bake it into the consumer's jar, which is exactly what this
    /// option exists to avoid.
    private void writeFragment(Path fragmentDir, Model.Library library, TypeElement origin) {
        var fragmentPath = fragmentDir.resolve(library.sourceType().toString() + ".json");
        try {
            Files.createDirectories(fragmentPath.getParent());
            try (var w = new BufferedWriter(Files.newBufferedWriter(fragmentPath, StandardCharsets.UTF_8))) {
                SchemaJson.writeFragment(library, w);
            }
        } catch (IOException e) {
            processingEnv.getMessager().printError(
                "Failed to write JSON fragment for " + origin.getQualifiedName() + ": " + e.getMessage(),
                origin);
        }
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(LuaLibrary.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_25;
    }
}
