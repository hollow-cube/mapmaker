package net.hollowcube.mapmaker.map.scripting.api.entity;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static net.kyori.adventure.key.Key.key;

public final class LuaEntityCtor {

    public static @Nullable LuaEntity create(@NotNull Key key, @Nullable UUID uuid) {
        var ctor = ENTITY_CTORS.get(key);
        if (ctor == null) return null; // No constructor for this key
        return ctor.apply(uuid);
    }

    private static final Map<Key, Function<UUID, LuaEntity>> ENTITY_CTORS = Map.ofEntries(
            Map.entry(key("minecraft:block_display"), LuaDisplayEntity.Block::new),
            Map.entry(key("minecraft:item_display"), LuaDisplayEntity.Item::new),
            Map.entry(key("minecraft:text_display"), LuaDisplayEntity.Text::new)
    );
}
