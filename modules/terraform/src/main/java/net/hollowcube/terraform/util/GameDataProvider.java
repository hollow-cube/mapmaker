package net.hollowcube.terraform.util;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.MinecraftServer;

public interface GameDataProvider {
    GameDataProvider NOOP = () -> MinecraftServer.DATA_VERSION;

    int dataVersion();

    default String upgradeBlockState(int fromVersion, int toVersion, String state) {
        return state;
    }

    default TypedData upgradeBlockEntity(int fromVersion, int toVersion, TypedData blockEntity) {
        return blockEntity;
    }

    record TypedData(String type, CompoundBinaryTag nbt) {}
}
