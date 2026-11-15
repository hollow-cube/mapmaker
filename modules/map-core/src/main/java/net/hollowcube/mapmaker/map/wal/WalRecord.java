package net.hollowcube.mapmaker.map.wal;

import net.hollowcube.terraform.buffer.EditBuffer;
import net.kyori.adventure.nbt.BinaryTag;
import org.jetbrains.annotations.Nullable;

public sealed interface WalRecord {

    long time();

    record Edit(long time, EditBuffer buffer) implements WalRecord {}

    /// Null is a removal
    record WorldTag(long time, int dataVersion, String key, @Nullable BinaryTag value) implements WalRecord {}
}
