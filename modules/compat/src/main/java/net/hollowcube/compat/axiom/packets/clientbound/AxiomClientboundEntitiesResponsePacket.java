package net.hollowcube.compat.axiom.packets.clientbound;

import net.hollowcube.compat.axiom.AxiomAPI;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

import java.util.Map;
import java.util.UUID;

public record AxiomClientboundEntitiesResponsePacket(
    long sequence,
    boolean finished,
    Map<UUID, CompoundBinaryTag> data
) implements AxiomClientboundModPacket<AxiomClientboundEntitiesResponsePacket> {

    public static final Type<AxiomClientboundEntitiesResponsePacket> TYPE = Type.of(
        AxiomAPI.CHANNEL, "response_entity_data",
        NetworkBufferTemplate.template(
            NetworkBuffer.LONG, AxiomClientboundEntitiesResponsePacket::sequence,
            NetworkBuffer.BOOLEAN, AxiomClientboundEntitiesResponsePacket::finished,
            NetworkBuffer.UUID.mapValue(NetworkBuffer.NBT_COMPOUND), AxiomClientboundEntitiesResponsePacket::data,
            AxiomClientboundEntitiesResponsePacket::new
        )
    );

    @Override
    public Type<AxiomClientboundEntitiesResponsePacket> getType() {
        return TYPE;
    }
}
