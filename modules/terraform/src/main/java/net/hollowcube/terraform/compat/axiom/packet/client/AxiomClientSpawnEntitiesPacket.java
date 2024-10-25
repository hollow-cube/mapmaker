package net.hollowcube.terraform.compat.axiom.packet.client;

import net.hollowcube.common.util.NetworkBufferTypes;
import net.hollowcube.terraform.compat.axiom.packet.AxiomClientPacket;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public record AxiomClientSpawnEntitiesPacket(
        @NotNull List<@NotNull Entry> entries
) implements AxiomClientPacket {
    public static final NetworkBuffer.Type<AxiomClientSpawnEntitiesPacket> SERIALIZER = NetworkBufferTemplate.template(
            Entry.SERIALIZER.list(1024), AxiomClientSpawnEntitiesPacket::entries,
            AxiomClientSpawnEntitiesPacket::new);

    public record Entry(
            @NotNull UUID uuid,
            @NotNull Pos pos,
            @Nullable UUID copyFrom,
            @NotNull CompoundBinaryTag nbt
    ) {
        public static final NetworkBuffer.Type<Entry> SERIALIZER = NetworkBufferTemplate.template(
                NetworkBuffer.UUID, Entry::uuid,
                NetworkBuffer.POS, Entry::pos,
                NetworkBuffer.UUID.optional(), Entry::copyFrom,
                NetworkBufferTypes.NBT_COMPOUND_OR_END, Entry::nbt,
                Entry::new);
    }

    public AxiomClientSpawnEntitiesPacket {
        entries = List.copyOf(entries);
    }
}
