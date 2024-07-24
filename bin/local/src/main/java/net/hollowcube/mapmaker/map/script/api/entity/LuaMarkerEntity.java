package net.hollowcube.mapmaker.map.script.api.entity;

import net.hollowcube.luau.annotation.LuaBindable;
import net.hollowcube.luau.annotation.LuaObject;
import net.hollowcube.luau.annotation.LuaProperty;
import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.hollowcube.mapmaker.map.script.api.LuaEventSource;
import org.jetbrains.annotations.NotNull;

@LuaObject
public class LuaMarkerEntity extends LuaEntity {

    public LuaEventSource<Callbacks.OnEnter> onEnter;
    public LuaEventSource<Callbacks.OnExit> onExit;

    public LuaMarkerEntity(@NotNull MarkerEntity entity) {
        super(entity);
    }

    @LuaProperty
    public @NotNull String getMarkerType() {
        return ((MarkerEntity) entity).getType();
    }

//    private void createListener(@NotNull LuaState state) {
//        if (this.listener != null) return;
//        Point min = entity.getMin(), max = entity.getMax();
//        if (min == null || max == null) return;
//
//        var currentPlayers = new HashSet<UUID>();
//        var bb = new LuaCuboid(entity.getPosition().add(min), entity.getPosition().add(max));
//
//        this.listener = EventListener.of(EntityTickEvent.class, event -> {
//            for (var player : event.getInstance().getPlayers()) {
//                if (currentPlayers.contains(player.getUuid())) continue;
//
//                var playerBB = new LuaCuboid(player);
////                System.out.println(player.getUsername() + " " + bb.intersects0(playerBB));
//                if (bb.intersects0(playerBB)) {
//                    currentPlayers.add(player.getUuid());
//                    ((LuaEventSource.TriggerImpl<Callbacks.OnEnter>) this.entered.get())
//                            //todo creating this pin is a leak.
//                            .trigger(f -> f.call(Pin.value(new LuaPlayer(player))));
//                }
//            }
//            for (var player : currentPlayers) {
//                var p = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(player);
//                var playerBB = new LuaCuboid(p);
//                if (!bb.intersects0(playerBB)) {
//                    currentPlayers.remove(player);
//                    ((LuaEventSource.TriggerImpl<Callbacks.OnExit>) this.exited.get())
//                            //todo creating this pin is a leak.
//                            .trigger(f -> f.call(Pin.value(new LuaPlayer(p))));
//                }
//            }
//        });
//        ((ScriptContainer) state.getThreadData()).addListener(listener);
//    }

    public static final class Callbacks {

        @LuaBindable
        public interface OnEnter {
            void call(@NotNull LuaPlayer player);
        }

        @LuaBindable
        public interface OnExit {
            void call(@NotNull LuaPlayer player);
        }

    }
}
