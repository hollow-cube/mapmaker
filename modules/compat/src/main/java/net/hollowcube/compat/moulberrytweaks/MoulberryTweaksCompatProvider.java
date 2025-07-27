package net.hollowcube.compat.moulberrytweaks;

import com.google.auto.service.AutoService;
import net.hollowcube.compat.api.CompatProvider;
import net.hollowcube.compat.api.packet.PacketRegistry;
import net.hollowcube.compat.moulberrytweaks.packets.ClientboundDebugRenderAddPacket;
import net.hollowcube.compat.moulberrytweaks.packets.ClientboundDebugRenderClearNamespacePacket;
import net.hollowcube.compat.moulberrytweaks.packets.ClientboundDebugRenderClearPacket;
import net.hollowcube.compat.moulberrytweaks.packets.ClientboundDebugRenderRemovePacket;

@AutoService(CompatProvider.class)
public class MoulberryTweaksCompatProvider implements CompatProvider {

    @Override
    public void registerPackets(PacketRegistry registry) {
        registry.register(ClientboundDebugRenderAddPacket.TYPE);
        registry.register(ClientboundDebugRenderClearNamespacePacket.TYPE);
        registry.register(ClientboundDebugRenderClearPacket.TYPE);
        registry.register(ClientboundDebugRenderRemovePacket.TYPE);
    }

}
