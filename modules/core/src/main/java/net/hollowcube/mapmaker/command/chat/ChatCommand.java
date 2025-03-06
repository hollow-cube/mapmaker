package net.hollowcube.mapmaker.command.chat;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.PlayerSettings;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class ChatCommand extends CommandDsl {

    private final Argument<Channel> channelArg = Argument.Enum("channel", Channel.class)
            .description("The channel to send the message to");

    private final PlayerService playerService;

    public ChatCommand(@NotNull PlayerService playerService) {
        super("chat");
        this.playerService = playerService;

        this.description = "Switch chat channels";
        this.category = CommandCategories.SOCIAL;

        addSyntax(playerOnly(this::handle), channelArg);
    }

    private void handle(@NotNull Player player, @NotNull CommandContext context) {
        var channel = context.get(channelArg);
        var playerData = PlayerDataV2.fromPlayer(player);
        playerData.setSetting(PlayerSettings.CHAT_CHANNEL, channel.name().toLowerCase(Locale.ROOT));
        playerData.writeUpdatesUpstream(playerService);

        player.sendMessage(Component.translatable(channel.translation));
    }

    private enum Channel {
        GLOBAL("commands.chat.switching.global"),
        LOCAL("commands.chat.switching.local")
        ;

        public final String translation;

        Channel(String translation) {
            this.translation = translation;
        }
    }
}
