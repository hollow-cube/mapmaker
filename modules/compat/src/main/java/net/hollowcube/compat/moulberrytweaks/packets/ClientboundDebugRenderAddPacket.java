package net.hollowcube.compat.moulberrytweaks.packets;

import net.hollowcube.compat.api.packet.ClientboundModPacket;
import net.hollowcube.compat.moulberrytweaks.MoulberryTweaksAPI;
import net.hollowcube.compat.moulberrytweaks.debugrender.DebugShape;
import net.kyori.adventure.key.Key;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.NetworkBufferTemplate;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Nullable;

public record ClientboundDebugRenderAddPacket(
    @Nullable Key key,
    DebugShape shape,
    @MagicConstant(flagsFromClass = DebugShape.class) int flags,
    int lifetimeTicks
) implements ClientboundModPacket<ClientboundDebugRenderAddPacket> {
    public static final Type<ClientboundDebugRenderAddPacket> TYPE = Type.of(
        MoulberryTweaksAPI.DEBUG_RENDER_CHANNEL, "add",
        NetworkBufferTemplate.template(
            NetworkBuffer.KEY.optional(), ClientboundDebugRenderAddPacket::key,
            DebugShape.NETWORK_TYPE, ClientboundDebugRenderAddPacket::shape,
            NetworkBuffer.VAR_INT, ClientboundDebugRenderAddPacket::flags,
            NetworkBuffer.VAR_INT, ClientboundDebugRenderAddPacket::lifetimeTicks,
            ClientboundDebugRenderAddPacket::new)
    );

    @Override
    public Type<ClientboundDebugRenderAddPacket> getType() {
        return TYPE;
    }
}
