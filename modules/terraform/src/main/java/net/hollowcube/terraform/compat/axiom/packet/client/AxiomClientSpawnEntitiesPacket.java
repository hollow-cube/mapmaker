package net.hollowcube.terraform.compat.axiom.packet.client;

import net.hollowcube.terraform.util.ProtocolUtil;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public record AxiomClientSpawnEntitiesPacket(
        @NotNull List<@NotNull Entry> entries
) implements AxiomClientPacket {

    public AxiomClientSpawnEntitiesPacket {
        entries = List.copyOf(entries);
    }

    public AxiomClientSpawnEntitiesPacket(@NotNull NetworkBuffer buffer, int apiVersion) {
        this(buffer.readCollection(b1 -> new Entry(b1, apiVersion)));
    }

    public record Entry(
            @NotNull UUID uuid,
            @NotNull Pos pos,
            @Nullable UUID copyFrom,
            @NotNull NBTCompound nbt
    ) {

        public Entry(@NotNull NetworkBuffer buffer, int apiVersion) {
            this(buffer.read(NetworkBuffer.UUID), ProtocolUtil.readPos(buffer),
                    buffer.readOptional(NetworkBuffer.UUID),
                    (NBTCompound) buffer.read(NetworkBuffer.NBT));
        }

    }
}
