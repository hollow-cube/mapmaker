package net.hollowcube.terraform.compat.axiom.packet.server;

import net.hollowcube.terraform.compat.axiom.packet.AxiomServerPacket;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

import static net.minestom.server.network.NetworkBuffer.VAR_INT;

public record AxiomAckWorldPropertyPacket(
        int sequenceId
) implements AxiomServerPacket {
    public static final NetworkBuffer.Type<AxiomAckWorldPropertyPacket> SERIALIZER = NetworkBufferTemplate.template(
            VAR_INT, AxiomAckWorldPropertyPacket::sequenceId,
            AxiomAckWorldPropertyPacket::new);
}
