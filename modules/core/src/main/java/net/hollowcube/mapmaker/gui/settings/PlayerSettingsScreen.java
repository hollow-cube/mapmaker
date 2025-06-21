package net.hollowcube.mapmaker.gui.settings;

import net.hollowcube.common.dialogs.DialogBuilder;
import net.hollowcube.common.events.RequestStatsEvent;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.common.util.ProtocolVersions;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.minestom.server.network.packet.server.common.ShowDialogPacket;
import net.minestom.server.network.packet.server.play.CloseWindowPacket;
import org.jetbrains.annotations.NotNull;

public class PlayerSettingsScreen {

    public static void init(@NotNull PlayerService players, @NotNull GlobalEventHandler events) {
        events.addListener(PlayerCustomClickEvent.class, event -> {
            if (!event.getKey().equals(PlayerSettingsOptions.SETTINGS_DIALOG_ID)) return;
            if (!(event.getPayload() instanceof CompoundBinaryTag tag)) return;

            var data = PlayerData.fromPlayer(event.getPlayer());

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
        var data = PlayerData.fromPlayer(player);
        var dialog = DialogBuilder.create()
                .title(Component.translatable("dialog.settings.title"))
                .closeOnEscape()
                .inputs(it -> {
                    for (var option : PlayerSettingsOptions.OPTIONS) {
                        it.input(option.input().apply(data));
                    }
                })
                .buildNotice(PlayerSettingsOptions.SETTINGS_DIALOG_ID, null);
        player.sendPacket(new ShowDialogPacket(dialog));
    }
}
