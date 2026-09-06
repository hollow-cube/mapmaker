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
            (buffer, packet) -> {
                buffer.write(NetworkBuffer.BOOLEAN, packet.config() != null);
                switch (packet.config()) {
                    case ServerConfig.V9 config -> buffer.write(ServerConfig.V9.NETWORK_TYPE, config);
                    case ServerConfig.V10 config -> buffer.write(ServerConfig.V10.NETWORK_TYPE, config);
                    case null -> {}
                }
            }
    );

    @Override
    public Type<AxiomClientboundEnablePacket> getType() {
        return TYPE;
    }

    /// The layout is chosen per negotiated API when the packet is built, so serializing it never depends
    /// on the recipient's session state.
    public sealed interface ServerConfig {

        NetworkBuffer.Type<List<Block>> BLOCKS = Block.ID_NETWORK_TYPE.list(Short.MAX_VALUE);

        record V9(
                int maxBufferSize,
                int blueprintVersion,
                @NotNull List<Block> blocksWithCustomData,
                @NotNull List<Block> ignoreRotationSet
        ) implements ServerConfig {
            public static final NetworkBuffer.Type<V9> NETWORK_TYPE = NetworkBufferTemplate.template(
                    NetworkBuffer.INT, V9::maxBufferSize,
                    NetworkBuffer.VAR_INT, V9::blueprintVersion,
                    BLOCKS, V9::blocksWithCustomData,
                    BLOCKS, V9::ignoreRotationSet,
                    V9::new
            );
        }

        record V10(
                int blueprintVersion,
                int tunnelSplitSize,
                int maximumTunnelPacketSize,
                @NotNull List<Block> blocksWithCustomData,
                @NotNull List<Block> ignoreRotationSet,
                @NotNull List<String> supportedServerboundPackets
        ) implements ServerConfig {
            private static final byte FORMAT_VERSION = 0;

            public static final NetworkBuffer.Type<V10> NETWORK_TYPE = NetworkBufferTemplate.template(
                    NetworkBuffer.BYTE, _ -> FORMAT_VERSION,
                    NetworkBuffer.VAR_INT, V10::blueprintVersion,
                    NetworkBuffer.INT, V10::tunnelSplitSize,
                    NetworkBuffer.INT, V10::maximumTunnelPacketSize,
                    BLOCKS, V10::blocksWithCustomData,
                    BLOCKS, V10::ignoreRotationSet,
                    NetworkBuffer.STRING.list(Short.MAX_VALUE), V10::supportedServerboundPackets,
                    (_, blueprintVersion, tunnelSplitSize, maximumTunnelPacketSize, blocksWithCustomData, ignoreRotationSet, supportedServerboundPackets) ->
                            new V10(blueprintVersion, tunnelSplitSize, maximumTunnelPacketSize, blocksWithCustomData, ignoreRotationSet, supportedServerboundPackets)
            );
        }
    }
}
