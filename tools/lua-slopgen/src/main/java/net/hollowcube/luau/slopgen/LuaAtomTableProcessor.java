package net.hollowcube.luau.slopgen;

import com.google.auto.service.AutoService;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.slopgen.emit.AtomTableEmitter;
import net.hollowcube.luau.slopgen.model.AtomTable;
import net.hollowcube.luau.slopgen.model.UserDataTagTable;
import net.hollowcube.luau.slopgen.parse.LibraryModelBuilder;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;

/// Cross-cutting atom-table emitter. Marked **aggregating** in
/// `META-INF/gradle/incremental.annotation.processors` — Gradle reruns it whenever any `@LuaLibrary`
/// changes, since adding/removing a property may shift the global atom set.
///
/// Emits two files into `net.hollowcube.luau.gen.runtime`:
///
/// - `LuaStringAtoms.java` — `public static final short A_<name> = N;` constants. Compile-time
///   constants (constant variables per JLS 4.12.4), valid as `case` labels in generated glue.
/// - `GeneratedStringAtoms.java` — runtime callback registration consumed by the Luau VM.
@AutoService(Processor.class)
public class LuaAtomTableProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        var elementUtils = processingEnv.getElementUtils();
        var filer = processingEnv.getFiler();

        var atomTable = new AtomTable();
        var tagTable = new UserDataTagTable();
        var modelBuilder = new LibraryModelBuilder(processingEnv, atomTable, tagTable);

        var sortedElements = new ArrayList<Element>(roundEnv.getElementsAnnotatedWith(LuaLibrary.class));
        sortedElements.sort(Comparator.comparing(e -> {
            if (e instanceof TypeElement t) return t.getQualifiedName().toString();
            return e.getSimpleName().toString();
        }));

        var originating = new ArrayList<Element>();
        for (var annotatedElement : sortedElements) {
            if (annotatedElement instanceof TypeElement typeElement) {
                modelBuilder.build(typeElement); // discard model — we only need to populate atomTable
                originating.add(typeElement);
            }
        }

        if (atomTable.isEmpty()) return false;

        var emitter = new AtomTableEmitter();
        var originatingArr = originating.toArray(new Element[0]);
        try {
            if (elementUtils.getTypeElement(AtomTableEmitter.LUA_STRING_ATOMS.canonicalName()) == null)
                emitter.emitConstants(atomTable, originatingArr).writeTo(filer);
            if (elementUtils.getTypeElement(AtomTableEmitter.GENERATED_STRING_ATOMS.canonicalName()) == null)
                emitter.emit(atomTable, originatingArr).writeTo(filer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return false;
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
