package net.hollowcube.mapmaker.map.script.object;

import net.hollowcube.luau.annotation.LuaObject;
import net.hollowcube.luau.annotation.LuaProperty;
import net.hollowcube.luau.util.Pin;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

@LuaObject
public class LuaPlayer {

    private final Player player;

    @LuaProperty
    public final Pin<LuaWorldView> world;

    public LuaPlayer(@NotNull Player player) {
        this.player = player;

        this.world = Pin.value(new LuaWorldView(player));
    }

    @LuaProperty
    public @NotNull Point getPosition() { //todo add vector mapper for Point
        return player.getPosition();
    }

}
