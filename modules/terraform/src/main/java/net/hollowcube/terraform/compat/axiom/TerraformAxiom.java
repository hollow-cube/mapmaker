package net.hollowcube.terraform.compat.axiom;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.hollowcube.terraform.compat.axiom.packet.client.*;
import net.hollowcube.terraform.compat.axiom.packet.server.AxiomBlockEntitiesPacket;
import net.hollowcube.terraform.compat.axiom.packet.server.AxiomEnablePacket;
import net.hollowcube.terraform.util.PaletteUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.condition.CommandCondition;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerLoginEvent;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.network.packet.server.play.AcknowledgeBlockChangePacket;
import net.minestom.server.network.packet.server.play.MultiBlockChangePacket;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("UnstableApiUsage")
public class TerraformAxiom {
    private static final Logger logger = LoggerFactory.getLogger(TerraformAxiom.class);
    private static final GlobalEventHandler GLOBAL_EVENTS = MinecraftServer.getGlobalEventHandler();

    public static void init(@NotNull EventNode<? extends InstanceEvent> eventNode, @Nullable CommandCondition condition) {
        GLOBAL_EVENTS.addListener(PlayerLoginEvent.class, TerraformAxiom::handlePlayerLogin);
        GLOBAL_EVENTS.addListener(PlayerPluginMessageEvent.class, TerraformAxiom::handlePluginMessage);
    }

    private static void handlePlayerLogin(@NotNull PlayerLoginEvent event) {
        var player = event.getPlayer();
        player.sendPluginMessage("minecraft:register", String.join("\0", Axiom.INCOMING_CHANNELS));
        //todo minestom needs a way to register incoming plugin messages so multiple sources can do it at once.
    }

    private static void handlePluginMessage(@NotNull PlayerPluginMessageEvent event) {
        var player = event.getPlayer();
        if (event.getIdentifier().equals("minecraft:register")) {
            handleRegisterPluginMessageChannels(player, event.getMessageString());
            return;
        }
        if (!event.getIdentifier().startsWith("axiom:")) return;

        var rawPacket = Axiom.readPacket(event);
        if (rawPacket == null) {
            logger.warn("Unhandled (incoming) axiom channel: {}", event.getIdentifier());
            return;
        }

        switch (rawPacket) {
            case AxiomClientHelloPacket packet -> handleHelloMessage(player, packet);
            case AxiomClientSetGameModePacket packet -> handleSetGamemode(player, packet);
            case AxiomClientSetFlySpeedPacket packet -> handleSetFlySpeed(player, packet);
            case AxiomClientSetBlockPacket packet -> handleSetBlock(player, packet);
//            case "axiom:set_hotbar_slot" -> {}
//            case "axiom:switch_active_hotbar" -> {}
            case AxiomClientTeleportPacket packet -> handleTeleport(player, packet);
//            case "axiom:set_editor_views" -> {}
            case AxiomClientRequestBlockEntityPacket packet -> handleRequestBlockEntities(player, packet);
            case AxiomClientSetBufferPacket packet -> handleSetBuffer(player, packet);
        }
    }

    private static void handleRegisterPluginMessageChannels(@NotNull Player player, @NotNull String data) {
        for (var channel : data.split("\0")) {
            if (channel.startsWith("axiom:") && !Axiom.OUTGOING_CHANNELS.contains(channel)) {
                logger.warn("Unhandled (outgoing) axiom channel: {}", channel);
            }
        }
    }

    private static void handleHelloMessage(@NotNull Player player, @NotNull AxiomClientHelloPacket packet) {
        var clientInfo = new Axiom.ClientInfo(packet.apiVersion(), packet.extraData());
        logger.info("Axiom is present for {} (API {})", player.getUsername(), clientInfo.apiVersion());
        if (clientInfo.apiVersion() < Axiom.MIN_API_VERSION) {
            player.sendMessage("Your version of Axiom is too old, please update to the latest version.");
        } else if (clientInfo.apiVersion() > Axiom.MAX_API_VERSION) {
            player.sendMessage("Your version of Axiom is not yet supported. Please be patient while we update.");
        } else {
            player.setTag(Axiom.CLIENT_INFO_TAG, clientInfo);

            // Respond with a disable message for now, though it may be followed up with an enable message.
//            sendDisableMessage(player);
            Axiom.enable(player);
//            sendEnableMessage(player);
        }
    }

    static void sendEnableMessage(@NotNull Player player) {
        // todo: world properties
        var packet = new AxiomEnablePacket(
                true,
                0x100000, // 1mb, todo: constant/configurable
                false, false,
                5, // todo: constant/configurable
                16, // todo: constant/configurable
                true // todo: constant/configurable
        );
        player.sendPacket(packet.toPacket(player));

        //todo init hotbars
        //todo init views
    }

    static void sendDisableMessage(@NotNull Player player) {
        var packet = new AxiomEnablePacket(false);
        player.sendPacket(packet.toPacket(player));
    }

    private static void handleSetGamemode(@NotNull Player player, @NotNull AxiomClientSetGameModePacket packet) {
        if (!Axiom.isEnabled(player)) return;
        try {
            player.setGameMode(GameMode.fromId(packet.gameModeId()));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid gamemode received from {} ({})", player.getUuid(), packet.gameModeId());
        }
    }

