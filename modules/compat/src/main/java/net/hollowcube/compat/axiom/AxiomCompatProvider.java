package net.hollowcube.compat.axiom;

import com.google.auto.service.AutoService;
import net.hollowcube.compat.api.CompatProvider;
import net.hollowcube.compat.api.ModChannelRegisterEvent;
import net.hollowcube.compat.api.packet.PacketRegistry;
import net.hollowcube.compat.axiom.packets.clientbound.*;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.entity.EntitySpawnEvent;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerTickEndEvent;

@AutoService(CompatProvider.class)
public class AxiomCompatProvider implements CompatProvider {

    @Override
    public void registerPackets(PacketRegistry registry) {
        for (var definition : AxiomPackets.SERVERBOUND) definition.register(registry);

        registry.register(AxiomClientboundHelloPacket.TYPE);
        registry.register(AxiomClientboundGoodbyePacket.TYPE);
        registry.register(AxiomClientboundAckWorldPropertyPacket.TYPE);
        // Add server heightmap
        registry.register(AxiomClientboundAnnotationUpdatePacket.TYPE);
        // Custom blocks
        // editor warning
        registry.register(AxiomClientboundEnablePacket.TYPE);
        registry.register(AxiomClientboundIgnoreDisplayEntitiesPacket.TYPE);
        registry.register(AxiomClientboundMarkerDataPacket.TYPE);
        registry.register(AxiomClientboundMarkerResponsePacket.TYPE);
        // custom blocks v2
        // custom items
        registry.register(AxiomClientboundRegisterWorldPropertiesPacket.TYPE);
        // chunk response
        registry.register(AxiomClientboundEntitiesResponsePacket.TYPE);
        registry.register(AxiomClientboundSetRestrictionsPacket.TYPE);
        registry.register(AxiomClientboundSetWorldPropertyPacket.TYPE);
        registry.register(AxiomClientboundUpdateAvailableDispatchesPacket.TYPE);
    }

    @Override
    public void registerListeners(GlobalEventHandler events) {
        events.addListener(ModChannelRegisterEvent.class, event -> {
            // Disable Axiom for people who arent on the latest version because we don't handle
            // converting the block or item states axiom uses.
            if (event.getPlayerProtocolVersion() != MinecraftServer.PROTOCOL_VERSION)
                event.excludeNamespace(AxiomAPI.NAME, AxiomAPI.CHANNEL);
        });

        events.addListener(PlayerSpawnEvent.class, AxiomEventHandler::onPlayerSpawn);
        events.addListener(PlayerDisconnectEvent.class, AxiomEventHandler::onPlayerDisconnect);
        events.addListener(RemoveEntityFromInstanceEvent.class, AxiomEventHandler::onEntityRemoved);
        events.addListener(EntitySpawnEvent.class, AxiomEventHandler::onEntitySpawned);
        events.addListener(PlayerTickEndEvent.class, AxiomEventHandler::onPlayerTick);
    }
}
