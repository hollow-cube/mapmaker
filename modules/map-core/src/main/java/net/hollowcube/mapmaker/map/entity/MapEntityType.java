package net.hollowcube.mapmaker.map.entity;

import net.minestom.server.entity.Entity;
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
    private static final Map<EntityType, @NotNull BiFunction<EntityType, UUID, ? extends Entity>> constructorMap = new ConcurrentHashMap<>();

    public static @NotNull Entity create(@NotNull String key, @NotNull UUID uuid) {
        var entityType = Objects.requireNonNull(EntityType.fromKey(key));
        return create(entityType, uuid);
    }

    public static @NotNull Entity create(@NotNull EntityType entityType, @NotNull UUID uuid) {
        var constructor = constructorMap.getOrDefault(entityType, MapEntityType::defaultConstructor);
        return constructor.apply(entityType, uuid);
    }

    public static boolean hasOverride(@NotNull EntityType entityType) {
        return constructorMap.containsKey(entityType);
    }

    public interface Constructor1 {
        @NotNull Entity create(@NotNull UUID uuid);
    }

    public static void override(@NotNull EntityType entityType, @NotNull Constructor1 constructor) {
        Check.stateCondition(constructorMap.containsKey(entityType), "Entity type " + entityType + " is already overridden");
        constructorMap.put(entityType, (_, uuid) -> constructor.create(uuid));
    }

    private static @NotNull Entity defaultConstructor(@NotNull EntityType entityType, @NotNull UUID uuid) {
        logger.warn("Using default constructor for entity type {} (this is probably a mistake!!)", entityType);
        return new Entity(entityType, uuid);
    }

}
