package net.hollowcube.mapmaker.util.thesneaky;

import net.hollowcube.common.events.UpdateSignTextEvent;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventListener;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.*;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * SHHHHHHHHH
 */
@SuppressWarnings({"UnnecessaryUnicodeEscape", "UnstableApiUsage"})
public final class TheSneaky {

    private static boolean sneaky = false;
    private static final TheSneaky theSneaky = new TheSneaky();

    public static @NotNull TheSneaky getTheSneaky() {
        return theSneaky;
    }

    public enum Severity {
        INFO, // Just to keep track for now
        LIKELY_INCOMPATIBILITY, // Most likely has compatibility issues but hasn't been documented
        KNOWN_INCOMPATIBILITY, // Documented compatibility issues (eg with resource pack)
        CRITICAL, // Problematic mod (hacked client, etc)
    }

    private record TestEntry(@NotNull String modId, @NotNull String tkey, @NotNull Severity severity, boolean inverse) {
    }

    private static final List<TestEntry> TEST_ENTRIES = List.of(
//            new TestEntry("self_detection", "gui.recipebook.moreRecips", Severity.INFO, false),
            new TestEntry("fabric_api", "fabric.gui.creativeTabPage", Severity.INFO, false), // Relevant to some other tests
            new TestEntry("axiom", "axiom.buildertool.erase", Severity.INFO, false), // Relevant to some other tests
            new TestEntry("lb", "liquidbounce.command.bind.description", Severity.INFO, false), // Relevant to some other tests
            new TestEntry("aaa", "gui.recipebook.moreRecipes", Severity.INFO, false) // Relevant to some other tests
//            new TestEntry("wurst", "item.bratwurst.name", Severity.CRITICAL, true), // Depends on self_detection
//            new TestEntry("meteor-client", "time.sunmeteor-clientbound.name", Severity.CRITICAL, true), // Depends on self_detection
//            new TestEntry("xaero_minimap", "gui.xaero_minimap_settings", Severity.INFO, false),
//            new TestEntry("vivecraft", "vivecraft.options.screen.main", Severity.CRITICAL, false),
//            new TestEntry("zergatul_freecam", "key.zergatul.freecam.toggle", Severity.CRITICAL, false),
//            new TestEntry("itemswapper", "key.itemswapper.moveright", Severity.INFO, false),
//            new TestEntry("jexclient", "jex.name", Severity.CRITICAL, false),
//            new TestEntry("x13_xray", "x13.mod.mode", Severity.CRITICAL, false),
//            new TestEntry("optifine", "of.key.zoom", Severity.INFO, false),
//            new TestEntry("antighost", "key.antighost.reveal", Severity.CRITICAL, false),
//            new TestEntry("invmove", "config.invmove.title", Severity.INFO, false),
//            new TestEntry("flymod", "key.flymod.toggle", Severity.CRITICAL, false),
//            new TestEntry("thunderclient", "descriptions.client.thundergui", Severity.CRITICAL, false),
//            new TestEntry("inventory_essentials", "key.categories.inventoryessentials", Severity.INFO, false),
//            new TestEntry("litematica", "litematica.error.area_selection.copy_failed", Severity.INFO, false),
//            new TestEntry("inventory_tabs", "inventorytabs.key.next_tab", Severity.INFO, false),
//            new TestEntry("flighthelper", "key.categories.flighthelper", Severity.CRITICAL, false),
//            new TestEntry("viafabric", "gui.hide_via_button.disable", Severity.KNOWN_INCOMPATIBILITY, false),
//            new TestEntry("sodium", "sodium.options.view_distance.tooltip", Severity.INFO, false),
//            new TestEntry("nvidium", "nvidium.options.pages.nvidium", Severity.LIKELY_INCOMPATIBILITY, false)

            // MISSING:
            // VulkanMod (could probably PR lang files if i wanted)
            // FeatherClient: I think this sets a brand name, but need to check (i dont want to install the spyware)
            // Salwyrr: Hacked client, shipped as a launcher (will install in a vm someday)
            // Aristois: Hacked client, need to look more into it
            // LabyMod: Sets a brand name i think, need to confirm
            // Badlion: Sets a brand name i think, need to confirm
            // Essential: Sets a brand name i think, need to confirm
            // Impact: Hacked client, dont think its updated to 1.20.4
            // Baritone: Hacked client, not sure how to detect
            // RusherHack: Costs $20, pass.
    );

