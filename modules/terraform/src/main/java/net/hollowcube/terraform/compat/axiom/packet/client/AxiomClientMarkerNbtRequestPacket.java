package net.hollowcube.terraform.compat.axiom.packet.client;

import net.hollowcube.terraform.compat.axiom.packet.AxiomClientPacket;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record AxiomClientMarkerNbtRequestPacket(@NotNull UUID uuid) implements AxiomClientPacket {
    public static final NetworkBuffer.Type<AxiomClientMarkerNbtRequestPacket> SERIALIZER = NetworkBufferTemplate.template(
            NetworkBuffer.UUID, AxiomClientMarkerNbtRequestPacket::uuid,
            AxiomClientMarkerNbtRequestPacket::new);
}
