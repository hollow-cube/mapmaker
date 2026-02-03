package net.hollowcube.compat.noxesium;

import com.google.auto.service.AutoService;
import net.hollowcube.compat.api.CompatProvider;
import net.hollowcube.compat.api.ModChannelRegisterEvent;
import net.hollowcube.compat.api.packet.PacketRegistry;
import net.hollowcube.compat.noxesium.packets.ClientboundChangeEntityRulesPacket;
import net.hollowcube.compat.noxesium.packets.ClientboundChangeServerRulesPacket;
import net.hollowcube.compat.noxesium.packets.ClientboundServerInformationPacket;
import net.hollowcube.compat.noxesium.packets.ServerboundClientInformationPacket;
import net.hollowcube.compat.noxesium.rules.NoxesiumServerRules;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerGameModeChangeEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;

@AutoService(CompatProvider.class)
public class NoxesiumCompatProvider implements CompatProvider {

    @Override
    public void registerPackets(PacketRegistry registry) {
        registry.register(ClientboundServerInformationPacket.TYPE);
        registry.register(ClientboundChangeServerRulesPacket.TYPE);
        registry.register(ClientboundChangeEntityRulesPacket.TYPE);

        registry.register(ServerboundClientInformationPacket.TYPE, (player, packet) -> {
            new ClientboundServerInformationPacket(NoxesiumAPI.VERSION).send(player);

            player.setTag(NoxesiumAPI.NOXESIUM_VERSION, packet.version());
            if (packet.version() < NoxesiumAPI.VERSION) {
                player.sendMessage("Your Noxesium version is outdated, please update.");
            }
        });
    }

    @Override
    public void registerListeners(GlobalEventHandler events) {
        events.addListener(ModChannelRegisterEvent.class, event -> {
            // Disable Noxesium for people who arent on the latest version because it does not handle component
            // changes across versions properly. We could maybe fix this as an extension to ViaVersion but it
            // doesn't seem worth it.
            if (event.getPlayerProtocolVersion() < NoxesiumAPI.MIN_PROTOCOL_VERSION) {
                event.excludeNamespace(NoxesiumAPI.NAME, NoxesiumAPI.CHANNEL);
            } else if (event.getPlayerProtocolVersion() > NoxesiumAPI.MAX_PROTOCOL_VERSION) {
                event.excludeNamespace(NoxesiumAPI.CHANNEL);
            }
        });

        events.addListener(PlayerSpawnEvent.class, event -> {
            ClientboundChangeServerRulesPacket.builder()
                    .add(NoxesiumServerRules.DISABLE_SPIN_ATTACK_COLLISIONS, true)
                    .build()
                    .send(event.getPlayer());
        });

        events.addListener(PlayerGameModeChangeEvent.class, event -> {
            var player = event.getPlayer();
            if (!player.hasTag(NoxesiumAPI.NOXESIUM_VERSION)) return;
            int offset = event.getNewGameMode() == GameMode.CREATIVE ? 8 : 0;
            ClientboundChangeServerRulesPacket.itemNameOffset(offset).send(player);
        });
    }
}
