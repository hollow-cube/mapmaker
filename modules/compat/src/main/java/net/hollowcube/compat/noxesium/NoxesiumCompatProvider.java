package net.hollowcube.compat.noxesium;

import com.google.auto.service.AutoService;
import net.hollowcube.compat.api.CompatProvider;
import net.hollowcube.compat.api.ModChannelRegisterEvent;
import net.hollowcube.compat.api.packet.PacketRegistry;
import net.hollowcube.compat.api.packet.ServerboundModPacket;
import net.hollowcube.compat.noxesium.components.NoxesiumGameComponents;
import net.hollowcube.compat.noxesium.handshake.NoxesiumHandshakeAPI;
import net.hollowcube.compat.noxesium.handshake.NoxesiumPlayer;
import net.hollowcube.compat.noxesium.packets.v2.ServerboundClientInformationPacket;
import net.hollowcube.compat.noxesium.packets.v3.*;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerStartFlyingWithElytraEvent;

import java.util.function.BiConsumer;

@AutoService(CompatProvider.class)
public class NoxesiumCompatProvider implements CompatProvider {

    @Override
    public void registerPackets(PacketRegistry registry) {
        // v2 (required for them to let us know so we can tell them to use v3)
        registry.register(ServerboundClientInformationPacket.TYPE, (_, _) -> {});

        registry.register(ClientboundHandshakeAcknowledgePacket.TYPE);
        registry.register(ClientboundRegistryIdsUpdatePacket.TYPE);
        registry.register(ClientboundHandshakeCompletePacket.TYPE);
        registry.register(ClientboundHandshakeCancelPacket.TYPE);
        registry.register(ClientboundUpdateGameComponentsPacket.TYPE);

        registry.register(ServerboundHandshakePacket.TYPE, NoxesiumHandshakeAPI::handleHandshake);
        registry.register(ServerboundHandshakeAcknowledgePacket.TYPE, handle(NoxesiumPlayer::handle));
        registry.register(ServerboundRegistryUpdateResultPacket.TYPE, handle(NoxesiumPlayer::handle));
        registry.register(ServerboundGlidePacket.TYPE, (player, packet) -> {
            var noxesium = NoxesiumPlayer.get(player);
            if (noxesium.has(NoxesiumGameComponents.CLIENT_AUTHORITATIVE_ELYTRA)) {
                player.setFlyingWithElytra(packet.isGliding());
                EventDispatcher.call(new PlayerStartFlyingWithElytraEvent(player));
            }
        });
    }

    @Override
    public void registerListeners(GlobalEventHandler events) {
        events.addListener(ModChannelRegisterEvent.class, event -> {
            // Disable Noxesium for V2 clients
            event.excludeNamespace(NoxesiumAPI.NAME, "noxesium-v2");
        });
    }

    public <T extends ServerboundModPacket<T>> BiConsumer<Player, T> handle(BiConsumer<NoxesiumPlayer, T> handler) {
        return (player, packet) -> handler.accept(NoxesiumPlayer.get(player), packet);
    }
}
