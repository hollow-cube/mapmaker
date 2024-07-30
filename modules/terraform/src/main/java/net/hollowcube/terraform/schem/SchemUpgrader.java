package net.hollowcube.terraform.schem;

import ca.spottedleaf.dataconverter.minecraft.MCDataConverter;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import net.hollowcube.schem.util.GameDataProvider;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

public class SchemUpgrader implements GameDataProvider {

    @Override
    public int dataVersion() {
        return MinecraftServer.DATA_VERSION;
    }

    @Override
    public @NotNull String upgradeBlockState(int fromVersion, int toVersion, @NotNull String blockState) {
        return (String) MCDataConverter.convert(MCTypeRegistry.FLAT_BLOCK_STATE, blockState, fromVersion, toVersion);
    }

    @Override
    public @NotNull CompoundBinaryTag upgradeBlockEntity(int fromVersion, int toVersion, @NotNull String id, @NotNull CompoundBinaryTag data) {
        return MCDataConverter.convertTag(MCTypeRegistry.TILE_ENTITY, data.putString("id", id), fromVersion, toVersion);
    }

    @Override
    public @NotNull CompoundBinaryTag upgradeEntity(int fromVersion, int toVersion, @NotNull CompoundBinaryTag data) {
        var preData = data.getCompound("Data").putString("id", data.getString("Id")); // DataConverter wants the ID here.
        var upgradedData = MCDataConverter.convertTag(MCTypeRegistry.ENTITY, preData, fromVersion, toVersion);
        return data.put("Data", upgradedData);
    }
}
