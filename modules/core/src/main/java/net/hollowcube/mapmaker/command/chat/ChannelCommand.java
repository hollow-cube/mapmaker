package net.hollowcube.mapmaker.command.chat;

import net.hollowcube.mapmaker.chat.ChatMessageListener;
import net.hollowcube.mapmaker.command.CommandCategories;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.Permission;
import net.hollowcube.mapmaker.session.SessionManager;
import net.hollowcube.mapmaker.temp.ClientChatMessageData;

import static net.hollowcube.mapmaker.command.CoreCommandCondition.staffPerm;

public class ChannelCommand extends AbstractChatCommand {

    private ChannelCommand(
        SessionManager sessions, MapService maps, ChatMessageListener messages,
        String channel, String name, String... aliases
    ) {
        super(sessions, maps, messages, name, aliases);

        this.description = "Send a message to a specific chat channel.";
        this.category = CommandCategories.SOCIAL;

        var arg = CoreArgument.Message("message").description("The message content to send in the chat.");

        addSyntax(playerOnly((player, ctx) -> this.handle(player, channel, ctx.get(arg))), arg);
    }

    public static class Global extends ChannelCommand {
        public Global(SessionManager sessions, MapService maps, ChatMessageListener messages) {
            super(sessions, maps, messages, ClientChatMessageData.CHANNEL_GLOBAL, "gc");
        }
    }

    public static class Local extends ChannelCommand {
        public Local(SessionManager sessions, MapService maps, ChatMessageListener messages) {
            super(sessions, maps, messages, ClientChatMessageData.CHANNEL_LOCAL, "lc");
        }
    }

    public static class Reply extends ChannelCommand {
        public Reply(SessionManager sessions, MapService maps, ChatMessageListener messages) {
            super(sessions, maps, messages, ClientChatMessageData.CHANNEL_REPLY, "reply", "r");
        }
    }

    public static class Staff extends ChannelCommand {
        public Staff(SessionManager sessions, MapService maps, ChatMessageListener messages) {
            super(sessions, maps, messages, ClientChatMessageData.CHANNEL_STAFF, "sc");

            setCondition(staffPerm(Permission.GENERIC_STAFF));
        }
    }

}
