package net.hollowcube.mapmaker.map.script.object;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.annotation.LuaBindable;
import net.hollowcube.luau.annotation.LuaMethod;
import net.hollowcube.luau.annotation.LuaObject;
import net.hollowcube.luau.annotation.LuaProperty;
import net.hollowcube.luau.util.Pin;
import net.hollowcube.luau.util.Pinned;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.event.vnext.MapPlayerCompleteMapEvent;
import net.hollowcube.mapmaker.map.script.type.VectorTypeImpl;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.RelativeFlags;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

@LuaObject
public class LuaPlayer implements Pinned {

    private final Player player;

    @LuaProperty
    public final Pin<LuaWorldView> world;

    @LuaProperty
    public final Pin<LuaEventSource<Callbacks.UseItem>> useItem;

    public LuaPlayer(@NotNull Player player) {
        this.player = player;

        this.world = Pin.value(new LuaWorldView(player));

        this.useItem = LuaEventSource.create(
                Callbacks.UseItem.class, PlayerUseItemEvent.class,
                (_, useItem) -> useItem.call()
        );
    }

    @LuaProperty
    public @NotNull Point getPosition() {
        return player.getPosition();
    }

    @LuaProperty
    public @NotNull LuaCuboid getBoundingBox() {
        return new LuaCuboid(player);
    }

    @LuaMethod
    public int teleport(@NotNull LuaState state) {
        // function Teleport(self, pos: vector, yaw: number?, pitch: number?, relativeFlags: string?)
        // Start at 2 because the first argument is the object itself

        int top = state.getTop();

        Pos pos = Pos.fromPoint(VectorTypeImpl.checkLuaArg(state, 2));
        float yaw = top >= 3 ? (float) state.checkNumberArg(3) : player.getPosition().yaw();
        float pitch = top >= 4 ? (float) state.checkNumberArg(4) : player.getPosition().pitch();
        String relativeFlagsStr = top >= 5 ? state.checkStringArg(5) : "";

        int relativeFlags = 0;
        for (char c : relativeFlagsStr.toCharArray()) {
            int flag = switch (c) {
                case 'x' -> RelativeFlags.X;
                case 'y' -> RelativeFlags.Y;
                case 'z' -> RelativeFlags.Z;
                case 'r' -> RelativeFlags.YAW;
                case 'p' -> RelativeFlags.PITCH;
                default -> {
                    state.argError(5, "Unknown relative teleport flag: " + c);
                    yield 0;
                }
            };
            if ((relativeFlags & flag) != 0) {
                state.argError(5, "Duplicate relative teleport flag: " + c);
                return 0;
            }
            relativeFlags |= flag;
        }

        player.teleport(new Pos(pos, yaw, pitch), null, relativeFlags);
        return 0;
    }

    @LuaMethod
    public void completeMap() {
        var world = MapWorld.forPlayerOptional(player);
        if (world == null) return;
        world.callEvent(new MapPlayerCompleteMapEvent(player, world, "scripted"));
    }

    @LuaMethod
    public void giveItem() {
        player.getInventory().setItemStack(3, ItemStack.of(Material.STICK));
    }

    @Override
    public void unpin() {
        this.useItem.close();

        world.close();
    }

    public static final class Callbacks {

        @LuaBindable
        public interface UseItem {
            void call();
        }

    }
}
