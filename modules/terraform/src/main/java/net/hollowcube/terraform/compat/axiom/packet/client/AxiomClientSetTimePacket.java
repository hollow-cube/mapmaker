package net.hollowcube.terraform.compat.axiom.packet.client;

import net.hollowcube.terraform.compat.axiom.packet.AxiomClientPacket;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record AxiomClientSetTimePacket(
        @NotNull String dimensionName,
        @Nullable Integer time,
        @Nullable Boolean freezeTime
) implements AxiomClientPacket {
    public static final NetworkBuffer.Type<AxiomClientSetTimePacket> SERIALIZER = NetworkBufferTemplate.template(
            NetworkBuffer.STRING, AxiomClientSetTimePacket::dimensionName,
            NetworkBuffer.INT.optional(), AxiomClientSetTimePacket::time,
            NetworkBuffer.BOOLEAN.optional(), AxiomClientSetTimePacket::freezeTime,
            AxiomClientSetTimePacket::new);
}
