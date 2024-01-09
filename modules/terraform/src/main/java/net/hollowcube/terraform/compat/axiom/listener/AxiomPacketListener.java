package net.hollowcube.terraform.compat.axiom.listener;

import net.hollowcube.terraform.compat.axiom.Axiom;
import net.hollowcube.terraform.compat.axiom.TerraformAxiom;
import net.hollowcube.terraform.compat.axiom.packet.client.*;
import net.hollowcube.terraform.compat.axiom.packet.server.AxiomAckWorldPropertyPacket;
import net.hollowcube.terraform.compat.axiom.packet.server.AxiomBlockEntitiesPacket;
import net.hollowcube.terraform.compat.axiom.packet.server.AxiomEnablePacket;
import net.hollowcube.terraform.compat.axiom.world.property.WorldPropertiesRegistry;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.network.packet.server.play.AcknowledgeBlockChangePacket;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class AxiomPacketListener {
    private static final Logger logger = LoggerFactory.getLogger(AxiomPacketListener.class);

    public void handleHelloMessage(@NotNull Player player, @NotNull AxiomClientHelloPacket packet) {
        var clientInfo = new Axiom.ClientInfo(packet.apiVersion(), packet.extraData());
        logger.info("Axiom is present for {} (API {})", player.getUsername(), clientInfo.apiVersion());
        if (clientInfo.apiVersion() < Axiom.MIN_API_VERSION) {
            player.sendMessage("Your version of Axiom is too old, please update to the latest version.");
        } else if (clientInfo.apiVersion() > Axiom.MAX_API_VERSION) {
            player.sendMessage("Your version of Axiom is not yet supported. Please be patient while we update.");
        } else {
            player.setTag(Axiom.CLIENT_INFO_TAG, clientInfo);

            // Respond with a disable message for now, though it may be followed up with an enable message.
            var disablePacket = new AxiomEnablePacket(false);
            player.sendPacket(disablePacket.toPacket(player));


//            Axiom.enable(player);
//            var registry = WorldPropertiesRegistry.get(player.getInstance());
//            registry.add(
//                    new Category("terraform.test", false),
//                    WorldProperty.global(
//                            NamespaceID.from("terraform:test_checkbox"),
//                            "Test Checkbox", false,
//                            WidgetType.Checkbox(),
//                            (property, unused, value) -> {
//                                System.out.println("Checkbox: " + value);
//                                return true;
//                            }, false
//                    ),
//                    WorldProperty.global(
//                            NamespaceID.from("terraform:test_slider"),
//                            "Test Slider", false,
//                            WidgetType.Slider(0, 100),
//                            (property, unused, value) -> {
//                                System.out.println("Slider: " + value);
//                                return true;
//                            }, 0
//                    ),
//                    WorldProperty.global(
//                            NamespaceID.from("terraform:test_textbox"),
//                            "Test Text Box", false,
//                            WidgetType.TextBox(),
//                            (property, unused, value) -> {
//                                System.out.println("Text box: " + value);
//                                return true;
//                            }, ""
//                    ),
//                    WorldProperty.global(
//                            NamespaceID.from("terraform:test_button"),
//                            "Test Button", false,
//                            WidgetType.Button(),
//                            (property, unused, unused1) -> {
//                                System.out.println("Button");
//                                return false;
//                            }, null
//                    ),
//                    WorldProperty.global(
//                            NamespaceID.from("terraform:test_button_array"),
//                            "Test Button Array", false,
//                            WidgetType.ButtonArray("Option 1", "Option 2", "Option 3"),
//                            (property, unused, value) -> {
//                                System.out.println("Button Array: " + value);
//                                return true;
//                            }, 0
//                    )
//            );
        }
    }


    public void handleSetGamemode(@NotNull Player player, @NotNull AxiomClientSetGameModePacket packet) {
        if (!Axiom.isEnabled(player)) return;
        try {
            player.setGameMode(GameMode.fromId(packet.gameModeId()));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid gamemode received from {} ({})", player.getUuid(), packet.gameModeId());
        }
    }

    public void handleSetFlySpeed(@NotNull Player player, @NotNull AxiomClientSetFlySpeedPacket packet) {
        if (!Axiom.isEnabled(player)) return;
        player.setFlyingSpeed(packet.flySpeed());
    }

    public void handleSetBlock(@NotNull Player player, @NotNull AxiomClientSetBlockPacket packet) {
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

    public void handleTeleport(@NotNull Player player, @NotNull AxiomClientTeleportPacket packet) {
        if (!Axiom.isEnabled(player)) return;

        var playerDimensionName = player.getInstance().getDimensionName();
        if (!packet.dimensionName().equals(playerDimensionName)) {
            logger.warn("Received axiom teleport to different dimension ({} -> {}) from {}",
                    playerDimensionName, packet.dimensionName(), player.getUuid());
            return;
        }

        player.teleport(packet.position());
    }

    public void handleRequestBlockEntities(@NotNull Player player, @NotNull AxiomClientRequestBlockEntityPacket packet) {
        if (!Axiom.isEnabled(player)) return;
        logger.warn("Received axiom:request_block_entity from {}", player.getUuid());

        //todo implement me
        var response = new AxiomBlockEntitiesPacket(List.of());
        player.sendPacket(response.toPacket(player));
    }

    public void handleSetBuffer(@NotNull Player player, @NotNull AxiomClientSetBufferPacket packet) {
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
                case AxiomClientSetBufferPacket.BlockBuffer blockBuffer ->
                        TerraformAxiom.applyBlockBuffer(player, blockBuffer);
                case AxiomClientSetBufferPacket.BiomeBuffer biomeBuffer ->
                        TerraformAxiom.handleSetBiomeBuffer(player, biomeBuffer);
            }
        });
    }

    public void handleSetWorldProperty(@NotNull Player player, @NotNull AxiomClientSetWorldPropertyPacket packet) {
        if (!Axiom.isEnabled(player)) return;
        logger.warn("Received axiom:set_world_property from {}", player.getUuid());

        var registry = WorldPropertiesRegistry.get(player.getInstance());
        var handled = registry.handlePropertyChange(player, packet);
        if (!handled) return;

        var response = new AxiomAckWorldPropertyPacket(packet.sequenceId());
        player.sendPacket(response.toPacket(player));
    }
}
