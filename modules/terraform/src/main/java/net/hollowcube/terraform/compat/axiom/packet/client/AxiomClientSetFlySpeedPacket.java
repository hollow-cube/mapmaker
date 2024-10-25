package net.hollowcube.terraform.compat.axiom.packet.client;

import net.hollowcube.terraform.compat.axiom.packet.AxiomClientPacket;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

public record AxiomClientSetFlySpeedPacket(
        float flySpeed
) implements AxiomClientPacket {
    public static final NetworkBuffer.Type<AxiomClientSetFlySpeedPacket> SERIALIZER = NetworkBufferTemplate.template(
            NetworkBuffer.FLOAT, AxiomClientSetFlySpeedPacket::flySpeed,
            AxiomClientSetFlySpeedPacket::new);
}
