package net.hollowcube.terraform.compat.axiom;

import net.hollowcube.terraform.TerraformModule;
import net.hollowcube.terraform.compat.axiom.listener.AxiomPacketListener;
import net.hollowcube.terraform.compat.axiom.packet.client.*;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class AxiomModule implements TerraformModule {
    private static final Logger logger = LoggerFactory.getLogger(AxiomModule.class);

    private final EventNode<Event> axiomEvents = EventNode.all("tf/compat/axiom")
            .addListener(PlayerSpawnEvent.class, this::handlePlayerConfig)
            .addListener(PlayerPluginMessageEvent.class, this::handlePluginMessage);

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
        player.sendPluginMessage("minecraft:register", String.join("\0", Axiom.INCOMING_CHANNELS));
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
            case AxiomClientSetBlockPacket packet -> handler.handleSetBlock(player, packet);
////            case "axiom:set_hotbar_slot" -> {}
////            case "axiom:switch_active_hotbar" -> {}
            case AxiomClientTeleportPacket packet -> handler.handleTeleport(player, packet);
////            case "axiom:set_editor_views" -> {}
            case AxiomClientRequestBlockEntityPacket packet -> handler.handleRequestBlockEntities(player, packet);
            case AxiomClientSetBufferPacket packet -> handler.handleSetBuffer(player, packet);
            case AxiomClientSetWorldPropertyPacket packet -> handler.handleSetWorldProperty(player, packet);
            case null -> logger.warn("Unhandled (incoming) axiom channel: {}", event.getIdentifier());
        }
    }

    private void handleRegisterPluginMessageChannels(@NotNull Player player, @NotNull String data) {
        for (var channel : data.split("\0")) {
            if (channel.startsWith("axiom:") && !Axiom.OUTGOING_CHANNELS.contains(channel)) {
                logger.warn("Unhandled (outgoing) axiom channel: {}", channel);
            }
        }
    }
}
