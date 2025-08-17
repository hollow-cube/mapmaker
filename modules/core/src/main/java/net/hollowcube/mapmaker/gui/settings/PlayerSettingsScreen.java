package net.hollowcube.mapmaker.gui.settings;

import net.hollowcube.common.events.RequestStatsEvent;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.common.util.ProtocolVersions;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.minestom.server.dialog.*;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.minestom.server.network.packet.server.common.ShowDialogPacket;
import net.minestom.server.network.packet.server.play.CloseWindowPacket;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PlayerSettingsScreen {

    public static void init(@NotNull PlayerService players, @NotNull GlobalEventHandler events) {
        events.addListener(PlayerCustomClickEvent.class, event -> {
            if (!event.getKey().equals(PlayerSettingsOptions.SETTINGS_DIALOG_ID)) return;
            if (!(event.getPayload() instanceof CompoundBinaryTag tag)) return;

            var data = PlayerDataV2.fromPlayer(event.getPlayer());

            for (var option : PlayerSettingsOptions.OPTIONS) {
                var value = tag.get(option.key());
                if (value == null) continue;
                option.applicator().accept(data, value);
            }

            FutureUtil.submitVirtual(() -> data.writeUpdatesUpstream(players));
        });
        events.addListener(RequestStatsEvent.class, event -> {
            if (ProtocolVersions.getProtocolVersion(event.player()) < ProtocolVersions.V1_21_6) {
                event.player().sendMessage(Component.translatable("dialog.settings.unsupported"));
            } else {
                event.player().sendPacket(new CloseWindowPacket(-1));
                openSettingsDialog(event.player());
            }
        });
    }

    private static void openSettingsDialog(@NotNull Player player) {
        var data = PlayerDataV2.fromPlayer(player);
        var dialog = new Dialog.Notice(
                new DialogMetadata(
                        LanguageProviderV2.translate(Component.translatable("dialog.settings.title")),
                        LanguageProviderV2.translate(Component.translatable("dialog.settings.title")),
                        true,
                        true,
                        DialogAfterAction.CLOSE,
                        List.of(),
                        PlayerSettingsOptions.OPTIONS.stream().map(it -> it.input().apply(data)).toList()
                ),
                new DialogActionButton(
                        LanguageProviderV2.translate(Component.translatable("dialog.settings.close")),
                        null,
                        PlayerSettingsOptions.OPTION_WIDTH,
                        new DialogAction.DynamicCustom(PlayerSettingsOptions.SETTINGS_DIALOG_ID, null)
                )
        );

        player.sendPacket(new ShowDialogPacket(dialog));
    }
}
