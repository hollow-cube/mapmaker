package net.hollowcube.compat.axiom.packets.serverbound;

import net.hollowcube.compat.api.packet.ExtraNetworkBuffers;
import net.hollowcube.compat.api.packet.ServerboundModPacket;
import net.hollowcube.compat.axiom.AxiomAPI;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public record AxiomServerboundRemoveEntitiesPacket(
        @NotNull Set<UUID> entities
) implements ServerboundModPacket<AxiomServerboundRemoveEntitiesPacket> {

    public static final Type<AxiomServerboundRemoveEntitiesPacket> TYPE = Type.of(
            AxiomAPI.CHANNEL, "delete_entity",
            NetworkBufferTemplate.template(
                    ExtraNetworkBuffers.collection(NetworkBuffer.UUID, HashSet::new), AxiomServerboundRemoveEntitiesPacket::entities,
                    AxiomServerboundRemoveEntitiesPacket::new
            )
    );

    @Override
    public Type<AxiomServerboundRemoveEntitiesPacket> getType() {
        return TYPE;
    }
}
