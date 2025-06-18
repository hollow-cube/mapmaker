package net.minestom.server.registry;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class RegistryTagHacks {

    public static <T> void insertIntoTag(@NotNull Registry<T> registry, @NotNull Key tag, @NotNull T value) {
        var registryTag = (RegistryTagImpl.Backed<T>) registry.getTag(tag);
        registryTag.add(Objects.requireNonNull(registry.getKey(value)));
    }
}
