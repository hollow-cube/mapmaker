package net.hollowcube.mapmaker.map.script.object;

import net.hollowcube.luau.LuaState;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.script.friendly.LuaObject;
import net.hollowcube.mapmaker.map.script.friendly.Ref;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LuaPlayer implements LuaObject {
    private static final String TYPE_NAME = LuaPlayer.class.getName();

    public static void initGlobalLib(@NotNull LuaState global) {
        global.newMetaTable(TYPE_NAME);

        global.pushCFunction((state) -> {
            LuaPlayer ref = (LuaPlayer) state.checkUserDataArg(1, TYPE_NAME);
            String key = state.checkStringArg(2);

            return switch (key) {
                case "Position" -> {
                    var pos = ref.getPosition();
                    state.pushVector((float) pos.x(), (float) pos.y(), (float) pos.z());
                    yield 1;
                }
                case "World" -> {
                    ref.getWorldRef().push(state);
                    yield 1;
                }
                default -> {
                    state.argError(2, "No such key: " + key);
                    yield 0; // Never reached
                }
            };
        }, "__index");
        global.setField(-2, "__index");

        global.pushCFunction((state) -> {
            state.error("No such method: " + state.checkStringArg(2));
            return 0;
        }, "__namecall");
        global.setField(-2, "__namecall");

        global.pop(1); // Pop the metatable
    }

    private final LuaState state;
    private final Player player;

    private final Ref<LuaPlayerWorld> worldRef;

    public LuaPlayer(@NotNull LuaState state, @NotNull Player player) {
        this.state = state;
        this.player = player;

        this.worldRef = new Ref<>(state, new LuaPlayerWorld(state, player, MapWorld.forPlayer(player)));
    }

    public @NotNull Pos getPosition() {
        return player.getPosition();
    }

    public @NotNull Ref<LuaPlayerWorld> getWorldRef() {
        return worldRef;
    }

    public void close(@NotNull LuaState state) {
        worldRef.close(this.state);
    }
}
