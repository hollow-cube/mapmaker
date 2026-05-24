package net.hollowcube.scripting.emit;

import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import com.palantir.javapoet.ClassName;
import net.hollowcube.scripting.Idents;
import net.hollowcube.scripting.Model;
import net.hollowcube.scripting.gen.LuaLibrary;
import net.hollowcube.scripting.types.LuauType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// Regression for the "case label is dominated by a preceding case label" failure that surfaced
/// flakily when incremental annotation processing handed `library.exports()` to the emitter in
/// an order other than source-declaration order. The dispatch root's `switch (self)` requires
/// subtypes to precede their supertypes; the emitter must topo-sort independently of input
/// order.
class InheritanceOrderingTest {

    /// Pathological input: three-level chain `Entity → Display → TextDisplay` reversed so the
    /// children come before the parent. The generated `switch (self)` for the `Entity` dispatch
    /// root must list cases in the order `TextDisplay`, `Display` so the deeper subtype's case
    /// isn't dominated by the shallower one.
    @Test
    void reversedInputOrderProducesValidCaseSequence() {
        var library = buildEntityChain(/*reverse=*/true);
        var src = emit(library);
        for (var meta : List.of("entity$index$meta", "entity$newindex$meta", "entity$namecall$meta")) {
            var body = extractMethodBody(src, meta);
            int textDisplayCase = body.indexOf("case LibEntity.TextDisplay");
            int displayCase = body.indexOf("case LibEntity.Display ");
            assertTrue(textDisplayCase >= 0,
                meta + " missing TextDisplay case:\n" + body);
            assertTrue(displayCase >= 0,
                meta + " missing Display case:\n" + body);
            if (textDisplayCase >= displayCase) {
                fail(meta + " has Display before TextDisplay (would dominate). Body:\n" + body);
            }
        }
    }

    /// Generated case order must not depend on input-order-of-exports: for any input ordering
    /// of the same hierarchy, the `switch (self)` case sequence is the same. (Other parts of
    /// the emitter legitimately preserve input order — push methods, dispatch bodies — so we
    /// only assert stability of the case sequence inside the dispatch-root meta methods.)
    @Test
    void caseOrderStableAcrossInputPermutations() {
        var srcA = emit(buildEntityChain(/*reverse=*/false));
        var srcB = emit(buildEntityChain(/*reverse=*/true));
        for (var meta : List.of("entity$index$meta", "entity$newindex$meta", "entity$namecall$meta")) {
            var bodyA = extractMethodBody(srcA, meta);
            var bodyB = extractMethodBody(srcB, meta);
            if (!bodyA.equals(bodyB)) {
                fail("case sequence in " + meta + " differs between source-order and reversed-order inputs.\n"
                     + "--- source-order ---\n" + bodyA + "\n--- reversed ---\n" + bodyB);
            }
        }
    }

    /// End-to-end: feed a realistic `Entity → Display → TextDisplay` library to the emitter and
    /// run the generated source through `javac`. This is the closest reproduction of the
    /// "case label is dominated" error the user hit. If the topo-sort is wrong, javac rejects
    /// the file regardless of how the AP serialised it.
    @Test
    void generatedSourceCompilesUnderJavacForReversedInput() {
        var library = buildEntityChain(/*reverse=*/true);
        var generated = emit(library);
        var compilation = Compiler.javac()
            .withClasspathFrom(getClass().getClassLoader())
            .compile(
                JavaFileObjects.forSourceString("fixtures.LibEntity", LIB_ENTITY_FIXTURE),
                JavaFileObjects.forSourceString("fixtures.LibEntity$luau", generated));
        assertThat(compilation).succeeded();
    }

