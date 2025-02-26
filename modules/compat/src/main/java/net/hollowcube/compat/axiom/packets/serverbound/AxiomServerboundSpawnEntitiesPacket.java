package net.hollowcube.compat.axiom.packets.serverbound;

import net.hollowcube.compat.api.packet.ServerboundModPacket;
import net.hollowcube.compat.axiom.AxiomAPI;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public record AxiomServerboundSpawnEntitiesPacket(
    @NotNull List<Entry> entries
) implements ServerboundModPacket<AxiomServerboundSpawnEntitiesPacket> {

    public static final Type<AxiomServerboundSpawnEntitiesPacket> TYPE = Type.of(
            AxiomAPI.CHANNEL, "spawn_entity",
            NetworkBufferTemplate.template(
                    Entry.SERIALIZER.list(), AxiomServerboundSpawnEntitiesPacket::entries,
                    AxiomServerboundSpawnEntitiesPacket::new
            )
    );

    @Override
    public Type<AxiomServerboundSpawnEntitiesPacket> getType() {
        return TYPE;
    }

    public record Entry(
            @NotNull UUID id,
            @NotNull Pos pos,
            @Nullable UUID copyFrom,
            @NotNull CompoundBinaryTag nbt
    ) {

        public static final NetworkBuffer.Type<Entry> SERIALIZER = NetworkBufferTemplate.template(
                NetworkBuffer.UUID, Entry::id,
                NetworkBuffer.POS, Entry::pos,
                NetworkBuffer.UUID.optional(), Entry::copyFrom,
                NetworkBuffer.NBT_COMPOUND, Entry::nbt,
                Entry::new
        );
    }
}
