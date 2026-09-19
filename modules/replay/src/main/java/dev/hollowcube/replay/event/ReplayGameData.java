package dev.hollowcube.replay.event;

import dev.hollowcube.replay.data.ChunkIndex;
import net.hollowcube.datafix.DataFixer;
import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.util.Value;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.entity.EntityType;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.registry.Registries;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/// Game data is stored by name rather than protocol ID, since an update can renumber a registry,
/// and upgraded from the chunk's data version on read.
public final class ReplayGameData {

    private ReplayGameData() {
    }

    /// The server's when there is one, so a backend does not load a second set, and vanilla's
    /// otherwise, which resolves every item mapmaker records since it adds no registry entries.
    public static Registries registries() {
        var process = MinecraftServer.process();
        if (process != null) return process;

        class Vanilla {
            static final Registries REGISTRIES = Registries.vanilla();
        }
        return Vanilla.REGISTRIES;
    }

    public static Block readBlock(NetworkBuffer buffer, ChunkIndex chunk) {
        return readName(buffer, chunk, LegacyIds::blockState, () -> Fixer.BLOCK_STATE, Block::fromState, "block state");
    }

    public static EntityType readEntityType(NetworkBuffer buffer, ChunkIndex chunk) {
        return readName(buffer, chunk, LegacyIds::entityType, () -> Fixer.ENTITY_TYPE, EntityType::fromKey, "entity type");
    }

    public static CompoundBinaryTag upgradeItemStack(CompoundBinaryTag item, ChunkIndex chunk) {
        if (chunk.dataVersion() >= MinecraftServer.DATA_VERSION) return item;
        return Fixer.upgradeItemStack(item, chunk.dataVersion());
    }

    // fixerType is a supplier so that a current chunk never initializes Fixer.
    private static <T> T readName(
        NetworkBuffer buffer, ChunkIndex chunk, IntFunction<String> legacyLookup,
        Supplier<DataType> fixerType, Function<String, @Nullable T> resolve, String kind
    ) {
        var name = chunk.legacyIds()
            ? legacyLookup.apply(buffer.read(NetworkBuffer.VAR_INT))
            : buffer.read(NetworkBuffer.STRING);
        var upgraded = chunk.dataVersion() >= MinecraftServer.DATA_VERSION ? name
            : Fixer.upgrade(fixerType.get(), name, chunk.dataVersion());
        var value = resolve.apply(upgraded);
        if (value == null)
            throw new IllegalStateException("unknown " + kind + " " + upgraded
                + " (recorded as " + name + " at data version " + chunk.dataVersion() + ")");
        return value;
    }

    /// Lazy because building the fixer is not safe to race, and initializing [DataTypes] before
    /// [DataFixer] fails.
    private static final class Fixer {
        static {
            DataFixer.buildModel();
        }

        static final DataType BLOCK_STATE = DataTypes.FLAT_BLOCK_STATE;
        static final DataType ENTITY_TYPE = DataTypes.ENTITY_NAME;

        static String upgrade(DataType type, String name, int dataVersion) {
            return DataFixer.upgrade(type, Value.wrap(name), dataVersion, MinecraftServer.DATA_VERSION)
                .as(String.class, name);
        }

        static CompoundBinaryTag upgradeItemStack(CompoundBinaryTag item, int dataVersion) {
            return (CompoundBinaryTag) DataFixer.upgrade(DataTypes.ITEM_STACK, Transcoder.NBT, item,
                dataVersion, MinecraftServer.DATA_VERSION);
        }
    }
}
