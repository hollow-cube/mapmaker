package net.hollowcube.schem;

import net.hollowcube.schem.util.BlockConsumer;
import net.kyori.adventure.nbt.*;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record LitematicaSchematic(
        @NotNull CompoundBinaryTag metadata,
        @NotNull Point size,
        @NotNull Map<String, Schematic> regions
) implements Schematic {

    @Override
    public void forEachBlock(@NotNull Rotation rotation, @NotNull BlockConsumer consumer) {

    }

    @Override
    public @Nullable String name() {
        var name = metadata.get("Name");
        if (name != null && name.type() == BinaryTagTypes.STRING)
            return ((StringBinaryTag) name).value();
        return null;
    }

    @Override
    public @Nullable String author() {
        var name = metadata.get("Author");
        if (name != null && name.type() == BinaryTagTypes.STRING)
            return ((StringBinaryTag) name).value();
        return null;
    }

    @Override
    public @Nullable Instant createdAt() {
        var name = metadata.get("TimeCreated");
        if (name != null && name.type() == BinaryTagTypes.LONG)
            return Instant.ofEpochMilli(((LongBinaryTag) name).value());
        return null;
    }

    @Override
    public boolean hasBlockData() {
        return true;
    }

    @Override
    public @NotNull List<Block> blockPalette() {
        return Schematic.super.blockPalette();
    }

    @Override
    public @NotNull ByteArrayBinaryTag blockData() {
        return Schematic.super.blockData();
    }

    @Override
    public @NotNull List<BlockEntityData> blockEntities() {
        var allBlockEntites = new ArrayList<BlockEntityData>();
        for (var region : regions.values())
            allBlockEntites.addAll(region.blockEntities());
        return allBlockEntites;
    }

    @Override
    public @NotNull List<CompoundBinaryTag> entities() {
        var allEntities = new ArrayList<CompoundBinaryTag>();
        for (var region : regions.values())
            allEntities.addAll(region.entities());
        return allEntities;
    }
}
