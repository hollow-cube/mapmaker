package net.hollowcube.mapmaker.map;

import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record MapCollection(
        @NotNull String collectionId,
        @NotNull String owner,
        @Nullable String name,
        @Nullable Material icon,
        @NotNull List<String> mapIds
) {

    public @NotNull MapCollection withName(@NotNull String name) {
        return new MapCollection(collectionId, owner, name, icon, mapIds);
    }

    public @NotNull MapCollection withIcon(@NotNull Material icon) {
        return new MapCollection(collectionId, owner, name, icon, mapIds);
    }
}
