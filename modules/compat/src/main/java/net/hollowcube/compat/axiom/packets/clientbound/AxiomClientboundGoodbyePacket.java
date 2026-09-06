package net.hollowcube.compat.axiom.packets.clientbound;

import net.hollowcube.compat.api.packet.ClientboundModPacket;
import net.hollowcube.compat.axiom.AxiomAPI;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;

public record AxiomClientboundGoodbyePacket(
        @NotNull String reason
) implements ClientboundModPacket<AxiomClientboundGoodbyePacket> {

    public static final Type<AxiomClientboundGoodbyePacket> TYPE = Type.of(
            AxiomAPI.CHANNEL, "goodbye",
            NetworkBufferTemplate.template(
                    NetworkBuffer.STRING, AxiomClientboundGoodbyePacket::reason,
                    AxiomClientboundGoodbyePacket::new
            )
    );

    @Override
    public Type<AxiomClientboundGoodbyePacket> getType() {
        return TYPE;
    }
}
