package net.hollowcube.luau.slopgen;

import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import org.junit.jupiter.api.Test;

import static com.google.testing.compile.CompilationSubject.assertThat;

/// Compiles representative `@LuaLibrary` inputs end-to-end with the real production processors
/// ([LuaLibraryProcessor] + [LuaAtomTableProcessor]). The point is to fail loudly if generated
/// source has a syntax issue or a stale reference to the runtime API — the per-library
/// [emit.LibraryEmitterTest] checks shape, but only this suite proves the output links against
/// a real `luau.core` jar.
class EndToEndTest {

    @Test
    void inheritanceChainCompiles() {
        var compilation = Compiler.javac()
            .withProcessors(new LuaLibraryProcessor(), new LuaAtomTableProcessor())
            .compile(JavaFileObjects.forSourceString("fixtures.LibE2E", """
                package fixtures;
                import net.hollowcube.luau.LuaState;
                import net.hollowcube.luau.gen.LuaExport;
                import net.hollowcube.luau.gen.LuaLibrary;
                import net.hollowcube.luau.gen.LuaMethod;
                import net.hollowcube.luau.gen.LuaProperty;
                @LuaLibrary(name = "@e2e/inherit")
                public final class LibE2E {
                    @LuaExport public static class Animal {
                        @LuaProperty public int getName(LuaState s) { s.pushString("a"); return 1; }
                    }
                    @LuaExport public static class Dog extends Animal {
                        @LuaProperty public int getBreed(LuaState s) { s.pushString("d"); return 1; }
                        @LuaProperty public void setBreed(LuaState s) {}
                        @LuaMethod public void bark(LuaState s) {}
                    }
                    @LuaExport public static final class Puppy extends Dog {
                        @LuaMethod public int age(LuaState s) { s.pushInteger(1); return 1; }
                    }
                }
                """));
        assertThat(compilation).succeededWithoutWarnings();
    }

    @Test
    void allMetamethodKindsCompile() {
        var compilation = Compiler.javac()
            .withProcessors(new LuaLibraryProcessor(), new LuaAtomTableProcessor())
            .compile(JavaFileObjects.forSourceString("fixtures.LibAllMeta", """
                package fixtures;
                import net.hollowcube.luau.LuaState;
                import net.hollowcube.luau.gen.LuaExport;
                import net.hollowcube.luau.gen.LuaLibrary;
                import net.hollowcube.luau.gen.LuaMethod;
                import net.hollowcube.luau.gen.Meta;
                @LuaLibrary(name = "@e2e/meta")
                public final class LibAllMeta {
                    @LuaExport public static final class Vec {
                        @LuaMethod(meta = Meta.ADD)      public int add(LuaState s) { return 1; }
                        @LuaMethod(meta = Meta.SUB)      public int sub(LuaState s) { return 1; }
                        @LuaMethod(meta = Meta.UNM)      public int unm(LuaState s) { return 1; }
                        @LuaMethod(meta = Meta.EQ)       public int eq(LuaState s) { return 1; }
                        @LuaMethod(meta = Meta.LT)       public int lt(LuaState s) { return 1; }
                        @LuaMethod(meta = Meta.LEN)      public int len(LuaState s) { return 1; }
                        @LuaMethod(meta = Meta.TOSTRING) public int str(LuaState s) { return 1; }
                        @LuaMethod(meta = Meta.CALL)     public int call(LuaState s) { return 1; }
                    }
                }
                """));
        assertThat(compilation).succeededWithoutWarnings();
    }

    @Test
    void mixedStaticsAndExportCompiles() {
        var compilation = Compiler.javac()
            .withProcessors(new LuaLibraryProcessor(), new LuaAtomTableProcessor())
            .compile(JavaFileObjects.forSourceString("fixtures.LibMix", """
                package fixtures;
                import net.hollowcube.luau.LuaState;
                import net.hollowcube.luau.gen.LuaExport;
                import net.hollowcube.luau.gen.LuaLibrary;
                import net.hollowcube.luau.gen.LuaMethod;
                import net.hollowcube.luau.gen.LuaProperty;
                @LuaLibrary(name = "@e2e/mix")
                public final class LibMix {
                    @LuaProperty public static int getVersion(LuaState s) { s.pushInteger(1); return 1; }
                    @LuaMethod public static int build(LuaState s) { s.pushInteger(1); return 1; }
                    @LuaExport public static final class Thing {
                        @LuaProperty public int getX(LuaState s) { return 1; }
                        @LuaProperty public void setX(LuaState s) {}
                        @LuaMethod public void touch(LuaState s) {}
                    }
                }
                """));
        assertThat(compilation).succeededWithoutWarnings();
    }
}
