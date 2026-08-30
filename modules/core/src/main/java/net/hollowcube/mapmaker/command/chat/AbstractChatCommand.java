package net.hollowcube.mapmaker.command.chat;

import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.ipc.chat.ChatChannel;
import net.hollowcube.mapmaker.chat.ChatMessageListener;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public abstract class AbstractChatCommand extends CommandDsl {

    private final ChatMessageListener messages;

    public AbstractChatCommand(
        @NotNull ChatMessageListener messages,
        @NotNull String name, @NotNull String... aliases
    ) {
        super(name, aliases);

        this.messages = messages;
    }

    protected void handle(
        @NotNull Player player,
        @NotNull ChatChannel channel,
        @Nullable String targetId,
        @NotNull String message
    ) {
        this.messages.trySendChatMessage(player, channel, targetId, message);
    }
}
