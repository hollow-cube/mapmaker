package net.hollowcube.compat.noxesium;

import com.noxcrew.noxesium.api.NoxesiumReferences;
import net.hollowcube.compat.api.CompatProvider;
import net.hollowcube.compat.api.packet.PacketRegistry;
import net.hollowcube.compat.noxesium.packets.ClientboundChangeServerRulesPacket;
import net.hollowcube.compat.noxesium.packets.ClientboundServerInformationPacket;
import net.hollowcube.compat.noxesium.packets.ServerboundClientInformationPacket;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerGameModeChangeEvent;

public class NoxesiumCompatProvider implements CompatProvider {

    @Override
    public void registerPackets(PacketRegistry registry) {
        registry.register(ClientboundServerInformationPacket.TYPE);
        registry.register(ClientboundChangeServerRulesPacket.TYPE);

        registry.register(ServerboundClientInformationPacket.TYPE, (player, packet) -> {
            new ClientboundServerInformationPacket(NoxesiumReferences.VERSION).send(player);

            player.setTag(NoxesiumAPI.NOXESIUM_VERSION, packet.version());
            if (packet.version() < NoxesiumReferences.VERSION) {
                player.sendMessage("Your Noxesium version is outdated, please update.");
            }
        });
    }

    @Override
    public void registerListeners(GlobalEventHandler events) {
        events.addListener(PlayerGameModeChangeEvent.class, event -> {
            var player = event.getPlayer();
            if (!player.hasTag(NoxesiumAPI.NOXESIUM_VERSION)) return;
            int offset = event.getNewGameMode() == GameMode.CREATIVE ? 8 : 0;
            ClientboundChangeServerRulesPacket.itemNameOffset(offset).send(player);
        });
    }
}
