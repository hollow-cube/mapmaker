package net.hollowcube.terraform.compat.axiom;

import net.hollowcube.terraform.TerraformModule;
import net.hollowcube.terraform.compat.axiom.listener.AxiomPacketListener;
import net.hollowcube.terraform.compat.axiom.packet.client.*;
import net.hollowcube.terraform.compat.axiom.packet.server.AxiomMarkerDataPacket;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.entity.EntitySpawnEvent;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class AxiomModule implements TerraformModule {
    static final Logger logger = LoggerFactory.getLogger(AxiomModule.class);

    private final EventNode<Event> axiomEvents = EventNode.all("tf/compat/axiom")
            .addListener(PlayerSpawnEvent.class, this::handlePlayerConfig)
            .addListener(PlayerPluginMessageEvent.class, this::handlePluginMessage)
            .addListener(EntitySpawnEvent.class, this::handleEntitySpawn)
            .addListener(RemoveEntityFromInstanceEvent.class, this::handleEntityRemove);

    private final AxiomPacketListener handler = new AxiomPacketListener();

    @Override
    public @NotNull Set<EventNode<InstanceEvent>> eventNodes() {
        //todo this is NOT good, need some way to provide global event handlers
        MinecraftServer.getGlobalEventHandler().removeChild(axiomEvents);
        MinecraftServer.getGlobalEventHandler().addChild(axiomEvents);
        return Set.of();
    }

    private void handlePlayerConfig(@NotNull PlayerSpawnEvent event) {
        if (!event.isFirstSpawn()) return;

        var player = event.getPlayer();
        player.sendPluginMessage("minecraft:register", String.join("\0", Axiom.CLIENT_PACKETS.channels()));
        //todo minestom needs a way to register incoming plugin messages so multiple sources can do it at once.
    }

    private void handlePluginMessage(@NotNull PlayerPluginMessageEvent event) {
        var player = event.getPlayer();
        if (event.getIdentifier().equals("minecraft:register")) {
            handleRegisterPluginMessageChannels(player, event.getMessageString());
            return;
        }
        if (!event.getIdentifier().startsWith("axiom:")) return;

        switch (Axiom.readPacket(event)) {
            case AxiomClientHelloPacket packet -> handler.handleHelloMessage(player, packet);
            case AxiomClientSetGameModePacket packet -> handler.handleSetGamemode(player, packet);
            case AxiomClientSetFlySpeedPacket packet -> handler.handleSetFlySpeed(player, packet);
            case AxiomClientSetHotbarSlotPacket packet -> handler.handleSetHotbarSlot(player, packet);
            case AxiomClientSwitchActiveHotbarPacket packet -> handler.handleSwitchActiveHotbar(player, packet);
            case AxiomClientTeleportPacket packet -> handler.handleTeleport(player, packet);
            case AxiomClientSetEditorViewsPacket packet -> handler.handleSetEditorViews(player, packet);
            case AxiomClientChunkDataRequestPacket packet -> handler.handleRequestChunkData(player, packet);
            case AxiomClientSetBlockPacket packet -> handler.handleSetBlock(player, packet);
            case AxiomClientSetBufferPacket packet -> handler.handleSetBuffer(player, packet);
            case AxiomClientSetWorldPropertyPacket packet -> handler.handleSetWorldProperty(player, packet);
            case AxiomClientSetTimePacket packet -> handler.handleSetTime(player, packet);
            case AxiomClientSpawnEntitiesPacket packet -> handler.handleSpawnEntities(player, packet);
            case AxiomClientModifyEntitiesPacket packet -> handler.handleModifyEntities(player, packet);
            case AxiomClientDeleteEntitiesPacket packet -> handler.handleDeleteEntities(player, packet);
            case AxiomClientMarkerNbtRequestPacket packet -> handler.handleRequestMarkerData(player, packet);
            case AxiomClientAnnotationUpdatePacket packet -> handler.handleAnnotationUpdate(player, packet);
            case null, default -> logger.warn("Unhandled (incoming) axiom channel: {}", event.getIdentifier());
        }
    }

    private void handleEntitySpawn(@NotNull EntitySpawnEvent event) {
        var entity = event.getEntity();
        if (!entity.getEntityType().equals(EntityType.MARKER)) return;

        var addPacket = new AxiomMarkerDataPacket(new AxiomMarkerDataPacket.Entry(entity.getUuid(), entity.getPosition()));
        Axiom.sendPacket(event.getSpawnInstance(), addPacket);
    }

    private void handleEntityRemove(@NotNull RemoveEntityFromInstanceEvent event) {
        var entity = event.getEntity();
        if (!entity.getEntityType().equals(EntityType.MARKER)) return;

        var removePacket = new AxiomMarkerDataPacket(entity.getUuid());
        Axiom.sendPacket(event.getInstance(), removePacket);
    }

    private void handleRegisterPluginMessageChannels(@NotNull Player player, @NotNull String data) {
        for (var channel : data.split("\0")) {
            if (channel.startsWith("axiom:") && !Axiom.SERVER_PACKETS.channels().contains(channel)) {
                logger.warn("Unhandled (outgoing) axiom channel: {}", channel);
            }
        }
    }
}
