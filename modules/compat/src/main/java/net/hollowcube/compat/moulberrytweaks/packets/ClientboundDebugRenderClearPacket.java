package net.hollowcube.compat.moulberrytweaks.packets;

import net.hollowcube.compat.api.packet.ClientboundModPacket;
import net.hollowcube.compat.moulberrytweaks.MoulberryTweaksAPI;
import net.minestom.server.network.NetworkBufferTemplate;

public record ClientboundDebugRenderClearPacket() implements ClientboundModPacket<ClientboundDebugRenderClearPacket> {
    public static final Type<ClientboundDebugRenderClearPacket> TYPE = Type.of(
            MoulberryTweaksAPI.DEBUG_RENDER_CHANNEL, "clear",
            NetworkBufferTemplate.template(ClientboundDebugRenderClearPacket::new)
    );

    @Override
    public Type<ClientboundDebugRenderClearPacket> getType() {
        return TYPE;
    }
}
