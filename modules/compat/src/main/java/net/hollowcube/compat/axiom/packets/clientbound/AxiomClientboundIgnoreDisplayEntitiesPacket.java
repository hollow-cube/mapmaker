package net.hollowcube.compat.axiom.packets.clientbound;

import net.hollowcube.compat.axiom.AxiomAPI;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

import java.util.List;
import java.util.UUID;

public record AxiomClientboundIgnoreDisplayEntitiesPacket(
    List<UUID> entities
) implements AxiomClientboundModPacket<AxiomClientboundIgnoreDisplayEntitiesPacket> {

    public static final Type<AxiomClientboundIgnoreDisplayEntitiesPacket> TYPE = Type.of(
            AxiomAPI.CHANNEL, "ignore_display_entities",
            NetworkBufferTemplate.template(
                    NetworkBuffer.UUID.list(), AxiomClientboundIgnoreDisplayEntitiesPacket::entities,
                    AxiomClientboundIgnoreDisplayEntitiesPacket::new
            )
    );

    @Override
    public Type<AxiomClientboundIgnoreDisplayEntitiesPacket> getType() {
        return TYPE;
    }
}
