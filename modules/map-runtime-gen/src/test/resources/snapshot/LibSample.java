package fixtures;

import net.hollowcube.luau.LuaState;
import net.hollowcube.scripting.gen.LuaExport;
import net.hollowcube.scripting.gen.LuaLibrary;
import net.hollowcube.scripting.gen.LuaMethod;
import net.hollowcube.scripting.gen.LuaProperty;

/// A fixture library with a static getter, a base export, and a final subtype.
@LuaLibrary(name = "@test/sample")
public final class LibSample {

    /// The current sample API version.
    /// @luaReturn number
    @LuaProperty
    public static int getVersion(LuaState state) {
        state.pushInteger(1);
        return 1;
    }

    /// A non-final base export representing any animal.
    @LuaExport
    public static class Animal {
        /// The animal's display name.
        /// @luaReturn string
        @LuaProperty
        public int getName(LuaState state) {
            state.pushString("anim");
            return 1;
        }
    }

    /// A specific kind of animal.
    @LuaExport
    public static final class Dog extends Animal {
        /// @luaReturn string
        @LuaProperty
        public int getBreed(LuaState state) {
            state.pushString("husky");
            return 1;
        }

        /// @luaParam value string
        @LuaProperty
        public void setBreed(LuaState state) {
        }

        /// Make the dog bark loudly.
        @LuaMethod
        public void bark(LuaState state) {
        }
    }
}
