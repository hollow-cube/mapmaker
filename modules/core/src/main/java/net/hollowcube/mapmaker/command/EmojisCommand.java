package net.hollowcube.mapmaker.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.misc.Emoji;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EmojisCommand extends CommandDsl {
    private static final String SPACE_1PX = FontUtil.computeOffset(1);

    public EmojisCommand() {
        super("emojis");

        category = CommandCategories.SOCIAL;

        addSyntax(playerOnly(this::showEmojiList));
    }

    public void showEmojiList(@NotNull Player player, @NotNull CommandContext context) {
        player.sendMessage(LanguageProviderV2.translateMultiMerged("command.emojis.header", List.of()));
        for (var category : Emoji.categories()) {
            var msg = Component.text();
            msg.append(Component.text(category.displayName() + ": ", TextColor.color(0xB0B0B0)));

            for (var emoji : category.emojis()) {
                msg.append(emoji.component()).append(Component.text(SPACE_1PX));
            }

            player.sendMessage(msg.build());
        }
    }
}
