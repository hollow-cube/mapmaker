package fixtures;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.gen.LuaExport;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.gen.LuaMethod;
import net.hollowcube.luau.gen.LuaProperty;

@LuaLibrary(name = "@test/sample")
public final class LibSample {

    @LuaProperty
    public static int getVersion(LuaState state) {
        state.pushInteger(1);
        return 1;
    }

    @LuaExport
    public static class Animal {
        @LuaProperty
        public int getName(LuaState state) {
            state.pushString("anim");
            return 1;
        }
    }

    @LuaExport
    public static final class Dog extends Animal {
        @LuaProperty
        public int getBreed(LuaState state) {
            state.pushString("husky");
            return 1;
        }

        @LuaProperty
        public void setBreed(LuaState state) {
        }

        @LuaMethod
        public void bark(LuaState state) {
        }
    }
}
