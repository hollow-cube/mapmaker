package net.hollowcube.mapmaker.map.polar;

import ca.spottedleaf.dataconverter.minecraft.MCDataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCVersions;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.util.datafix.HCTypeRegistry;
import net.hollowcube.polar.PolarDataConverter;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class PolarDataFixer implements PolarDataConverter {
    public static final PolarDataFixer INSTANCE = new PolarDataFixer();

    private PolarDataFixer() {
    }

    @Override
    public int defaultDataVersion() {
        // We added this in 1.20.5 so assume all untagged worlds are 1.20.4
        return MCVersions.V1_20_4;
    }

    @Override
    public int dataVersion() {
        return MapWorld.DATA_VERSION;
    }

    @Override
    public void convertBlockPalette(@NotNull String[] palette, int fromVersion, int toVersion) {
        for (int i = 0; i < palette.length; i++) {
            palette[i] = (String) MCDataConverter.convert(MCTypeRegistry.FLAT_BLOCK_STATE, palette[i], fromVersion, toVersion);
        }
    }

    @Override
    public @NotNull Map.Entry<String, CompoundBinaryTag> convertBlockEntityData(@NotNull String id, @NotNull CompoundBinaryTag data, int fromVersion, int toVersion) {
        // Merge into one which is what vanilla wants
        var compound = CompoundBinaryTag.builder().putString("id", id).put(data).build();
        var converted = MCDataConverter.convertTag(HCTypeRegistry.BLOCK_ENTITY, compound, fromVersion, toVersion);
        return Map.entry(converted.getString("id"), converted.remove("id"));
    }
}
