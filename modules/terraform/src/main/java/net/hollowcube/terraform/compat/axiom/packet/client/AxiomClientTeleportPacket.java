package net.hollowcube.terraform.compat.axiom.packet.client;

import net.hollowcube.terraform.compat.axiom.packet.AxiomClientPacket;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

public record AxiomClientTeleportPacket(
        @NotNull String dimensionName,
        @NotNull Pos position
) implements AxiomClientPacket {
    public static final NetworkBuffer.Type<AxiomClientTeleportPacket> SERIALIZER = NetworkBufferTemplate.template(
            NetworkBuffer.STRING, AxiomClientTeleportPacket::dimensionName,
            NetworkBuffer.POS, AxiomClientTeleportPacket::position,
            AxiomClientTeleportPacket::new);
}
