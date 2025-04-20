package net.hollowcube.terraform.schem;

import net.hollowcube.datafix.DataFixer;
import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.util.Value;
import net.hollowcube.schem.util.GameDataProvider;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.codec.Transcoder;
import org.jetbrains.annotations.NotNull;

public class SchemUpgrader implements GameDataProvider {

    @Override
    public int dataVersion() {
        return MinecraftServer.DATA_VERSION;
    }

    @Override
    public @NotNull String upgradeBlockState(int fromVersion, int toVersion, @NotNull String blockState) {
        return DataFixer.upgrade(DataTypes.FLAT_BLOCK_STATE, Value.wrap(blockState), fromVersion, toVersion)
                .as(String.class, blockState);
    }

    @Override
    public @NotNull CompoundBinaryTag upgradeBlockEntity(int fromVersion, int toVersion, @NotNull String id, @NotNull CompoundBinaryTag data) {
        return (CompoundBinaryTag) DataFixer.upgrade(DataTypes.BLOCK_ENTITY, Transcoder.NBT, data.putString("id", id), fromVersion, toVersion);
    }

    @Override
    public @NotNull CompoundBinaryTag upgradeEntity(int fromVersion, int toVersion, @NotNull CompoundBinaryTag data) {
        var preData = data.getCompound("Data").putString("id", data.getString("Id")); // DataConverter wants the ID here.
        var upgradedData = (CompoundBinaryTag) DataFixer.upgrade(DataTypes.ENTITY, Transcoder.NBT, preData, fromVersion, toVersion);
        return data.put("Data", upgradedData);
    }
}
