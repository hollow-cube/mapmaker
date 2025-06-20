package net.hollowcube.compat.noxesium;

import com.google.auto.service.AutoService;
import com.noxcrew.noxesium.api.NoxesiumReferences;
import com.noxcrew.noxesium.api.qib.QibDefinition;
import net.hollowcube.compat.api.CompatProvider;
import net.hollowcube.compat.api.ModChannelRegisterEvent;
import net.hollowcube.compat.api.packet.PacketRegistry;
import net.hollowcube.compat.noxesium.packets.ClientboundChangeEntityRulesPacket;
import net.hollowcube.compat.noxesium.packets.ClientboundChangeServerRulesPacket;
import net.hollowcube.compat.noxesium.packets.ClientboundServerInformationPacket;
import net.hollowcube.compat.noxesium.packets.ServerboundClientInformationPacket;
import net.hollowcube.compat.noxesium.qib.QibDefinitionManager;
import net.hollowcube.compat.noxesium.rules.NoxesiumServerRules;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerGameModeChangeEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;

import java.util.Map;

@AutoService(CompatProvider.class)
public class NoxesiumCompatProvider implements CompatProvider {

    @Override
    public void registerPackets(PacketRegistry registry) {
        registry.register(ClientboundServerInformationPacket.TYPE);
        registry.register(ClientboundChangeServerRulesPacket.TYPE);
        registry.register(ClientboundChangeEntityRulesPacket.TYPE);

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
        events.addListener(ModChannelRegisterEvent.class, event -> {
            // Disable Noxesium for people who arent on the latest version because it does not handle component
            // changes across versions properly. We could maybe fix this as an extension to ViaVersion but it
            // doesn't seem worth it.
            if (event.getPlayerProtocolVersion() != MinecraftServer.PROTOCOL_VERSION)
                event.excludeNamespace(NoxesiumAPI.NAME, NoxesiumAPI.CHANNEL);
        });

        events.addListener(PlayerSpawnEvent.class, event -> {
            Map<String, QibDefinition> defs = event.getInstance().getTag(QibDefinitionManager.QIB_DEFINITIONS);
            if (defs != null) {
                ClientboundChangeServerRulesPacket.qibs(defs).send(event.getPlayer());
            }

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
