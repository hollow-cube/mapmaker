package net.hollowcube.mapmaker.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.misc.Emoji;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EmojisCommand extends CommandDsl {
    private static final String SPACE_1PX = FontUtil.computeOffset(1);

    public EmojisCommand() {
        super("emojis");

        this.description = "Lists all of our emojis that you can use in chat";
        this.category = CommandCategories.SOCIAL;

        addSyntax(playerOnly(this::showEmojiList));
    }

    public void showEmojiList(@NotNull Player player, @NotNull CommandContext context) {
        player.sendMessage(LanguageProviderV2.translateMultiMerged("command.emojis.header", List.of()));
        var msg = Component.text();
        for (var emoji : Emoji.values()) {
            if (!emoji.showInHelp()) continue;
            msg.append(emoji.supplier().get()).append(Component.text(SPACE_1PX));
        }
        player.sendMessage(msg.build());
    }
}
