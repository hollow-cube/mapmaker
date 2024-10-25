package net.hollowcube.terraform.compat.axiom.packet.server;

import net.hollowcube.terraform.compat.axiom.packet.AxiomServerPacket;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Enables or disables axiom for the client. May be sent multiple times.
 *
 * @param serverConfig Present to enable, null to disable
 */
public record AxiomEnablePacket(
        @Nullable ServerConfig serverConfig
) implements AxiomServerPacket {
    public static final NetworkBuffer.Type<AxiomEnablePacket> SERIALIZER = NetworkBufferTemplate.template(
            ServerConfig.SERIALIZER.optional(), AxiomEnablePacket::serverConfig,
            AxiomEnablePacket::new);

    public record ServerConfig(
            int maxBufferSize,
            boolean sendSourceInfo,
            boolean sendSourceSettings,
            int maxReachDistance,
            int maxViews,
            boolean editableViews,
            @NotNull List<Block> blocksWithCustomData,
            @NotNull List<Block> ignoreRotationSet,
            int blueprintVersion
    ) {
        public static final NetworkBuffer.Type<ServerConfig> SERIALIZER = NetworkBufferTemplate.template(
                NetworkBuffer.INT, ServerConfig::maxBufferSize,
                NetworkBuffer.BOOLEAN, ServerConfig::sendSourceInfo,
                NetworkBuffer.BOOLEAN, ServerConfig::sendSourceSettings,
                NetworkBuffer.VAR_INT, ServerConfig::maxReachDistance,
                NetworkBuffer.VAR_INT, ServerConfig::maxViews,
                NetworkBuffer.BOOLEAN, ServerConfig::editableViews,
                Block.NETWORK_TYPE.list(Short.MAX_VALUE), ServerConfig::blocksWithCustomData,
                Block.NETWORK_TYPE.list(Short.MAX_VALUE), ServerConfig::ignoreRotationSet,
                NetworkBuffer.VAR_INT, ServerConfig::blueprintVersion,
                ServerConfig::new);
    }
}
