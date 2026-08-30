package net.hollowcube.mapmaker.chat;

import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.ipc.chat.ChatChannel;
import net.hollowcube.ipc.chat.ChatMessage;
import net.hollowcube.ipc.chat.MessagePart;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/// A message from a server still sending its chat the old way, off `CHAT_PROCESSED`.
///
/// Only what an unsigned message carries: the system messages that shared the shape were the api
/// answering a player, and the api answers its caller now. Every type here is the raw int Go wrote
/// rather than an enum read by ordinal, so that reading one does not depend on the order constants
/// happen to be declared in.
///
/// Delete this, and the consumer that decodes it, once no server that old is running.
@RuntimeGson
record LegacyChatMessage(
    int type,
    @Nullable String channel,
    @Nullable String sender,
    @Nullable List<Part> parts,
    long seed,
    boolean senderHasHypercube,
    /// Set by the api on the copy it publishes here of a message it already published on
    /// [ChatMessage#SUBJECT], which this server is also reading. Absent on a message from a server
    /// that really is too old to send it any other way.
    boolean mirrored
) {
    /// `model.ChatUnsigned`. The other one was a system message, which nothing sends here now.
    private static final int TYPE_UNSIGNED = 0;

    private static final int PART_RAW = 0;
    private static final int PART_EMOJI = 1;
    private static final int PART_MAP = 2;
    private static final int PART_URL = 3;

    @RuntimeGson
    record Part(int type, @Nullable String text, @Nullable String name, @Nullable String mapId) {
    }

    /// This message as everything else here reads one, or null if it is not one to deliver.
    ///
    /// No map comes with it — that server did not have one to send — so a local message falls back
    /// to the session service's view of where its sender is, which is what that server is doing too.
    @Nullable ChatMessage toChatMessage() {
        if (mirrored || type != TYPE_UNSIGNED || channel == null || sender == null) return null;

        var converted = new ArrayList<MessagePart>(parts == null ? 0 : parts.size());
        if (parts != null) {
            for (var part : parts) {
                // A part whose field is missing is dropped rather than rendered: the alternative is
                // the word `null` in someone's chat.
                var one = switch (part.type()) {
                    case PART_RAW -> part.text() == null ? null : new MessagePart.Raw(part.text());
                    case PART_EMOJI -> part.name() == null ? null : new MessagePart.Emoji(part.name());
                    case PART_MAP -> part.mapId() == null ? null : new MessagePart.Map(part.mapId());
                    case PART_URL -> part.text() == null ? null : new MessagePart.Url(part.text());
                    default -> null;
                };
                if (one != null) converted.add(one);
            }
        }

        var known = ChatChannels.of(channel);
        // Anything that is not one of the names is the target's uuid, which is how Go said "direct".
        var direct = !channel.equals(ChatChannels.GLOBAL) && !channel.equals(ChatChannels.LOCAL)
            && !channel.equals(ChatChannels.STAFF);
        return new ChatMessage(sender, direct ? ChatChannel.DIRECT : known, direct ? channel : null, null,
            List.copyOf(converted), seed, senderHasHypercube);
    }
}