    public enum State {
        UNKNOWN,
        LIKELY_PRESENT,
        PRESENT,
    }

    public record Report(
            @NotNull String playerId,
            @NotNull String brand,
            @NotNull Map<String, State> entries
    ) {
    }

    private TheSneaky() {
        Check.stateCondition(sneaky, "TheSneaky instance already exists");
        sneaky = true;
        MinecraftServer.getGlobalEventHandler()
                .addListener(UpdateSignTextEvent.class, this::handleUpdateSignText);
    }

    /**
     * Creates a report and returns it if this current session has not been tested, otherwise
     * returns the existing report.
     *
     * @param player the player to create the report for
     * @return the report
     */
    public @NotNull CompletableFuture<Report> createReport(@NotNull Player player) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public @NotNull CompletableFuture<Boolean> send(@NotNull Player player) {
        var worldMin = player.getInstance().getCachedDimensionType().minY();
//        var signPos = player.getPosition().sub(50, 50, 50).withY(y -> Math.max(worldMin, y));
        var signPos = player.getPosition();

        TextComponent.Builder[] lines = new TextComponent.Builder[4];
        for (int i = 0; i < 4; i++) {
            lines[i] = Component.text();
            for (int j = 0; j < TEST_ENTRIES.size(); j++) {
                if (j % 4 != i) continue;
                lines[i].append(Component.translatable(TEST_ENTRIES.get(j).tkey)).append(Component.translatable("not.exists", "%s", List.of(Component.text("whatever"))));
            }
        }
        var messages = ListBinaryTag.builder(BinaryTagTypes.STRING);
        for (var line : lines) {
            var elem = GsonComponentSerializer.gson().serializeToTree(line.build());
            messages.add(StringBinaryTag.stringBinaryTag(GsonComponentSerializer.gson().serializer().toJson(elem)));
        }
        var signData = CompoundBinaryTag.builder()
                .put("front_text", CompoundBinaryTag.builder()
                        .put("messages", messages.build())
                        .build())
                .build();

        MinecraftServer.getGlobalEventHandler().addListener(EventListener.builder(UpdateSignTextEvent.class)
                .filter(e -> e.getPlayer() == player && e.position().sameBlock(signPos))
                .expireCount(1)
                .handler(e -> {
                    var content = e.lines().stream().collect(Collectors.joining());
                    var result = new HashMap<String, State>();
                    System.out.println("RESULT CONTENT: " + content);

                    for (var entry : TEST_ENTRIES) {
                        result.put(entry.modId, content.contains(entry.tkey) ? State.UNKNOWN : State.PRESENT);
                    }

                    // Some post processing is required for the inverse entries
                    var self = result.get("self_detection");
                    for (var entry : TEST_ENTRIES) {
                        if (!entry.inverse) continue;

                        var presence = result.get(entry.modId);
                        if (self == State.PRESENT && presence == State.UNKNOWN) {
                            result.put(entry.modId, State.PRESENT); // Definitely present in this case
                        } else if (self == State.PRESENT && presence == State.PRESENT) {
                            result.put(entry.modId, State.UNKNOWN); // Definitely not present
                        } else {
                            result.put(entry.modId, State.UNKNOWN); // Cannot check but will flag for tkey block
                        }
                    }

                    var report = new Report(player.getUuid().toString(), "unknown", result);
                    System.out.println("UPDATE SIGN " + report);
                })
                .build());

        player.sendPacket(new BundlePacket()); // Begin transaction
        player.sendPacket(new BlockChangePacket(signPos, Block.OAK_SIGN)); // Add the sign
        player.sendPacket(new BlockEntityDataPacket(signPos, 7, signData)); // Set the sign data
        player.sendPacket(new OpenSignEditorPacket(signPos, true)); // Open the editor
        player.sendPacket(new CloseWindowPacket((byte) 0)); // Close the editor (sending the sign update message)
        player.sendPacket(new BlockChangePacket(signPos, player.getInstance().getBlock(signPos))); // Remove the sign
        player.sendPacket(new BundlePacket()); // End transaction

        return null;
    }

    private void handleUpdateSignText(@NotNull UpdateSignTextEvent event) {

    }
}
