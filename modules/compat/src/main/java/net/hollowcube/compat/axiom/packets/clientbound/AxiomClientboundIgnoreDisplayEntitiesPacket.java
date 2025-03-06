package net.hollowcube.compat.axiom.packets.clientbound;

import net.hollowcube.compat.api.packet.ExtraNetworkBuffers;
import net.hollowcube.compat.axiom.AxiomAPI;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public record AxiomClientboundIgnoreDisplayEntitiesPacket(
    Set<UUID> entities
) implements AxiomClientboundModPacket<AxiomClientboundIgnoreDisplayEntitiesPacket> {

    public static final Type<AxiomClientboundIgnoreDisplayEntitiesPacket> TYPE = Type.of(
            AxiomAPI.CHANNEL, "ignore_display_entities",
            NetworkBufferTemplate.template(
                    ExtraNetworkBuffers.collection(NetworkBuffer.UUID, HashSet::new), AxiomClientboundIgnoreDisplayEntitiesPacket::entities,
                    AxiomClientboundIgnoreDisplayEntitiesPacket::new
            )
    );

    @Override
    public Type<AxiomClientboundIgnoreDisplayEntitiesPacket> getType() {
        return TYPE;
    }
}
