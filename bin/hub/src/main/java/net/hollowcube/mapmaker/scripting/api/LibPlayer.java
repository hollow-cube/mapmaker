package net.hollowcube.mapmaker.scripting.api;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.gen.LuaExport;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.gen.LuaProperty;
import net.hollowcube.mapmaker.map.MapPlayer;
import org.jetbrains.annotations.Nullable;

@LuaLibrary(name = "@mapmaker/player")
public final class LibPlayer {
    // The player library only exports the type itself currently.

    @LuaExport
    public static final class Player {
        private final MapPlayer player;

        private @Nullable LibBase.EventSource onHitPlayer; // lazy

        public Player(MapPlayer player) {
            this.player = player;
        }

        @LuaProperty
        public int getUuid(LuaState state) {
            state.pushString(player.getUuid().toString());
            return 1;
        }

        @LuaProperty
        public int getName(LuaState state) {
            state.pushString(player.getUsername());
            return 1;
        }

        @LuaProperty
        public int getOnHitPlayer(LuaState state) {
            if (onHitPlayer == null) {
                onHitPlayer = new LibBase.EventSource();
            }
            LibBase$luau.pushEventSource(state, onHitPlayer);
            return 1;
        }
    }

    public static void pushPlayer(LuaState state, net.minestom.server.entity.Player player) {
        LibPlayer$luau.pushPlayer(state, new Player((MapPlayer) player));
    }

    public static MapPlayer checkPlayerArg(LuaState state, int argIndex) {
        return LibPlayer$luau.checkPlayerArg(state, argIndex).player;
    }

}
