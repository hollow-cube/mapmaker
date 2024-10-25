package net.hollowcube.terraform.compat.axiom.packet.client;

import net.hollowcube.terraform.compat.axiom.packet.AxiomClientPacket;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public record AxiomClientDeleteEntitiesPacket(
        @NotNull List<@NotNull UUID> uuids
) implements AxiomClientPacket {
    public static final NetworkBuffer.Type<AxiomClientDeleteEntitiesPacket> SERIALIZER = NetworkBufferTemplate.template(
            NetworkBuffer.UUID.list(1024), AxiomClientDeleteEntitiesPacket::uuids,
            AxiomClientDeleteEntitiesPacket::new);

    public AxiomClientDeleteEntitiesPacket {
        uuids = List.copyOf(uuids);
    }
}
