package net.hollowcube.mapmaker.scripting.api;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.gen.LuaProperty;
import net.hollowcube.mapmaker.map.MapPlayer;
import net.hollowcube.mapmaker.map.event.MapPlayerJoinEvent;
import net.hollowcube.mapmaker.map.event.MapPlayerLeaveEvent;
import net.hollowcube.mapmaker.map.event.PlayerLandEvent;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.player.PlayerBlockInteractEvent;

import static net.hollowcube.mapmaker.scripting.api.LibPlayer.pushPlayer;

/// Events that fire across all players in the map.
///
/// ```luau
/// local players = require("@mapmaker/players")
/// players.on_join:listen(function(player)
///     player:send_message("welcome, " .. player.name)
/// end)
/// ```
@LuaLibrary(name = "@mapmaker/players")
public final class LibPlayers {

    /// Fires when a player joins the map.
    ///
    /// @luaReturn @mapmaker.EventSource<@mapmaker/player.Player>
    @LuaProperty
    public static int getOnJoin(LuaState state) {
        class Impl {
            static int pushArgs(LuaState state, MapPlayerJoinEvent event) {
                pushPlayer(state, event.player());
                return 1;
            }
        }

        LibBase.pushEventSource(state, MapPlayerJoinEvent.class, Impl::pushArgs);
        return 1;
    }

    /// Fires when a player leaves the map.
    /// @luaReturn @mapmaker.EventSource<@mapmaker/player.Player>
    @LuaProperty
    public static int getOnLeave(LuaState state) {
        class Impl {
            static int pushArgs(LuaState state, MapPlayerLeaveEvent event) {
                pushPlayer(state, event.player());
                return 1;
            }
        }

        LibBase.pushEventSource(state, MapPlayerLeaveEvent.class, Impl::pushArgs);
        return 1;
    }

    /// Fires when a player lands on the ground after being airborne.
    /// @luaReturn @mapmaker.EventSource<@mapmaker/player.Player>
    @LuaProperty
    public static int getOnLand(LuaState state) {
        class Impl {
            static int pushArgs(LuaState state, PlayerLandEvent event) {
                pushPlayer(state, event.player());
                return 1;
            }
        }

        LibBase.pushEventSource(state, PlayerLandEvent.class, Impl::pushArgs);
        return 1;
    }

    /// Fires when a player right-clicks a block. Receives the player and the block position.
    /// @luaReturn @mapmaker.EventSource<@mapmaker/player.Player, vector>
    @LuaProperty
    public static int getOnBlockInteract(LuaState state) {
        class Impl {
            static int pushArgs(LuaState state, PlayerBlockInteractEvent event) {
                if (!(event.getPlayer() instanceof MapPlayer mp) || event.getHand() != PlayerHand.MAIN) return -1;

                pushPlayer(state, mp);
                LuaVector.push(state, event.getBlockPosition());
                return 2;
            }
        }

        LibBase.pushEventSource(state, PlayerBlockInteractEvent.class, Impl::pushArgs);
        return 1;
    }

}