    private static void handleSetFlySpeed(@NotNull Player player, @NotNull AxiomClientSetFlySpeedPacket packet) {
        if (!Axiom.isEnabled(player)) return;
        player.setFlyingSpeed(packet.flySpeed());
    }

    private static void handleSetBlock(@NotNull Player player, @NotNull AxiomClientSetBlockPacket packet) {
        if (!Axiom.isEnabled(player)) return;
        logger.info("Received axiom:set_block from {}", player.getUuid());

        try {
            var instance = player.getInstance();

            //todo use the other stuff here to do whatever fancy things.

            for (var entry : packet.blocks().entrySet()) {
                var blockPosition = entry.getKey();
                var block = entry.getValue();
                instance.setBlock(blockPosition, block, packet.updateNeighbors());
            }

        } finally {
            player.sendPacket(new AcknowledgeBlockChangePacket(packet.sequence()));
        }
    }

    private static void handleTeleport(@NotNull Player player, @NotNull AxiomClientTeleportPacket packet) {
        if (!Axiom.isEnabled(player)) return;

        var playerDimensionName = player.getInstance().getDimensionName();
        if (!packet.dimensionName().equals(playerDimensionName)) {
            logger.warn("Received axiom teleport to different dimension ({} -> {}) from {}",
                    playerDimensionName, packet.dimensionName(), player.getUuid());
            return;
        }

        player.teleport(packet.position());
    }

    private static void handleRequestBlockEntities(@NotNull Player player, @NotNull AxiomClientRequestBlockEntityPacket packet) {
        if (!Axiom.isEnabled(player)) return;
        logger.warn("Received axiom:request_block_entity from {}", player.getUuid());

        //todo implement me
        var response = new AxiomBlockEntitiesPacket(List.of());
        player.sendPacket(response.toPacket(player));
    }

    private static void handleSetBuffer(@NotNull Player player, @NotNull AxiomClientSetBufferPacket packet) {
        if (!Axiom.isEnabled(player)) return;
        logger.warn("Received axiom:set_buffer from {}", player.getUuid());

        var playerDimensionName = player.getInstance().getDimensionName();
        if (!packet.dimensionName().equals(playerDimensionName)) {
            logger.warn("Received axiom set buffer for a different dimension ({} -> {}) from {}",
                    playerDimensionName, packet.dimensionName(), player.getUuid());
            return;
        }

        //todo not sure if it is worth tracking correlation IDs for something
        Thread.startVirtualThread(() -> {
            switch (packet.buffer()) {
                case AxiomClientSetBufferPacket.BlockBuffer blockBuffer -> applyBlockBuffer(player, blockBuffer);
                case AxiomClientSetBufferPacket.BiomeBuffer biomeBuffer -> handleSetBiomeBuffer(player, biomeBuffer);
            }
        });
    }

    @Blocking
    private static void applyBlockBuffer(@NotNull Player player, @NotNull AxiomClientSetBufferPacket.BlockBuffer buffer) {
        var instance = player.getInstance();

        int[] paletteData = new int[4096]; // Reused buffer
        var sectionChangeCache = new LongArrayList();
        for (var sectionUpdate : buffer.updates()) {
            int chunkX = PaletteUtil.unpackX(sectionUpdate.index());
            int sectionY = PaletteUtil.unpackY(sectionUpdate.index());
            int chunkZ = PaletteUtil.unpackZ(sectionUpdate.index());

            // Ensure chunk is loaded
            var chunk = instance.getChunk(chunkX, chunkZ);
            if (chunk == null) {
                logger.warn("Received block buffer for unloaded chunk ({}, {}) from {}",
                        chunkX, chunkZ, player.getUuid());
                continue;
            }

            // Apply the changes to the section and queue the update for the viewers
            var section = chunk.getSection(sectionY);
            sectionChangeCache.clear();
            synchronized (chunk) {
                //todo optimize this, fixed palette can be a single full update
                sectionUpdate.palette().read(paletteData); // Read palette into shared buffer

                var indexCache = new AtomicInteger(0);
                section.blockPalette().getAll((sx, sy, sz, stateId) -> {
                    var paletteIndex = indexCache.getAndIncrement();
                    var newBlockState = paletteData[paletteIndex];

                    if (newBlockState == Axiom.EMPTY_BLOCK_STATE) {
                        paletteData[paletteIndex] = stateId;
                    } else {
                        sectionChangeCache.add(((long) newBlockState << 12) | ((long) sx << 8 | (long) sz << 4 | sy));
                    }
                });

                indexCache.set(0);
                section.blockPalette().setAll((x, y, z) -> paletteData[indexCache.getAndIncrement()]);
            }

            var updateIndex = (((long) chunkX & 0x3FFFFF) << 42) | ((long) sectionY & 0xFFFFF) | (((long) chunkZ & 0x3FFFFF) << 20);
            var packet = new MultiBlockChangePacket(updateIndex, sectionChangeCache.toLongArray());
            chunk.sendPacketsToViewers(packet);
        }

        logger.warn("Received block buffer with too many changes ({} remaining) from {}",
                buffer.overflow(), player.getUuid());
    }

    @Blocking
    private static void handleSetBiomeBuffer(@NotNull Player player, @NotNull AxiomClientSetBufferPacket.BiomeBuffer buffer) {
        logger.warn("Do not know how to apply biome buffer!");
        //todo
    }

}
