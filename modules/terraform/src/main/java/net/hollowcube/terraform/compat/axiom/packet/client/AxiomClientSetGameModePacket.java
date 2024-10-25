package net.hollowcube.terraform.compat.axiom.packet.client;

import net.hollowcube.terraform.compat.axiom.packet.AxiomClientPacket;
import net.minestom.server.entity.GameMode;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

public record AxiomClientSetGameModePacket(
        @NotNull GameMode gameMode
) implements AxiomClientPacket {
    public static final NetworkBuffer.Type<AxiomClientSetGameModePacket> SERIALIZER = NetworkBufferTemplate.template(
            GameMode.NETWORK_TYPE, AxiomClientSetGameModePacket::gameMode,
            AxiomClientSetGameModePacket::new);
}
