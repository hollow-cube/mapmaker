package net.hollowcube.mapmaker.scripting.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.LuaType;
import net.hollowcube.luau.gen.LuaExport;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.gen.LuaMethod;
import net.hollowcube.mapmaker.scripting.util.LuaHelpers;
import net.minestom.server.tag.Tag;

@LuaLibrary(name = "@mapmaker/store")
public final class LibStore {

    @LuaMethod
    public static int defineState(LuaState state) {
        var name = state.checkString(1);
        state.checkType(2, LuaType.TABLE);

        LibStore$luau.pushStateDefinition(state, new StateDefinition(Tag.Transient(name)));
        return 1;
    }

    @LuaExport
    public record StateDefinition(Tag<JsonElement> tag) {

        @LuaMethod
        public int get(LuaState state) {
            var player = LibPlayer.checkPlayerArg(state, 1);

            var data = player.getTag(tag);
            // TODO: handle default value from definition
            if (data == null) data = new JsonObject();

            LuaHelpers.pushJsonElement(state, data);
            return 1;
        }

        // TODO: in the future we should just return a proxy from get which has __index and __newindex to handle get/set, but for now we can just have separate get/set methods
        @LuaMethod
        public void set(LuaState state) {
            var player = LibPlayer.checkPlayerArg(state, 1);
            state.checkType(2, LuaType.TABLE);

            var data = LuaHelpers.toJsonElement(state, 2);

            player.setTag(tag, data);
        }

    }

}
