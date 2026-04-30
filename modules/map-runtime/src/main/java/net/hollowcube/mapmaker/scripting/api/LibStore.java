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

/// Per-player state storage.
///
/// ```luau
/// local store = require("@mapmaker/store")
/// local stats = store.define_state("stats", { wins = 0, kills = 0 })
///
/// players.on_join:listen(function(p)
///     local data = stats:get(p)
///     p:send_message("welcome back, you have " .. data.wins .. " wins")
/// end)
/// ```
@LuaLibrary(name = "@mapmaker/store")
public final class LibStore {

    /// Defines a named state slot. Cache the returned definition at module scope; calling
    /// `define_state` more than once for the same name produces independent slots.
    ///
    /// @luaParam name string
    /// @luaParam defaultValue { [string]: any }
    /// @luaReturn @mapmaker/store.StateDefinition
    @LuaMethod
    public static int defineState(LuaState state) {
        var name = state.checkString(1);
        state.checkType(2, LuaType.TABLE);

        LibStore$luau.pushStateDefinition(state, new StateDefinition(Tag.Transient(name)));
        return 1;
    }

    /// A handle to one named state slot. Returned by `define_state`.
    @LuaExport
    public record StateDefinition(Tag<JsonElement> tag) {

        /// Returns this player's value for the slot.
        ///
        /// @luaParam player @mapmaker/player.Player
        /// @luaReturn { [string]: any }
        @LuaMethod
        public int get(LuaState state) {
            var player = LibPlayer.checkPlayerArg(state, 1);

            var data = player.getTag(tag);
            // TODO: handle default value from definition
            if (data == null) data = new JsonObject();

            LuaHelpers.pushJsonElement(state, data);
            return 1;
        }

        /// Replaces this player's value for the slot.
        ///
        /// @luaParam player @mapmaker/player.Player
        /// @luaParam value { [string]: any }
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
