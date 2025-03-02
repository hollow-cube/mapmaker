package net.hollowcube.compat.axiom.packets.serverbound;

import net.hollowcube.compat.api.packet.ServerboundModPacket;
import net.hollowcube.compat.axiom.AxiomAPI;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record AxiomServerboundEntityRequestPacket(
    long sequence,
    Set<UUID> ids
) implements ServerboundModPacket<AxiomServerboundEntityRequestPacket> {

    public static final Type<AxiomServerboundEntityRequestPacket> TYPE = Type.of(
            AxiomAPI.CHANNEL, "request_entity_data",
            NetworkBufferTemplate.template(
                    NetworkBuffer.LONG, AxiomServerboundEntityRequestPacket::sequence,
                    NetworkBuffer.UUID.list(), p -> new ArrayList<>(p.ids()),
                    AxiomServerboundEntityRequestPacket::new
            )
    );

    private AxiomServerboundEntityRequestPacket(long sequence, List<UUID> id) {
        this(sequence, Set.copyOf(id));
    }

    @Override
    public Type<AxiomServerboundEntityRequestPacket> getType() {
        return TYPE;
    }
}
