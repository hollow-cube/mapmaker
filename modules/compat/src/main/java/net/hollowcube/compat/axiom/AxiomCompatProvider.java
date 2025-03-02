package net.hollowcube.compat.axiom;

import com.google.auto.service.AutoService;
import net.hollowcube.compat.api.CompatProvider;
import net.hollowcube.compat.api.packet.PacketRegistry;
import net.hollowcube.compat.axiom.packets.clientbound.*;
import net.hollowcube.compat.axiom.packets.serverbound.*;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.entity.EntitySpawnEvent;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;

@AutoService(CompatProvider.class)
public class AxiomCompatProvider implements CompatProvider {

    @Override
    public void registerPackets(PacketRegistry registry) {
        // serverbound:
        // request entity data, request chunk data,

        registry.register(AxiomServerboundHelloPacket.TYPE, AxiomPacketHandler::onHello);
        registry.register(AxiomServerboundSetFlySpeedPacket.TYPE, AxiomPacketHandler.handle(AxiomPacketHandler::onSetFlySpeed));
        registry.register(AxiomServerboundTeleportPacket.TYPE, AxiomPacketHandler.handle(AxiomPacketHandler::onTeleport));
        registry.register(AxiomServerboundSetGameModePacket.TYPE, AxiomPacketHandler.handle(AxiomPacketHandler::onSetGameMode));
        registry.register(AxiomServerboundSetWorldPropertyPacket.TYPE, AxiomPacketHandler.handle(AxiomPacketHandler::onSetWorldProperty));

        registry.register(AxiomServerboundMarkerRequestPacket.TYPE, AxiomPacketHandler.handle(AxiomPacketHandler::onMarkerDataRequest));
        registry.register(AxiomServerboundEntityRequestPacket.TYPE, AxiomPacketHandler.handle(AxiomPacketHandler::onEntityDataRequest));

        registry.register(AxiomServerboundSetBlockPacket.TYPE, AxiomPacketHandler.handle(AxiomPacketHandler::onSetBlock));
        registry.register(AxiomServerboundSetBufferPacket.TYPE, AxiomPacketHandler.handle(AxiomPacketHandler::onSetBuffer));

        registry.register(AxiomServerboundRemoveEntitiesPacket.TYPE, AxiomPacketHandler.handle(AxiomPacketHandler::onRemoveEntities));
        registry.register(AxiomServerboundSpawnEntitiesPacket.TYPE, AxiomPacketHandler.handle(AxiomPacketHandler::onSpawnEntities));
        registry.register(AxiomServerboundModifyEntitiesPacket.TYPE, AxiomPacketHandler.handle(AxiomPacketHandler::onModifyEntities));

        registry.register(AxiomServerboundAnnotationUpdatePacket.TYPE, AxiomPacketHandler.handle(AxiomPacketHandler::onAnnotationUpdates));

        registry.register(AxiomServerboundSetTimePacket.TYPE, AxiomPacketHandler.disabled("Time modification is disabled on HollowCube."));

        // clientbound:
        // chunk response, entity response

        registry.register(AxiomClientboundEnablePacket.TYPE);
        registry.register(AxiomClientboundMarkerDataPacket.TYPE);
        registry.register(AxiomClientboundSetWorldPropertyPacket.TYPE);
        registry.register(AxiomClientboundRegisterWorldPropertiesPacket.TYPE);
        registry.register(AxiomClientboundAckWorldPropertyPacket.TYPE);
        registry.register(AxiomClientboundSetRestrictionsPacket.TYPE);
        registry.register(AxiomClientboundMarkerResponsePacket.TYPE);
        registry.register(AxiomClientboundEntitiesResponsePacket.TYPE);
        registry.register(AxiomClientboundAllowedGamemodesPacket.TYPE);
        registry.register(AxiomClientboundIgnoreDisplayEntitiesPacket.TYPE);
        registry.register(AxiomClientboundAnnotationUpdatePacket.TYPE);
    }

    @Override
    public void registerListeners(GlobalEventHandler events) {
        events.addListener(RemoveEntityFromInstanceEvent.class, AxiomEventHandler::onEntityRemoved);
        events.addListener(EntitySpawnEvent.class, AxiomEventHandler::onEntitySpawned);
    }
}
