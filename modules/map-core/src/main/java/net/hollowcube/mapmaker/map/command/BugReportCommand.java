package net.hollowcube.mapmaker.map.command;


import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.ServerRuntime;
import net.hollowcube.common.dialogs.DialogBuilder;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.ProtocolVersions;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.posthog.PostHog;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.minestom.server.network.packet.server.common.ShowDialogPacket;

import java.util.Map;

public class BugReportCommand extends CommandDsl {
    private static final Key BUG_REPORT_MESSAGE_ID = Key.key("mapmaker:bug_report");

    static {
        MinecraftServer.getGlobalEventHandler().addListener(PlayerCustomClickEvent.class, event -> {
            if (!event.getKey().equals(BUG_REPORT_MESSAGE_ID)) return;
            if (!(event.getPayload() instanceof CompoundBinaryTag payload))
                return;

            if (!payload.getBoolean("submitted")) return;
            collectAndSubmit(event.getPlayer(), payload.getString("id"));
        });
    }

    private static final Argument<String> messageArg = Argument.GreedyString("message");

    public BugReportCommand() {
        super("bugreport");

        addSyntax(playerOnly(this::handleReportDialog));
        addSyntax(playerOnly(this::handleReportMessage), messageArg);
    }

    private void handleReportDialog(Player player, CommandContext context) {
        if (!ProtocolVersions.hasProtocolVersion(player, ProtocolVersions.V1_21_6)) {
            player.sendMessage(Component.translatable("dialog.bugreport.unsupported"));
            return;
        }

        var dialog = new DialogBuilder()
            .title(Component.translatable("dialog.bugreport.title"))
            .inputs(ib -> ib
                // TODO: this is a minestom bug, it is not translating components inside here. Should fix.
                .multiline("id", LanguageProviderV2.translate(Component.translatable("dialog.bugreport.prompt")),
                    "", Short.MAX_VALUE, -1, 310, 100))
            .buildSubmitConfirmation(BUG_REPORT_MESSAGE_ID, CompoundBinaryTag.builder()
                .putBoolean("submitted", true)
                .build());
        player.sendPacket(new ShowDialogPacket(dialog));
    }

    private void handleReportMessage(Player player, CommandContext context) {
        collectAndSubmit(player, context.get(messageArg));
    }

    private static void collectAndSubmit(Player player, String message) {
        if ((message = message.trim()).isEmpty()) {
            player.sendMessage(Component.translatable("dialog.bugreport.no_message"));
            return;
        }

        var runtime = ServerRuntime.getRuntime();

        var world = MapWorld.forPlayer(player);
        if (world == null) return; // Sanity, even the hub is a world

        var playerId = player.getUuid().toString();
        var session = world.server().sessionManager().getSession(playerId);
        if (session == null) return; // Sanity

        var pvn = ProtocolVersions.getProtocolVersion(player);
        PostHog.capture(playerId, "bug_reported", Map.ofEntries(
            Map.entry("server", runtime.hostname()),
            Map.entry("server_version", runtime.version()),
            Map.entry("server_size", runtime.size()),
            Map.entry("location", String.format("%.2f %.2f %.2f", player.getPosition().x(), player.getPosition().y(), player.getPosition().z())),
            Map.entry("session_start", session.createdAt()),
            Map.entry("game_version", ProtocolVersions.getProtocolName(pvn)),
            Map.entry("proxy", session.proxyId()),
            Map.entry("presence_type", session.presence().type()),
            Map.entry("presence_state", session.presence().state()),
            Map.entry("map_id", world.map().id()),
            Map.entry("message", message)
        ));

        player.sendMessage(Component.translatable("dialog.bugreport.success"));
    }

}
