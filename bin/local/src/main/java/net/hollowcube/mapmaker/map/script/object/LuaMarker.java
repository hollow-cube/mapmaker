package net.hollowcube.mapmaker.map.script.object;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.annotation.LuaBindable;
import net.hollowcube.luau.annotation.LuaObject;
import net.hollowcube.luau.annotation.LuaProperty;
import net.hollowcube.luau.util.Pin;
import net.hollowcube.luau.util.Pinned;
import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.hollowcube.mapmaker.map.script.engine.ScriptContainer;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.event.EventListener;
import net.minestom.server.event.entity.EntityTickEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.UUID;

@LuaObject
public class LuaMarker implements Pinned {

    private final MarkerEntity entity;

    private EventListener<EntityTickEvent> listener;

    @LuaProperty
    public final Pin<LuaEventSource<Callbacks.OnEnter>> entered;
    @LuaProperty
    public final Pin<LuaEventSource<Callbacks.OnExit>> exited;

    public LuaMarker(@NotNull MarkerEntity entity) {
        this.entity = entity;

        this.entered = LuaEventSource.trigger(Callbacks.OnEnter.class, this::createListener);
        this.exited = LuaEventSource.trigger(Callbacks.OnExit.class, this::createListener);
    }

    @LuaProperty
    public @NotNull String getMarkerType() {
        return entity.getType();
    }

    @Override
    public void unpin() {
        this.entered.close();
        this.exited.close();
    }

    private void createListener(@NotNull LuaState state) {
        if (this.listener != null) return;
        Point min = entity.getMin(), max = entity.getMax();
        if (min == null || max == null) return;

        var currentPlayers = new HashSet<UUID>();
        var bb = new LuaCuboid(entity.getPosition().add(min), entity.getPosition().add(max));

        this.listener = EventListener.of(EntityTickEvent.class, event -> {
            for (var player : event.getInstance().getPlayers()) {
                if (currentPlayers.contains(player.getUuid())) continue;

                var playerBB = new LuaCuboid(player);
//                System.out.println(player.getUsername() + " " + bb.intersects0(playerBB));
                if (bb.intersects0(playerBB)) {
                    currentPlayers.add(player.getUuid());
                    ((LuaEventSource.TriggerImpl<Callbacks.OnEnter>) this.entered.get())
                            //todo creating this pin is a leak.
                            .trigger(f -> f.call(Pin.value(new LuaPlayer(player))));
                }
            }
            for (var player : currentPlayers) {
                var p = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(player);
                var playerBB = new LuaCuboid(p);
                if (!bb.intersects0(playerBB)) {
                    currentPlayers.remove(player);
                    ((LuaEventSource.TriggerImpl<Callbacks.OnExit>) this.exited.get())
                            //todo creating this pin is a leak.
                            .trigger(f -> f.call(Pin.value(new LuaPlayer(p))));
                }
            }
        });
        ((ScriptContainer) state.getThreadData()).addListener(listener);
    }

    public static final class Callbacks {

        @LuaBindable
        public interface OnEnter {
            void call(@NotNull Pin<LuaPlayer> player);
        }

        @LuaBindable
        public interface OnExit {
            void call(@NotNull Pin<LuaPlayer> player);
        }

    }
}
