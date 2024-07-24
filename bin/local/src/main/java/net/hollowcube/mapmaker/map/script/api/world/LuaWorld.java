package net.hollowcube.mapmaker.map.script.api.world;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.annotation.LuaMethod;
import net.hollowcube.luau.annotation.LuaObject;
import net.hollowcube.luau.annotation.LuaProperty;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.hollowcube.mapmaker.map.script.api.entity.LuaMarkerEntity;
import net.hollowcube.mapmaker.map.script.api.entity.LuaMarkerEntity$Wrapper;
import org.jetbrains.annotations.NotNull;

@LuaObject
public record LuaWorld(@NotNull MapWorld world) {

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

            var luaEntity = new LuaMarkerEntity(m);
            state.newUserData(luaEntity);
            state.getMetaTable(LuaMarkerEntity$Wrapper.TYPE_NAME);
            state.setMetaTable(-2);

            state.rawSetI(-2, i++);
        }

        return 1;
    }
}
