package net.hollowcube.mapmaker.command.chat;

import net.hollowcube.ipc.chat.ChatChannel;
import net.hollowcube.mapmaker.chat.ChatMessageListener;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.player.Permission;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.command.CoreCommandCondition.staffPerm;

public class ChannelCommand extends AbstractChatCommand {

    private ChannelCommand(
        @NotNull ChatMessageListener messages,
        @NotNull ChatChannel channel, @NotNull String name, @NotNull String... aliases
    ) {
        super(messages, name, aliases);

        this.description = "Send a message to a specific chat channel.";
        this.category = CommandCategories.SOCIAL;

        var arg = CoreArgument.Message("message").description("The message content to send in the chat.");

        addSyntax(playerOnly((player, ctx) -> this.handle(player, channel, null, ctx.get(arg))), arg);
    }

    public static class Global extends ChannelCommand {
        public Global(@NotNull ChatMessageListener messages) {
            super(messages, ChatChannel.GLOBAL, "gc");
        }
    }

    public static class Local extends ChannelCommand {
        public Local(@NotNull ChatMessageListener messages) {
            super(messages, ChatChannel.LOCAL, "lc");
        }
    }

    public static class Reply extends ChannelCommand {
        public Reply(@NotNull ChatMessageListener messages) {
            super(messages, ChatChannel.REPLY, "reply", "r");
        }
    }

    public static class Staff extends ChannelCommand {
        public Staff(@NotNull ChatMessageListener messages) {
            super(messages, ChatChannel.STAFF, "sc");

            setCondition(staffPerm(Permission.GENERIC_STAFF));
        }
    }
}
