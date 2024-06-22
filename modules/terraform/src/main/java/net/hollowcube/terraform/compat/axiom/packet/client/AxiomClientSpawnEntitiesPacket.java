package net.hollowcube.terraform.compat.axiom.packet.client;

import net.hollowcube.terraform.util.ProtocolUtil;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public record AxiomClientSpawnEntitiesPacket(
        @NotNull List<@NotNull Entry> entries
) implements AxiomClientPacket {
    private static final int MAX_ENTRIES = 1024;

    public AxiomClientSpawnEntitiesPacket {
        entries = List.copyOf(entries);
    }

    public AxiomClientSpawnEntitiesPacket(@NotNull NetworkBuffer buffer, int apiVersion) {
        this(buffer.readCollection(b1 -> new Entry(b1, apiVersion), MAX_ENTRIES));
    }

    public record Entry(
            @NotNull UUID uuid,
            @NotNull Pos pos,
            @Nullable UUID copyFrom,
            @NotNull CompoundBinaryTag nbt
    ) {

        public Entry(@NotNull NetworkBuffer buffer, int apiVersion) {
            this(buffer.read(NetworkBuffer.UUID), ProtocolUtil.readPos(buffer),
                    buffer.readOptional(NetworkBuffer.UUID),
                    buffer.read(NetworkBuffer.NBT) instanceof CompoundBinaryTag compound ? compound : CompoundBinaryTag.empty());
        }

    }
}
