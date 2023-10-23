package net.hollowcube.mapmaker.dev.command;

import net.hollowcube.common.util.FontUtil;
import net.hollowcube.mapmaker.dev.DevServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class EmojisCommand extends Command {
    private static final String SPACE_1PX = FontUtil.computeOffset(1);

    public EmojisCommand() {
        super("emojis");

        setDefaultExecutor(this::showEmojiList);
    }

    public void showEmojiList(@NotNull CommandSender sender, @NotNull CommandContext context) {
        for (var entry : DevServer.EMOJIS_BY_CATEGORY.entrySet()) {
            var msg = Component.text();
            msg.append(Component.text(entry.getKey() + ": ", TextColor.color(0xB0B0B0)));

            for (var emojiName : entry.getValue()) {
                var emoji = Objects.requireNonNull(DevServer.EMOJIS.get(emojiName));
                msg.append(emoji).append(Component.text(SPACE_1PX));
            }

            sender.sendMessage(msg.build());
        }
    }
}
