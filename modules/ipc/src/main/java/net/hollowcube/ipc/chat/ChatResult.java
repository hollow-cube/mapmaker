package net.hollowcube.ipc.chat;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

/// Why a message did not go out, or that it did.
///
/// The sending server renders these; the api never sends a message back to a player, which is what
/// the `CHAT_SYSTEM` message type used to be for.
///
/// A family rather than an enum with one nullable field beside it, because what a rejection needs
/// to say differs per rejection — a mute has an expiry, a direct message that bounced has a target,
/// most have nothing — and a shared bag of optional fields is a lie about all of them: every reader
/// has to know which fields its own case actually fills. A result this build does not know decodes
/// to [Unknown].
public sealed interface ChatResult permits ChatResult.Sent, ChatResult.Muted, ChatResult.Censored,
    ChatResult.TargetOffline, ChatResult.DmDisabled, ChatResult.MapNotPublished, ChatResult.Unknown {

    @RuntimeGson
    record Sent() implements ChatResult {
    }

    /// The sender has an active mute.
    ///
    /// @param expiresAt when it lifts, or null for a permanent one
    @RuntimeGson
    record Muted(@Nullable Instant expiresAt) implements ChatResult {
    }

    @RuntimeGson
    record Censored() implements ChatResult {
    }

    /// There is nobody to say it to.
    ///
    /// @param targetId the player who has no session anywhere, or null for a [ChatChannel#REPLY]
    ///                 from someone who has nobody to reply to — there is no name to name
    @RuntimeGson
    record TargetOffline(@Nullable String targetId) implements ChatResult {
    }

    /// Direct messages are turned off.
    ///
    /// @param targetId the player who turned them off, or null when that is the sender themselves —
    ///                 someone who does not take direct messages does not get to send them either
    @RuntimeGson
    record DmDisabled(@Nullable String targetId) implements ChatResult {
    }

    /// `[map]` written somewhere that is not a published map.
    @RuntimeGson
    record MapNotPublished() implements ChatResult {
    }

    /// A rejection from an api newer than this build. There is nothing to say about it beyond that
    /// the message did not go out.
    @RuntimeGson
    record Unknown(@Nullable String type) implements ChatResult {
    }
}
