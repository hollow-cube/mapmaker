package net.hollowcube.compat.axiom.packets.clientbound;

import net.hollowcube.compat.api.packet.ClientboundModPacket;
import net.hollowcube.compat.axiom.AxiomAPI;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record AxiomClientboundEnablePacket(
    @Nullable ServerConfig config
) implements ClientboundModPacket<AxiomClientboundEnablePacket> {

    public static final Type<AxiomClientboundEnablePacket> TYPE = Type.of(
            AxiomAPI.CHANNEL, "enable",
            NetworkBufferTemplate.template(
                    ServerConfig.SERIALIZER.optional(), AxiomClientboundEnablePacket::config,
                    AxiomClientboundEnablePacket::new
            )
    );

    @Override
    public Type<AxiomClientboundEnablePacket> getType() {
        return TYPE;
    }

    public record ServerConfig(
            int maxBufferSize,
            int blueprintVersion,
            @NotNull List<Block> blocksWithCustomData,
            @NotNull List<Block> ignoreRotationSet
    ) {
        public static final NetworkBuffer.Type<ServerConfig> SERIALIZER = NetworkBufferTemplate.template(
                NetworkBuffer.INT, ServerConfig::maxBufferSize,
                NetworkBuffer.VAR_INT, ServerConfig::blueprintVersion,
                Block.NETWORK_TYPE.list(Short.MAX_VALUE), ServerConfig::blocksWithCustomData,
                Block.NETWORK_TYPE.list(Short.MAX_VALUE), ServerConfig::ignoreRotationSet,
                ServerConfig::new
        );
    }
}
