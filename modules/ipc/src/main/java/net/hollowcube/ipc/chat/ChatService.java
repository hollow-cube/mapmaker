package net.hollowcube.ipc.chat;

import net.hollowcube.ipc.util.Ipc;
import org.jetbrains.annotations.Nullable;

@Ipc
public interface ChatService {

    /// Everything that happens to a message between a player pressing enter and every server
    /// seeing it: strip, resolve the reply target, check direct messages are wanted, check the
    /// `[map]` is one, filter, log, tokenize and publish. Answers why it did not go out, or that
    /// it did.
    ///
    /// One call rather than a queue because the sender is waiting on it: the outcome is what the
    /// server tells them, and every rejection here used to be a message published back to them.
    ///
    /// @param targetId     who [ChatChannel#DIRECT] is for; ignored otherwise, and resolved here
    ///                     for [ChatChannel#REPLY]
    ChatResult send(String senderId, String serverId, ChatChannel channel, @Nullable String targetId,
                    String message, @Nullable String currentMapId);

    /// Stores what a player ran, for whoever later asks what someone was doing.
    ///
    /// Here rather than on a service of its own because a command is the other thing a player
    /// types, and it lands in the same place by the same route.
    void logCommand(CommandExecution execution);
}
