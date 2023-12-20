package net.hollowcube.mapmaker.dev.command;

import net.hollowcube.command.Command;
import net.hollowcube.command.CommandContext;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.dev.DevServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class EmojisCommand extends Command {
    private static final String SPACE_1PX = FontUtil.computeOffset(1);

    public EmojisCommand() {
        super("emojis");

        addSyntax(playerOnly(this::showEmojiList));
    }

    public void showEmojiList(@NotNull Player player, @NotNull CommandContext context) {
        player.sendMessage(LanguageProviderV2.translateMultiMerged("command.emojis.header", List.of()));
        for (var entry : DevServer.EMOJIS_BY_CATEGORY.entrySet()) {
            var msg = Component.text();
            msg.append(Component.text(entry.getKey() + ": ", TextColor.color(0xB0B0B0)));

            for (var emojiName : entry.getValue()) {
                var emoji = Objects.requireNonNull(DevServer.EMOJIS.get(emojiName));
                msg.append(emoji).append(Component.text(SPACE_1PX));
            }

            player.sendMessage(msg.build());
        }
    }
}
