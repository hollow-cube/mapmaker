package net.hollowcube.ipc.chat;

import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.ipc.util.NatsMessage;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/// A message that passed every check, as every server receives it.
///
/// @param mapId  where the sender was, on [ChatChannel#LOCAL]. A recipient compares it with its own
///               world, which is exact on both ends — unlike the session presence both sides used
///               to look the sender up in. Null from a server old enough to only send its map
///               alongside `[map]`, which is why a `LOCAL` message without one is still forwarded.
/// @param seed   seeds the randomness in rendering, so a message that picks a random emoji picks the
///               same one everywhere
@RuntimeGson
@NatsMessage(subject = ChatMessage.SUBJECT)
public record ChatMessage(
    String senderId,
    ChatChannel channel,
    @Nullable String targetId,
    @Nullable String mapId,
    List<MessagePart> parts,
    long seed,
    boolean senderHasHypercube
) {
    public static final String SUBJECT = "chat.message";
}
