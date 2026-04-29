package net.hollowcube.luau.slopgen;

import com.google.auto.service.AutoService;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.slopgen.docs.RawLibraryJsonEmitter;
import net.hollowcube.luau.slopgen.emit.AtomResolver;
import net.hollowcube.luau.slopgen.emit.AtomTableEmitter;
import net.hollowcube.luau.slopgen.emit.LibraryEmitter;
import net.hollowcube.luau.slopgen.model.AtomTable;
import net.hollowcube.luau.slopgen.model.LibrarySpec;
import net.hollowcube.luau.slopgen.model.UserDataTagTable;
import net.hollowcube.luau.slopgen.parse.LibraryModelBuilder;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;

/// Per-library glue emitter. Marked **isolating** in `META-INF/gradle/incremental.annotation.processors`
/// — Gradle reruns this only for the `@LuaLibrary` files that actually change. Atom references in
/// the generated source are symbolic (`LuaStringAtoms.A_<name>`), so changing one library's method
/// body does not touch any other generated file.
///
/// Also emits a per-library raw JSON document at `META-INF/luau-slopgen/<fqcn>.json` for the
/// docs-module aggregator to consume.
@AutoService(Processor.class)
public class LuaLibraryProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        var messager = processingEnv.getMessager();
        var elementUtils = processingEnv.getElementUtils();
        var filer = processingEnv.getFiler();

        // Per-library throwaway tables — atom values assigned here are unused; the SymbolicResolver
        // emits names not values. The tags are also throwaway since per-library codegen doesn't
        // need globally-unique tags (the runtime issues real tags from a separate space, but the
        // current codepath bakes a constant per export — that's a pre-existing concern, not split-
        // related).
        var atomTable = new AtomTable();
        var tagTable = new UserDataTagTable();
        var modelBuilder = new LibraryModelBuilder(processingEnv, atomTable, tagTable);
        var emitter = new LibraryEmitter(atomTable, AtomResolver.symbolic(AtomTableEmitter.LUA_STRING_ATOMS));

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
            var libName = luaLibrary.name();
            boolean isGlobal = luaLibrary.scope() == LuaLibrary.Scope.GLOBAL;
            if (isGlobal && !libName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$"))
                messager.printError("Global-scoped LuaLibrary name must be a valid global identifier", annotatedElement);
            if (!isGlobal && !libName.startsWith("@"))
                messager.printError("Require-scoped LuaLibrary name must start with @", annotatedElement);

            var packageName = elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
            var glueQualifiedName = packageName + "." + typeElement.getSimpleName() + "$luau";
            if (elementUtils.getTypeElement(glueQualifiedName) != null) continue;

            var spec = modelBuilder.build(typeElement);
            if (spec == null) continue;

            try {
                emitter.emit(spec, typeElement).writeTo(filer);
            } catch (IOException e) {
                messager.printError("Failed to write generated file for " + annotatedElement.getSimpleName(), annotatedElement);
            }

            writeRawJson(filer, spec, typeElement);
        }

        // Return false so the annotation also reaches LuaAtomTableProcessor.
        return false;
    }

    /// Emits the per-library raw JSON next to the generated Java glue. Path is keyed by the
    /// library's source class FQCN so each library lands in a distinct file in
    /// `META-INF/luau-slopgen/`. Failures are reported as compile errors.
    private void writeRawJson(Filer filer, LibrarySpec spec, TypeElement origin) {
        var resourcePath = "META-INF/luau-slopgen/" + spec.sourceType().toString() + ".json";
        try {
            var resource = filer.createResource(StandardLocation.CLASS_OUTPUT, "", resourcePath, origin);
            try (Writer w = resource.openWriter()) {
                w.write(RawLibraryJsonEmitter.toJson(spec));
            }
        } catch (IOException e) {
            processingEnv.getMessager().printError(
                "Failed to write raw JSON for " + origin.getQualifiedName() + ": " + e.getMessage(),
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
