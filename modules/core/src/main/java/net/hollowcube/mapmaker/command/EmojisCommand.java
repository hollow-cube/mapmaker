package net.hollowcube.mapmaker.command;

import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.misc.Emoji;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EmojisCommand extends CommandDsl {
    private static final String SPACE_1PX = FontUtil.computeOffset(1);

    private final Component emojiList;

    public EmojisCommand() {
        super("emojis");

        this.description = "Lists all of our emojis that you can use in chat";
        this.category = CommandCategories.SOCIAL;

        this.emojiList = buildMessage();

        addSyntax((sender, ignored) -> sender.sendMessage(emojiList));
    }

    private static @NotNull Component buildMessage() {
        TextComponent.Builder publicMsg = Component.text(), hypercubeMsg = Component.text();
        publicMsg.append(LanguageProviderV2.translateMultiMerged("command.emojis.header.public", List.of())).appendNewline();
        hypercubeMsg.append(LanguageProviderV2.translateMultiMerged("command.emojis.header.hypercube", List.of())).appendNewline();

        for (var emoji : Emoji.values()) {
            if (!emoji.showInHelp()) continue;
            (Emoji.isPublic(emoji) ? publicMsg : hypercubeMsg)
                    .append(emoji.supplier().apply(null))
                    .append(Component.text(SPACE_1PX));
        }

        return publicMsg.appendNewline().appendNewline().append(hypercubeMsg).build();
    }
}
