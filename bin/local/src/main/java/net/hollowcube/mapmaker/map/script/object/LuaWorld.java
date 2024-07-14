package net.hollowcube.mapmaker.map.script.object;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.annotation.LuaMethod;
import net.hollowcube.luau.annotation.LuaObject;
import net.hollowcube.luau.annotation.LuaProperty;
import net.hollowcube.luau.util.Pin;
import net.hollowcube.luau.util.PinImpl;
import net.hollowcube.luau.util.Pinned;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import org.jetbrains.annotations.NotNull;

@LuaObject
public class LuaWorld implements Pinned {

    private final MapWorld world;

    private final Int2ObjectMap<Pin<LuaMarker>> markers = new Int2ObjectArrayMap<>();

    public LuaWorld(@NotNull MapWorld world) {
        this.world = world;
    }

    @LuaProperty
    public @NotNull String getName() {
        return world.worldId();
    }

    @LuaMethod
    public int findMarkers(@NotNull LuaState state) {
        state.newTable();

        int i = 1;
        for (var entity : world.instance().getEntities()) {
            if (!(entity instanceof MarkerEntity m)) continue;

            var mp = markers.computeIfAbsent(m.getEntityId(), _ -> Pin.value(new LuaMarker(m)));
            ((PinImpl<?>) mp).push(state);
            state.rawSetI(-2, i++);
        }

        return 1;
    }

    @Override
    public void unpin() {
        markers.values().forEach(Pin::close);
        markers.clear();
    }
}
