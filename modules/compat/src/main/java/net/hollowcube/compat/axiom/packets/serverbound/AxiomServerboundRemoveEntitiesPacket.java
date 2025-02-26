package net.hollowcube.compat.axiom.packets.serverbound;

import net.hollowcube.compat.api.packet.ServerboundModPacket;
import net.hollowcube.compat.axiom.AxiomAPI;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public record AxiomServerboundRemoveEntitiesPacket(
        @NotNull List<UUID> entities
) implements ServerboundModPacket<AxiomServerboundRemoveEntitiesPacket> {

    public static final Type<AxiomServerboundRemoveEntitiesPacket> TYPE = Type.of(
            AxiomAPI.CHANNEL, "delete_entity",
            NetworkBufferTemplate.template(
                    NetworkBuffer.UUID.list(), AxiomServerboundRemoveEntitiesPacket::entities,
                    AxiomServerboundRemoveEntitiesPacket::new
            )
    );

    @Override
    public Type<AxiomServerboundRemoveEntitiesPacket> getType() {
        return TYPE;
    }
}
