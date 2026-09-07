package net.hollowcube.mapmaker.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.ipc.player.PlayerService;
import net.hollowcube.mapmaker.PlayerSettings;
import net.hollowcube.mapmaker.chat.components.ChatLanguage;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.player.LocalPlayer.localPlayer;

public class UwUCommand extends CommandDsl {

    private final PlayerService service;

    public UwUCommand(PlayerService service) {
        super("uwu");
        this.service = service;

        this.description = "UwU-ifies all chat messages.";
        this.category = CommandCategories.GLOBAL;

        addSyntax(playerOnly(this::invoke));
    }

    private void invoke(@NotNull Player player, @NotNull CommandContext context) {
        var data = localPlayer(player);
        var current = data.getSetting(PlayerSettings.CHAT_LANGUAGE);
        data.setSetting(PlayerSettings.CHAT_LANGUAGE, switch (current) {
            case ORIGINAL -> ChatLanguage.UWU;
            case UWU -> ChatLanguage.ORIGINAL;
        });
        switch (current) {
            case ORIGINAL -> player.sendMessage(Component.translatable("commands.uwu.on"));
            case UWU -> player.sendMessage(Component.translatable("commands.uwu.off"));
        }
        data.writeUpdatesUpstream(service);
    }
}
