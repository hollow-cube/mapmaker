package net.hollowcube.mapmaker.scripting.api;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.LuaType;
import net.hollowcube.luau.gen.LuaExport;
import net.hollowcube.luau.gen.LuaLibrary;
import net.hollowcube.luau.gen.LuaMethod;
import net.hollowcube.luau.gen.LuaProperty;
import net.hollowcube.mapmaker.map.MapPlayer;
import net.hollowcube.mapmaker.map.entity.impl.DisplayEntity;
import net.hollowcube.mapmaker.scripting.Disposable;
import net.hollowcube.mapmaker.scripting.ScriptContext;
import net.hollowcube.mapmaker.scripting.util.LuaHelpers;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@LuaLibrary(name = "@mapmaker/player")
public final class LibPlayer {
    // The player library only exports the type itself currently.

    @LuaExport
    public static final class Player {
        private final MapPlayer player;

        private @Nullable LibBase.EventSource onHitPlayer; // lazy

        Player(MapPlayer player) {
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
        public int getPosition(LuaState state) {
            var pos = player.getPosition();
            state.pushVector((float) pos.x(), (float) pos.y(), (float) pos.z());
            return 1;
        }

        @LuaProperty
        public int getYaw(LuaState state) {
            state.pushNumber(player.getPosition().yaw());
            return 1;
        }

        @LuaProperty
        public int getPitch(LuaState state) {
            state.pushNumber(player.getPosition().pitch());
            return 1;
        }

        @LuaProperty
        public int getWorld(LuaState state) {
            LibPlayer$luau.pushWorldView(state, new WorldView(player));
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

    @LuaExport
    public static final class WorldView {
        private final MapPlayer player;

        WorldView(MapPlayer player) {
            this.player = player;
        }

        @LuaMethod
        public int spawnEntity(LuaState state) {
            var typeName = state.checkString(1); // entity type
            state.checkType(2, LuaType.TABLE); // init

            if (!typeName.equals("text"))
                throw state.error("Only text entity is supported");

            var entity = new DisplayEntity.Text(UUID.randomUUID());
            entity.updateViewerRule(other -> other == player);

            var luaEntity = new LibEntity.TextDisplay(entity);
            LuaHelpers.tableForEach(state, 2, (key) -> {
                if ("position".equals(key) || "yaw".equals(key) || "pitch".equals(key))
                    return; // Special handling below
                if (!luaEntity.readField(state, key, -1)) {
                    state.argError(2, "Unknown property: " + key);
                }
            });

            if (!LuaHelpers.tableGet(state, 2, "position"))
                state.argError(2, "Missing position");
            Point point = LuaVector.check(state, -1);
            state.pop(1); // remove position
            float yaw = 0, pitch = 0;
            if (LuaHelpers.tableGet(state, 2, "yaw")) {
                yaw = (float) state.toNumber(-1);
                state.pop(1); // remove yaw
            }
            if (LuaHelpers.tableGet(state, 2, "pitch")) {
                pitch = (float) state.toNumber(-1);
                state.pop(1); // remove position
            }

            entity.setInstance(player.getInstance(), new Pos(point, yaw, pitch));
            ScriptContext.get(state).track(new Disposable() {
                @Override
                public void dispose() {
                    entity.remove();
                }

                @Override
                public boolean isDisposed() {
                    return entity.isRemoved();
                }
            });

            LibEntity.pushEntity(state, luaEntity);
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
