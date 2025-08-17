package net.hollowcube.mapmaker.command.chat;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ParseResult;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.hollowcube.mapmaker.PlayerSettings;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.perm.PlatformPerm;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.function.BiPredicate;

public class ChatCommand extends CommandDsl {


    private final PlayerService players;
    private final PermManager permissions;
    private final Argument<Channel> channelArg;

    public ChatCommand(@NotNull PlayerService players, @NotNull PermManager permissions) {
        super("chat");
        this.players = players;
        this.permissions = permissions;
        this.channelArg = new ChannelArgument();

        this.description = "Switch chat channels";
        this.category = CommandCategories.SOCIAL;

        addSyntax(playerOnly(this::handle), this.channelArg);
    }

    private void handle(@NotNull Player player, @NotNull CommandContext context) {
        var channel = context.get(channelArg);
        var playerData = PlayerData.fromPlayer(player);
        if (!channel.available.test(this.permissions, player)) return;

        playerData.setSetting(PlayerSettings.CHAT_CHANNEL, channel.name().toLowerCase(Locale.ROOT));
        playerData.writeUpdatesUpstream(players);

        player.sendMessage(Component.translatable(channel.translation));
    }

    private class ChannelArgument extends Argument<Channel> {

        protected ChannelArgument() {
            super("channel");
        }

        @Override
        public @NotNull ParseResult<Channel> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
            var word = reader.readWord(WordType.BRIGADIER).toLowerCase(Locale.ROOT);

            boolean isPartial = false;
            for (var value : Channel.values()) {
                if (!(sender instanceof Player player) || !value.available.test(ChatCommand.this.permissions, player))
                    continue;

                var name = value.name().toLowerCase(Locale.ROOT);
                if (name.equals(word)) return success(value);
                if (name.startsWith(word)) isPartial = true;
            }

            return isPartial ? partial() : syntaxError();
        }

        @Override
        public void suggest(@NotNull CommandSender sender, @NotNull String raw, @NotNull Suggestion suggestion) {
            raw = raw.toLowerCase(Locale.ROOT);
            for (var value : Channel.values()) {
                if (!(sender instanceof Player player) || !value.available.test(ChatCommand.this.permissions, player))
                    continue;

                var name = value.name().toLowerCase(Locale.ROOT);
                if (name.startsWith(raw)) suggestion.add(name);
            }
        }
    }

    private enum Channel {
        GLOBAL("commands.chat.switching.global"),
        LOCAL("commands.chat.switching.local"),
        STAFF("commands.chat.switching.staff", (perms, player) -> perms.hasPlatformPermission(player, PlatformPerm.MAP_ADMIN)),
        ;

        public final String translation;
        public final BiPredicate<PermManager, Player> available;

        Channel(String translation) {
            this(translation, (_, _) -> true);
        }

        Channel(String translation, BiPredicate<PermManager, Player> available) {
            this.translation = translation;
            this.available = available;
        }
    }
}
