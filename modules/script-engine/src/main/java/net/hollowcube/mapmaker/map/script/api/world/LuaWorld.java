package net.hollowcube.mapmaker.map.script.api.world;

import net.hollowcube.luau.LuaState;
import net.hollowcube.luau.annotation.LuaBindable;
import net.hollowcube.luau.annotation.LuaMethod;
import net.hollowcube.luau.annotation.LuaObject;
import net.hollowcube.luau.annotation.LuaProperty;
import net.hollowcube.luau.error.LuaArgError;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.MapEntityType;
import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.hollowcube.mapmaker.map.script.AbstractRefManager;
import net.hollowcube.mapmaker.map.script.api.LuaEventSource;
import net.hollowcube.mapmaker.map.script.api.entity.*;
import net.hollowcube.mapmaker.map.script.api.math.VectorTypeImpl;
import net.kyori.adventure.sound.Sound;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.EntityType;
import net.minestom.server.event.instance.InstanceTickEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.sound.SoundEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
@LuaObject
public class LuaWorld {
    private final MapWorld world;

    private LuaEventSource<Callbacks.OnWorldTick> onTick;

    public LuaWorld(@NotNull MapWorld world) {
        this.world = world;
    }

    @LuaProperty
    public @NotNull String getName() {
        return world.worldId();
    }

    @LuaProperty
    public int getAge() {
        return (int) world.instance().getWorldAge();
    }

    @LuaProperty
    public @NotNull LuaEventSource<Callbacks.OnWorldTick> getOnTick() {
        if (onTick == null) onTick = LuaEventSource.create(
                Callbacks.OnWorldTick.class, InstanceTickEvent.class,
                (_, onTick) -> onTick.call());
        return onTick;
    }

    @LuaMethod
    public int spawnEntity(@NotNull LuaState state) {
        var entityTypeName = state.checkStringArg(2);
        var entityType = EntityType.fromNamespaceId(entityTypeName);
        if (entityType == null) throw new LuaArgError(2, "Invalid entity type");

        var pos = VectorTypeImpl.checkLuaArg(state, 3);
        var entity = MapEntityType.create(entityType, UUID.randomUUID());
        LuaEntityMetaReader.applyMeta(entity, state, 4);
        entity.setInstance(world.instance(), pos);

        ((AbstractRefManager) state.getThreadData()).addEntity(entity);
        state.newUserData(new LuaEntity(entity));
        state.getMetaTable(LuaEntity$Wrapper.TYPE_NAME);
        state.setMetaTable(-2);
        return 1;
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

    @LuaMethod
    public @NotNull Block getBlock(@NotNull Point pos) {
        return world.instance().getBlock(pos, Block.Getter.Condition.TYPE);
    }

    @LuaMethod
    public void setBlock(@NotNull Point pos, @NotNull Block block) {
        //todo check world border
        world.instance().setBlock(pos, block);
    }

    @LuaMethod
    public void playSound(@NotNull String sound, @Nullable Point pos, @Nullable Double volume, @Nullable Double pitch) {
        var soundEvent = SoundEvent.fromNamespaceId(sound);
        if (soundEvent == null) throw new LuaArgError(0, "Invalid sound name");

        //todo source as arg
        var s = Sound.sound(soundEvent, Sound.Source.MASTER, (float) orElse(volume, 1.0), (float) orElse(pitch, 1.0));
        if (pos != null) world.instance().playSound(s, pos);
        else world.instance().playSound(s);
    }

    public static final class Callbacks {

        @LuaBindable
        public interface OnWorldTick {
            void call();
        }

    }

    private double orElse(@Nullable Double d, double def) {
        return d == null ? def : d;
    }

}
