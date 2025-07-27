package net.hollowcube.mapmaker.map.polar;

import net.hollowcube.datafix.DataFixer;
import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.util.Value;
import net.hollowcube.polar.PolarDataConverter;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.codec.Transcoder;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class PolarDataFixer implements PolarDataConverter {
    public static final PolarDataFixer INSTANCE = new PolarDataFixer();

    private PolarDataFixer() {
    }

    @Override
    public int defaultDataVersion() {
        // We added this in 1.20.5 so assume all untagged worlds are 1.20.4
        return 4189;
    }

    @Override
    public int dataVersion() {
        return DataFixer.maxVersion();
    }

    @Override
    public void convertBlockPalette(@NotNull String[] palette, int fromVersion, int toVersion) {
        for (int i = 0; i < palette.length; i++) {
            palette[i] = DataFixer.upgrade(DataTypes.FLAT_BLOCK_STATE, Value.wrap(palette[i]), fromVersion, toVersion).as(String.class, palette[i]);
        }
    }

    @Override
    public @NotNull Map.Entry<String, CompoundBinaryTag> convertBlockEntityData(@NotNull String id, @NotNull CompoundBinaryTag data, int fromVersion, int toVersion) {
        // Merge into one which is what vanilla wants
        var compound = CompoundBinaryTag.builder().putString("id", id).put(data).build();
        var converted = (CompoundBinaryTag) DataFixer.upgrade(DataTypes.BLOCK_ENTITY, Transcoder.NBT, compound, fromVersion, toVersion);
        return Map.entry(converted.getString("id"), converted.remove("id"));
    }
}