    /// Sibling subtypes share inheritance depth, so the topo sort must keep them stable in
    /// source order. (Order between siblings doesn't matter for dominance, but a deterministic
    /// output is still a desirable invariant for goldens / diffs.)
    @Test
    void siblingSubtypesPreserveSourceOrder() {
        var entity = export("Entity", null, /*isFinal=*/false, /*hasSubtypes=*/true);
        var dog = export("Dog", entity, /*isFinal=*/true, /*hasSubtypes=*/false);
        var cat = export("Cat", entity, /*isFinal=*/true, /*hasSubtypes=*/false);
        var src = emit(library(List.of(entity, dog, cat)));
        var body = extractMethodBody(src, "entity$index$meta");
        int dogIdx = body.indexOf("case LibEntity.Dog");
        int catIdx = body.indexOf("case LibEntity.Cat");
        assertTrue(dogIdx >= 0 && catIdx >= 0, body);
        assertTrue(dogIdx < catIdx, "Dog case should precede Cat in stable output:\n" + body);
    }

    // ---------- helpers ----------

    private static String emit(Model.Library library) {
        var idents = new Idents();
        for (var ex : library.exports())
            for (var p : ex.properties()) idents.atomFor(p.luaName());
        return new LibraryEmitter(idents, null).emit(library).toString();
    }

    private static Model.Library buildEntityChain(boolean reverse) {
        var entity = export("Entity", null, /*isFinal=*/false, /*hasSubtypes=*/true);
        var display = export("Display", entity, /*isFinal=*/false, /*hasSubtypes=*/true);
        var textDisplay = export("TextDisplay", display, /*isFinal=*/true, /*hasSubtypes=*/false);
        var exports = new ArrayList<Model.Export>(List.of(entity, display, textDisplay));
        if (reverse) java.util.Collections.reverse(exports);
        return library(exports);
    }

    private static Model.Library library(List<Model.Export> exports) {
        return new Model.Library(
            ClassName.get("fixtures", "LibEntity"),
            ClassName.get("fixtures", "LibEntity$luau"),
            "@t/entity",
            LuaLibrary.Scope.REQUIRE,
            List.copyOf(exports),
            List.of(),
            List.of(),
            "");
    }

    private static Model.Export export(String name, Model.Export superExport, boolean isFinal, boolean hasSubtypes) {
        var stringT = new LuauType.Named(null, "string", List.of());
        var enclosing = ClassName.get("fixtures", "LibEntity", name);
        var getter = new Model.Accessor("getName", enclosing, "", null, stringT);
        var prop = new Model.Property("name", getter, null);
        return new Model.Export(
            enclosing,
            name,
            superExport == null ? null : superExport.javaType(),
            isFinal,
            /*generics=*/List.of(),
            List.of(prop),
            List.of(),
            List.of(),
            /*userDataTag=*/1,
            hasSubtypes,
            Model.Export.Kind.STRUCT, List.of(), null,
            "");
    }

    private static String extractMethodBody(String src, String methodName) {
        int idx = src.indexOf(methodName + "(");
        assertTrue(idx >= 0, "method " + methodName + " not found in source:\n" + src);
        int depth = 0;
        boolean started = false;
        var out = new StringBuilder();
        for (int i = idx; i < src.length(); i++) {
            char c = src.charAt(i);
            out.append(c);
            if (c == '{') {
                depth++;
                started = true;
            } else if (c == '}') {
                depth--;
                if (started && depth == 0) return out.toString();
            }
        }
        return out.toString();
    }

    /// Source for the test fixture compiled together with the generated `$luau`. Mirrors the
    /// real `Entity → Display → TextDisplay` chain from `LibEntity.java` minimally — just enough
    /// types and a `getName(LuaState)` method so javac can resolve the dispatch bodies.
    private static final String LIB_ENTITY_FIXTURE = """
        package fixtures;
        import net.hollowcube.luau.LuaState;
        import net.hollowcube.scripting.gen.LuaLibrary;
        import net.hollowcube.scripting.gen.LuaExport;
        import net.hollowcube.scripting.gen.LuaProperty;
        @LuaLibrary(name = "@t/entity")
        public final class LibEntity {
            @LuaExport public static class Entity {
                @LuaProperty public int getName(LuaState s) { s.pushString("e"); return 1; }
            }
            @LuaExport public static class Display extends Entity {
                @Override @LuaProperty public int getName(LuaState s) { s.pushString("d"); return 1; }
            }
            @LuaExport public static final class TextDisplay extends Display {
                @Override @LuaProperty public int getName(LuaState s) { s.pushString("t"); return 1; }
            }
        }
        """;
}
