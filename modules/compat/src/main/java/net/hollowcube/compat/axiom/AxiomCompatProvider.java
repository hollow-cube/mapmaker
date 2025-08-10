package net.hollowcube.compat.axiom;

import com.google.auto.service.AutoService;
import net.hollowcube.compat.api.CompatProvider;
import net.hollowcube.compat.api.ModChannelRegisterEvent;
import net.hollowcube.compat.api.packet.PacketRegistry;
import net.hollowcube.compat.axiom.packets.clientbound.*;
import net.hollowcube.compat.axiom.packets.serverbound.*;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.entity.EntitySpawnEvent;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.minestom.server.event.player.PlayerTickEndEvent;

@AutoService(CompatProvider.class)
public class AxiomCompatProvider implements CompatProvider {

    @Override
    public void registerPackets(PacketRegistry registry) {
        // Serverbound packets
        registry.register(AxiomServerboundAnnotationUpdatePacket.TYPE, AxiomPacketHandler.handle(AxiomPacketHandler::onAnnotationUpdates));
        registry.register(AxiomServerboundRemoveEntitiesPacket.TYPE, AxiomPacketHandler.handle(AxiomPacketHandler::onRemoveEntities));
        registry.register(AxiomServerboundHelloPacket.TYPE, AxiomPacketHandler::onHello);
        registry.register(AxiomServerboundModifyEntitiesPacket.TYPE, AxiomPacketHandler.handle(AxiomPacketHandler::onModifyEntities));
        registry.register(AxiomServerboundMarkerRequestPacket.TYPE, AxiomPacketHandler.handle(AxiomPacketHandler::onMarkerDataRequest));
        // request chunk
        registry.register(AxiomServerboundEntityRequestPacket.TYPE, AxiomPacketHandler.handle(AxiomPacketHandler::onEntityDataRequest));
        registry.register(AxiomServerboundSetBlockPacket.TYPE, AxiomPacketHandler.handle(AxiomPacketHandler::onSetBlock));
        registry.register(AxiomServerboundSetBufferPacket.TYPE, AxiomPacketHandler.handle(AxiomPacketHandler::onSetBuffer));
        registry.register(AxiomServerboundSetFlySpeedPacket.TYPE, AxiomPacketHandler.handle(AxiomPacketHandler::onSetFlySpeed));
        registry.register(AxiomServerboundSetGameModePacket.TYPE, AxiomPacketHandler.handle(AxiomPacketHandler::onSetGameMode));
        // set no physical trigger
        registry.register(AxiomServerboundSetTimePacket.TYPE, AxiomPacketHandler.disabled("Time modification is disabled on HollowCube."));
        registry.register(AxiomServerboundSetWorldPropertyPacket.TYPE, AxiomPacketHandler.handle(AxiomPacketHandler::onSetWorldProperty));
        registry.register(AxiomServerboundSpawnEntitiesPacket.TYPE, AxiomPacketHandler.handle(AxiomPacketHandler::onSpawnEntities));
        registry.register(AxiomServerboundTeleportPacket.TYPE, AxiomPacketHandler.handle(AxiomPacketHandler::onTeleport));

        // Clientbound packets
        registry.register(AxiomClientboundAckWorldPropertyPacket.TYPE);
        // Add server heightmap
        registry.register(AxiomClientboundAnnotationUpdatePacket.TYPE);
        // Custom blocks
        // editor warning
        registry.register(AxiomClientboundEnablePacket.TYPE);
        registry.register(AxiomClientboundIgnoreDisplayEntitiesPacket.TYPE);
        registry.register(AxiomClientboundMarkerDataPacket.TYPE);
        registry.register(AxiomClientboundMarkerResponsePacket.TYPE);
        // redo handshake
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

        events.addListener(RemoveEntityFromInstanceEvent.class, AxiomEventHandler::onEntityRemoved);
        events.addListener(EntitySpawnEvent.class, AxiomEventHandler::onEntitySpawned);
        events.addListener(PlayerTickEndEvent.class, AxiomEventHandler::onPlayerTick);
    }
}
