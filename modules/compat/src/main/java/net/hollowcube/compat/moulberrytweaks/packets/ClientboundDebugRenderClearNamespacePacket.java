package net.hollowcube.compat.moulberrytweaks.packets;

import net.hollowcube.compat.api.packet.ClientboundModPacket;
import net.hollowcube.compat.moulberrytweaks.MoulberryTweaksAPI;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

public record ClientboundDebugRenderClearNamespacePacket(
    String namespace
) implements ClientboundModPacket<ClientboundDebugRenderClearNamespacePacket> {
    public static final Type<ClientboundDebugRenderClearNamespacePacket> TYPE = Type.of(
        MoulberryTweaksAPI.DEBUG_RENDER_CHANNEL, "clear_namespace",
        NetworkBufferTemplate.template(
            NetworkBuffer.STRING, ClientboundDebugRenderClearNamespacePacket::namespace,
            ClientboundDebugRenderClearNamespacePacket::new)
    );

    @Override
    public Type<ClientboundDebugRenderClearNamespacePacket> getType() {
        return TYPE;
    }
}
