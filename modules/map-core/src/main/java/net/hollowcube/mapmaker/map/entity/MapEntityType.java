package net.hollowcube.mapmaker.map.entity;

import net.minestom.server.entity.EntityType;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

public class MapEntityType {
    private static final Logger logger = LoggerFactory.getLogger(MapEntityType.class);
    private static final Map<EntityType, @NotNull BiFunction<EntityType, UUID, ? extends MapEntity>> constructorMap = new ConcurrentHashMap<>();

    public static @NotNull MapEntity create(@NotNull String id, @NotNull UUID uuid) {
        var entityType = Objects.requireNonNull(EntityType.fromNamespaceId(id));
        return create(entityType, uuid);
    }

    public static @NotNull MapEntity create(@NotNull EntityType entityType, @NotNull UUID uuid) {
        var constructor = constructorMap.getOrDefault(entityType, MapEntityType::defaultConstructor);
        return constructor.apply(entityType, uuid);
    }

    public interface Constructor1 {
        @NotNull MapEntity create(@NotNull UUID uuid);
    }

    public interface Constructor2 {
        @NotNull MapEntity create(@NotNull EntityType entityType, @NotNull UUID uuid);
    }

    public static void override(@NotNull EntityType entityType, @NotNull Constructor1 constructor) {
        Check.stateCondition(constructorMap.containsKey(entityType), "Entity type " + entityType + " is already overridden");
        constructorMap.put(entityType, (_, uuid) -> constructor.create(uuid));
    }

    public static void override(@NotNull EntityType entityType, @NotNull Constructor2 constructor) {
        Check.stateCondition(constructorMap.containsKey(entityType), "Entity type " + entityType + " is already overridden");
        constructorMap.put(entityType, constructor::create);
    }

    private static @NotNull MapEntity defaultConstructor(@NotNull EntityType entityType, @NotNull UUID uuid) {
        return new MapEntity(entityType, uuid);
    }

}
