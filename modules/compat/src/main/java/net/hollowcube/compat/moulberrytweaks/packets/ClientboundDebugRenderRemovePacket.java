package net.hollowcube.compat.moulberrytweaks.packets;

import net.hollowcube.compat.api.packet.ClientboundModPacket;
import net.hollowcube.compat.moulberrytweaks.MoulberryTweaksAPI;
import net.kyori.adventure.key.Key;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;

public record ClientboundDebugRenderRemovePacket(
    Key key
) implements ClientboundModPacket<ClientboundDebugRenderRemovePacket> {
    public static final Type<ClientboundDebugRenderRemovePacket> TYPE = Type.of(
        MoulberryTweaksAPI.DEBUG_RENDER_CHANNEL, "remove",
        NetworkBufferTemplate.template(
            NetworkBuffer.KEY, ClientboundDebugRenderRemovePacket::key,
            ClientboundDebugRenderRemovePacket::new)
    );

    @Override
    public Type<ClientboundDebugRenderRemovePacket> getType() {
        return TYPE;
    }
}
